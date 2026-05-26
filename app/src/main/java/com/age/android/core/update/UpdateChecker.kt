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
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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

sealed class ApkDownloadResult {
    data class Completed(val apkFile: File) : ApkDownloadResult()
    data class Failed(val message: String) : ApkDownloadResult()
}

@Singleton
class UpdateChecker @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val repo = "vikiea/age_android"

    /** GitHub API sources: direct + Chinese mirrors */
    private val apiSources = listOf(
        "https://api.github.com/repos/$repo/releases/latest",
        "https://mirror.ghproxy.com/https://api.github.com/repos/$repo/releases/latest",
        "https://gh-proxy.com/https://api.github.com/repos/$repo/releases/latest",
    )

    /** Download proxy prefixes (tried in order) */
    private val downloadProxies = listOf(
        "",  // direct
        "https://mirror.ghproxy.com/",
        "https://gh-proxy.com/",
    )

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
        withContext(Dispatchers.IO) {
            var lastException: Exception? = null
            for (apiUrl in apiSources) {
                try {
                    val result = fetchReleaseFrom(apiUrl)
                    return@withContext result
                } catch (e: Exception) {
                    lastException = e
                    Log.w("UpdateChecker", "Source failed: $apiUrl", e)
                }
            }
            throw lastException ?: Exception("所有更新源均不可用")
        }
    }

    private fun fetchReleaseFrom(apiUrl: String): ReleaseInfo? {
        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val json = JSONObject(response.body?.string() ?: return null)
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

        if (apkUrl.isEmpty()) return null

        val currentVersion = getCurrentVersion()
        return if (isNewer(versionName, currentVersion)) {
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
        // Try direct first, fallback to proxy mirrors
        val downloadUrl = tryDownloadUrl(url)
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = DownloadManager.Request(downloadUrl.toUri())
            .setTitle("Age Android v$versionName")
            .setDescription("正在下载新版本...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "age-v$versionName.apk")
            .setMimeType("application/vnd.android.package-archive")
        return dm.enqueue(request)
    }

    suspend fun awaitApkDownload(downloadId: Long, versionName: String): ApkDownloadResult =
        withContext(Dispatchers.IO) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var result: ApkDownloadResult? = null
            while (result == null) {
                result = queryDownload(dm, downloadId, versionName)
                if (result == null) delay(1000)
            }
            result
        }

    private fun getDownloadedApkFile(versionName: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        return File(dir, "age-v$versionName.apk")
    }

    private fun queryDownload(
        downloadManager: DownloadManager,
        downloadId: Long,
        versionName: String
    ): ApkDownloadResult? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return ApkDownloadResult.Failed("无法查询下载状态")
        cursor.use {
            if (!it.moveToFirst()) {
                return if (getDownloadedApkFile(versionName).exists()) {
                    ApkDownloadResult.Completed(getDownloadedApkFile(versionName))
                } else {
                    ApkDownloadResult.Failed("下载任务不存在")
                }
            }

            val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex < 0) return ApkDownloadResult.Failed("无法读取下载状态")

            return when (it.getInt(statusIndex)) {
                DownloadManager.STATUS_SUCCESSFUL -> ApkDownloadResult.Completed(
                    resolveDownloadedApkFile(it, versionName)
                )
                DownloadManager.STATUS_FAILED -> {
                    val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = if (reasonIndex >= 0) it.getInt(reasonIndex).toString() else "未知原因"
                    ApkDownloadResult.Failed("下载失败: $reason")
                }
                else -> null
            }
        }
    }

    private fun resolveDownloadedApkFile(cursor: android.database.Cursor, versionName: String): File {
        val localUriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
        if (localUriIndex >= 0) {
            val localUri = cursor.getString(localUriIndex)
            val file = localUri
                ?.let { it.toUri() }
                ?.takeIf { it.scheme == "file" }
                ?.path
                ?.let(::File)
            if (file != null) return file
        }
        return getDownloadedApkFile(versionName)
    }

    /** Find a reachable download URL by probing proxies */
    private fun tryDownloadUrl(originalUrl: String): String {
        for (prefix in downloadProxies) {
            val url = "$prefix$originalUrl"
            try {
                val request = okhttp3.Request.Builder()
                    .url(url)
                    .head()
                    .build()
                val response = client.newCall(request).execute()
                if (response.isSuccessful || response.code == 302 || response.code == 301) {
                    Log.d("UpdateChecker", "Download URL reachable: $url")
                    return url
                }
            } catch (_: Exception) {
                Log.w("UpdateChecker", "Download URL unreachable: $url")
            }
        }
        // Fallback: return original
        return originalUrl
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

    fun canInstallDownloadedApks(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun createInstallPermissionSettingsIntent(): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            "package:${context.packageName}".toUri()
        )
    }

    fun openInstallPermissionSettings() {
        val intent = createInstallPermissionSettingsIntent() ?: return
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
