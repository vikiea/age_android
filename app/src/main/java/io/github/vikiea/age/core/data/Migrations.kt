/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.github.vikiea.age.core.security.PrivateKeyStore

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE key_entries ADD COLUMN privateKey TEXT DEFAULT NULL")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE operation_records ADD COLUMN outputFiles TEXT NOT NULL DEFAULT ''")
    }
}

fun migration3To4(privateKeyStore: PrivateKeyStore) = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val migratedSecrets = mutableMapOf<Long, String>()
        db.query("SELECT id, privateKey FROM key_entries WHERE privateKey IS NOT NULL AND privateKey != ''").use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("id")
            val privateKeyIndex = cursor.getColumnIndexOrThrow("privateKey")
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val privateKey = cursor.getString(privateKeyIndex)
                val secretRef = "age-key-$id"
                privateKeyStore.write(secretRef, privateKey)
                check(privateKeyStore.read(secretRef) == privateKey) { "Private key migration verification failed" }
                migratedSecrets[id] = secretRef
            }
        }

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS key_entries_v4 (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                publicKey TEXT NOT NULL,
                ageKeyType TEXT NOT NULL,
                secretRef TEXT,
                createdAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO key_entries_v4 (id, name, publicKey, ageKeyType, secretRef, createdAt)
            SELECT id, name, publicKey,
                CASE WHEN publicKey LIKE 'age1pq1%' THEN 'POST_QUANTUM' ELSE 'X25519' END,
                NULL, createdAt
            FROM key_entries
            """.trimIndent()
        )
        migratedSecrets.forEach { (id, secretRef) ->
            db.execSQL("UPDATE key_entries_v4 SET secretRef = ? WHERE id = ?", arrayOf<Any>(secretRef, id))
        }
        db.execSQL("DROP TABLE key_entries")
        db.execSQL("ALTER TABLE key_entries_v4 RENAME TO key_entries")

        db.execSQL("ALTER TABLE operation_records ADD COLUMN authMethod TEXT NOT NULL DEFAULT 'UNKNOWN'")
        db.execSQL("ALTER TABLE operation_records ADD COLUMN keyHint TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE operation_records ADD COLUMN compression TEXT NOT NULL DEFAULT 'UNKNOWN'")
        db.execSQL("ALTER TABLE operation_records ADD COLUMN duplicateStrategy TEXT NOT NULL DEFAULT 'UNKNOWN'")
        db.execSQL("ALTER TABLE operation_records ADD COLUMN concurrency INTEGER NOT NULL DEFAULT 1")
    }
}
