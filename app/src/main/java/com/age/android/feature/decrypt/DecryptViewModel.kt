/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.decrypt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.age.AgeEngine
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.KeyRepository
import com.age.android.core.data.OperationRepository
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.model.*
import com.age.android.core.util.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject

data class DecryptFileItem(val uri: Uri, val name: String)

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
        val newFiles = uris.filter { it !in existingUris }.map { uri -> DecryptFileItem(uri, fileHelper.getFileName(uri)) }
        if (newFiles.isEmpty()) return
        _uiState.update { it.copy(files = it.files + newFiles) }
    }

    fun addFilesFromFolder(dirUri: Uri) {
        val dirFiles = fileHelper.listFilesInDir(dirUri)
        val existingUris = _uiState.value.files.map { it.uri }.toSet()
        val newFiles = dirFiles.filter { it.uri !in existingUris }.map { DecryptFileItem(it.uri, it.name) }
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
            val subDirUri = fileHelper.getSubDirUri(Uri.parse(customUriStr), "decrypted") ?: return
            val files = state.outputFiles.mapNotNull { fileHelper.readSafFileToCache(subDirUri, it) }
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
            _uiState.update { it.copy(isProcessing = true, error = null, result = null, progress = 0f, processedCount = 0, totalCount = state.files.size, successCount = 0, failCount = 0, outputFiles = emptyList(), outputDir = null) }

            val record = OperationRecord(
                type = OperationType.DECRYPT,
                mode = EncryptMode.DECRYPT,
                inputFiles = state.files.map { it.name },
                outputPath = fileHelper.getOutputDir().absolutePath,
                recipientInfo = if (state.usePassphrase) "密码解密" else "私钥解密",
                status = OperationStatus.RUNNING
            )
            val recordId = operationRepository.insertOperation(record)

            try {
                var success = 0
                var fail = 0
                val outputNames = mutableListOf<String>()
                val strategy = settingsDataStore.getDuplicateStrategyOnce()

                for ((index, file) in state.files.withIndex()) {
                    var sourceTemp: java.io.File? = null
                    val decryptedTemp = java.io.File(fileHelper.getCacheDir(), "dec_out_${System.currentTimeMillis()}.tmp")
                    try {
                        // Stream URI to temp file (no memory load)
                        sourceTemp = fileHelper.streamUriToTemp(file.uri, "dec_src")

                        // Stream decrypt
                        if (state.usePassphrase) {
                            ageEngine.decryptStreamToFile(sourceTemp.absolutePath, decryptedTemp.absolutePath, state.passphrase)
                        } else {
                            ageEngine.decryptStreamToFileWithKey(sourceTemp.absolutePath, decryptedTemp.absolutePath, state.selectedPrivateKey)
                        }
                        sourceTemp?.let { fileHelper.deleteTempFile(it) }

                        // Try streaming untar (tar-only format, no gzip)
                        var isTar = false
                        try {
                            fileHelper.untarStreaming(decryptedTemp) { entryName, entryStream, entrySize ->
                                writeStreamToFile(entryName, entryStream, entrySize, strategy)
                                outputNames.add(entryName)
                            }
                            isTar = true
                            success++
                        } catch (_: Exception) {
                            // Not a plain tar, try tar.gz
                        }

                        if (!isTar) {
                            try {
                                fileHelper.untarGzipStreaming(decryptedTemp) { entryName, entryStream, entrySize ->
                                    writeStreamToFile(entryName, entryStream, entrySize, strategy)
                                    outputNames.add(entryName)
                                }
                                isTar = true
                                success++
                            } catch (_: Exception) {
                                // Not tar.gz either
                            }
                        }

                        if (!isTar) {
                            // Raw single file (legacy format)
                            val outName = file.name.removeSuffix(".age").ifEmpty { "decrypted_output" }
                            val decompressedTemp = try {
                                fileHelper.gunzipToTemp(decryptedTemp)
                            } catch (_: Exception) {
                                decryptedTemp
                            }
                            writeOutputFromTemp(outName, decompressedTemp, strategy)
                            if (decompressedTemp !== decryptedTemp) fileHelper.deleteTempFile(decompressedTemp)
                            success++
                            outputNames.add(outName)
                        }
                    } catch (e: Exception) {
                        fail++
                    } finally {
                        sourceTemp?.let { fileHelper.deleteTempFile(it) }
                        fileHelper.deleteTempFile(decryptedTemp)
                    }
                    _uiState.update { it.copy(processedCount = success + fail, successCount = success, failCount = fail, progress = (index + 1).toFloat() / state.files.size) }
                    yield()
                }

                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS))
                val outDir = getOutputDir()
                _uiState.update { it.copy(isProcessing = false, result = "解密完成！成功: $success, 失败: $fail", progress = 1f, outputFiles = outputNames, outputDir = outDir.absolutePath) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "解密失败: ${e.message}") }
            }
        }
    }

    private fun writeStreamToFile(name: String, input: java.io.InputStream, size: Long, strategy: DuplicateStrategy) {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = Uri.parse(customUriStr)
            val subDirUri = fileHelper.getSubDirUri(customUri, "decrypted")
                ?: throw Exception("无法创建输出子目录")
            val actualName = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueSafFileName(subDirUri, name) else name
            if (!fileHelper.writeStreamToSafFile(subDirUri, actualName, input)) {
                throw Exception("写入文件失败")
            }
        } else {
            val outDir = getOutputDir()
            val outFile = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueFile(outDir, name) else File(outDir, name)
            outFile.outputStream().use { output -> input.copyTo(output, bufferSize = 8192) }
        }
    }

    private fun writeOutputFromTemp(name: String, srcFile: java.io.File, strategy: DuplicateStrategy) {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = Uri.parse(customUriStr)
            val subDirUri = fileHelper.getSubDirUri(customUri, "decrypted")
                ?: throw Exception("无法创建输出子目录")
            val actualName = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueSafFileName(subDirUri, name) else name
            if (!fileHelper.copyFileToSaf(subDirUri, actualName, srcFile)) {
                throw Exception("写入文件失败")
            }
        } else {
            val outDir = getOutputDir()
            val outFile = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueFile(outDir, name) else File(outDir, name)
            srcFile.copyTo(outFile, overwrite = true)
        }
    }
}
