/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.update

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
import io.github.vikiea.age.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class ReleaseInfo(
    val tagName: String,
    val versionName: String,
    val body: String,
    val apkUrl: String,
    val apkSize: Long,
    val sha256: String = ""
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
            throw lastException ?: Exception(context.getString(R.string.update_sources_unavailable))
        }
    }

    private fun fetchReleaseFrom(apiUrl: String): ReleaseInfo? {
        val request = Request.Builder()
            .url(apiUrl)
            .header("Accept", "application/vnd.github.v3+json")
            .build()

        val json = client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Update source returned HTTP ${response.code}")
            }
            JSONObject(response.body?.string() ?: throw IOException("Update source returned an empty response"))
        }
        val tagName = json.getString("tag_name") // e.g. "v1.0.0"
        val versionName = tagName.removePrefix("v")
        val body = json.optString("body", "")

        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        val assets = json.getJSONArray("assets")
        var apkUrl = ""
        var apkSize = 0L
        var apkSha256 = ""
        val apkAssets = (0 until assets.length()).map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk") }
        val selectedAsset = supportedAbis.firstNotNullOfOrNull { abi ->
            apkAssets.firstOrNull { it.optString("name").contains(abi, ignoreCase = true) }
        } ?: apkAssets.firstOrNull { it.optString("name").contains("universal", ignoreCase = true) }
        selectedAsset?.let { asset ->
            apkUrl = asset.getString("browser_download_url")
            apkSize = asset.getLong("size")
            apkSha256 = asset.optString("digest").removePrefix("sha256:").lowercase()
        }

        if (apkUrl.isEmpty()) return null

        val currentVersion = getCurrentVersion()
        return if (isNewer(versionName, currentVersion)) {
            ReleaseInfo(tagName, versionName, body, apkUrl, apkSize, apkSha256)
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
            .setDescription(context.getString(R.string.update_download_description))
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "age-v$versionName.apk")
            .setMimeType("application/vnd.android.package-archive")
        return dm.enqueue(request)
    }

    suspend fun awaitApkDownload(downloadId: Long, versionName: String, expectedSha256: String): ApkDownloadResult =
        withContext(Dispatchers.IO) {
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var result: ApkDownloadResult? = null
            while (result == null) {
                result = queryDownload(dm, downloadId, versionName, expectedSha256)
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
        versionName: String,
        expectedSha256: String
    ): ApkDownloadResult? {
        val query = DownloadManager.Query().setFilterById(downloadId)
        val cursor = downloadManager.query(query) ?: return ApkDownloadResult.Failed(context.getString(R.string.download_query_failed))
        cursor.use {
            if (!it.moveToFirst()) {
                return if (getDownloadedApkFile(versionName).exists()) {
                    verifyDownloadedApk(getDownloadedApkFile(versionName), expectedSha256)
                } else {
                    ApkDownloadResult.Failed(context.getString(R.string.download_missing))
                }
            }

            val statusIndex = it.getColumnIndex(DownloadManager.COLUMN_STATUS)
            if (statusIndex < 0) return ApkDownloadResult.Failed(context.getString(R.string.download_status_unreadable))

            return when (it.getInt(statusIndex)) {
                DownloadManager.STATUS_SUCCESSFUL -> verifyDownloadedApk(
                    resolveDownloadedApkFile(it, versionName), expectedSha256
                )
                DownloadManager.STATUS_FAILED -> {
                    val reasonIndex = it.getColumnIndex(DownloadManager.COLUMN_REASON)
                    val reason = if (reasonIndex >= 0) it.getInt(reasonIndex).toString() else context.getString(R.string.download_unknown_reason)
                    ApkDownloadResult.Failed(context.getString(R.string.download_failed_reason, reason))
                }
                else -> null
            }
        }
    }

    private fun verifyDownloadedApk(apkFile: File, expectedSha256: String): ApkDownloadResult {
        if (!apkFile.isFile) return ApkDownloadResult.Failed(context.getString(R.string.download_package_missing))
        if (expectedSha256.length != 64) {
            apkFile.delete()
            return ApkDownloadResult.Failed(context.getString(R.string.download_digest_missing))
        }
        val actualSha256 = apkFile.inputStream().use { input ->
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        }
        if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
            apkFile.delete()
            return ApkDownloadResult.Failed(context.getString(R.string.download_digest_mismatch))
        }
        if (!hasSameSigningCertificate(apkFile)) {
            apkFile.delete()
            return ApkDownloadResult.Failed(context.getString(R.string.download_signature_mismatch))
        }
        return ApkDownloadResult.Completed(apkFile)
    }

    @Suppress("DEPRECATION")
    private fun hasSameSigningCertificate(apkFile: File): Boolean {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val current = context.packageManager.getPackageInfo(context.packageName, flags)
        val archive = context.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
        fun certificates(info: android.content.pm.PackageInfo): Set<String> {
            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val signingInfo = info.signingInfo ?: return emptySet()
                if (signingInfo.hasMultipleSigners()) signingInfo.apkContentsSigners
                else signingInfo.signingCertificateHistory
            } else {
                info.signatures
            }
            return signatures.orEmpty().mapTo(mutableSetOf()) { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
        }
        return certificates(current).isNotEmpty() && certificates(current) == certificates(archive)
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
