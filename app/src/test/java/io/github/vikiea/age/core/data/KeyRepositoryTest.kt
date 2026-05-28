package io.github.vikiea.age.core.data

import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.core.model.KeyType
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class KeyRepositoryTest {
    private lateinit var keyDao: KeyDao
    private lateinit var repository: KeyRepository

    @Before
    fun setup() {
        keyDao = mockk()
        repository = KeyRepository(keyDao)
    }

    @Test
    fun `getAllKeys returns flow from dao`() = runTest {
        val keys = listOf(
            KeyEntry(id = 1, name = "test", publicKey = "age1test", keyType = KeyType.AGE_KEY, hasPrivateKey = true)
        )
        every { keyDao.getAllKeys() } returns flowOf(keys)

        val result = repository.getAllKeys().first()
        assertEquals(1, result.size)
        assertEquals("test", result[0].name)
    }

    @Test
    fun `saveKey delegates to dao`() = runTest {
        val key = KeyEntry(name = "test", publicKey = "age1test", keyType = KeyType.AGE_KEY, hasPrivateKey = true)
        coEvery { keyDao.insertKey(key) } returns 1L

        val id = repository.saveKey(key)
        assertEquals(1L, id)
        coVerify { keyDao.insertKey(key) }
    }

    @Test
    fun `deleteKey delegates to dao`() = runTest {
        coEvery { keyDao.deleteKeyById(1L) } just Runs

        repository.deleteKey(1L)
        coVerify { keyDao.deleteKeyById(1L) }
    }
}
