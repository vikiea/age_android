/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vikiea.age.core.security.PrivateKeyStore
import java.io.File
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlinx.coroutines.runBlocking

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private lateinit var context: Context
    private lateinit var databaseFile: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        databaseFile = context.getDatabasePath("migration-test.db")
        context.deleteDatabase(databaseFile.name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(databaseFile.name)
    }

    @Test
    fun migration3To4MovesPrivateKeyOutOfRoom() {
        createVersion3Database()
        val store = InMemoryPrivateKeyStore()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseFile.name)
            .addMigrations(migration3To4(store))
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val key = database.keyDao().getAllKeysOnce().single()
                assertEquals("age-key-1", key.secretRef)
                assertEquals("AGE-SECRET-KEY-1TEST", store.read("age-key-1"))
                assertTrue(key.hasPrivateKey)
            }
        } finally {
            database.close()
        }

        rawDatabase().use { database ->
            val columns = mutableSetOf<String>()
            database.rawQuery("PRAGMA table_info(key_entries)", null).use { cursor ->
                val nameIndex = cursor.getColumnIndexOrThrow("name")
                while (cursor.moveToNext()) columns += cursor.getString(nameIndex)
            }
            assertFalse("privateKey" in columns)
            assertTrue("secretRef" in columns)
        }
    }

    @Test
    fun migration1To4KeepsKeysAndHistory() {
        createVersion1Database()
        val store = InMemoryPrivateKeyStore()

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseFile.name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, migration3To4(store))
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                assertEquals(1, database.keyDao().getAllKeysOnce().size)
                database.query("SELECT authMethod, compression, concurrency FROM operation_records", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("UNKNOWN", cursor.getString(0))
                    assertEquals("UNKNOWN", cursor.getString(1))
                    assertEquals(1, cursor.getInt(2))
                }
            }
        } finally {
            database.close()
        }
    }

    @Test
    fun migrationFailureLeavesVersion3DataIntact() {
        createVersion3Database()
        val failingStore = InMemoryPrivateKeyStore(failWrites = true)

        val failure = runCatching {
            Room.databaseBuilder(context, AppDatabase::class.java, databaseFile.name)
                .addMigrations(migration3To4(failingStore))
                .allowMainThreadQueries()
                .build().openHelper.writableDatabase
        }.exceptionOrNull()
        assertNotNull(failure)

        rawDatabase().use { database ->
            assertEquals(3, database.version)
            database.rawQuery("SELECT privateKey FROM key_entries WHERE id = 1", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals("AGE-SECRET-KEY-1TEST", cursor.getString(0))
            }
        }
    }

    private fun createVersion1Database() {
        rawDatabase().use { db ->
            createKeyTable(db, includePrivateKey = false)
            createOperationTable(db, includeOutputFiles = false)
            db.execSQL("INSERT INTO key_entries VALUES (1, 'legacy', 'age1test', 'AGE_KEY', 0, NULL, 1)")
            db.execSQL("INSERT INTO operation_records VALUES (1, 'ENCRYPT', 'BATCH_PACK', 'a.txt', 'a.age', 'Passphrase', 'SUCCESS', NULL, 1)")
            db.version = 1
        }
    }

    private fun createVersion3Database() {
        rawDatabase().use { db ->
            createKeyTable(db, includePrivateKey = true)
            createOperationTable(db, includeOutputFiles = true)
            db.execSQL("INSERT INTO key_entries VALUES (1, 'legacy', 'age1test', 'AGE-SECRET-KEY-1TEST', 'AGE_KEY', 1, NULL, 1)")
            db.execSQL("INSERT INTO operation_records VALUES (1, 'ENCRYPT', 'BATCH_PACK', 'a.txt', 'a.age', '', 'Passphrase', 'SUCCESS', NULL, 1)")
            db.version = 3
        }
    }

    private fun createKeyTable(db: SQLiteDatabase, includePrivateKey: Boolean) {
        val privateColumn = if (includePrivateKey) "privateKey TEXT," else ""
        db.execSQL(
            "CREATE TABLE key_entries (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, publicKey TEXT NOT NULL, $privateColumn keyType TEXT NOT NULL, hasPrivateKey INTEGER NOT NULL, keystoreAlias TEXT, createdAt INTEGER NOT NULL)"
        )
    }

    private fun createOperationTable(db: SQLiteDatabase, includeOutputFiles: Boolean) {
        val outputColumn = if (includeOutputFiles) "outputFiles TEXT NOT NULL," else ""
        db.execSQL(
            "CREATE TABLE operation_records (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, type TEXT NOT NULL, mode TEXT NOT NULL, inputFiles TEXT NOT NULL, outputPath TEXT NOT NULL, $outputColumn recipientInfo TEXT NOT NULL, status TEXT NOT NULL, errorMessage TEXT, timestamp INTEGER NOT NULL)"
        )
    }

    private fun rawDatabase(): SQLiteDatabase =
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null)

    private class InMemoryPrivateKeyStore(
        private val failWrites: Boolean = false
    ) : PrivateKeyStore {
        private val values = mutableMapOf<String, String>()
        override fun write(secretRef: String, privateKey: String) {
            if (failWrites) error("simulated write failure")
            values[secretRef] = privateKey
        }
        override fun read(secretRef: String): String = values.getValue(secretRef)
        override fun delete(secretRef: String) { values.remove(secretRef) }
        override fun exists(secretRef: String): Boolean = secretRef in values
        override fun listRefs(): Set<String> = values.keys
    }
}
