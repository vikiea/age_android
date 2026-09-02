/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.model

enum class EncryptMode {
    BATCH_PACK,   // Pack selected inputs into one encrypted archive.
    SEPARATE,     // Encrypt each selected input independently.
    DECRYPT
}
