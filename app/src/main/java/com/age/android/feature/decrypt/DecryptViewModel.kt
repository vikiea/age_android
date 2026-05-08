package com.age.android.feature.decrypt

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.age.AgeEngine
import com.age.android.core.data.KeyRepository
import com.age.android.core.data.OperationRepository
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
    val result: String? = null
)

@HiltViewModel
class DecryptViewModel @Inject constructor(
    private val ageEngine: AgeEngine,
    private val keyRepository: KeyRepository,
    private val operationRepository: OperationRepository,
    private val fileHelper: FileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecryptUiState())
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addFiles(uris: List<Uri>) {
        val newFiles = uris.map { uri -> DecryptFileItem(uri, fileHelper.getFileName(uri)) }
        _uiState.update { it.copy(files = it.files + newFiles) }
    }

    fun removeFile(index: Int) {
        _uiState.update { it.copy(files = it.files.toMutableList().apply { removeAt(index) }) }
    }

    fun setUsePassphrase(value: Boolean) {
        _uiState.update { it.copy(usePassphrase = value) }
    }

    fun setPassphrase(value: String) {
        _uiState.update { it.copy(passphrase = value) }
    }

    fun setSelectedPrivateKey(value: String) {
        _uiState.update { it.copy(selectedPrivateKey = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
            _uiState.update { it.copy(isProcessing = true, error = null, progress = 0f, totalCount = state.files.size) }

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

                val jobs = state.files.map { file ->
                    viewModelScope.async {
                        semaphore.withPermit {
                            try {
                                val data = fileHelper.readUri(file.uri) ?: throw Exception("无法读取: ${file.name}")
                                val decrypted = if (state.usePassphrase) {
                                    ageEngine.decryptWithPassphrase(data, state.passphrase)
                                } else {
                                    ageEngine.decryptWithPrivateKey(data, state.selectedPrivateKey)
                                }
                                val outName = file.name.removeSuffix(".tar.gz.age").removeSuffix(".age")
                                val outFile = File(fileHelper.getOutputDir(), outName)
                                ageEngine.writeFile(outFile.absolutePath, decrypted)
                                synchronized(this) {
                                    success++
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

                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS))
                _uiState.update { it.copy(isProcessing = false, result = "解密完成！成功: $success, 失败: $fail", progress = 1f) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "解密失败: ${e.message}") }
            }
        }
    }
}
