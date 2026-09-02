/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.keys

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vikiea.age.R
import io.github.vikiea.age.core.age.AgeEngine
import io.github.vikiea.age.core.data.KeyRepository
import io.github.vikiea.age.core.model.AgeKeyType
import io.github.vikiea.age.core.model.KeyEntry
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
    val newKeyType: AgeKeyType = AgeKeyType.POST_QUANTUM,
    val importName: String = "",
    val importPublicKey: String = "",
    val importPrivateKey: String = "",
    val importFileUri: Uri? = null,
    val error: String? = null,
    val success: String? = null,
    val tip: String? = null,
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
    private var parsedFileKeys: List<ParsedKey> = emptyList()

    private data class ParsedKey(
        val publicKey: String,
        val privateKey: String? = null,
        val type: AgeKeyType
    )

    fun setNewName(value: String) { _uiState.update { it.copy(newName = value) } }
    fun setNewKeyType(value: AgeKeyType) { _uiState.update { it.copy(newKeyType = value) } }
    fun setImportName(value: String) { _uiState.update { it.copy(importName = value) } }
    fun setImportPublicKey(value: String) { _uiState.update { it.copy(importPublicKey = value) } }
    fun setImportPrivateKey(value: String) { _uiState.update { it.copy(importPrivateKey = value) } }
    fun setRenameText(value: String) { _uiState.update { it.copy(renameText = value) } }

    fun showGenerateDialog() { _uiState.update { it.copy(showGenerateDialog = true, newName = "", newKeyType = AgeKeyType.POST_QUANTUM, error = null, tip = null) } }
    fun hideGenerateDialog() { _uiState.update { it.copy(showGenerateDialog = false) } }
    fun showImportDialog() { parsedFileKeys = emptyList(); _uiState.update { it.copy(showImportDialog = true, importName = "", importPublicKey = "", importPrivateKey = "", importFileUri = null, error = null, tip = null) } }
    fun hideImportDialog() { parsedFileKeys = emptyList(); _uiState.update { it.copy(showImportDialog = false, importFileUri = null, importPrivateKey = "") } }
    fun clearMessages() { _uiState.update { it.copy(error = null, success = null) } }
    fun clearTip() { _uiState.update { it.copy(tip = null) } }

    fun showRenameDialog(key: KeyEntry) {
        _uiState.update { it.copy(showRenameDialog = true, renameTargetId = key.id, renameText = key.name, error = null) }
    }
    fun hideRenameDialog() { _uiState.update { it.copy(showRenameDialog = false) } }

    fun renameKey() {
        val state = _uiState.value
        val newName = state.renameText.trim()
        if (newName.isBlank()) { _uiState.update { it.copy(error = context.getString(R.string.error_name_required)) }; return }

        viewModelScope.launch {
            try {
                val key = keyRepository.getKeyById(state.renameTargetId) ?: return@launch
                keyRepository.updateKey(key.copy(name = newName))
                _uiState.update { it.copy(showRenameDialog = false, success = context.getString(R.string.key_renamed)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.error_rename_key)) }
            }
        }
    }

    fun generateKey() {
        val name = _uiState.value.newName.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = context.getString(R.string.error_name_required)) }; return }

        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            try {
                val type = _uiState.value.newKeyType
                val keyPair = ageEngine.generateKeyPair(if (type == AgeKeyType.X25519) "x25519" else "post-quantum")
                keyRepository.saveKey(name, keyPair.first, keyPair.second, type)
                _uiState.update { it.copy(isGenerating = false, showGenerateDialog = false, success = context.getString(R.string.key_generated, keyHint(keyPair.first))) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isGenerating = false, error = context.getString(R.string.error_generate_key)) }
            }
        }
    }

    fun importKey() {
        val state = _uiState.value
        if (state.importFileUri != null && parsedFileKeys.isNotEmpty()) doImportParsedFile(state)
        else doImportFromFields(state)
    }

    private fun doImportParsedFile(state: KeysUiState) {
        val baseName = state.importName.trim()
        if (baseName.isBlank()) { _uiState.update { it.copy(error = context.getString(R.string.error_name_required)) }; return }
        viewModelScope.launch {
            try {
                val existingNames = keys.value.map { it.name }.toMutableSet()
                parsedFileKeys.forEachIndexed { index, parsed ->
                    val candidate = if (parsedFileKeys.size == 1) baseName else "$baseName ${index + 1}"
                    val uniqueName = generateUniqueName(candidate, existingNames)
                    keyRepository.importKey(uniqueName, parsed.publicKey, parsed.privateKey, parsed.type)
                    existingNames += uniqueName
                }
                val count = parsedFileKeys.size
                parsedFileKeys = emptyList()
                _uiState.update { it.copy(showImportDialog = false, importFileUri = null, importPrivateKey = "", tip = context.getString(R.string.keys_imported, count)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.error_import_key)) }
            }
        }
    }

    private fun doImportFromFields(state: KeysUiState) {
        val name = state.importName.trim()
        val publicKey = state.importPublicKey.trim()
        if (name.isBlank()) { _uiState.update { it.copy(error = context.getString(R.string.error_name_required)) }; return }
        val privateKey = state.importPrivateKey.trim().ifBlank { null }
        if (publicKey.isBlank() && privateKey == null) { _uiState.update { it.copy(error = context.getString(R.string.error_key_material_required)) }; return }

        viewModelScope.launch {
            try {
                val derivedPublic = privateKey?.let { ageEngine.recipientForIdentity(it) }
                if (publicKey.isNotBlank() && derivedPublic != null && publicKey != derivedPublic) {
                    error(context.getString(R.string.error_key_mismatch))
                }
                val resolvedPublic = publicKey.ifBlank { requireNotNull(derivedPublic) }
                ageEngine.encryptWithRecipient(ByteArray(0), resolvedPublic)
                keyRepository.importKey(name, resolvedPublic, privateKey, keyType(resolvedPublic))
                _uiState.update { it.copy(showImportDialog = false, importFileUri = null, importPrivateKey = "", success = null, tip = context.getString(R.string.key_imported)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.error_import_key)) }
            }
        }
    }

    // Parse file content into import dialog fields, but don't import yet
    fun parseKeyFile(uri: Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                    ?: throw Exception(context.getString(R.string.error_read_file))

                val publicKeys = content.lineSequence().map(String::trim)
                    .mapNotNull { line ->
                        when {
                            line.startsWith("# public key:") -> line.substringAfter(":").trim()
                            line.startsWith("age1") -> line
                            else -> null
                        }
                    }.filter(String::isNotBlank).distinct().toMutableList()
                val privateKeys = content.lineSequence().map(String::trim)
                    .filter { it.startsWith("AGE-SECRET-KEY-") }.distinct().toList()

                val privatePairs = privateKeys.map { privateKey ->
                    val derived = ageEngine.recipientForIdentity(privateKey)
                    ParsedKey(derived, privateKey, keyType(derived))
                }
                privatePairs.forEach { pair -> publicKeys.remove(pair.publicKey) }
                val parsed = privatePairs + publicKeys.map { publicKey ->
                    ageEngine.encryptWithRecipient(ByteArray(0), publicKey)
                    ParsedKey(publicKey, type = keyType(publicKey))
                }
                if (parsed.isEmpty()) throw Exception(context.getString(R.string.error_parse_key_file))
                parsedFileKeys = parsed

                // Derive name from filename
                val fileName = getFileName(uri).substringBeforeLast(".")
                val existingNames = keys.value.map { it.name }.toSet()
                val uniqueName = generateUniqueName(fileName, existingNames)

                _uiState.update {
                    it.copy(
                        importName = uniqueName,
                        importPublicKey = parsed.first().publicKey,
                        importPrivateKey = if (parsed.size == 1 && parsed.first().privateKey != null) context.getString(R.string.private_key_recognized) else "",
                        importFileUri = uri,
                        error = null,
                        tip = context.getString(R.string.keys_recognized, parsed.size)
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.error_parse_key)) }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = context.getString(R.string.imported_key_name)
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
            _uiState.update { it.copy(success = context.getString(R.string.key_deleted)) }
        }
    }

    fun withPrivateKey(keyId: Long, action: (String) -> Unit) {
        viewModelScope.launch {
            try {
                keyRepository.withPrivateKey(keyId) { action(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.error_read_private_key)) }
            }
        }
    }

    fun prepareExport(key: KeyEntry, includePrivate: Boolean) {
        viewModelScope.launch {
            try {
                val privateKey = if (includePrivate) {
                    keyRepository.withPrivateKey(key.id) { it }
                } else null
                pendingExportContent = buildString {
                    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US)
                    appendLine("# created: ${isoFormat.format(Date(key.createdAt))}")
                    appendLine("# public key: ${key.publicKey}")
                    privateKey?.let(::appendLine)
                }
                val fileName = if (includePrivate) "${key.name}.txt" else "${key.name}_public.txt"
                _uiState.update { it.copy(pendingExportFileName = fileName) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = context.getString(R.string.error_prepare_export)) }
            }
        }
    }

    fun writeExportToUri(uri: Uri) {
        val content = pendingExportContent ?: return
        pendingExportContent = null
        _uiState.update { it.copy(pendingExportFileName = null) }
        try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(content.toByteArray())
            }
            _uiState.update { it.copy(success = context.getString(R.string.key_exported)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(error = context.getString(R.string.error_export_key)) }
        }
    }

    fun cancelExport() {
        pendingExportContent = null
        _uiState.update { it.copy(pendingExportFileName = null) }
    }

    private fun keyType(publicKey: String): AgeKeyType =
        if (publicKey.startsWith("age1pq1")) AgeKeyType.POST_QUANTUM else AgeKeyType.X25519

    private fun keyHint(publicKey: String): String =
        "${if (publicKey.startsWith("age1pq1")) "PQ" else "X25519"} ·${publicKey.takeLast(8)}"
}
