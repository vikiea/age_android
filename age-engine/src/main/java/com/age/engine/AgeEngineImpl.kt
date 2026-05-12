package com.age.engine

import com.age.android.core.age.AgeEngine
import com.age.engine.ageengine.Ageengine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AgeEngineImpl : AgeEngine {

    override suspend fun generateKeyPair(): Pair<String, String> = withContext(Dispatchers.IO) {
        val result = Ageengine.generateKeyPair()
        Pair(result.publicKey, result.privateKey)
    }

    override suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.encryptWithPassphrase(data, passphrase)
        }

    override suspend fun encryptWithPublicKey(data: ByteArray, publicKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.encryptWithPublicKey(data, publicKey)
        }

    override suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.decryptWithPassphrase(data, passphrase)
        }

    override suspend fun decryptWithPrivateKey(data: ByteArray, privateKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            Ageengine.decryptWithPrivateKey(data, privateKey)
        }

    override suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        Ageengine.readFile(path)
    }

    override suspend fun writeFile(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        Ageengine.writeFile(path, data)
    }

    override suspend fun encryptStreamToFile(inputPath: String, outputPath: String, passphrase: String) = withContext(Dispatchers.IO) {
        Ageengine.encryptStreamToFile(inputPath, outputPath, passphrase)
    }

    override suspend fun encryptStreamToFileWithKey(inputPath: String, outputPath: String, publicKey: String) = withContext(Dispatchers.IO) {
        Ageengine.encryptStreamToFileWithKey(inputPath, outputPath, publicKey)
    }

    override suspend fun decryptStreamToFile(inputPath: String, outputPath: String, passphrase: String) = withContext(Dispatchers.IO) {
        Ageengine.decryptStreamToFile(inputPath, outputPath, passphrase)
    }

    override suspend fun decryptStreamToFileWithKey(inputPath: String, outputPath: String, privateKey: String) = withContext(Dispatchers.IO) {
        Ageengine.decryptStreamToFileWithKey(inputPath, outputPath, privateKey)
    }

    override suspend fun tarSingleFile(filePath: String, entryName: String, outputPath: String) = withContext(Dispatchers.IO) {
        Ageengine.tarSingleFile(filePath, entryName, outputPath)
    }

    override suspend fun tarFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String) = withContext(Dispatchers.IO) {
        Ageengine.tarFilesDelim(filePathsDelim, fileNamesDelim, outputPath)
    }

    override suspend fun tarGzipFilesDelim(filePathsDelim: String, fileNamesDelim: String, outputPath: String) = withContext(Dispatchers.IO) {
        Ageengine.tarGzipFilesDelim(filePathsDelim, fileNamesDelim, outputPath)
    }
}
