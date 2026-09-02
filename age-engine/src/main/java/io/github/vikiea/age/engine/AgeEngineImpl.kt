/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.engine

import io.github.vikiea.age.core.age.AgeEngine
import io.github.vikiea.age.core.age.AgeCancellationToken
import io.github.vikiea.age.engine.ageengine.Ageengine
import io.github.vikiea.age.engine.ageengine.CancelToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgeEngineImpl : AgeEngine {

    private class GoCancellationToken(val delegate: CancelToken) : AgeCancellationToken {
        override fun cancel() = delegate.cancel()
        override val isCancelled: Boolean get() = delegate.isCancelled
    }

    override fun newCancellationToken(): AgeCancellationToken =
        GoCancellationToken(Ageengine.newCancelToken())

    private fun AgeCancellationToken?.goToken(): CancelToken? =
        (this as? GoCancellationToken)?.delegate

    override suspend fun generateKeyPair(keyType: String): Pair<String, String> = withContext(Dispatchers.IO) {
        val result = Ageengine.generateKeyPair(keyType)
        Pair(result.publicKey, result.privateKey)
    }

    override suspend fun recipientForIdentity(identity: String): String = withContext(Dispatchers.IO) {
        Ageengine.recipientForIdentity(identity)
    }

    override suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.encryptWithPassphrase(data, passphrase)
        }

    override suspend fun encryptWithRecipient(data: ByteArray, recipient: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.encryptWithRecipient(data, recipient)
        }

    override suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.decryptWithPassphrase(data, passphrase)
        }

    override suspend fun decryptWithIdentity(data: ByteArray, identity: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.decryptWithIdentity(data, identity)
        }

    override suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        Ageengine.readFile(path)
    }

    override suspend fun writeFile(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        Ageengine.writeFile(path, data)
    }

    override suspend fun encryptStreamToFile(inputPath: String, outputPath: String, passphrase: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.encryptStreamToFile(inputPath, outputPath, passphrase, token.goToken())
    }

    override suspend fun encryptStreamToFileWithRecipient(inputPath: String, outputPath: String, recipient: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.encryptStreamToFileWithRecipient(inputPath, outputPath, recipient, token.goToken())
    }

    override suspend fun decryptStreamToFile(inputPath: String, outputPath: String, passphrase: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.decryptStreamToFile(inputPath, outputPath, passphrase, token.goToken())
    }

    override suspend fun decryptStreamToFileWithIdentity(inputPath: String, outputPath: String, identity: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.decryptStreamToFileWithIdentity(inputPath, outputPath, identity, token.goToken())
    }

    override suspend fun tarSingleFile(filePath: String, entryName: String, outputPath: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.tarSingleFile(filePath, entryName, outputPath, token.goToken())
    }

    override suspend fun tarFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.tarFilesDelim(filePathsDelim, fileNamesDelim, outputPath, token.goToken())
    }

    override suspend fun tarGzipFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String, token: AgeCancellationToken?) = withContext(Dispatchers.IO) {
        Ageengine.tarGzipFilesDelim(filePathsDelim, fileNamesDelim, outputPath, token.goToken())
    }
}
