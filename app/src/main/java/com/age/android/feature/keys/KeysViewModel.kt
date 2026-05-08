package com.age.android.feature.keys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.age.AgeEngine
import com.age.android.core.data.KeyRepository
import com.age.android.core.model.KeyEntry
import com.age.android.core.model.KeyType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class KeysUiState(
    val isGenerating: Boolean = false,
    val newName: String = "",
    val importName: String = "",
    val importPublicKey: String = "",
    val importPrivateKey: String = "",
    val error: String? = null,
    val success: String? = null,
    val showGenerateDialog: Boolean = false,
    val showImportDialog: Boolean = false
)

@HiltViewModel
class KeysViewModel @Inject constructor(
    private val keyRepository: KeyRepository,
    private val ageEngine: AgeEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeysUiState())
    val uiState: StateFlow<KeysUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setNewName(value: String) { _uiState.update { it.copy(newName = value) } }
    fun setImportName(value: String) { _uiState.update { it.copy(importName = value) } }
    fun setImportPublicKey(value: String) { _uiState.update { it.copy(importPublicKey = value) } }
    fun setImportPrivateKey(value: String) { _uiState.update { it.copy(importPrivateKey = value) } }

    fun showGenerateDialog() { _uiState.update { it.copy(showGenerateDialog = true, newName = "", error = null) } }
    fun hideGenerateDialog() { _uiState.update { it.copy(showGenerateDialog = false) } }
    fun showImportDialog() { _uiState.update { it.copy(showImportDialog = true, importName = "", importPublicKey = "", importPrivateKey = "", error = null) } }
    fun hideImportDialog() { _uiState.update { it.copy(showImportDialog = false) } }
    fun clearMessages() { _uiState.update { it.copy(error = null, success = null) } }

    fun generateKey() {
        val name = _uiState.value.newName.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = "请输入密钥名称") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val keyPair = ageEngine.generateKeyPair()
                keyRepository.saveKey(KeyEntry(name = name, publicKey = keyPair.first, keyType = KeyType.AGE_KEY, hasPrivateKey = true))
                _uiState.update { it.copy(isGenerating = false, showGenerateDialog = false, success = "密钥已生成: ${keyPair.first}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = "生成失败: ${e.message}") }
            }
        }
    }

    fun importKey() {
        val state = _uiState.value
        val name = state.importName.trim()
        val publicKey = state.importPublicKey.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = "请输入密钥名称") }; return }
        if (publicKey.isBlank()) { _uiState.update { it.copy(error = "请输入公钥") }; return }

        viewModelScope.launch {
            try {
                keyRepository.importKey(name, publicKey, state.importPrivateKey.ifBlank { null })
                _uiState.update { it.copy(showImportDialog = false, success = "密钥已导入") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "导入失败: ${e.message}") }
            }
        }
    }

    fun deleteKey(id: Long) {
        viewModelScope.launch {
            keyRepository.deleteKey(id)
            _uiState.update { it.copy(success = "密钥已删除") }
        }
    }
}
