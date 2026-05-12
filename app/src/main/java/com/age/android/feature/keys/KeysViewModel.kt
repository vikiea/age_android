/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.keys

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.age.AgeEngine
import com.age.android.core.data.KeyRepository
import com.age.android.core.model.KeyEntry
import com.age.android.core.model.KeyType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class KeysUiState(
    val isGenerating: Boolean = false,
    val newName: String = "",
    val importName: String = "",
    val importPublicKey: String = "",
    val importPrivateKey: String = "",
    val importFileUri: Uri? = null,
    val error: String? = null,
    val success: String? = null,
    val showGenerateDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showRenameDialog: Boolean = false,
    val renameTargetId: Long = 0,
    val renameText: String = "",
    val pendingExportFileName: String? = null
)

@HiltViewModel
class KeysViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyRepository: KeyRepository,
    private val ageEngine: AgeEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(KeysUiState())
    val uiState: StateFlow<KeysUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var pendingExportContent: String? = null

    fun setNewName(value: String) { _uiState.update { it.copy(newName = value) } }
    fun setImportName(value: String) { _uiState.update { it.copy(importName = value) } }
    fun setImportPublicKey(value: String) { _uiState.update { it.copy(importPublicKey = value) } }
    fun setImportPrivateKey(value: String) { _uiState.update { it.copy(importPrivateKey = value) } }
    fun setRenameText(value: String) { _uiState.update { it.copy(renameText = value) } }

    fun showGenerateDialog() { _uiState.update { it.copy(showGenerateDialog = true, newName = "", error = null) } }
    fun hideGenerateDialog() { _uiState.update { it.copy(showGenerateDialog = false) } }
    fun showImportDialog() { _uiState.update { it.copy(showImportDialog = true, importName = "", importPublicKey = "", importPrivateKey = "", importFileUri = null, error = null) } }
    fun hideImportDialog() { _uiState.update { it.copy(showImportDialog = false, importFileUri = null) } }
    fun clearMessages() { _uiState.update { it.copy(error = null, success = null) } }

    fun showRenameDialog(key: KeyEntry) {
        _uiState.update { it.copy(showRenameDialog = true, renameTargetId = key.id, renameText = key.name, error = null) }
    }
    fun hideRenameDialog() { _uiState.update { it.copy(showRenameDialog = false) } }

    fun renameKey() {
        val state = _uiState.value
        val newName = state.renameText.trim()
        if (newName.isBlank()) { _uiState.update { it.copy(error = "名称不能为空") }; return }

        viewModelScope.launch {
            try {
                val key = keyRepository.getKeyById(state.renameTargetId) ?: return@launch
                keyRepository.updateKey(key.copy(name = newName))
                _uiState.update { it.copy(showRenameDialog = false, success = "已重命名") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "重命名失败: ${e.message}") }
            }
        }
    }

    fun generateKey() {
        val name = _uiState.value.newName.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = "请输入密钥名称") }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val keyPair = ageEngine.generateKeyPair()
                keyRepository.saveKey(KeyEntry(name = name, publicKey = keyPair.first, privateKey = keyPair.second, keyType = KeyType.AGE_KEY, hasPrivateKey = true))
                _uiState.update { it.copy(isGenerating = false, showGenerateDialog = false, success = "密钥已生成: ${keyPair.first}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = "生成失败: ${e.message}") }
            }
        }
    }

    fun importKey() {
        val state = _uiState.value
        val fileUri = state.importFileUri

        if (fileUri != null) {
            // File import: use the fields populated by parseKeyFile
            doImportFromFields(state)
        } else {
            // Text import
            doImportFromFields(state)
        }
    }

    private fun doImportFromFields(state: KeysUiState) {
        val name = state.importName.trim()
        val publicKey = state.importPublicKey.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = "请输入密钥名称") }; return }
        if (publicKey.isBlank()) { _uiState.update { it.copy(error = "请输入公钥") }; return }

        viewModelScope.launch {
            try {
                keyRepository.importKey(name, publicKey, state.importPrivateKey.ifBlank { null })
                _uiState.update { it.copy(showImportDialog = false, importFileUri = null, success = "密钥已导入") }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "导入失败: ${e.message}") }
            }
        }
    }

    // Parse file content into import dialog fields, but don't import yet
    fun parseKeyFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw Exception("无法读取文件")

                val lines = content.lines()
                var publicKey: String? = null
                var secretKey: String? = null

                for (line in lines) {
                    val trimmed = line.trim()
                    when {
                        trimmed.startsWith("# public key:") -> publicKey = trimmed.substringAfter(":").trim()
                        trimmed.startsWith("AGE-SECRET-KEY-") -> secretKey = trimmed
                        trimmed.startsWith("age1") && publicKey == null -> publicKey = trimmed
                    }
                }

                if (publicKey == null && secretKey == null) {
                    throw Exception("无法解析密钥文件，未找到公钥或私钥")
                }

                // Derive name from filename
                val fileName = getFileName(uri).substringBeforeLast(".")
                val existingNames = keys.value.map { it.name }.toSet()
                val uniqueName = generateUniqueName(fileName, existingNames)

                _uiState.update {
                    it.copy(
                        importName = uniqueName,
                        importPublicKey = publicKey ?: "",
                        importPrivateKey = secretKey ?: "",
                        importFileUri = uri,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "解析失败: ${e.message}") }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "导入的密钥"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: name
            }
        }
        return name
    }

    private fun generateUniqueName(base: String, existing: Set<String>): String {
        if (base !in existing) return base
        var i = 2
        while ("$base ($i)" in existing) i++
        return "$base ($i)"
    }

    fun deleteKey(id: Long) {
        viewModelScope.launch {
            keyRepository.deleteKey(id)
            _uiState.update { it.copy(success = "密钥已删除") }
        }
    }

    fun prepareExport(key: KeyEntry, includePrivate: Boolean) {
        if (includePrivate && key.privateKey == null) return

        val content = buildString {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
            appendLine("# created: ${isoFormat.format(Date(key.createdAt))}")
            appendLine("# public key: ${key.publicKey}")
            if (includePrivate) {
                appendLine(key.privateKey)
            }
        }
        pendingExportContent = content
        val fileName = if (includePrivate) "${key.name}.txt" else "${key.name}_public.txt"
        _uiState.update { it.copy(pendingExportFileName = fileName) }
    }

    fun writeExportToUri(uri: Uri) {
        val content = pendingExportContent ?: return
        pendingExportContent = null
        _uiState.update { it.copy(pendingExportFileName = null) }
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray())
            }
            _uiState.update { it.copy(success = "密钥已导出") }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = "导出失败: ${e.message}") }
        }
    }

    fun cancelExport() {
        pendingExportContent = null
        _uiState.update { it.copy(pendingExportFileName = null) }
    }
}
