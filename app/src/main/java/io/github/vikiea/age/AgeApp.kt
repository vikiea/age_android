/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age

import android.app.Application
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
}
