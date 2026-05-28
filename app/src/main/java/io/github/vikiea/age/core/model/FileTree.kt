/*
 * Copyright (c) 2026 vikiea <vikiea@users.noreply.github.com>
 * This code is released under the MIT License.
 * See LICENSE for details.
 */
package io.github.vikiea.age.core.model

data class FileTreeNode(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val children: List<FileTreeNode> = emptyList()
) {
    val fileCount: Int
        get() = if (!isDirectory) 1 else children.sumOf { it.fileCount }
}

fun normalizeRelativePath(path: String, fallback: String = "output"): String {
    val parts = path
        .replace('\\', '/')
        .split('/')
        .map { it.replace('\n', ' ').replace('\r', ' ').trim() }
        .filter { it.isNotEmpty() && it != "." && it != ".." }
    return parts.joinToString("/").ifBlank { fallback }
}

fun buildFileTree(paths: List<String>): List<FileTreeNode> {
    val root = MutableTreeNode("", "")
    paths.forEach { rawPath ->
        val safePath = normalizeRelativePath(rawPath)
        var current = root
        val segments = safePath.split('/')
        segments.forEachIndexed { index, segment ->
            val childPath = if (current.relativePath.isBlank()) segment else "${current.relativePath}/$segment"
            val isLeaf = index == segments.lastIndex
            current = current.children.getOrPut(segment) {
                MutableTreeNode(segment, childPath, isDirectory = !isLeaf)
            }
            if (!isLeaf) current.isDirectory = true
        }
    }
    return root.toFileTreeNodes()
}

private data class MutableTreeNode(
    val name: String,
    val relativePath: String,
    var isDirectory: Boolean = true,
    val children: MutableMap<String, MutableTreeNode> = linkedMapOf()
) {
    fun toFileTreeNodes(): List<FileTreeNode> =
        children.values
            .sortedWith(compareByDescending<MutableTreeNode> { it.isDirectory }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name })
            .map { child ->
                FileTreeNode(
                    name = child.name,
                    relativePath = child.relativePath,
                    isDirectory = child.isDirectory,
                    children = child.toFileTreeNodes()
                )
            }
}
