package com.age.android.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class KeyType {
    AGE_KEY,
    SSH_KEY,
    PASSPHRASE
}

@Entity(tableName = "key_entries")
data class KeyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val publicKey: String,
    val keyType: KeyType,
    val hasPrivateKey: Boolean,
    val keystoreAlias: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
