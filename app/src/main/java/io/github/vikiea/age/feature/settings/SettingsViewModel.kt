/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.vikiea.age.R
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vikiea.age.core.data.DuplicateStrategy
import io.github.vikiea.age.core.data.SettingsDataStore
import io.github.vikiea.age.core.data.ThemeAccent
import io.github.vikiea.age.core.data.ThemeMode
import io.github.vikiea.age.core.data.AppLanguage
import io.github.vikiea.age.core.update.ApkDownloadResult
import io.github.vikiea.age.core.update.ReleaseInfo
import io.github.vikiea.age.core.update.UpdateChecker
import io.github.vikiea.age.core.util.FileHelper
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
    val downloadedApkPath: String? = null,
    val requiresInstallPermission: Boolean = false,
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

    val concurrency: StateFlow<Int> = settingsDataStore.concurrency
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 4)

    val themeMode: StateFlow<ThemeMode> = settingsDataStore.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val themeAccent: StateFlow<ThemeAccent> = settingsDataStore.themeAccent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeAccent.LIQUID_DEFAULT)

    val appLanguage: StateFlow<AppLanguage> = settingsDataStore.appLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppLanguage.SYSTEM)

    private val _updateState = MutableStateFlow(UpdateState())
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun getCurrentVersion(): String = updateChecker.getCurrentVersion()

    fun checkForUpdate() {
        viewModelScope.launch {
            _updateState.update {
                it.copy(
                    isChecking = true,
                    error = null,
                    message = null,
                    downloadedApkPath = null,
                    requiresInstallPermission = false
                )
            }
            updateChecker.checkForUpdate()
                .onSuccess { release ->
                    _updateState.update {
                        if (release != null) {
                            it.copy(isChecking = false, releaseInfo = release, message = null)
                        } else {
                            it.copy(isChecking = false, releaseInfo = null, message = context.getString(R.string.update_latest))
                        }
                    }
                }
                .onFailure { e ->
                    _updateState.update {
                        it.copy(isChecking = false, error = context.getString(R.string.update_check_failed, e.message.orEmpty()))
                    }
                }
        }
    }

    fun downloadUpdate() {
        val release = _updateState.value.releaseInfo ?: return
        val downloadId = updateChecker.downloadApk(release.apkUrl, release.versionName)
        _updateState.update {
            it.copy(
                isDownloading = true,
                downloadId = downloadId,
                downloadedApkPath = null,
                requiresInstallPermission = false
            )
        }
        awaitDownloadResult(downloadId, release.versionName, release.sha256)
    }

    private fun awaitDownloadResult(downloadId: Long, versionName: String, expectedSha256: String) {
        viewModelScope.launch {
            when (val result = updateChecker.awaitApkDownload(downloadId, versionName, expectedSha256)) {
                is ApkDownloadResult.Completed -> {
                    if (downloadId != _updateState.value.downloadId) return@launch
                    _updateState.update {
                        it.copy(
                            isDownloading = false,
                            downloadedApkPath = result.apkFile.absolutePath,
                            error = null
                        )
                    }
                    tryInstallApk(result.apkFile)
                }
                is ApkDownloadResult.Failed -> {
                    if (downloadId != _updateState.value.downloadId) return@launch
                    _updateState.update {
                        it.copy(
                            isDownloading = false,
                            downloadedApkPath = null,
                            requiresInstallPermission = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun installDownloadedUpdate() {
        val apkFile = _updateState.value.downloadedApkPath?.let(::File) ?: return
        if (apkFile.exists()) {
            tryInstallApk(apkFile)
        } else {
            _updateState.update {
                it.copy(
                    downloadedApkPath = null,
                    requiresInstallPermission = false,
                    error = context.getString(R.string.update_apk_missing)
                )
            }
        }
    }

    fun requestInstallPermission() {
        updateChecker.openInstallPermissionSettings()
    }

    fun createInstallPermissionIntent(): Intent? =
        updateChecker.createInstallPermissionSettingsIntent()

    fun onInstallPermissionResult() {
        val apkFile = _updateState.value.downloadedApkPath?.let(::File) ?: return
        if (apkFile.exists()) {
            tryInstallApk(apkFile)
        }
    }

    private fun tryInstallApk(apkFile: File) {
        if (!apkFile.exists()) return
        if (updateChecker.canInstallDownloadedApks()) {
            _updateState.update { it.copy(requiresInstallPermission = false, error = null) }
            updateChecker.installApk(apkFile)
        } else {
            _updateState.update {
                it.copy(
                    requiresInstallPermission = true,
                    error = context.getString(R.string.update_install_permission_needed)
                )
            }
        }
    }

    fun clearUpdateError() {
        _updateState.update { it.copy(error = null) }
    }

    fun dismissUpdate() {
        _updateState.update {
            it.copy(
                releaseInfo = null,
                downloadedApkPath = null,
                requiresInstallPermission = false,
                error = null,
                message = null
            )
        }
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

    fun setConcurrency(value: Int) {
        viewModelScope.launch {
            settingsDataStore.setConcurrency(value)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setThemeMode(mode)
        }
    }

    fun setThemeAccent(accent: ThemeAccent) {
        viewModelScope.launch {
            settingsDataStore.setThemeAccent(accent)
        }
    }

    fun setAppLanguage(value: AppLanguage) {
        viewModelScope.launch { settingsDataStore.setAppLanguage(value) }
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
