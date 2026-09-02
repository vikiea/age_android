/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.core.model.AgeKeyType
import io.github.vikiea.age.core.security.PrivateKeyStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyRepository @Inject constructor(
    private val keyDao: KeyDao,
    private val privateKeyStore: PrivateKeyStore
) {
    fun getAllKeys(): Flow<List<KeyEntry>> = keyDao.getAllKeys()

    suspend fun getKeyById(id: Long): KeyEntry? = keyDao.getKeyById(id)

    suspend fun savePublicKey(key: KeyEntry): Long = keyDao.insertKey(key.copy(secretRef = null))

    suspend fun saveKey(
        name: String,
        publicKey: String,
        privateKey: String?,
        ageKeyType: AgeKeyType
    ): Long {
        require(keyDao.getKeyByPublicKey(publicKey) == null) { "This key is already in the keyring" }
        val id = keyDao.insertKey(
            KeyEntry(name = name, publicKey = publicKey, ageKeyType = ageKeyType)
        )
        if (privateKey == null) return id

        val secretRef = "age-key-$id"
        try {
            privateKeyStore.write(secretRef, privateKey)
            check(privateKeyStore.read(secretRef) == privateKey) { "Private key verification failed" }
            keyDao.updateKey(
                requireNotNull(keyDao.getKeyById(id)).copy(secretRef = secretRef)
            )
            return id
        } catch (error: Throwable) {
            privateKeyStore.delete(secretRef)
            keyDao.deleteKeyById(id)
            throw error
        }
    }

    suspend fun updateKey(key: KeyEntry) = keyDao.updateKey(key)

    suspend fun deleteKey(id: Long) {
        val key = keyDao.getKeyById(id)
        keyDao.deleteKeyById(id)
        key?.secretRef?.let(privateKeyStore::delete)
    }

    suspend fun importKey(
        name: String,
        publicKey: String,
        privateKey: String?,
        ageKeyType: AgeKeyType
    ): Long = saveKey(name, publicKey, privateKey, ageKeyType)

    suspend fun <T> withPrivateKey(id: Long, block: suspend (String) -> T): T {
        val key = keyDao.getKeyById(id) ?: error("Key not found")
        val secretRef = key.secretRef ?: error("This key does not contain a private key")
        return block(privateKeyStore.read(secretRef))
    }

    suspend fun reconcileSecrets() {
        val keys = keyDao.getAllKeysOnce()
        val referenced = mutableSetOf<String>()
        keys.forEach { key ->
            val ref = key.secretRef ?: return@forEach
            val valid = try {
                privateKeyStore.read(ref).isNotBlank()
            } catch (_: Throwable) {
                false
            }
            if (valid) referenced += ref else keyDao.clearSecretRef(key.id)
        }
        (privateKeyStore.listRefs() - referenced).forEach(privateKeyStore::delete)
    }
}
