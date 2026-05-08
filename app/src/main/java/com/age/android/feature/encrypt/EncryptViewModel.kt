package com.age.android.feature.encrypt

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

data class FileItem(val uri: Uri, val name: String)

data class EncryptUiState(
    val mode: EncryptMode = EncryptMode.BATCH_PACK,
    val files: List<FileItem> = emptyList(),
    val usePassphrase: Boolean = true,
    val passphrase: String = "",
    val selectedPublicKey: String = "",
    val outputFileName: String = "archive.tar.gz.age",
    val useArmor: Boolean = true,
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
class EncryptViewModel @Inject constructor(
    private val ageEngine: AgeEngine,
    private val keyRepository: KeyRepository,
    private val operationRepository: OperationRepository,
    private val fileHelper: FileHelper
) : ViewModel() {

    private val _uiState = MutableStateFlow(EncryptUiState())
    val uiState: StateFlow<EncryptUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setMode(mode: EncryptMode) {
        _uiState.update { it.copy(mode = mode) }
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
    }

    fun setPassphrase(value: String) {
        _uiState.update { it.copy(passphrase = value) }
    }

    fun setSelectedPublicKey(value: String) {
        _uiState.update { it.copy(selectedPublicKey = value) }
    }

    fun setOutputFileName(value: String) {
        _uiState.update { it.copy(outputFileName = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
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
            _uiState.update { it.copy(isProcessing = true, error = null, progress = 0f, totalCount = state.files.size) }

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
                when (state.mode) {
                    EncryptMode.BATCH_PACK -> batchEncrypt(state)
                    EncryptMode.SEPARATE -> separateEncrypt(state)
                    else -> throw Exception("无效的加密模式")
                }
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS))
                _uiState.update { it.copy(isProcessing = false, result = "加密完成！", progress = 1f) }
            } catch (e: Exception) {
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.FAILED, errorMessage = e.message))
                _uiState.update { it.copy(isProcessing = false, error = "加密失败: ${e.message}") }
            }
        }
    }

    private suspend fun batchEncrypt(state: EncryptUiState) {
        val allData = state.files.mapNotNull { fileHelper.readUri(it.uri) }
        if (allData.size != state.files.size) throw Exception("部分文件读取失败")

        // Simple concatenation format: [nameLen(4bytes)][name][dataLen(4bytes)][data]...
        val combined = buildByteArray {
            for (i in allData.indices) {
                val nameBytes = state.files[i].name.toByteArray()
                writeInt(nameBytes.size)
                write(nameBytes)
                writeInt(allData[i].size)
                write(allData[i])
            }
        }

        val encrypted = if (state.usePassphrase) {
            ageEngine.encryptWithPassphrase(combined, state.passphrase)
        } else {
            ageEngine.encryptWithPublicKey(combined, state.selectedPublicKey)
        }

        val outFile = File(fileHelper.getOutputDir(), state.outputFileName)
        ageEngine.writeFile(outFile.absolutePath, encrypted)
    }

    private suspend fun separateEncrypt(state: EncryptUiState) {
        val semaphore = Semaphore(4)
        var success = 0
        var fail = 0

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
                        val outFile = File(fileHelper.getOutputDir(), "${file.name}.tar.gz.age")
                        ageEngine.writeFile(outFile.absolutePath, encrypted)
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
    }
}

private class ByteArrayBuilder {
    private val buffer = mutableListOf<Byte>()
    fun writeInt(v: Int) { buffer.addAll(byteArrayOf((v shr 24).toByte(), (v shr 16).toByte(), (v shr 8).toByte(), v.toByte()).toList()) }
    fun write(data: ByteArray) { buffer.addAll(data.toList()) }
    fun toByteArray(): ByteArray = buffer.toByteArray()
}

private fun buildByteArray(block: ByteArrayBuilder.() -> Unit): ByteArray = ByteArrayBuilder().apply(block).toByteArray()
