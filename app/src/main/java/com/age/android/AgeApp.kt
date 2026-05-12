package com.age.android

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
