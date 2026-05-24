package com.age.android.feature.settings

import android.content.Context
import com.age.android.core.data.DuplicateStrategy
import com.age.android.core.data.SettingsDataStore
import com.age.android.core.data.ThemeAccent
import com.age.android.core.data.ThemeMode
import com.age.android.core.update.ApkDownloadResult
import com.age.android.core.update.ReleaseInfo
import com.age.android.core.update.UpdateChecker
import com.age.android.core.util.FileHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var settingsDataStore: SettingsDataStore
    private lateinit var updateChecker: UpdateChecker
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsDataStore = mockk {
            every { duplicateStrategy } returns flowOf(DuplicateStrategy.RENAME)
            every { outputDirUri } returns flowOf(null)
            every { compressEnabled } returns flowOf(true)
            every { concurrency } returns flowOf(4)
            every { themeMode } returns flowOf(ThemeMode.SYSTEM)
            every { themeAccent } returns flowOf(ThemeAccent.LIQUID_DEFAULT)
            coEvery { setThemeAccent(any()) } just runs
        }
        val fileHelper = mockk<FileHelper>()
        updateChecker = mockk {
            every { getCurrentVersion() } returns "3.0.0"
        }
        val context = mockk<Context>(relaxed = true)
        viewModel = SettingsViewModel(settingsDataStore, fileHelper, updateChecker, context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `downloadUpdate clears downloading state when DownloadManager reports completion`() = runTest(testDispatcher) {
        val release = ReleaseInfo(
            tagName = "v2.2.0",
            versionName = "3.1.0",
            body = "",
            apkUrl = "https://example.com/app.apk",
            apkSize = 123L
        )
        val apkFile = File("build/tmp/test-age-v2.2.0.apk").absoluteFile
        apkFile.parentFile?.mkdirs()
        apkFile.writeText("apk")
        coEvery { updateChecker.checkForUpdate() } returns Result.success(release)
        every { updateChecker.downloadApk(release.apkUrl, release.versionName) } returns 42L
        coEvery { updateChecker.awaitApkDownload(42L, release.versionName) } returns ApkDownloadResult.Completed(apkFile)
        every { updateChecker.installApk(apkFile) } just runs

        viewModel.checkForUpdate()
        advanceUntilIdle()

        viewModel.downloadUpdate()
        assertTrue(viewModel.updateState.value.isDownloading)

        advanceUntilIdle()

        assertFalse(viewModel.updateState.value.isDownloading)
        assertEquals(apkFile.absolutePath, viewModel.updateState.value.downloadedApkPath)
        coVerify { updateChecker.awaitApkDownload(42L, release.versionName) }
        coVerify { updateChecker.installApk(apkFile) }
    }

    @Test
    fun `installDownloadedUpdate installs downloaded apk without starting another download`() = runTest(testDispatcher) {
        val release = ReleaseInfo(
            tagName = "v2.2.0",
            versionName = "3.1.0",
            body = "",
            apkUrl = "https://example.com/app.apk",
            apkSize = 123L
        )
        val apkFile = File("build/tmp/test-age-v3.1.0.apk").absoluteFile
        apkFile.parentFile?.mkdirs()
        apkFile.writeText("apk")
        coEvery { updateChecker.checkForUpdate() } returns Result.success(release)
        every { updateChecker.downloadApk(release.apkUrl, release.versionName) } returns 42L
        coEvery { updateChecker.awaitApkDownload(42L, release.versionName) } returns ApkDownloadResult.Completed(apkFile)
        every { updateChecker.installApk(apkFile) } just runs

        viewModel.checkForUpdate()
        advanceUntilIdle()
        viewModel.downloadUpdate()
        advanceUntilIdle()

        viewModel.installDownloadedUpdate()

        coVerify(exactly = 1) { updateChecker.downloadApk(release.apkUrl, release.versionName) }
        coVerify(exactly = 2) { updateChecker.installApk(apkFile) }
    }

    @Test
    fun `setThemeAccent persists selected accent`() = runTest(testDispatcher) {
        viewModel.setThemeAccent(ThemeAccent.SOLID_BLUE)
        advanceUntilIdle()

        coVerify { settingsDataStore.setThemeAccent(ThemeAccent.SOLID_BLUE) }
    }
}
