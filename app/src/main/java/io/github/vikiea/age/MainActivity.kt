/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.fragment.app.FragmentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.compose.runtime.LaunchedEffect
import io.github.vikiea.age.core.data.AppLanguage
import io.github.vikiea.age.core.data.SettingsDataStore
import io.github.vikiea.age.core.data.ThemeAccent
import io.github.vikiea.age.core.data.ThemeMode
import io.github.vikiea.age.navigation.AppNavigation
import io.github.vikiea.age.ui.theme.AgeAndroidTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    private val sharedUris = mutableStateOf<List<Uri>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleShareIntent(intent)
        enableEdgeToEdge()
        setContent {
            val themeMode by settingsDataStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val themeAccent by settingsDataStore.themeAccent.collectAsState(initial = ThemeAccent.LIQUID_DEFAULT)
            val glassEffectEnabled by settingsDataStore.glassEffectEnabled.collectAsState(initial = true)
            val dynamicColorEnabled by settingsDataStore.dynamicColorEnabled.collectAsState(initial = false)
            val appLanguage by settingsDataStore.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
            LaunchedEffect(appLanguage) {
                val tags = when (appLanguage) {
                    AppLanguage.SYSTEM -> ""
                    AppLanguage.ZH_CN -> "zh-CN"
                    AppLanguage.ENGLISH -> "en"
                }
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags))
            }
            AgeAndroidTheme(
                themeMode = themeMode,
                themeAccent = themeAccent,
                glassEffectEnabled = glassEffectEnabled,
                dynamicColor = dynamicColorEnabled
            ) {
                AppNavigation(
                    sharedUris = sharedUris.value,
                    onSharedUrisConsumed = { sharedUris.value = null },
                    glassEffectEnabled = glassEffectEnabled
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleShareIntent(intent)
    }

    private fun handleShareIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val uri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                if (uri != null) sharedUris.value = listOf(uri)
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                if (!uris.isNullOrEmpty()) sharedUris.value = uris
            }
        }
    }
}
