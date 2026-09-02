package io.github.vikiea.age.core.data

import io.github.vikiea.age.core.model.KeyEntry
import io.github.vikiea.age.core.model.AgeKeyType
import io.github.vikiea.age.core.security.PrivateKeyStore
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
    private lateinit var privateKeyStore: PrivateKeyStore

    @Before
    fun setup() {
        keyDao = mockk()
        privateKeyStore = mockk(relaxed = true)
        repository = KeyRepository(keyDao, privateKeyStore)
    }

    @Test
    fun `getAllKeys returns flow from dao`() = runTest {
        val keys = listOf(
            KeyEntry(id = 1, name = "test", publicKey = "age1test", ageKeyType = AgeKeyType.X25519, secretRef = "age-key-1")
        )
        every { keyDao.getAllKeys() } returns flowOf(keys)

        val result = repository.getAllKeys().first()
        assertEquals(1, result.size)
        assertEquals("test", result[0].name)
    }

    @Test
    fun `saveKey delegates to dao`() = runTest {
        val key = KeyEntry(name = "test", publicKey = "age1test", ageKeyType = AgeKeyType.X25519)
        coEvery { keyDao.getKeyByPublicKey(key.publicKey) } returns null
        coEvery { keyDao.insertKey(match { it.name == key.name && it.publicKey == key.publicKey && it.ageKeyType == key.ageKeyType }) } returns 1L

        val id = repository.saveKey(key.name, key.publicKey, null, key.ageKeyType)
        assertEquals(1L, id)
        coVerify { keyDao.insertKey(match { it.name == key.name && it.publicKey == key.publicKey && it.ageKeyType == key.ageKeyType }) }
    }

    @Test
    fun `deleteKey delegates to dao`() = runTest {
        coEvery { keyDao.getKeyById(1L) } returns null
        coEvery { keyDao.deleteKeyById(1L) } just Runs

        repository.deleteKey(1L)
        coVerify { keyDao.deleteKeyById(1L) }
    }
}
