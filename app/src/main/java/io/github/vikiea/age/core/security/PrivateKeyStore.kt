/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

interface PrivateKeyStore {
    fun write(secretRef: String, privateKey: String)
    fun read(secretRef: String): String
    fun delete(secretRef: String)
    fun exists(secretRef: String): Boolean
    fun listRefs(): Set<String>
}

@Singleton
class AndroidPrivateKeyStore @Inject constructor(
    @ApplicationContext context: Context
) : PrivateKeyStore {
    private val directory = File(context.noBackupFilesDir, DIRECTORY).apply { mkdirs() }
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    @Synchronized
    override fun write(secretRef: String, privateKey: String) {
        require(validRef(secretRef)) { "Invalid secret reference" }
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        }
        val ciphertext = cipher.doFinal(privateKey.toByteArray(Charsets.UTF_8))
        val atomicFile = AtomicFile(fileFor(secretRef))
        val stream = atomicFile.startWrite()
        try {
            stream.write(FILE_VERSION)
            stream.write(cipher.iv.size)
            stream.write(cipher.iv)
            stream.write(ciphertext)
            atomicFile.finishWrite(stream)
        } catch (error: Throwable) {
            atomicFile.failWrite(stream)
            throw error
        }
    }

    @Synchronized
    override fun read(secretRef: String): String {
        require(validRef(secretRef)) { "Invalid secret reference" }
        val bytes = AtomicFile(fileFor(secretRef)).readFully()
        require(bytes.size > 2 && bytes[0].toInt() == FILE_VERSION) { "Unsupported private key record" }
        val ivLength = bytes[1].toInt() and 0xff
        require(ivLength in 12..32 && bytes.size > 2 + ivLength) { "Invalid private key record" }
        val iv = bytes.copyOfRange(2, 2 + ivLength)
        val ciphertext = bytes.copyOfRange(2 + ivLength, bytes.size)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, iv))
        }
        return cipher.doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    @Synchronized
    override fun delete(secretRef: String) {
        if (validRef(secretRef)) AtomicFile(fileFor(secretRef)).delete()
    }

    override fun exists(secretRef: String): Boolean = validRef(secretRef) && fileFor(secretRef).isFile

    override fun listRefs(): Set<String> =
        directory.listFiles().orEmpty().filter { it.isFile && it.name.endsWith(SUFFIX) }
            .mapTo(mutableSetOf()) { it.name.removeSuffix(SUFFIX) }

    private fun fileFor(secretRef: String) = File(directory, "$secretRef$SUFFIX")

    private fun validRef(secretRef: String): Boolean =
        secretRef.isNotBlank() && secretRef.all { it.isLetterOrDigit() || it == '-' || it == '_' }

    private fun masterKey(): SecretKey =
        (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("Private key protection key is unavailable")

    private fun getOrCreateMasterKey(): SecretKey {
        (keyStore.getKey(MASTER_KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "age_android_private_keys_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val DIRECTORY = "private_keys"
        const val SUFFIX = ".key"
        const val FILE_VERSION = 1
    }
}
