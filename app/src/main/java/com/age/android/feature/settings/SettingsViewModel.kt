package com.age.android.feature.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.util.FileHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val fileHelper: FileHelper,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val duplicateStrategy: StateFlow<DuplicateStrategy> = settingsDataStore.duplicateStrategy
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DuplicateStrategy.RENAME)

    val outputDirUri: StateFlow<String?> = settingsDataStore.outputDirUri
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val compressEnabled: StateFlow<Boolean> = settingsDataStore.compressEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

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
