/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class AgeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Clean up temp files left by OOM kills
        try {
            val tempDir = File(cacheDir, "age_temp")
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { it.delete() }
            }
        } catch (_: Exception) {}
    }
}
