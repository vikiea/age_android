/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.core.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val body: String,
    val apkUrl: String,
    val apkSize: Long
)

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val repo = "vikiea/age_android"

    fun getCurrentVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }
    }

    fun getCurrentVersionCode(): Long {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun checkForUpdate(): Result<ReleaseInfo?> = runCatching {
        val request = Request.Builder()
            .url("https://api.github.com/repos/$repo/releases/latest")
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@runCatching null

        val json = JSONObject(response.body?.string() ?: return@runCatching null)
        val tagName = json.getString("tag_name") // e.g. "v1.0.0"
        val versionName = tagName.removePrefix("v")
        val body = json.optString("body", "")

        // Find arm64 APK asset
        val assets = json.getJSONArray("assets")
        var apkUrl = ""
        var apkSize = 0L
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            val name = asset.getString("name")
            if (name.contains("arm64") && name.endsWith(".apk")) {
                apkUrl = asset.getString("browser_download_url")
                apkSize = asset.getLong("size")
                break
            }
        }

        // Fallback to universal APK
        if (apkUrl.isEmpty()) {
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.contains("universal") && name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    apkSize = asset.getLong("size")
                    break
                }
            }
        }

        if (apkUrl.isEmpty()) return@runCatching null

        val currentVersion = getCurrentVersion()
        if (isNewer(versionName, currentVersion)) {
            ReleaseInfo(tagName, versionName, body, apkUrl, apkSize)
        } else {
            null
        }
    }

    /**
     * Compare semver strings: "1.2.3" vs "1.2.0"
     * Returns true if v1 > v2
     */
    private fun isNewer(v1: String, v2: String): Boolean {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }
        val maxLen = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLen) {
            val a = parts1.getOrElse(i) { 0 }
            val b = parts2.getOrElse(i) { 0 }
            if (a > b) return true
            if (a < b) return false
        }
        return false
    }

    fun downloadApk(url: String, versionName: String): Long {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Age Android v$versionName")
            .setDescription("正在下载新版本...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "age-v$versionName.apk")
            .setMimeType("application/vnd.android.package-archive")
        return dm.enqueue(request)
    }

    fun installApk(apkFile: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
