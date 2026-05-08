package com.age.engine

import com.age.android.core.age.AgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Implementation of [AgeEngine] that delegates to the native Go age library
 * via Gomobile bindings.
 *
 * The native library must be compiled with:
 *   gomobile bind -target=android -o age-engine.aar ./age-engine/src/main/go
 *
 * The resulting .aar should be placed in app/libs/ and referenced in app/build.gradle.kts.
 */
class AgeEngineImpl : AgeEngine {

    companion object {
        init {
            System.loadLibrary("ageengine")
        }
    }

    override suspend fun generateKeyPair(): Pair<String, String> = withContext(Dispatchers.IO) {
        val result = AgeengineBridge.generateKeyPair()
        Pair(result.publicKey, result.privateKey)
    }

    override suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            AgeengineBridge.encryptWithPassphrase(data, passphrase)
        }

    override suspend fun encryptWithPublicKey(data: ByteArray, publicKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            AgeengineBridge.encryptWithPublicKey(data, publicKey)
        }

    override suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            AgeengineBridge.decryptWithPassphrase(data, passphrase)
        }

    override suspend fun decryptWithPrivateKey(data: ByteArray, privateKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            AgeengineBridge.decryptWithPrivateKey(data, privateKey)
        }

    override suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        AgeengineBridge.readFile(path)
    }

    override suspend fun writeFile(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        AgeengineBridge.writeFile(path, data)
    }
}

/**
 * Gomobile-generated bridge object. This will be available after running:
 *   gomobile bind -target=android -o age-engine.aar ./age-engine/src/main/go
 *
 * Until then, this file will not compile. The bridge exposes the Go functions
 * as static methods on this object.
 */
private object AgeengineBridge {
    @JvmStatic
    external fun generateKeyPair(): KeyPairResult

    @JvmStatic
    external fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray

    @JvmStatic
    external fun encryptWithPublicKey(data: ByteArray, publicKey: String): ByteArray

    @JvmStatic
    external fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray

    @JvmStatic
    external fun decryptWithPrivateKey(data: ByteArray, privateKey: String): ByteArray

    @JvmStatic
    external fun readFile(path: String): ByteArray

    @JvmStatic
    external fun writeFile(path: String, data: ByteArray)
}

/**
 * Data class representing a generated key pair from the native library.
 */
data class KeyPairResult(
    val publicKey: String,
    val privateKey: String
)
