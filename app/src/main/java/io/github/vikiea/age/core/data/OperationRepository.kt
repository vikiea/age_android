/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import io.github.vikiea.age.core.model.OperationRecord
import io.github.vikiea.age.core.model.OperationType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OperationRepository @Inject constructor(
    private val operationDao: OperationDao
) {
    fun getAllOperations(): Flow<List<OperationRecord>> = operationDao.getAllOperations()

    fun getOperationsByType(type: OperationType): Flow<List<OperationRecord>> =
        operationDao.getOperationsByType(type.name)

    suspend fun insertOperation(record: OperationRecord): Long =
        operationDao.insertOperation(record)

    suspend fun updateOperation(record: OperationRecord) =
        operationDao.updateOperation(record)

    suspend fun clearAll() = operationDao.clearAll()

    fun getOperationById(id: Long): Flow<OperationRecord?> = operationDao.getOperationById(id)

    suspend fun deleteOperation(id: Long) = operationDao.deleteOperation(id)
}
