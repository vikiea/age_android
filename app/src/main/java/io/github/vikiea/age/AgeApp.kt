/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age

import android.app.Application
import android.app.LocaleManager
import android.os.Build
import android.os.LocaleList
import dagger.hilt.android.HiltAndroidApp
import io.github.vikiea.age.core.data.KeyRepository
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class AgeApp : Application() {
    @Inject lateinit var keyRepository: KeyRepository

    override fun onCreate() {
        super.onCreate()
        clearLegacyAppLocaleOverrideOnce()
        // Clean up temp files left by OOM kills
        try {
            val tempDir = File(cacheDir, "age_temp")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { keyRepository.reconcileSecrets() }
        }
    }

    private fun clearLegacyAppLocaleOverrideOnce() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val migrationPreferences = getSharedPreferences(LOCALE_MIGRATION_PREFERENCES, MODE_PRIVATE)
        if (migrationPreferences.getBoolean(LOCALE_OVERRIDE_CLEARED, false)) return

        val localeManager = getSystemService(LocaleManager::class.java)
        if (!localeManager.applicationLocales.isEmpty) {
            localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
        }
        migrationPreferences.edit().putBoolean(LOCALE_OVERRIDE_CLEARED, true).apply()
    }

    private companion object {
        const val LOCALE_MIGRATION_PREFERENCES = "age_migrations"
        const val LOCALE_OVERRIDE_CLEARED = "legacy_locale_override_cleared_v1"
    }
}
