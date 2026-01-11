package com.dayscounter.viewmodel

import android.app.Application
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme
import com.dayscounter.domain.usecase.IconManager
import com.dayscounter.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для ThemeIconViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@DisplayName("Тесты для ThemeIconViewModel")
class ThemeIconViewModelTest {
    private lateinit var mockDataStore: AppSettingsDataStore
    private lateinit var mockIconManager: IconManager
    private lateinit var mockLogger: Logger
    private lateinit var mockApplication: Application
    private lateinit var viewModel: ThemeIconViewModel
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        mockDataStore = mockk(relaxed = true)
        mockIconManager = mockk()
        mockLogger = mockk(relaxed = true)
        mockApplication = mockk(relaxed = true)
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    @DisplayName("updateTheme должен сохранять тему в DataStore")
    fun updateTheme_shouldSaveThemeToDataStore() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            viewModel.updateTheme(AppTheme.LIGHT)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setTheme(AppTheme.LIGHT) }
        }

    @Test
    @DisplayName("updateTheme должен обновлять все темы")
    fun updateTheme_shouldUpdateAllThemes() =
        runTest(testDispatcher) {
            // Given
            val themes =
                listOf(
                    AppTheme.LIGHT,
                    AppTheme.DARK,
                    AppTheme.SYSTEM,
                )

            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            themes.forEach { theme ->
                viewModel.updateTheme(theme)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            themes.forEach { theme ->
                coVerify { mockDataStore.setTheme(theme) }
            }
        }

    @Test
    @DisplayName("updateIcon должен сохранять иконку в DataStore и вызывать IconManager")
    fun updateIcon_shouldSaveIconToDataStoreAndCallIconManager() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just runs
            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            viewModel.updateIcon(AppIcon.ICON_2)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setIcon(AppIcon.ICON_2) }
            verify(atLeast = 1) { mockIconManager.changeIcon(AppIcon.ICON_2) }
        }

    @Test
    @DisplayName("updateIcon должен обновлять все иконки")
    fun updateIcon_shouldUpdateAllIcons() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just runs
            val icons =
                listOf(
                    AppIcon.DEFAULT,
                    AppIcon.ICON_2,
                    AppIcon.ICON_3,
                    AppIcon.ICON_4,
                    AppIcon.ICON_5,
                    AppIcon.ICON_6,
                )

            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            icons.forEach { icon ->
                viewModel.updateIcon(icon)
            }
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            icons.forEach { icon ->
                coVerify { mockDataStore.setIcon(icon) }
                verify(atLeast = 1) { mockIconManager.changeIcon(icon) }
            }
        }

    @Test
    @DisplayName("updateTheme должен вызывать setTheme для LIGHT темы")
    fun updateTheme_shouldCallSetThemeForLight() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            viewModel.updateTheme(AppTheme.LIGHT)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockDataStore.setTheme(AppTheme.LIGHT) }
        }

    @Test
    @DisplayName("updateTheme должен вызывать setTheme для DARK темы")
    fun updateTheme_shouldCallSetThemeForDark() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            viewModel.updateTheme(AppTheme.DARK)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockDataStore.setTheme(AppTheme.DARK) }
        }

    @Test
    @DisplayName("updateTheme должен вызывать setTheme для SYSTEM темы")
    fun updateTheme_shouldCallSetThemeForSystem() =
        runTest(testDispatcher) {
            // Given
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            viewModel.updateTheme(AppTheme.SYSTEM)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { mockDataStore.setTheme(AppTheme.SYSTEM) }
        }

    @Test
    @DisplayName("updateTheme и updateIcon должны корректно работать")
    fun updateMethods_shouldWorkCorrectly() =
        runTest(testDispatcher) {
            // Given
            every { mockIconManager.changeIcon(any()) } just runs
            coEvery { mockDataStore.setTheme(any()) } coAnswers { }
            coEvery { mockDataStore.setIcon(any()) } coAnswers { }
            coEvery { mockDataStore.theme } returns flowOf(AppTheme.SYSTEM)
            coEvery { mockDataStore.icon } returns flowOf(AppIcon.DEFAULT)

            // When
            viewModel = ThemeIconViewModel(mockDataStore, mockIconManager, mockLogger)
            viewModel.updateTheme(AppTheme.LIGHT)
            viewModel.updateIcon(AppIcon.ICON_3)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            coVerify { mockDataStore.setTheme(AppTheme.LIGHT) }
            coVerify { mockDataStore.setIcon(AppIcon.ICON_3) }
            verify(atLeast = 1) { mockIconManager.changeIcon(AppIcon.ICON_3) }
        }
}
