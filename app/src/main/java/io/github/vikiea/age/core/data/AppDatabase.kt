/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.core.model.OperationRecord

@Database(entities = [KeyEntry::class, OperationRecord::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun keyDao(): KeyDao
    abstract fun operationDao(): OperationDao
}
