package io.github.vikiea.age.feature.keys

import android.content.Context
import io.github.vikiea.age.R
import io.github.vikiea.age.core.age.AgeEngine
import io.github.vikiea.age.core.data.KeyRepository
import io.github.vikiea.age.core.model.AgeKeyType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KeysViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var keyRepository: KeyRepository
    private lateinit var ageEngine: AgeEngine
    private lateinit var viewModel: KeysViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        keyRepository = mockk {
            every { getAllKeys() } returns flowOf(emptyList())
        }
        ageEngine = mockk {
            coEvery { encryptWithRecipient(any(), any()) } returns byteArrayOf()
        }
        viewModel = KeysViewModel(
            context = mockk<Context>(relaxed = true) {
                every { getString(R.string.key_imported) } returns "密钥已导入"
            },
            keyRepository = keyRepository,
            ageEngine = ageEngine
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `importKey reports imported key as transient tip`() = runTest(testDispatcher) {
        coEvery { keyRepository.importKey("work", "age1public", null, AgeKeyType.X25519) } returns 1L

        viewModel.showImportDialog()
        viewModel.setImportName("work")
        viewModel.setImportPublicKey("age1public")
        viewModel.importKey()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.showImportDialog)
        assertEquals("密钥已导入", viewModel.uiState.value.tip)
        assertNull(viewModel.uiState.value.success)
        coVerify { keyRepository.importKey("work", "age1public", null, AgeKeyType.X25519) }
    }
}
