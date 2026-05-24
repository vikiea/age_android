/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.encrypt

import android.net.Uri
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

data class FileItem(
    val uri: Uri,
    val name: String,
    val relativePath: String,
    val kind: FileSelectionKind,
    val files: List<SelectedFileLeaf>
)

data class EncryptUiState(
    val mode: EncryptMode = EncryptMode.BATCH_PACK,
    val files: List<FileItem> = emptyList(),
    val usePassphrase: Boolean = true,
    val passphrase: String = "",
    val selectedPublicKey: String = "",
    val outputFileBaseName: String = "archive",
    val outputFileName: String = "archive.tar.gz.age",
    val compressEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val phase: String = "",
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
class EncryptViewModel @Inject constructor(
    private val ageEngine: AgeEngine,
    private val keyRepository: KeyRepository,
    private val operationRepository: OperationRepository,
    private val fileHelper: FileHelper,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncryptUiState())
    val uiState: StateFlow<EncryptUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val customOutputDir: StateFlow<String?> = settingsDataStore.outputDirUri
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        viewModelScope.launch {
            val savedMode = settingsDataStore.getEncryptModeOnce()
            val savedUsePassphrase = settingsDataStore.getEncryptUsePassphraseOnce()
            val savedPublicKey = settingsDataStore.getSelectedPublicKeyOnce()
            val savedCompress = settingsDataStore.getCompressEnabledOnce()
            _uiState.update {
                val baseName = "archive"
                it.copy(
                    mode = when (savedMode) {
                        "SEPARATE" -> EncryptMode.SEPARATE
                        else -> EncryptMode.BATCH_PACK
                    },
                    usePassphrase = savedUsePassphrase,
                    selectedPublicKey = savedPublicKey,
                    compressEnabled = savedCompress,
                    outputFileBaseName = baseName,
                    outputFileName = computeOutputFileName(baseName, savedCompress)
                )
            }
        }
        // Auto-sync compress setting changes from Settings page
        viewModelScope.launch {
            settingsDataStore.compressEnabled.collect { compress ->
                _uiState.update {
                    if (it.compressEnabled == compress) it
                    else it.copy(
                        compressEnabled = compress,
                        outputFileName = computeOutputFileName(it.outputFileBaseName, compress)
                    )
                }
            }
        }
    }

    fun setMode(mode: EncryptMode) {
        _uiState.update { it.copy(mode = mode) }
        viewModelScope.launch { settingsDataStore.setEncryptMode(mode.name) }
    }

    fun setCompressEnabled(value: Boolean) {
        _uiState.update {
            it.copy(
                compressEnabled = value,
                outputFileName = computeOutputFileName(it.outputFileBaseName, value)
            )
        }
        viewModelScope.launch { settingsDataStore.setCompressEnabled(value) }
    }

    fun setOutputFileBaseName(value: String) {
        _uiState.update {
            it.copy(
                outputFileBaseName = value,
                outputFileName = computeOutputFileName(value, it.compressEnabled)
            )
        }
    }

    private fun computeOutputFileName(baseName: String, compress: Boolean): String {
        val name = baseName.ifBlank { "archive" }
        return if (compress) "$name.tar.gz.age" else "$name.tar.age"
    }

    fun addFiles(uris: List<Uri>) {
        val existingUris = _uiState.value.files.map { it.uri }.toSet()
        val newFiles = uris.filter { it !in existingUris }.map { uri ->
            val name = normalizeRelativePath(fileHelper.getFileName(uri))
            FileItem(
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
            .map { it.toFileItem() }
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
        viewModelScope.launch { settingsDataStore.setEncryptUsePassphrase(value) }
    }

    fun setPassphrase(value: String) {
        _uiState.update { it.copy(passphrase = value) }
    }

    fun setSelectedPublicKey(value: String) {
        _uiState.update { it.copy(selectedPublicKey = value) }
        viewModelScope.launch { settingsDataStore.setSelectedPublicKey(value) }
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
            val rootUri = Uri.parse(customUriStr)
            val files = state.outputFiles.mapNotNull { fileHelper.readSafFileToCacheRelative(rootUri, "encrypted", it) }
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
                val dir = File(basePath, "encrypted")
                if (dir.exists() || dir.mkdirs()) return dir
            }
        }
        return fileHelper.getFallbackEncryptedDir()
    }

    fun startEncrypt() {
        val state = _uiState.value
        if (state.files.isEmpty()) {
            _uiState.update { it.copy(error = "请先选择文件") }
            return
        }
        if (state.usePassphrase && state.passphrase.isBlank()) {
            _uiState.update { it.copy(error = "请输入密码") }
            return
        }
        if (!state.usePassphrase && state.selectedPublicKey.isBlank()) {
            _uiState.update { it.copy(error = "请选择或输入公钥") }
            return
        }

        viewModelScope.launch {
            val inputLeaves = state.inputLeaves()
            _uiState.update { it.copy(isProcessing = true, error = null, result = null, progress = 0f, phase = "", processedCount = 0, totalCount = inputLeaves.size, successCount = 0, failCount = 0, outputFiles = emptyList(), outputDir = null) }

            val record = OperationRecord(
                type = OperationType.ENCRYPT,
                mode = state.mode,
                inputFiles = state.files.map { it.displayPath() },
                outputPath = state.outputFileName,
                recipientInfo = if (state.usePassphrase) "密码加密" else state.selectedPublicKey.take(20) + "...",
                status = OperationStatus.RUNNING
            )
            val recordId = operationRepository.insertOperation(record)

            try {
                val outputNames = mutableListOf<String>()
                when (state.mode) {
                    EncryptMode.BATCH_PACK -> outputNames.addAll(batchEncrypt(state))
                    EncryptMode.SEPARATE -> outputNames.addAll(separateEncrypt(state))
                    else -> throw Exception("无效的加密模式")
                }
                val outDir = getOutputDir()
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS, outputPath = outDir.absolutePath, outputFiles = outputNames))
                _uiState.update { it.copy(isProcessing = false, result = "加密完成！共 ${outputNames.size} 个文件", progress = 1f, outputFiles = outputNames, outputDir = outDir.absolutePath) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "加密失败: ${e.message}") }
            }
        }
    }

    private suspend fun getStrategy(): DuplicateStrategy = settingsDataStore.getDuplicateStrategyOnce()

    private suspend fun batchEncrypt(state: EncryptUiState): List<String> {
        val files = state.inputLeaves()
        val totalFiles = files.size
        val concurrency = settingsDataStore.getConcurrencyOnce()

        // Phase 1: Concurrent stream URIs to temp files
        val phaseText = if (state.compressEnabled) "压缩中" else "打包中"
        _uiState.update { it.copy(progress = 0f, processedCount = 0, totalCount = totalFiles, phase = phaseText) }
        yield()

        val tempFiles = arrayOfNulls<File>(totalFiles)
        try {
            coroutineScope {
                val semaphore = Semaphore(concurrency)
                val doneCount = AtomicInteger(0)
                files.mapIndexed { index, file ->
                    async {
                        semaphore.withPermit {
                            val temp = fileHelper.streamUriToTemp(file.uri, "src_$index")
                            tempFiles[index] = temp
                            val done = doneCount.incrementAndGet()
                            _uiState.update { it.copy(processedCount = done, progress = done * 0.3f / totalFiles) }
                            yield()
                        }
                    }
                }.awaitAll()
            }

            // Tar/tar.gz via Go engine (true streaming, ~32KB memory for gzip)
            val tarFile = File(fileHelper.getCacheDir(), "batch_${System.currentTimeMillis()}.tar${if (state.compressEnabled) ".gz" else ""}")
            val pathsDelim = tempFiles.filterNotNull().joinToString("\n") { it.absolutePath }
            val namesDelim = files.joinToString("\n") { it.relativePath }
            if (state.compressEnabled) {
                ageEngine.tarGzipFilesDelim(pathsDelim, namesDelim, tarFile.absolutePath)
            } else {
                ageEngine.tarFilesDelim(pathsDelim, namesDelim, tarFile.absolutePath)
            }

            _uiState.update { it.copy(progress = 0.4f, phase = "压缩完成", processedCount = 0, totalCount = 1) }
            yield()

            // Phase 2: Streaming encrypt (40% → 90%)
            val encryptedTemp = File(fileHelper.getCacheDir(), "enc_${System.currentTimeMillis()}.tmp")
            try {
                _uiState.update { it.copy(phase = "加密中") }
                if (state.usePassphrase) {
                    ageEngine.encryptStreamToFile(tarFile.absolutePath, encryptedTemp.absolutePath, state.passphrase)
                } else {
                    ageEngine.encryptStreamToFileWithKey(tarFile.absolutePath, encryptedTemp.absolutePath, state.selectedPublicKey)
                }
                _uiState.update { it.copy(progress = 0.9f) }
                yield()

                // Phase 3: Write output (90% → 100%)
                _uiState.update { it.copy(phase = "写入中") }
                val outputName = writeOutputFile(state.outputFileName, encryptedTemp)
                _uiState.update { it.copy(progress = 1f, processedCount = 1, totalCount = 1, successCount = 1, phase = "完成") }
                return listOf(outputName)
            } finally {
                fileHelper.deleteTempFile(tarFile)
                fileHelper.deleteTempFile(encryptedTemp)
            }
        } finally {
            tempFiles.filterNotNull().forEach { fileHelper.deleteTempFile(it) }
        }
    }

    private suspend fun separateEncrypt(state: EncryptUiState): List<String> {
        val concurrency = settingsDataStore.getConcurrencyOnce()
        val successCount = AtomicInteger(0)
        val failCount = AtomicInteger(0)
        val processedCount = AtomicInteger(0)
        val outputNames = java.util.concurrent.ConcurrentLinkedQueue<String>()
        val units = state.files

        coroutineScope {
            val semaphore = Semaphore(concurrency)
            val totalFiles = units.sumOf { it.files.size }
            units.mapIndexed { index, item ->
                async {
                    semaphore.withPermit {
                        try {
                            val actualName = encryptSeparateUnit(state, item, index)
                            successCount.incrementAndGet()
                            outputNames.add(actualName)
                        } catch (_: Exception) {
                            failCount.incrementAndGet()
                        }
                        val done = processedCount.addAndGet(item.files.size)
                        _uiState.update {
                            it.copy(
                                processedCount = done,
                                successCount = successCount.get(),
                                failCount = failCount.get(),
                                progress = if (totalFiles == 0) 1f else done.toFloat() / totalFiles
                            )
                        }
                        yield()
                    }
                }
            }.awaitAll()
        }
        return outputNames.toList()
    }

    private suspend fun encryptSeparateUnit(state: EncryptUiState, item: FileItem, index: Int): String {
        val sourceTemps = mutableListOf<File>()
        var tarTemp: File? = null
        var encryptedTemp: File? = null
        try {
            item.files.forEach { leaf ->
                sourceTemps.add(fileHelper.streamUriToTemp(leaf.uri, "src"))
            }

            val tarPath = File(fileHelper.getCacheDir(), "single_${System.currentTimeMillis()}_${index}.tar")
            if (item.kind == FileSelectionKind.FOLDER) {
                val pathsDelim = sourceTemps.joinToString("\n") { it.absolutePath }
                val namesDelim = item.files.joinToString("\n") { it.relativePath }
                ageEngine.tarFilesDelim(pathsDelim, namesDelim, tarPath.absolutePath)
            } else {
                val leaf = item.files.firstOrNull() ?: throw Exception("空文件")
                ageEngine.tarSingleFile(sourceTemps.first().absolutePath, leaf.name, tarPath.absolutePath)
            }
            tarTemp = tarPath
            sourceTemps.forEach { fileHelper.deleteTempFile(it) }
            sourceTemps.clear()

            encryptedTemp = File(fileHelper.getCacheDir(), "enc_${System.currentTimeMillis()}_${index}.tmp")
            if (state.usePassphrase) {
                ageEngine.encryptStreamToFile(tarTemp.absolutePath, encryptedTemp.absolutePath, state.passphrase)
            } else {
                ageEngine.encryptStreamToFileWithKey(tarTemp.absolutePath, encryptedTemp.absolutePath, state.selectedPublicKey)
            }

            val outName = encryptedUnitRelativePath(item)
            return writeOutputFile(outName, encryptedTemp)
        } finally {
            sourceTemps.forEach { fileHelper.deleteTempFile(it) }
            tarTemp?.let { fileHelper.deleteTempFile(it) }
            encryptedTemp?.let { fileHelper.deleteTempFile(it) }
        }
    }

    /**
     * Write output file from a source temp file.
     * Uses SAF API for custom directory, direct file move for fallback.
     * Returns the actual output file name.
     */
    private suspend fun writeOutputFile(fileName: String, srcFile: File): String {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = Uri.parse(customUriStr)
            val strategy = getStrategy()
            return fileHelper.copyFileToSafRelative(customUri, "encrypted", fileName, srcFile, strategy)
                ?: throw Exception("写入文件失败")
        } else {
            val strategy = getStrategy()
            val outDir = getOutputDir()
            return fileHelper.copyFileToDirRelative(outDir, fileName, srcFile, strategy)
        }
    }
}

private fun SelectedFileItem.toFileItem(): FileItem =
    FileItem(
        uri = uri,
        name = name,
        relativePath = relativePath,
        kind = kind,
        files = files
    )

private fun EncryptUiState.inputLeaves(): List<SelectedFileLeaf> =
    files.flatMap { it.files }

private fun FileItem.displayPath(): String =
    if (kind == FileSelectionKind.FOLDER) "$relativePath/" else relativePath

private fun encryptedRelativePath(relativePath: String): String {
    val safePath = normalizeRelativePath(relativePath)
    val parent = safePath.substringBeforeLast("/", "")
    val fileName = safePath.substringAfterLast("/")
    val baseName = fileName.substringBeforeLast(".", fileName).ifBlank { fileName }
    val encryptedName = "$baseName.tar.age"
    return if (parent.isBlank()) encryptedName else "$parent/$encryptedName"
}

private fun encryptedUnitRelativePath(item: FileItem): String =
    if (item.kind == FileSelectionKind.FOLDER) "${normalizeRelativePath(item.relativePath)}.tar.age"
    else encryptedRelativePath(item.files.firstOrNull()?.relativePath ?: item.relativePath)
