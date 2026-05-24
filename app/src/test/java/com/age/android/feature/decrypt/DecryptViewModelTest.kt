package com.age.android.feature.decrypt

import com.age.android.core.age.AgeEngine
import com.age.android.core.data.KeyRepository
import com.age.android.core.data.OperationRepository
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.file.FileSelectionKind
import com.age.android.core.file.SelectedFileItem
import com.age.android.core.file.SelectedFileLeaf
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
class DecryptViewModelTest {
    private lateinit var viewModel: DecryptViewModel
    private lateinit var fileHelper: FileHelper
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val ageEngine = mockk<AgeEngine>()
        val keyRepository = mockk<KeyRepository> { every { getAllKeys() } returns flowOf(emptyList()) }
        val operationRepository = mockk<OperationRepository> {
            coEvery { insertOperation(any()) } returns 1L
            coEvery { updateOperation(any()) } just Runs
        }
        fileHelper = mockk()
        val settingsDataStore = mockk<SettingsDataStore> {
            every { outputDirUri } returns flowOf(null)
            every { duplicateStrategy } returns flowOf(DuplicateStrategy.RENAME)
            coEvery { getDecryptUsePassphraseOnce() } returns true
            coEvery { getSelectedPrivateKeyOnce() } returns ""
            coEvery { setDecryptUsePassphrase(any()) } just Runs
            coEvery { setSelectedPrivateKey(any()) } just Runs
        }
        viewModel = DecryptViewModel(ageEngine, keyRepository, operationRepository, fileHelper, settingsDataStore)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has empty files`() {
        assertTrue(viewModel.uiState.value.files.isEmpty())
    }

    @Test
    fun `startDecrypt with no files shows error`() {
        viewModel.startDecrypt()
        assertEquals("请先选择文件", viewModel.uiState.value.error)
    }

    @Test
    fun `setPassphrase updates passphrase`() {
        viewModel.setPassphrase("mypass")
        assertEquals("mypass", viewModel.uiState.value.passphrase)
    }

    @Test
    fun `addFilesFromFolder keeps encrypted files under subfolders as one folder unit`() {
        val dirUri = mockk<android.net.Uri>()
        every { fileHelper.listSelectableItemsInDir(dirUri) } returns listOf(
            SelectedFileItem(
                uri = mockk(),
                name = "sealed",
                relativePath = "sealed",
                kind = FileSelectionKind.FOLDER,
                files = listOf(
                    SelectedFileLeaf(
                        uri = mockk(),
                        name = "archive.tar.age",
                        relativePath = "sealed/archive.tar.age"
                    )
                )
            )
        )

        viewModel.addFilesFromFolder(dirUri)

        assertEquals(1, viewModel.uiState.value.files.size)
        assertEquals(FileSelectionKind.FOLDER, viewModel.uiState.value.files.first().kind)
        assertEquals("sealed/archive.tar.age", viewModel.uiState.value.files.first().files.first().relativePath)
    }
}
