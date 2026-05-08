package com.age.engine

import com.age.android.core.age.AgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Placeholder implementation of [AgeEngine] that delegates to the native Go age library
 * via Gomobile bindings.
 *
 * **This class will not work until the gomobile .aar is compiled.** The gomobile tool
 * generates a Java/Kotlin bridge class (`Ageengine`) from the Go package, which should
 * be used in place of these stubs once available.
 *
 * Build the native library with:
 *   gomobile bind -target=android -o age.aar -javapkg=com.age.engine ./age-engine/src/main/go/
 *
 * Then place the resulting .aar in app/libs/ and reference it in app/build.gradle.kts.
 */
class AgeEngineImpl : AgeEngine {

    private companion object {
        private const val NOT_COMPILED_MSG =
            "Age engine not compiled yet. Run: gomobile bind -target=android -o age.aar -javapkg=com.age.engine ./age-engine/src/main/go/"
    }

    override suspend fun generateKeyPair(): Pair<String, String> = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException(NOT_COMPILED_MSG)
    }

    override suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(NOT_COMPILED_MSG)
        }

    override suspend fun encryptWithPublicKey(data: ByteArray, publicKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(NOT_COMPILED_MSG)
        }

    override suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(NOT_COMPILED_MSG)
        }

    override suspend fun decryptWithPrivateKey(data: ByteArray, privateKey: String): ByteArray =
        withContext(Dispatchers.IO) {
            throw UnsupportedOperationException(NOT_COMPILED_MSG)
        }

    override suspend fun readFile(path: String): ByteArray = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException(NOT_COMPILED_MSG)
    }

    override suspend fun writeFile(path: String, data: ByteArray) = withContext(Dispatchers.IO) {
        throw UnsupportedOperationException(NOT_COMPILED_MSG)
    }
}
