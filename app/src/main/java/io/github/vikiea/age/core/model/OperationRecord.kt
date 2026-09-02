/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import io.github.vikiea.age.core.data.Converters

enum class OperationType {
    ENCRYPT,
    DECRYPT
}

enum class OperationAuthMethod { UNKNOWN, PASSPHRASE, RECIPIENT, IDENTITY }

enum class CompressionState { UNKNOWN, ENABLED, DISABLED }

enum class RecordedDuplicateStrategy { UNKNOWN, RENAME, OVERWRITE }

@Entity(tableName = "operation_records")
@TypeConverters(Converters::class)
data class OperationRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: OperationType,
    val mode: EncryptMode,
    val inputFiles: List<String>,
    val outputPath: String,
    val outputFiles: List<String> = emptyList(),
    val recipientInfo: String,
    val authMethod: OperationAuthMethod = OperationAuthMethod.UNKNOWN,
    val keyHint: String = "",
    val compression: CompressionState = CompressionState.UNKNOWN,
    val duplicateStrategy: RecordedDuplicateStrategy = RecordedDuplicateStrategy.UNKNOWN,
    val concurrency: Int = 1,
    val status: OperationStatus,
    val errorMessage: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
