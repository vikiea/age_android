/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import androidx.room.TypeConverter
import io.github.vikiea.age.core.model.*

class Converters {
    @TypeConverter
    fun fromStringList(value: List<String>): String = value.joinToString(",")

    @TypeConverter
    fun toStringList(value: String): List<String> = if (value.isEmpty()) emptyList() else value.split(",")

    @TypeConverter
    fun fromEncryptMode(value: EncryptMode): String = value.name

    @TypeConverter
    fun toEncryptMode(value: String): EncryptMode = EncryptMode.valueOf(value)

    @TypeConverter
    fun fromOperationStatus(value: OperationStatus): String = value.name

    @TypeConverter
    fun toOperationStatus(value: String): OperationStatus = OperationStatus.valueOf(value)

    @TypeConverter
    fun fromOperationType(value: OperationType): String = value.name

    @TypeConverter
    fun toOperationType(value: String): OperationType = OperationType.valueOf(value)
}
