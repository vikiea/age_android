package com.age.android.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.age.android.core.model.KeyEntry
import com.age.android.core.model.OperationRecord

@Database(entities = [KeyEntry::class, OperationRecord::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
    abstract fun operationDao(): OperationDao
}
