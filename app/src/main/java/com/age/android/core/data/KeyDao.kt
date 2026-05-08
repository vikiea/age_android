package com.age.android.core.data

import androidx.room.*
import com.age.android.core.model.KeyEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface KeyDao {
    @Query("SELECT * FROM key_entries ORDER BY createdAt DESC")
    fun getAllKeys(): Flow<List<KeyEntry>>

    @Query("SELECT * FROM key_entries WHERE id = :id")
    suspend fun getKeyById(id: Long): KeyEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKey(key: KeyEntry): Long

    @Update
    suspend fun updateKey(key: KeyEntry)

    @Delete
    suspend fun deleteKey(key: KeyEntry)

    @Query("DELETE FROM key_entries WHERE id = :id")
    suspend fun deleteKeyById(id: Long)
}
