/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.core.model.KeyType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyRepository @Inject constructor(
    private val keyDao: KeyDao
) {
    fun getAllKeys(): Flow<List<KeyEntry>> = keyDao.getAllKeys()

    suspend fun getKeyById(id: Long): KeyEntry? = keyDao.getKeyById(id)

    suspend fun saveKey(key: KeyEntry): Long = keyDao.insertKey(key)

    suspend fun updateKey(key: KeyEntry) = keyDao.updateKey(key)

    suspend fun deleteKey(id: Long) = keyDao.deleteKeyById(id)

    suspend fun importKey(name: String, publicKey: String, privateKey: String?): Long {
        val key = KeyEntry(
            name = name,
            publicKey = publicKey,
            privateKey = privateKey,
            keyType = KeyType.AGE_KEY,
            hasPrivateKey = privateKey != null
        )
        return keyDao.insertKey(key)
    }
}
