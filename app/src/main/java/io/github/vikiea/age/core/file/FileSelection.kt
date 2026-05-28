/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.file

import android.net.Uri

enum class FileSelectionKind {
    FILE,
    FOLDER
}

data class SelectedFileLeaf(
    val uri: Uri,
    val name: String,
    val relativePath: String
)

data class SelectedFileItem(
    val uri: Uri,
    val name: String,
    val relativePath: String,
    val kind: FileSelectionKind,
    val files: List<SelectedFileLeaf>
)
