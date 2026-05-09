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
}
