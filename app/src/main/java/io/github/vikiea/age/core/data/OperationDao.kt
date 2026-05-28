/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import androidx.room.*
import io.github.vikiea.age.core.model.OperationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface OperationDao {
    @Query("SELECT * FROM operation_records ORDER BY timestamp DESC")
    fun getAllOperations(): Flow<List<OperationRecord>>

    @Query("SELECT * FROM operation_records WHERE type = :type ORDER BY timestamp DESC")
    fun getOperationsByType(type: String): Flow<List<OperationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(record: OperationRecord): Long

    @Update
    suspend fun updateOperation(record: OperationRecord)

    @Query("DELETE FROM operation_records")
    suspend fun clearAll()

    @Query("SELECT * FROM operation_records WHERE id = :id")
    fun getOperationById(id: Long): Flow<OperationRecord?>

    @Query("DELETE FROM operation_records WHERE id = :id")
    suspend fun deleteOperation(id: Long)
}
