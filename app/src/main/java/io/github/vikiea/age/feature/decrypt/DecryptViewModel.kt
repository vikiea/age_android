/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.decrypt

import android.content.Context
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vikiea.age.core.age.AgeEngine
import io.github.vikiea.age.R
import io.github.vikiea.age.core.age.AgeCancellationToken
import io.github.vikiea.age.core.data.DuplicateStrategy
import io.github.vikiea.age.core.data.KeyRepository
import io.github.vikiea.age.core.data.OperationRepository
import io.github.vikiea.age.core.data.SettingsDataStore
import io.github.vikiea.age.core.file.FileSelectionKind
import io.github.vikiea.age.core.file.SelectedFileItem
import io.github.vikiea.age.core.file.SelectedFileLeaf
import io.github.vikiea.age.core.model.*
import io.github.vikiea.age.core.util.FileHelper
import io.github.vikiea.age.core.util.FileOperationCancelledException
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val usePassphrase: Boolean = false,
    val passphrase: String = "",
    val selectedPrivateKeyId: Long? = null,
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
) {
    val shouldShowActionFooter: Boolean
        get() = files.isNotEmpty() || isProcessing || error != null || result != null
}

@HiltViewModel
class DecryptViewModel @Inject constructor(
    private val ageEngine: AgeEngine,
    private val keyRepository: KeyRepository,
    private val operationRepository: OperationRepository,
    private val fileHelper: FileHelper,
    private val settingsDataStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DecryptUiState())
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    val keys = keyRepository.getAllKeys()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val customOutputDir: StateFlow<String?> = settingsDataStore.outputDirUri
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private var currentToken: AgeCancellationToken? = null

    init {
        viewModelScope.launch {
            val savedUsePassphrase = settingsDataStore.getDecryptUsePassphraseOnce()
            val savedPrivateKeyId = settingsDataStore.getSelectedPrivateKeyIdOnce()
            _uiState.update { it.copy(usePassphrase = savedUsePassphrase, selectedPrivateKeyId = savedPrivateKeyId) }
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

    fun setSelectedPrivateKeyId(value: Long?) {
        _uiState.update { it.copy(selectedPrivateKeyId = value) }
        viewModelScope.launch { settingsDataStore.setSelectedPrivateKeyId(value) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearResult() {
        fileHelper.cleanShareCache()
        _uiState.update { it.copy(result = null, outputFiles = emptyList(), outputDir = null) }
    }

    fun cancelOperation() {
        currentToken?.cancel()
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
            _uiState.update { it.copy(error = context.getString(R.string.error_select_files)) }
            return
        }
        if (state.usePassphrase && state.passphrase.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_enter_passphrase)) }
            return
        }
        if (!state.usePassphrase && state.selectedPrivateKeyId == null) {
            _uiState.update { it.copy(error = context.getString(R.string.error_select_identity)) }
            return
        }

        viewModelScope.launch {
            val token = ageEngine.newCancellationToken().also { currentToken = it }
            val inputLeaves = state.inputLeaves()
            _uiState.update { it.copy(isProcessing = true, error = null, result = null, progress = 0f, processedCount = 0, totalCount = inputLeaves.size, successCount = 0, failCount = 0, outputFiles = emptyList(), outputDir = null) }

            val outDirPath = getOutputDir().absolutePath
            val concurrency = settingsDataStore.getConcurrencyOnce()
            val duplicateStrategy = settingsDataStore.getDuplicateStrategyOnce()
            val selectedKey = state.selectedPrivateKeyId?.let { keyRepository.getKeyById(it) }
            val record = OperationRecord(
                type = OperationType.DECRYPT,
                mode = EncryptMode.DECRYPT,
                inputFiles = state.files.map { it.displayPath() },
                outputPath = outDirPath,
                recipientInfo = if (state.usePassphrase) "Passphrase" else "Identity key",
                authMethod = if (state.usePassphrase) OperationAuthMethod.PASSPHRASE else OperationAuthMethod.IDENTITY,
                keyHint = selectedKey?.let { identityHint(it.publicKey) }.orEmpty(),
                compression = CompressionState.UNKNOWN,
                duplicateStrategy = if (duplicateStrategy == DuplicateStrategy.RENAME) RecordedDuplicateStrategy.RENAME else RecordedDuplicateStrategy.OVERWRITE,
                concurrency = concurrency,
                status = OperationStatus.RUNNING
            )
            val recordId = operationRepository.insertOperation(record)

            try {
                val successCount = AtomicInteger(0)
                val failCount = AtomicInteger(0)
                val processedCount = AtomicInteger(0)
                val outputNames = java.util.concurrent.ConcurrentLinkedQueue<String>()
                val strategy = duplicateStrategy
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
                                    sourceTemp = fileHelper.streamUriToTemp(file.uri, "dec_src") { token.isCancelled }
                                    val encryptedSource = requireNotNull(sourceTemp)

                                    if (state.usePassphrase) {
                                        ageEngine.decryptStreamToFile(encryptedSource.absolutePath, decryptedTemp.absolutePath, state.passphrase, token)
                                    } else {
                                        keyRepository.withPrivateKey(requireNotNull(state.selectedPrivateKeyId)) { identity ->
                                            ageEngine.decryptStreamToFileWithIdentity(encryptedSource.absolutePath, decryptedTemp.absolutePath, identity, token)
                                        }
                                    }
                                    fileHelper.deleteTempFile(sourceTemp)
                                    sourceTemp = null

                                    var isTar = false
                                    try {
                                        fileHelper.untarStreaming(decryptedTemp, { token.isCancelled }) { entryName, entryStream, entrySize ->
                                            val actualName = writeStreamToFile(entryName, entryStream, entrySize, strategy, token)
                                            outputNames.add(actualName)
                                        }
                                        isTar = true
                                        successCount.incrementAndGet()
                                    } catch (error: Exception) {
                                        if (token.isCancelled) throw error
                                    }

                                    if (!isTar) {
                                        try {
                                            fileHelper.untarGzipStreaming(decryptedTemp, { token.isCancelled }) { entryName, entryStream, entrySize ->
                                                val actualName = writeStreamToFile(entryName, entryStream, entrySize, strategy, token)
                                                outputNames.add(actualName)
                                            }
                                            isTar = true
                                            successCount.incrementAndGet()
                                        } catch (error: Exception) {
                                            if (token.isCancelled) throw error
                                        }
                                    }

                                    if (!isTar) {
                                        val outName = decryptedRelativePath(file.relativePath)
                                        val decompressedTemp = try {
                                            fileHelper.gunzipToTemp(decryptedTemp) { token.isCancelled }
                                        } catch (error: Exception) {
                                            if (token.isCancelled) throw error
                                            decryptedTemp
                                        }
                                        val actualName = writeOutputFromTemp(outName, decompressedTemp, strategy, token)
                                        if (decompressedTemp !== decryptedTemp) fileHelper.deleteTempFile(decompressedTemp)
                                        successCount.incrementAndGet()
                                        outputNames.add(actualName)
                                    }
                                } catch (error: Exception) {
                                    if (token.isCancelled) throw error
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

                if (token.isCancelled) throw FileOperationCancelledException()

                val success = successCount.get()
                val fail = failCount.get()
                val outDir = getOutputDir()
                val outputFiles = outputNames.toList()
                operationRepository.updateOperation(record.copy(id = recordId, status = OperationStatus.SUCCESS, outputPath = outDir.absolutePath, outputFiles = outputFiles))
                _uiState.update { it.copy(isProcessing = false, result = context.getString(R.string.decrypt_result_count, success, fail), progress = 1f, outputFiles = outputFiles, outputDir = outDir.absolutePath) }
            } catch (e: Exception) {
                val cancelled = token.isCancelled
                operationRepository.updateOperation(record.copy(id = recordId, status = if (cancelled) OperationStatus.CANCELLED else OperationStatus.FAILED, errorMessage = if (cancelled) null else "Decryption failed"))
                _uiState.update { it.copy(isProcessing = false, error = if (cancelled) null else context.getString(R.string.decrypt_failed), result = if (cancelled) context.getString(R.string.common_operation_cancelled) else null) }
            } finally {
                if (currentToken === token) currentToken = null
            }
        }
    }

    private fun writeStreamToFile(name: String, input: java.io.InputStream, size: Long, strategy: DuplicateStrategy, token: AgeCancellationToken): String {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = customUriStr.toUri()
            return fileHelper.writeStreamToSafRelative(customUri, "decrypted", name, input, strategy) { token.isCancelled }
                ?: throw Exception(context.getString(R.string.error_write_file))
        } else {
            val outDir = getOutputDir()
            return fileHelper.writeStreamToDirRelative(outDir, name, input, strategy) { token.isCancelled }
        }
    }

    private fun writeOutputFromTemp(name: String, srcFile: java.io.File, strategy: DuplicateStrategy, token: AgeCancellationToken): String {
        val customUriStr = customOutputDir.value
        if (customUriStr != null) {
            val customUri = customUriStr.toUri()
            return fileHelper.copyFileToSafRelative(customUri, "decrypted", name, srcFile, strategy) { token.isCancelled }
                ?: throw Exception(context.getString(R.string.error_write_file))
        } else {
            val outDir = getOutputDir()
            return fileHelper.copyFileToDirRelative(outDir, name, srcFile, strategy) { token.isCancelled }
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

private fun identityHint(recipient: String): String =
    "${if (recipient.startsWith("age1pq1")) "PQ" else "X25519"} ·${recipient.takeLast(8)}"
