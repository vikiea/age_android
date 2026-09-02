/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AgeKeyType { POST_QUANTUM, X25519 }

@Entity(tableName = "key_entries")
data class KeyEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val publicKey: String,
    val ageKeyType: AgeKeyType,
    val secretRef: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val hasPrivateKey: Boolean get() = secretRef != null
}
