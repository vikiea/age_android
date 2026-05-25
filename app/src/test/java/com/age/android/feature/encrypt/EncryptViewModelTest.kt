package com.age.android.feature.encrypt

import com.age.android.core.age.AgeEngine
import com.age.android.core.data.KeyRepository
import com.age.android.core.data.OperationRepository
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.file.FileSelectionKind
import com.age.android.core.file.SelectedFileItem
import com.age.android.core.file.SelectedFileLeaf
import com.age.android.core.model.EncryptMode
import com.age.android.core.model.OperationStatus
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
import java.io.File
import java.nio.file.Files

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
        settingsDataStore = mockk {
            every { outputDirUri } returns flowOf(null)
            every { duplicateStrategy } returns flowOf(DuplicateStrategy.RENAME)
            every { compressEnabled } returns flowOf(true)
            coEvery { getEncryptModeOnce() } returns null
            coEvery { getEncryptUsePassphraseOnce() } returns true
            coEvery { getSelectedPublicKeyOnce() } returns ""
            coEvery { getCompressEnabledOnce() } returns true
            coEvery { getConcurrencyOnce() } returns 2
            coEvery { getDuplicateStrategyOnce() } returns DuplicateStrategy.RENAME
            coEvery { setEncryptMode(any()) } just Runs
            coEvery { setCompressEnabled(any()) } just Runs
            coEvery { setEncryptUsePassphrase(any()) } just Runs
            coEvery { setSelectedPublicKey(any()) } just Runs
        }
        viewModel = EncryptViewModel(ageEngine, keyRepository, operationRepository, fileHelper, settingsDataStore)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `initial state has batch mode`() {
        assertEquals(EncryptMode.BATCH_PACK, viewModel.uiState.value.mode)
    }

    @Test
    fun `idle empty state hides action footer`() {
        assertFalse(viewModel.uiState.value.shouldShowActionFooter)
    }

    @Test
    fun `selected files show action footer`() {
        val uri = mockk<android.net.Uri>()
        every { fileHelper.getFileName(uri) } returns "plain.txt"

        viewModel.addFiles(listOf(uri))

        assertTrue(viewModel.uiState.value.shouldShowActionFooter)
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

    @Test
    fun `addFilesFromFolder keeps top level subfolder as independent item`() {
        val dirUri = mockk<android.net.Uri>()
        val folderUri = mockk<android.net.Uri>()
        every { fileHelper.listSelectableItemsInDir(dirUri) } returns listOf(
            SelectedFileItem(
                uri = folderUri,
                name = "docs",
                relativePath = "docs",
                kind = FileSelectionKind.FOLDER,
                files = listOf(
                    SelectedFileLeaf(
                        uri = mockk(),
                        name = "a.txt",
                        relativePath = "docs/spec/a.txt"
                    )
                )
            )
        )

        viewModel.addFilesFromFolder(dirUri)

        assertEquals(1, viewModel.uiState.value.files.size)
        assertEquals(FileSelectionKind.FOLDER, viewModel.uiState.value.files.first().kind)
        assertEquals("docs/spec/a.txt", viewModel.uiState.value.files.first().files.first().relativePath)
    }

    @Test
    fun `separate encryption packs selected folder into one tar output`() = runTest(testDispatcher) {
        val dirUri = mockk<android.net.Uri>()
        val folderUri = mockk<android.net.Uri>()
        val firstUri = mockk<android.net.Uri>()
        val secondUri = mockk<android.net.Uri>()
        val cacheDir = Files.createTempDirectory("age-folder-unit-test").toFile()
        val firstTemp = File(cacheDir, "first.tmp").apply { writeText("a") }
        val secondTemp = File(cacheDir, "second.tmp").apply { writeText("b") }

        every { fileHelper.listSelectableItemsInDir(dirUri) } returns listOf(
            SelectedFileItem(
                uri = folderUri,
                name = "docs",
                relativePath = "docs",
                kind = FileSelectionKind.FOLDER,
                files = listOf(
                    SelectedFileLeaf(firstUri, "a.txt", "docs/a.txt"),
                    SelectedFileLeaf(secondUri, "b.txt", "docs/spec/b.txt")
                )
            )
        )
        every { fileHelper.getCacheDir() } returns cacheDir
        every { fileHelper.streamUriToTemp(firstUri, "src") } returns firstTemp
        every { fileHelper.streamUriToTemp(secondUri, "src") } returns secondTemp
        every { fileHelper.deleteTempFile(any()) } answers { firstArg<File>().delete(); Unit }
        every { fileHelper.getFallbackEncryptedDir() } returns File(cacheDir, "encrypted")
        every { fileHelper.copyFileToDirRelative(any(), "docs.tar.age", any(), DuplicateStrategy.RENAME) } returns "docs.tar.age"
        coEvery { ageEngine.tarFilesDelim(any(), any(), any()) } just Runs
        coEvery { ageEngine.encryptStreamToFile(any(), any(), "pw") } just Runs
        coEvery { ageEngine.tarSingleFile(any(), any(), any()) } just Runs

        viewModel.addFilesFromFolder(dirUri)
        viewModel.setMode(EncryptMode.SEPARATE)
        viewModel.setPassphrase("pw")

        viewModel.startEncrypt()
        advanceUntilIdle()

        assertEquals(listOf("docs.tar.age"), viewModel.uiState.value.outputFiles)
        coVerify(exactly = 1) {
            ageEngine.tarFilesDelim(
                "${firstTemp.absolutePath}\n${secondTemp.absolutePath}",
                "docs/a.txt\ndocs/spec/b.txt",
                any()
            )
        }
        coVerify(exactly = 0) { ageEngine.tarSingleFile(any(), any(), any()) }
    }

    @Test
    fun `successful encryption records output files for history detail tree`() = runTest(testDispatcher) {
        val uri = mockk<android.net.Uri>()
        val cacheDir = Files.createTempDirectory("age-history-output-test").toFile()
        val sourceTemp = File(cacheDir, "source.tmp").apply { writeText("plain") }

        every { fileHelper.getFileName(uri) } returns "plain.txt"
        every { fileHelper.getCacheDir() } returns cacheDir
        every { fileHelper.streamUriToTemp(uri, "src_0") } returns sourceTemp
        every { fileHelper.deleteTempFile(any()) } answers { firstArg<File>().delete(); Unit }
        every { fileHelper.getFallbackEncryptedDir() } returns File(cacheDir, "encrypted")
        every { fileHelper.copyFileToDirRelative(any(), "archive.tar.gz.age", any(), DuplicateStrategy.RENAME) } returns "archive.tar.gz.age"
        coEvery { ageEngine.tarGzipFilesDelim(any(), any(), any()) } just Runs
        coEvery { ageEngine.encryptStreamToFile(any(), any(), "pw") } just Runs

        viewModel.addFiles(listOf(uri))
        viewModel.setPassphrase("pw")

        viewModel.startEncrypt()
        advanceUntilIdle()

        coVerify {
            operationRepository.updateOperation(match {
                it.status == OperationStatus.SUCCESS && it.outputFiles == listOf("archive.tar.gz.age")
            })
        }
    }
}
