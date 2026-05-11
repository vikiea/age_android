package com.age.android.feature.encrypt

import com.age.android.core.age.AgeEngine
import com.age.android.core.data.KeyRepository
import com.age.android.core.data.OperationRepository
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.model.EncryptMode
import com.age.android.core.util.FileHelper
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EncryptViewModelTest {
    private lateinit var ageEngine: AgeEngine
    private lateinit var keyRepository: KeyRepository
    private lateinit var operationRepository: OperationRepository
    private lateinit var fileHelper: FileHelper
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var viewModel: EncryptViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ageEngine = mockk()
        keyRepository = mockk { every { getAllKeys() } returns flowOf(emptyList()) }
        operationRepository = mockk { coEvery { insertOperation(any()) } returns 1L; coEvery { updateOperation(any()) } just Runs }
        fileHelper = mockk()
        settingsDataStore = mockk { every { outputDirUri } returns flowOf(null); every { duplicateStrategy } returns flowOf(DuplicateStrategy.RENAME) }
        viewModel = EncryptViewModel(ageEngine, keyRepository, operationRepository, fileHelper, settingsDataStore)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has batch mode`() {
        assertEquals(EncryptMode.BATCH_PACK, viewModel.uiState.value.mode)
    }

    @Test
    fun `setMode updates mode`() {
        viewModel.setMode(EncryptMode.SEPARATE)
        assertEquals(EncryptMode.SEPARATE, viewModel.uiState.value.mode)
    }

    @Test
    fun `setPassphrase updates passphrase`() {
        viewModel.setPassphrase("test123")
        assertEquals("test123", viewModel.uiState.value.passphrase)
    }

    @Test
    fun `startEncrypt with no files shows error`() {
        viewModel.startEncrypt()
        assertEquals("请先选择文件", viewModel.uiState.value.error)
    }
}
