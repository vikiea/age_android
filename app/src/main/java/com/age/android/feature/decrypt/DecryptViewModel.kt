/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.decrypt

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.age.AgeEngine
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.KeyRepository
import com.age.android.core.data.OperationRepository
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.file.FileSelectionKind
import com.age.android.core.file.SelectedFileItem
import com.age.android.core.file.SelectedFileLeaf
import com.age.android.core.model.*
import com.age.android.core.util.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

data class DecryptFileItem(
    val uri: Uri,
    val name: String,
    val relativePath: String,
    val kind: FileSelectionKind,
    val files: List<SelectedFileLeaf>
)

data class DecryptUiState(
    val files: List<DecryptFileItem> = emptyList(),
    val usePassphrase: Boolean = true,
    val passphrase: String = "",
    val selectedPrivateKey: String = "",
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val processedCount: Int = 0,
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failCount: Int = 0,
    val error: String? = null,
    val result: String? = null,
    val outputFiles: List<String> = emptyList(),
    val outputDir: String? = null
)

@HiltViewModel
class DecryptViewModel @Inject constructor(
    private val ageEngine: AgeEngine,
    private val keyRepository: KeyRepository,
    private val operationRepository: OperationRepository,
    private val fileHelper: FileHelper,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecryptUiState())
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val customOutputDir: StateFlow<String?> = settingsDataStore.outputDirUri
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val savedUsePassphrase = settingsDataStore.getDecryptUsePassphraseOnce()
            val savedPrivateKey = settingsDataStore.getSelectedPrivateKeyOnce()
            _uiState.update { it.copy(usePassphrase = savedUsePassphrase, selectedPrivateKey = savedPrivateKey) }
        }
    }

    fun addFiles(uris: List<Uri>) {
        val existingUris = _uiState.value.files.map { it.uri }.toSet()
        val newFiles = uris.filter { it !in existingUris }.map { uri ->
            val name = normalizeRelativePath(fileHelper.getFileName(uri))
            DecryptFileItem(
                uri = uri,
                name = name,
                relativePath = name,
                kind = FileSelectionKind.FILE,
                files = listOf(SelectedFileLeaf(uri = uri, name = name, relativePath = name))
            )
        }
        if (newFiles.isEmpty()) return
        _uiState.update { it.copy(files = it.files + newFiles) }
    }

    fun addFilesFromFolder(dirUri: Uri) {
        val existingUris = _uiState.value.files.map { it.uri }.toSet()
        val newFiles = fileHelper.listSelectableItemsInDir(dirUri)
            .filter { it.uri !in existingUris }
            .map { it.toDecryptFileItem() }
        _uiState.update { it.copy(files = it.files + newFiles) }
    }

    fun removeFile(index: Int) {
        _uiState.update { it.copy(files = it.files.toMutableList().apply { removeAt(index) }) }
    }

    fun clearFiles() {
        _uiState.update { it.copy(files = emptyList()) }
    }

    fun setUsePassphrase(value: Boolean) {
        _uiState.update { it.copy(usePassphrase = value) }
        viewModelScope.launch { settingsDataStore.setDecryptUsePassphrase(value) }
    }

    fun setPassphrase(value: String) {
        _uiState.update { it.copy(passphrase = value) }
    }

    fun setSelectedPrivateKey(value: String) {
        _uiState.update { it.copy(selectedPrivateKey = value) }
        viewModelScope.launch { settingsDataStore.setSelectedPrivateKey(value) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearResult() {
        fileHelper.cleanShareCache()
        _uiState.update { it.copy(result = null, outputFiles = emptyList(), outputDir = null) }
    }

    fun shareOutput() {
        val state = _uiState.value
        if (state.outputFiles.isEmpty()) return

        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            // Custom dir: read via SAF API, skip cache cleanup to preserve SAF-cached files
            val rootUri = customUriStr.toUri()
            val files = state.outputFiles.mapNotNull { fileHelper.readSafFileToCacheRelative(rootUri, "decrypted", it) }
            if (files.isNotEmpty()) fileHelper.shareFiles(files)
        } else {
            // Fallback dir: direct file access
            val dir = getOutputDir()
            val files = state.outputFiles.map { File(dir, it) }.filter { it.exists() }
            if (files.isNotEmpty()) fileHelper.shareFiles(files)
        }
    }

    private fun getOutputDir(): File {
        val customUri = customOutputDir.value
        if (customUri != null) {
            val basePath = fileHelper.resolveUriToPath(customUri)
            if (basePath != null) {
                val dir = File(basePath, "decrypted")
                if (dir.exists() || dir.mkdirs()) return dir
            }
        }
        return fileHelper.getFallbackDecryptedDir()
    }

    fun startDecrypt() {
        val state = _uiState.value
        if (state.files.isEmpty()) {
            _uiState.update { it.copy(error = "请先选择文件") }
            return
        }
        if (state.usePassphrase && state.passphrase.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }
        if (!state.usePassphrase && state.selectedPrivateKey.isBlank()) {
            _uiState.update { it.copy(error = "请选择或输入私钥") }
            return
        }

        viewModelScope.launch {
            val inputLeaves = state.inputLeaves()
            _uiState.update { it.copy(isProcessing = true, error = null, result = null, progress = 0f, processedCount = 0, totalCount = inputLeaves.size, successCount = 0, failCount = 0, outputFiles = emptyList(), outputDir = null) }

            val outDirPath = getOutputDir().absolutePath
            val record = OperationRecord(
                type = OperationType.DECRYPT,
                mode = EncryptMode.DECRYPT,
                inputFiles = state.files.map { it.displayPath() },
                outputPath = outDirPath,
                recipientInfo = if (state.usePassphrase) "密码解密" else "私钥解密",
                status = OperationStatus.RUNNING
            )
            val recordId = operationRepository.insertOperation(record)

            try {
                val concurrency = settingsDataStore.getConcurrencyOnce()
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)
                val processedCount = AtomicInteger(0)
                val outputNames = java.util.concurrent.ConcurrentLinkedQueue<String>()
                val strategy = settingsDataStore.getDuplicateStrategyOnce()
                val files = state.inputLeaves()
                val totalFiles = files.size

                coroutineScope {
                    val semaphore = Semaphore(concurrency)
                    files.mapIndexed { index, file ->
                        async {
                            semaphore.withPermit {
                                var sourceTemp: java.io.File? = null
                                val decryptedTemp = java.io.File(fileHelper.getCacheDir(), "dec_out_${System.currentTimeMillis()}_${index}.tmp")
                                try {
                                    sourceTemp = fileHelper.streamUriToTemp(file.uri, "dec_src")

                                    if (state.usePassphrase) {
                                        ageEngine.decryptStreamToFile(sourceTemp.absolutePath, decryptedTemp.absolutePath, state.passphrase)
                                    } else {
                                        ageEngine.decryptStreamToFileWithKey(sourceTemp.absolutePath, decryptedTemp.absolutePath, state.selectedPrivateKey)
                                    }
                                    fileHelper.deleteTempFile(sourceTemp)
                                    sourceTemp = null

                                    var isTar = false
                                    try {
                                        fileHelper.untarStreaming(decryptedTemp) { entryName, entryStream, entrySize ->
                                            val actualName = writeStreamToFile(entryName, entryStream, entrySize, strategy)
                                            outputNames.add(actualName)
                                        }
                                        isTar = true
                                        successCount.incrementAndGet()
                                    } catch (_: Exception) {}

                                    if (!isTar) {
                                        try {
                                            fileHelper.untarGzipStreaming(decryptedTemp) { entryName, entryStream, entrySize ->
                                                val actualName = writeStreamToFile(entryName, entryStream, entrySize, strategy)
                                                outputNames.add(actualName)
                                            }
                                            isTar = true
                                            successCount.incrementAndGet()
                                        } catch (_: Exception) {}
                                    }

                                    if (!isTar) {
                                        val outName = decryptedRelativePath(file.relativePath)
                                        val decompressedTemp = try {
                                            fileHelper.gunzipToTemp(decryptedTemp)
                                        } catch (_: Exception) {
                                            decryptedTemp
                                        }
                                        val actualName = writeOutputFromTemp(outName, decompressedTemp, strategy)
                                        if (decompressedTemp !== decryptedTemp) fileHelper.deleteTempFile(decompressedTemp)
                                        successCount.incrementAndGet()
                                        outputNames.add(actualName)
                                    }
                                } catch (_: Exception) {
                                    failCount.incrementAndGet()
                                } finally {
                                    sourceTemp?.let { fileHelper.deleteTempFile(it) }
                                    fileHelper.deleteTempFile(decryptedTemp)
                                }
                                val done = processedCount.incrementAndGet()
                                _uiState.update {
                                    it.copy(
                                        processedCount = done,
                                        successCount = successCount.get(),
                                        failCount = failCount.get(),
                                        progress = done.toFloat() / totalFiles
                                    )
                                }
                                yield()
                            }
                        }
                    }.awaitAll()
                }

                val success = successCount.get()
                val fail = failCount.get()
                val outDir = getOutputDir()
                val outputFiles = outputNames.toList()
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS, outputPath = outDir.absolutePath, outputFiles = outputFiles))
                _uiState.update { it.copy(isProcessing = false, result = "解密完成！成功: $success, 失败: $fail", progress = 1f, outputFiles = outputFiles, outputDir = outDir.absolutePath) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "解密失败: ${e.message}") }
            }
        }
    }

    private fun writeStreamToFile(name: String, input: java.io.InputStream, size: Long, strategy: DuplicateStrategy): String {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = customUriStr.toUri()
            return fileHelper.writeStreamToSafRelative(customUri, "decrypted", name, input, strategy)
                ?: throw Exception("写入文件失败")
        } else {
            val outDir = getOutputDir()
            return fileHelper.writeStreamToDirRelative(outDir, name, input, strategy)
        }
    }

    private fun writeOutputFromTemp(name: String, srcFile: java.io.File, strategy: DuplicateStrategy): String {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = customUriStr.toUri()
            return fileHelper.copyFileToSafRelative(customUri, "decrypted", name, srcFile, strategy)
                ?: throw Exception("写入文件失败")
        } else {
            val outDir = getOutputDir()
            return fileHelper.copyFileToDirRelative(outDir, name, srcFile, strategy)
        }
    }
}

private fun SelectedFileItem.toDecryptFileItem(): DecryptFileItem =
    DecryptFileItem(
        uri = uri,
        name = name,
        relativePath = relativePath,
        kind = kind,
        files = files
    )

private fun DecryptUiState.inputLeaves(): List<SelectedFileLeaf> =
    files.flatMap { it.files }

private fun DecryptFileItem.displayPath(): String =
    if (kind == FileSelectionKind.FOLDER) "$relativePath/" else relativePath

private fun decryptedRelativePath(relativePath: String): String =
    normalizeRelativePath(relativePath).removeSuffix(".age").ifEmpty { "decrypted_output" }
