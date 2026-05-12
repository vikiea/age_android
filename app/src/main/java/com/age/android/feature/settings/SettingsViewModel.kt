/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.feature.settings

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.update.ReleaseInfo
import com.age.android.core.update.UpdateChecker
import com.age.android.core.util.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class UpdateState(
    val isChecking: Boolean = false,
    val releaseInfo: ReleaseInfo? = null,
    val isDownloading: Boolean = false,
    val downloadId: Long? = null,
    val error: String? = null,
    val message: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val fileHelper: FileHelper,
    private val updateChecker: UpdateChecker,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val duplicateStrategy: StateFlow<DuplicateStrategy> = settingsDataStore.duplicateStrategy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DuplicateStrategy.RENAME)

    val outputDirUri: StateFlow<String?> = settingsDataStore.outputDirUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val compressEnabled: StateFlow<Boolean> = settingsDataStore.compressEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun getCurrentVersion(): String = updateChecker.getCurrentVersion()

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.update { it.copy(isChecking = true, error = null, message = null) }
            updateChecker.checkForUpdate()
                .onSuccess { release ->
                    _updateState.update {
                        if (release != null) {
                            it.copy(isChecking = false, releaseInfo = release, message = null)
                        } else {
                            it.copy(isChecking = false, releaseInfo = null, message = "已是最新版本")
                        }
                    }
                }
                .onFailure { e ->
                    _updateState.update {
                        it.copy(isChecking = false, error = "检查更新失败: ${e.message}")
                    }
                }
        }
    }

    fun downloadUpdate() {
        val release = _updateState.value.releaseInfo ?: return
        val downloadId = updateChecker.downloadApk(release.apkUrl, release.versionName)
        _updateState.update { it.copy(isDownloading = true, downloadId = downloadId) }
    }

    fun onDownloadComplete(downloadId: Long) {
        if (downloadId != _updateState.value.downloadId) return
        _updateState.update { it.copy(isDownloading = false) }
        // Find the downloaded APK and install
        val dir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
        val apkFile = File(dir, "age-v${_updateState.value.releaseInfo?.versionName}.apk")
        if (apkFile.exists()) {
            updateChecker.installApk(apkFile)
        }
    }

    fun clearUpdateError() {
        _updateState.update { it.copy(error = null) }
    }

    fun dismissUpdate() {
        _updateState.update { it.copy(releaseInfo = null, error = null, message = null) }
    }

    fun setDuplicateStrategy(strategy: DuplicateStrategy) {
        viewModelScope.launch {
            settingsDataStore.setDuplicateStrategy(strategy)
        }
    }

    fun setCompressEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setCompressEnabled(value)
        }
    }

    fun setOutputDirUri(uri: Uri?) {
        viewModelScope.launch {
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }
            settingsDataStore.setOutputDirUri(uri?.toString())
        }
    }

    fun resolveUriToPath(uriString: String?): String? {
        if (uriString == null) return null
        return fileHelper.resolveUriToPath(uriString)
    }

    fun getDefaultDirPath(): String = fileHelper.getOutputDir().absolutePath
}
