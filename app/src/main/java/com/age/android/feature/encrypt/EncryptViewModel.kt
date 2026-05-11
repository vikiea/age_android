package com.age.android.feature.encrypt

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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject

data class FileItem(val uri: Uri, val name: String)

data class EncryptUiState(
    val mode: EncryptMode = EncryptMode.BATCH_PACK,
    val files: List<FileItem> = emptyList(),
    val usePassphrase: Boolean = true,
    val passphrase: String = "",
    val selectedPublicKey: String = "",
    val outputFileName: String = "archive.tar.gz.age",
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
            _uiState.update {
                it.copy(
                    mode = when (savedMode) {
                        "SEPARATE" -> EncryptMode.SEPARATE
                        else -> EncryptMode.BATCH_PACK
                    },
                    usePassphrase = savedUsePassphrase,
                    selectedPublicKey = savedPublicKey
                )
            }
        }
    }

    fun setMode(mode: EncryptMode) {
        _uiState.update { it.copy(mode = mode) }
        viewModelScope.launch { settingsDataStore.setEncryptMode(mode.name) }
    }

    fun addFiles(uris: List<Uri>) {
        val newFiles = uris.map { uri -> FileItem(uri, fileHelper.getFileName(uri)) }
        _uiState.update { it.copy(files = it.files + newFiles) }
    }

    fun removeFile(index: Int) {
        _uiState.update { it.copy(files = it.files.toMutableList().apply { removeAt(index) }) }
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

    fun setOutputFileName(value: String) {
        _uiState.update { it.copy(outputFileName = value) }
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
            val subDirUri = fileHelper.getSubDirUri(Uri.parse(customUriStr), "encrypted") ?: return
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
            _uiState.update { it.copy(isProcessing = true, error = null, result = null, progress = 0f, totalCount = state.files.size, outputFiles = emptyList(), outputDir = null) }

            val record = OperationRecord(
                type = OperationType.ENCRYPT,
                mode = state.mode,
                inputFiles = state.files.map { it.name },
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
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS))
                val outDir = getOutputDir()
                _uiState.update { it.copy(isProcessing = false, result = "加密完成！共 ${outputNames.size} 个文件", progress = 1f, outputFiles = outputNames, outputDir = outDir.absolutePath) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "加密失败: ${e.message}") }
            }
        }
    }

    private suspend fun getStrategy(): DuplicateStrategy = settingsDataStore.getDuplicateStrategyOnce()

    private suspend fun batchEncrypt(state: EncryptUiState): List<String> {
        val totalFiles = state.files.size

        // Phase 1: Read files
        _uiState.update { it.copy(progress = 0f, processedCount = 0) }
        yield()
        val entries = state.files.mapIndexed { index, file ->
            val data = fileHelper.readUri(file.uri) ?: throw Exception("无法读取: ${file.name}")
            _uiState.update { it.copy(progress = (index + 1).toFloat() / totalFiles * 0.3f) }
            yield()
            com.age.android.core.util.TarEntry(file.name, data)
        }

        // Phase 2: Streaming compress to temp file
        _uiState.update { it.copy(progress = 0.3f) }
        yield()
        val tempFile = fileHelper.tarGzipToTemp(entries)
        _uiState.update { it.copy(progress = 0.5f) }
        yield()

        try {
            // Phase 3: Read temp file and encrypt
            val compressed = fileHelper.readTempFile(tempFile)
            val encrypted = if (state.usePassphrase) {
                ageEngine.encryptWithPassphrase(compressed, state.passphrase)
            } else {
                ageEngine.encryptWithPublicKey(compressed, state.selectedPublicKey)
            }
            _uiState.update { it.copy(progress = 0.8f) }
            yield()

            // Phase 4: Write output
            val outputName = writeOutput(state.outputFileName, encrypted)
            _uiState.update { it.copy(progress = 1f, processedCount = 1, totalCount = 1, successCount = 1) }
            return listOf(outputName)
        } finally {
            fileHelper.deleteTempFile(tempFile)
        }
    }

    private suspend fun separateEncrypt(state: EncryptUiState): List<String> {
        val semaphore = Semaphore(4)
        var success = 0
        var fail = 0
        val outputNames = mutableListOf<String>()

        val jobs = state.files.mapIndexed { index, file ->
            viewModelScope.async {
                semaphore.withPermit {
                    try {
                        val data = fileHelper.readUri(file.uri) ?: throw Exception("无法读取: ${file.name}")
                        val encrypted = if (state.usePassphrase) {
                            ageEngine.encryptWithPassphrase(data, state.passphrase)
                        } else {
                            ageEngine.encryptWithPublicKey(data, state.selectedPublicKey)
                        }
                        val baseName = file.name.substringBeforeLast(".")
                        val outName = "${baseName}.age"
                        writeOutput(outName, encrypted)
                        synchronized(this) {
                            success++
                            outputNames.add(outName)
                            _uiState.update { it.copy(processedCount = success + fail, successCount = success, progress = (success + fail).toFloat() / state.files.size) }
                        }
                    } catch (e: Exception) {
                        synchronized(this) {
                            fail++
                            _uiState.update { it.copy(processedCount = success + fail, failCount = fail, progress = (success + fail).toFloat() / state.files.size) }
                        }
                    }
                }
            }
        }
        jobs.awaitAll()
        return outputNames
    }

    /**
     * Write output file: use SAF API for custom directory, direct file write for fallback.
     * Returns the actual output file name.
     */
    private suspend fun writeOutput(fileName: String, data: ByteArray): String {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = Uri.parse(customUriStr)
            val subDirUri = fileHelper.getSubDirUri(customUri, "encrypted")
                ?: throw Exception("无法创建输出子目录")
            val strategy = getStrategy()
            val actualName = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueSafFileName(subDirUri, fileName) else fileName
            if (!fileHelper.writeToSafFile(subDirUri, actualName, data)) {
                throw Exception("写入文件失败")
            }
            return actualName
        } else {
            val strategy = getStrategy()
            val outDir = getOutputDir()
            val outFile = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueFile(outDir, fileName) else File(outDir, fileName)
            ageEngine.writeFile(outFile.absolutePath, data)
            return outFile.name
        }
    }
}
