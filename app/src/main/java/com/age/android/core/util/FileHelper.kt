/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.core.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class TarEntry(val name: String, val data: ByteArray)

@Singleton
class FileHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    data class DirFile(val uri: Uri, val name: String)

    fun listFilesInDir(dirUri: Uri): List<DirFile> {
        val results = mutableListOf<DirFile>()
        val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return results
        dir.listFiles().forEach { file ->
            if (file.isFile && file.name != null) {
                results.add(DirFile(file.uri, file.name!!))
            }
        }
        return results
    }

    fun readUri(uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: Exception) {
            null
        }
    }

    fun getFileName(uri: Uri): String {
        var name = "unknown"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun getOutputDir(): File {
        val dir = File(context.getExternalFilesDir(null), "age_output")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFallbackEncryptedDir(): File {
        val dir = File(getOutputDir(), "encrypted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getFallbackDecryptedDir(): File {
        val dir = File(getOutputDir(), "decrypted")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getUniqueFile(dir: File, fileName: String): File {
        val baseName = fileName.substringBeforeLast(".", "")
        val ext = if (baseName.isNotEmpty() && fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        var candidate = File(dir, fileName)
        if (!candidate.exists()) return candidate
        var counter = 1
        while (true) {
            candidate = File(dir, "${baseName}_$counter$ext")
            if (!candidate.exists()) return candidate
            counter++
        }
    }

    fun getUniqueDocumentFileName(parentUri: Uri, fileName: String): String {
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return fileName
        val baseName = fileName.substringBeforeLast(".", "")
        val ext = if (baseName.isNotEmpty() && fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        if (parent.findFile(fileName) == null) return fileName
        var counter = 1
        while (true) {
            val candidate = "${baseName}_$counter$ext"
            if (parent.findFile(candidate) == null) return candidate
            counter++
        }
    }

    fun getCustomSubDir(parentUri: Uri, subDirName: String): Uri? {
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return null
        val existing = parent.findFile(subDirName)
        if (existing != null && existing.isDirectory) return existing.uri
        return parent.createDirectory(subDirName)?.uri
    }

    fun writeToDocumentFile(parentUri: Uri, fileName: String, data: ByteArray): Boolean {
        return try {
            val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return false
            val file = parent.createFile("application/octet-stream", fileName) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(data) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSubDirUri(parentUri: Uri, subDirName: String): Uri? {
        val parent = DocumentFile.fromTreeUri(context, parentUri) ?: return null
        val existing = parent.findFile(subDirName)
        if (existing != null && existing.isDirectory) return existing.uri
        return parent.createDirectory(subDirName)?.uri
    }

    fun cleanShareCache() {
        try {
            val shareDir = File(getCacheDir(), "share")
            shareDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    fun readSafFileToCache(dirUri: Uri, fileName: String): File? {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return null
            val docFile = dir.findFile(fileName) ?: return null
            val shareDir = File(getCacheDir(), "share")
            if (!shareDir.exists()) shareDir.mkdirs()
            val target = File(shareDir, fileName)
            context.contentResolver.openInputStream(docFile.uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
            target
        } catch (e: Exception) {
            null
        }
    }

    fun copyFileToSaf(dirUri: Uri, fileName: String, srcFile: File): Boolean {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return false
            dir.findFile(fileName)?.delete()
            val file = dir.createFile("application/octet-stream", fileName) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                srcFile.inputStream().use { input -> input.copyTo(out) }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeToSafFile(dirUri: Uri, fileName: String, data: ByteArray): Boolean {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return false
            // Delete existing file with same name (overwrite strategy)
            dir.findFile(fileName)?.delete()
            val file = dir.createFile("application/octet-stream", fileName) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { it.write(data) }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun writeStreamToSafFile(dirUri: Uri, fileName: String, input: InputStream): Boolean {
        return try {
            val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return false
            dir.findFile(fileName)?.delete()
            val file = dir.createFile("application/octet-stream", fileName) ?: return false
            context.contentResolver.openOutputStream(file.uri)?.use { output ->
                input.copyTo(output, bufferSize = 8192)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUniqueSafFileName(dirUri: Uri, fileName: String): String {
        val dir = DocumentFile.fromTreeUri(context, dirUri) ?: return fileName
        val baseName = fileName.substringBeforeLast(".", "")
        val ext = if (baseName.isNotEmpty() && fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
        if (dir.findFile(fileName) == null) return fileName
        var counter = 1
        while (true) {
            val candidate = "${baseName}_$counter$ext"
            if (dir.findFile(candidate) == null) return candidate
            counter++
        }
    }

    fun getCacheDir(): File {
        val dir = File(context.cacheDir, "age_temp")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Stream tar.gz compression directly from URIs.
     * Reads one file at a time, writes to tar.gz, then frees memory.
     */
    fun tarGzipFromUris(files: List<Pair<Uri, String>>, onFileProcessed: ((Int) -> Unit)? = null): File {
        val tempFile = File(getCacheDir(), "batch_${System.currentTimeMillis()}.tar.gz")
        TarArchiveOutputStream(GZIPOutputStream(BufferedOutputStream(FileOutputStream(tempFile)))).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            for ((index, file) in files.withIndex()) {
                val (uri, name) = file
                val size = getFileSize(uri)
                val tarEntry = TarArchiveEntry(name)
                tarEntry.size = size
                tar.putArchiveEntry(tarEntry)
                copyUriToStream(uri, tar)
                tar.closeArchiveEntry()
                onFileProcessed?.invoke(index + 1)
            }
        }
        return tempFile
    }

    /**
     * Stream tar-only (no compression) from URIs.
     * Much faster than tar.gz since no CPU overhead for compression.
     */
    fun tarFromUris(files: List<Pair<Uri, String>>, onFileProcessed: ((Int) -> Unit)? = null): File {
        val tempFile = File(getCacheDir(), "batch_${System.currentTimeMillis()}.tar")
        TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            for ((index, file) in files.withIndex()) {
                val (uri, name) = file
                val size = getFileSize(uri)
                val tarEntry = TarArchiveEntry(name)
                tarEntry.size = size
                tar.putArchiveEntry(tarEntry)
                copyUriToStream(uri, tar)
                tar.closeArchiveEntry()
                onFileProcessed?.invoke(index + 1)
            }
        }
        return tempFile
    }

    fun tarSingleFile(srcFile: File, entryName: String): File {
        val tempFile = File(getCacheDir(), "single_${System.currentTimeMillis()}.tar")
        TarArchiveOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            val tarEntry = TarArchiveEntry(entryName)
            tarEntry.size = srcFile.length()
            tar.putArchiveEntry(tarEntry)
            srcFile.inputStream().use { input -> input.copyTo(tar, bufferSize = 8192) }
            tar.closeArchiveEntry()
        }
        return tempFile
    }

    fun cleanTempFiles() {
        try {
            val dir = getCacheDir()
            dir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
    }

    private fun copyUriToStream(uri: Uri, output: OutputStream) {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw Exception("无法打开文件")
        pfd.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                input.copyTo(output, bufferSize = 8192)
            }
        }
    }

    fun getFileSize(uri: Uri): Long {
        return try {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else -1L
            } ?: -1L
        } catch (_: Exception) { -1L }
    }

    /**
     * Stream a URI's content to a temp file without loading into memory.
     */
    fun streamUriToTemp(uri: Uri, prefix: String): File {
        val tempFile = File(getCacheDir(), "${prefix}_${System.currentTimeMillis()}.tmp")
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            ?: throw Exception("无法打开文件")
        pfd.use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output, bufferSize = 8192) }
            }
        }
        return tempFile
    }

    /**
     * Stream tar.gz compression: packs multiple files into a tar.gz temp file.
     * Avoids holding all data in memory simultaneously.
     */
    fun tarGzipToTemp(entries: List<TarEntry>): File {
        val tempFile = File(getCacheDir(), "batch_${System.currentTimeMillis()}.tar.gz")
        TarArchiveOutputStream(GZIPOutputStream(BufferedOutputStream(FileOutputStream(tempFile)))).use { tar ->
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX)
            for (entry in entries) {
                val tarEntry = TarArchiveEntry(entry.name)
                tarEntry.size = entry.data.size.toLong()
                tar.putArchiveEntry(tarEntry)
                tar.write(entry.data)
                tar.closeArchiveEntry()
            }
        }
        return tempFile
    }

    /**
     * Stream gzip compression for a single file to a temp file.
     */
    fun gzipToTemp(data: ByteArray): File {
        val tempFile = File(getCacheDir(), "gzip_${System.currentTimeMillis()}.gz")
        GZIPOutputStream(BufferedOutputStream(FileOutputStream(tempFile))).use { gz ->
            gz.write(data)
        }
        return tempFile
    }

    /**
     * Stream gunzip decompression from a temp file.
     */
    fun gunzipFromTemp(tempFile: File): ByteArray {
        return GZIPInputStream(BufferedInputStream(FileInputStream(tempFile))).use { it.readBytes() }
    }

    fun gunzipToTemp(srcFile: File): File {
        val tempFile = File(getCacheDir(), "gunzip_${System.currentTimeMillis()}.tmp")
        GZIPInputStream(BufferedInputStream(FileInputStream(srcFile))).use { input ->
            tempFile.outputStream().use { output -> input.copyTo(output, bufferSize = 8192) }
        }
        return tempFile
    }

    /**
     * Stream untar+gunzip: extracts files from a tar.gz temp file.
     * Returns list of (fileName, fileData).
     */
    fun untarGzipFromTemp(tempFile: File): List<TarEntry> {
        val results = mutableListOf<TarEntry>()
        TarArchiveInputStream(GZIPInputStream(BufferedInputStream(FileInputStream(tempFile)))).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val data = tar.readBytes()
                    results.add(TarEntry(entry.name, data))
                }
                entry = tar.nextTarEntry
            }
        }
        return results
    }

    fun untarGzipStreaming(tempFile: File, onEntry: (String, InputStream, Long) -> Unit) {
        TarArchiveInputStream(GZIPInputStream(BufferedInputStream(FileInputStream(tempFile)))).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    onEntry(entry.name, tar, entry.size)
                }
                entry = tar.nextTarEntry
            }
        }
    }

    fun untarStreaming(tempFile: File, onEntry: (String, InputStream, Long) -> Unit) {
        TarArchiveInputStream(BufferedInputStream(FileInputStream(tempFile))).use { tar ->
            var entry = tar.nextTarEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    onEntry(entry.name, tar, entry.size)
                }
                entry = tar.nextTarEntry
            }
        }
    }

    /**
     * Read a temp file into ByteArray (for passing to age engine).
     */
    fun readTempFile(tempFile: File): ByteArray {
        return tempFile.readBytes()
    }

    /**
     * Clean up temp files.
     */
    fun deleteTempFile(tempFile: File) {
        try { tempFile.delete() } catch (_: Exception) {}
    }

    fun shareFile(file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/octet-stream"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "发送文件").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {}
    }

    fun shareDir(dir: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dir)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "resource/folder"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "发送文件夹").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {}
    }

    fun shareFiles(files: List<File>) {
        try {
            if (files.isEmpty()) return
            val cacheShareDir = File(getCacheDir(), "share")
            if (!cacheShareDir.exists()) cacheShareDir.mkdirs()
            val cachedFiles = files.map { file ->
                if (file.parentFile?.absolutePath == cacheShareDir.absolutePath) {
                    file // Already in share dir
                } else {
                    val cached = File(cacheShareDir, file.name)
                    file.copyTo(cached, overwrite = true)
                    cached
                }
            }
            if (cachedFiles.size == 1) {
                shareFile(cachedFiles[0])
                return
            }
            val uris = cachedFiles.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) }
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/octet-stream"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "发送文件").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (_: Exception) {}
    }

    fun openFileManager(dir: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", dir)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
            intent.setDataAndType(uri, "*/*")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
            context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {}
        }
    }

    fun resolveUriToPath(uriString: String?): String? {
        if (uriString == null) return null
        return try {
            val uri = Uri.parse(uriString)
            val treeDocId = android.provider.DocumentsContract.getTreeDocumentId(uri)
            val path = treeDocId.substringAfter(":", treeDocId)
            "/storage/emulated/0/$path"
        } catch (_: Exception) {
            uriString
        }
    }

    fun openFileManagerAtCustomUri(uri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
            intent.setDataAndType(uri, "*/*")
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return
            }
            context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {}
        }
    }
}
