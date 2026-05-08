package com.age.android.core.age

interface AgeEngine {
    suspend fun generateKeyPair(): Pair<String, String>
    suspend fun encryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray
    suspend fun encryptWithPublicKey(data: ByteArray, publicKey: String): ByteArray
    suspend fun decryptWithPassphrase(data: ByteArray, passphrase: String): ByteArray
    suspend fun decryptWithPrivateKey(data: ByteArray, privateKey: String): ByteArray
    suspend fun readFile(path: String): ByteArray
    suspend fun writeFile(path: String, data: ByteArray)
}
