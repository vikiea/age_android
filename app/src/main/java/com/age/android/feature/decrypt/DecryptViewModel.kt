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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
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
        val newFiles = uris.map { uri -> DecryptFileItem(uri, fileHelper.getFileName(uri)) }
        _uiState.update { it.copy(files = it.files + newFiles) }
    }

    fun removeFile(index: Int) {
        _uiState.update { it.copy(files = it.files.toMutableList().apply { removeAt(index) }) }
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
            _uiState.update { it.copy(isProcessing = true, error = null, result = null, progress = 0f, totalCount = state.files.size, outputFiles = emptyList(), outputDir = null) }

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
                val semaphore = Semaphore(4)
                var success = 0
                var fail = 0
                val outputNames = mutableListOf<String>()
                val strategy = settingsDataStore.getDuplicateStrategyOnce()

                val jobs = state.files.map { file ->
                    viewModelScope.async {
                        semaphore.withPermit {
                            var tempFile: java.io.File? = null
                            try {
                                val data = fileHelper.readUri(file.uri) ?: throw Exception("无法读取: ${file.name}")
                                val decrypted = if (state.usePassphrase) {
                                    ageEngine.decryptWithPassphrase(data, state.passphrase)
                                } else {
                                    ageEngine.decryptWithPrivateKey(data, state.selectedPrivateKey)
                                }

                                // Write decrypted data to temp file for streaming decompression
                                tempFile = java.io.File(fileHelper.getCacheDir(), "decrypt_${System.currentTimeMillis()}")
                                tempFile.writeBytes(decrypted)

                                // Try untar+gzip (batch archive format)
                                val untarred = try {
                                    fileHelper.untarGzipFromTemp(tempFile)
                                } catch (_: Exception) {
                                    null
                                }

                                if (untarred != null && untarred.isNotEmpty()) {
                                    // Batch archive: write each extracted file
                                    writeTarEntries(untarred, strategy)
                                    synchronized(this) {
                                        success++
                                        outputNames.addAll(untarred.map { it.name })
                                        _uiState.update { it.copy(processedCount = success + fail, successCount = success, progress = (success + fail).toFloat() / state.files.size) }
                                    }
                                } else {
                                    // Try gunzip (single file, gzip compressed)
                                    val decompressed = try {
                                        fileHelper.gunzipFromTemp(tempFile)
                                    } catch (_: Exception) {
                                        // Not gzipped, use raw decrypted data
                                        decrypted
                                    }
                                    // Single file: strip .age suffix and write
                                    val outName = file.name
                                        .removeSuffix(".age")
                                        .ifEmpty { "decrypted_output" }
                                    writeFile(outName, decompressed, strategy)
                                    synchronized(this) {
                                        success++
                                        outputNames.add(outName)
                                        _uiState.update { it.copy(processedCount = success + fail, successCount = success, progress = (success + fail).toFloat() / state.files.size) }
                                    }
                                }
                            } catch (e: Exception) {
                                synchronized(this) {
                                    fail++
                                    _uiState.update { it.copy(processedCount = success + fail, failCount = fail, progress = (success + fail).toFloat() / state.files.size) }
                                }
                            } finally {
                                tempFile?.let { fileHelper.deleteTempFile(it) }
                            }
                        }
                    }
                }
                jobs.awaitAll()

                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS))
                val outDir = getOutputDir()
                _uiState.update { it.copy(isProcessing = false, result = "解密完成！成功: $success, 失败: $fail", progress = 1f, outputFiles = outputNames, outputDir = outDir.absolutePath) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "解密失败: ${e.message}") }
            }
        }
    }

    private suspend fun writeFile(name: String, data: ByteArray, strategy: DuplicateStrategy) {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = Uri.parse(customUriStr)
            val subDirUri = fileHelper.getSubDirUri(customUri, "decrypted")
                ?: throw Exception("无法创建输出子目录")
            val actualName = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueSafFileName(subDirUri, name) else name
            if (!fileHelper.writeToSafFile(subDirUri, actualName, data)) {
                throw Exception("写入文件失败")
            }
        } else {
            val outDir = getOutputDir()
            val outFile = if (strategy == DuplicateStrategy.RENAME) fileHelper.getUniqueFile(outDir, name) else File(outDir, name)
            ageEngine.writeFile(outFile.absolutePath, data)
        }
    }

    private suspend fun writeTarEntries(entries: List<com.age.android.core.util.TarEntry>, strategy: DuplicateStrategy) {
        for (entry in entries) {
            writeFile(entry.name, entry.data, strategy)
        }
    }
}
