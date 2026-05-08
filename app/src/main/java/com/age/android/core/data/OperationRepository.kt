package com.age.android.core.data

import com.age.android.core.model.OperationRecord
import com.age.android.core.model.OperationType
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
}
