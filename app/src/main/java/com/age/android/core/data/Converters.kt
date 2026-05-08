package com.age.android.core.data

import androidx.room.TypeConverter
import com.age.android.core.model.*

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
