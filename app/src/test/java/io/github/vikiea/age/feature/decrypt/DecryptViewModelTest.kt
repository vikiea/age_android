package io.github.vikiea.age.feature.decrypt

import android.content.Context
import io.github.vikiea.age.R
import io.github.vikiea.age.core.age.AgeEngine
import io.github.vikiea.age.core.age.AgeCancellationToken
import io.github.vikiea.age.core.data.KeyRepository
import io.github.vikiea.age.core.data.OperationRepository
import io.github.vikiea.age.core.data.DuplicateStrategy
import io.github.vikiea.age.core.data.SettingsDataStore
import io.github.vikiea.age.core.file.FileSelectionKind
import io.github.vikiea.age.core.file.SelectedFileItem
import io.github.vikiea.age.core.file.SelectedFileLeaf
import io.github.vikiea.age.core.model.OperationStatus
import io.github.vikiea.age.core.util.FileHelper
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class)
class DecryptViewModelTest {
    private lateinit var viewModel: DecryptViewModel
    private lateinit var ageEngine: AgeEngine
    private lateinit var operationRepository: OperationRepository
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var fileHelper: FileHelper
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        ageEngine = mockk()
        every { ageEngine.newCancellationToken() } returns mockk<AgeCancellationToken> {
            every { isCancelled } returns false
            every { cancel() } just Runs
        }
        val keyRepository = mockk<KeyRepository> { every { getAllKeys() } returns flowOf(emptyList()) }
        operationRepository = mockk {
            coEvery { insertOperation(any()) } returns 1L
            coEvery { updateOperation(any()) } just Runs
        }
        fileHelper = mockk()
        settingsDataStore = mockk {
            every { outputDirUri } returns flowOf(null)
            every { duplicateStrategy } returns flowOf(DuplicateStrategy.RENAME)
            coEvery { getDecryptUsePassphraseOnce() } returns true
            coEvery { getSelectedPrivateKeyIdOnce() } returns null
            coEvery { getConcurrencyOnce() } returns 1
            coEvery { getDuplicateStrategyOnce() } returns DuplicateStrategy.RENAME
            coEvery { setDecryptUsePassphrase(any()) } just Runs
            coEvery { setSelectedPrivateKeyId(any()) } just Runs
        }
        val context = mockk<Context>(relaxed = true) {
            every { getString(R.string.error_select_files) } returns "请先选择文件"
        }
        viewModel = DecryptViewModel(ageEngine, keyRepository, operationRepository, fileHelper, settingsDataStore, context)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has empty files`() {
        assertTrue(viewModel.uiState.value.files.isEmpty())
    }

    @Test
    fun `idle empty state hides action footer`() {
        assertFalse(viewModel.uiState.value.shouldShowActionFooter)
    }

    @Test
    fun `selected files show action footer`() {
        val uri = mockk<android.net.Uri>()
        every { fileHelper.getFileName(uri) } returns "docs.tar.age"

        viewModel.addFiles(listOf(uri))

        assertTrue(viewModel.uiState.value.shouldShowActionFooter)
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

    @Test
    fun `successful decrypt records output files for history detail tree`() = runTest(testDispatcher) {
        val uri = mockk<android.net.Uri>()
        val cacheDir = Files.createTempDirectory("age-decrypt-history-test").toFile()
        val sourceTemp = File(cacheDir, "source.age").apply { writeText("cipher") }
        val decryptedTemp = File(cacheDir, "dec_out.tmp")

        every { fileHelper.getFileName(uri) } returns "docs.tar.age"
        every { fileHelper.getCacheDir() } returns cacheDir
        every { fileHelper.streamUriToTemp(uri, "dec_src", any()) } returns sourceTemp
        every { fileHelper.deleteTempFile(any()) } answers { firstArg<File>().delete(); Unit }
        every { fileHelper.getFallbackDecryptedDir() } returns File(cacheDir, "decrypted")
        every { fileHelper.untarStreaming(any(), any(), any()) } answers {
            thirdArg<(String, java.io.InputStream, Long) -> Unit>()
                .invoke("docs/readme.txt", "hello".byteInputStream(), 5L)
            Unit
        }
        every { fileHelper.writeStreamToDirRelative(any(), "docs/readme.txt", any(), DuplicateStrategy.RENAME, any()) } returns "docs/readme.txt"
        coEvery { ageEngine.decryptStreamToFile(sourceTemp.absolutePath, any(), "pw", any()) } answers {
            decryptedTemp.writeText("tar")
            Unit
        }

        viewModel.addFiles(listOf(uri))
        viewModel.setPassphrase("pw")

        viewModel.startDecrypt()
        advanceUntilIdle()

        coVerify {
            operationRepository.updateOperation(match {
                it.status == OperationStatus.SUCCESS && it.outputFiles == listOf("docs/readme.txt")
            })
        }
    }
}
