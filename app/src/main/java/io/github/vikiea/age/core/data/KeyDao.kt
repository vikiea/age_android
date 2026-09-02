/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import androidx.room.*
import io.github.vikiea.age.core.model.KeyEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM key_entries ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<KeyEntry>>

    @Query("SELECT * FROM key_entries WHERE id = :id")
    suspend fun getKeyById(id: Long): KeyEntry?

    @Query("SELECT * FROM key_entries")
    suspend fun getAllKeysOnce(): List<KeyEntry>

    @Query("SELECT * FROM key_entries WHERE publicKey = :publicKey LIMIT 1")
    suspend fun getKeyByPublicKey(publicKey: String): KeyEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntry): Long

    @Update
    suspend fun updateKey(key: KeyEntry)

    @Delete
    suspend fun deleteKey(key: KeyEntry)

    @Query("DELETE FROM key_entries WHERE id = :id")
    suspend fun deleteKeyById(id: Long)

    @Query("UPDATE key_entries SET secretRef = NULL WHERE id = :id")
    suspend fun clearSecretRef(id: Long)
}
