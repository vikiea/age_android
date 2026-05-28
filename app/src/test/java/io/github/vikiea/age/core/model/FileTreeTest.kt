package io.github.vikiea.age.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FileTreeTest {
    @Test
    fun `buildFileTree groups relative paths into folders before files`() {
        val tree = buildFileTree(
            listOf(
                "readme.txt",
                "docs/spec/a.txt",
                "docs/spec/b.txt",
                "docs/notes.md"
            )
        )

        assertEquals(listOf("docs", "readme.txt"), tree.map { it.name })
        val docs = tree.first()
        assertTrue(docs.isDirectory)
        assertEquals(3, docs.fileCount)
        assertEquals(listOf("spec", "notes.md"), docs.children.map { it.name })
        assertEquals(listOf("a.txt", "b.txt"), docs.children.first().children.map { it.name })
        assertFalse(tree.last().isDirectory)
    }

    @Test
    fun `normalizeRelativePath removes traversal and empty path segments`() {
        assertEquals("safe/report.txt", normalizeRelativePath("../safe//./report.txt"))
        assertEquals("output", normalizeRelativePath("../../"))
    }
}
