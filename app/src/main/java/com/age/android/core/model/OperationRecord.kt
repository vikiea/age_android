/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package com.age.android.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.age.android.core.data.Converters

enum class OperationType {
    ENCRYPT,
    DECRYPT
}

@Entity(tableName = "operation_records")
@TypeConverters(Converters::class)
data class OperationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: OperationType,
    val mode: EncryptMode,
    val inputFiles: List<String>,
    val outputPath: String,
    val recipientInfo: String,
    val status: OperationStatus,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
