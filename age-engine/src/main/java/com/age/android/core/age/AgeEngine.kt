/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.core.age

interface AgeEngine {
    suspend fun generateKeyPair(): Pair<String, String>
    suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray
    suspend fun encryptWithPublicKey(data: ByteArray, publicKey: String): ByteArray
    suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray
    suspend fun decryptWithPrivateKey(data: ByteArray, privateKey: String): ByteArray
    suspend fun readFile(path: String): ByteArray
    suspend fun writeFile(path: String, data: ByteArray)
    suspend fun encryptStreamToFile(inputPath: String, outputPath: String, passphrase: String)
    suspend fun encryptStreamToFileWithKey(inputPath: String, outputPath: String, publicKey: String)
    suspend fun decryptStreamToFile(inputPath: String, outputPath: String, passphrase: String)
    suspend fun decryptStreamToFileWithKey(inputPath: String, outputPath: String, privateKey: String)
    suspend fun tarSingleFile(filePath: String, entryName: String, outputPath: String)
    suspend fun tarFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String)
    suspend fun tarGzipFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String)
}
