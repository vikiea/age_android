/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.age

interface AgeCancellationToken {
    fun cancel()
    val isCancelled: Boolean
}

interface AgeEngine {
    fun newCancellationToken(): AgeCancellationToken
    suspend fun generateKeyPair(keyType: String = "post-quantum"): Pair<String, String>
    suspend fun recipientForIdentity(identity: String): String
    suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray
    suspend fun encryptWithRecipient(data: ByteArray, recipient: String): ByteArray
    suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray
    suspend fun decryptWithIdentity(data: ByteArray, identity: String): ByteArray
    suspend fun readFile(path: String): ByteArray
    suspend fun writeFile(path: String, data: ByteArray)
    suspend fun encryptStreamToFile(inputPath: String, outputPath: String, passphrase: String, token: AgeCancellationToken? = null)
    suspend fun encryptStreamToFileWithRecipient(inputPath: String, outputPath: String, recipient: String, token: AgeCancellationToken? = null)
    suspend fun decryptStreamToFile(inputPath: String, outputPath: String, passphrase: String, token: AgeCancellationToken? = null)
    suspend fun decryptStreamToFileWithIdentity(inputPath: String, outputPath: String, identity: String, token: AgeCancellationToken? = null)
    suspend fun tarSingleFile(filePath: String, entryName: String, outputPath: String, token: AgeCancellationToken? = null)
    suspend fun tarFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String, token: AgeCancellationToken? = null)
    suspend fun tarGzipFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String, token: AgeCancellationToken? = null)
}
