package com.dayscounter.ui.viewmodel

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.NoopAnalyticsProvider
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ExportBackupUseCase
import com.dayscounter.domain.usecase.ImportBackupUseCase
import com.dayscounter.util.NoOpLogger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.IOException
import java.io.OutputStream

/**
 * Unit-тесты для AppDataScreenViewModel.
 * Покрывают ветки открытия потоков через ContentResolver (ошибки → BackupException с текстами).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppDataScreenViewModelTest {
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var contentResolver: ContentResolver
    private lateinit var exportBackupUseCase: ExportBackupUseCase
    private lateinit var viewModel: AppDataScreenViewModel
    private lateinit var uri: Uri

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        contentResolver = mockk<ContentResolver>()
        val application = mockk<Application>()
        every { application.contentResolver } returns contentResolver
        every { application.getString(R.string.backup_data_saved) } returns BACKUP_SAVED_TEXT

        // Репозиторий нужен только для hasItemsFlow (WhileSubscribed) — отдаём пустой поток.
        val repository = mockk<ItemRepository>()
        every { repository.getAllItems() } returns MutableStateFlow(emptyList<Item>())

        exportBackupUseCase = mockk<ExportBackupUseCase>()

        viewModel =
            AppDataScreenViewModel(
                repository = repository,
                context = application,
                exportBackupUseCase = exportBackupUseCase,
                importBackupUseCase = mockk<ImportBackupUseCase>(),
                analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider())),
                logger = NoOpLogger()
            )

        uri = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Подписка на uiState: он собирается через WhileSubscribed, поэтому без подписчика
     * `.value` вернёт initialValue, а не актуальное состояние.
     */
    private fun TestScope.observeUiState() {
        backgroundScope.launch { viewModel.uiState.collect { } }
    }

    @Test
    fun exportBackup_when_open_output_stream_throws_io_exception_then_error() =
        runTest {
            // Given
            observeUiState()
            every { contentResolver.openOutputStream(uri) } throws IOException("Диск заполнен")

            // When
            viewModel.exportBackup(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isError, "Ошибка открытия OutputStream должна помечать состояние как ошибочное")
            assertEquals(
                "Не удалось экспортировать данные: Диск заполнен",
                state.resultMessage,
                "Сообщение должно содержать текст ошибки экспорта и причину"
            )
            assertFalse(state.isExporting, "Индикатор экспорта должен быть снят после ошибки")
        }

    @Test
    fun exportBackup_when_open_output_stream_returns_null_then_error() =
        runTest {
            // Given
            observeUiState()
            every { contentResolver.openOutputStream(uri) } returns null

            // When
            viewModel.exportBackup(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isError, "null вместо OutputStream должен считаться ошибкой")
            assertEquals(
                "Не удалось открыть OutputStream для записи",
                state.resultMessage,
                "Должно быть сообщение о невозможности открыть OutputStream"
            )
            assertFalse(state.isExporting, "Индикатор экспорта должен быть снят после ошибки")
        }

    @Test
    fun exportBackup_when_stream_opened_and_use_case_succeeds_then_success_message() =
        runTest {
            // Given
            observeUiState()
            every { contentResolver.openOutputStream(uri) } returns mockk<OutputStream>()
            coEvery { exportBackupUseCase(any()) } returns Result.success(2)

            // When
            viewModel.exportBackup(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertFalse(state.isError, "Успешный экспорт не должен считаться ошибкой")
            assertFalse(state.isExporting, "Индикатор экспорта должен быть снят после успеха")
            assertEquals(
                BACKUP_SAVED_TEXT,
                state.resultMessage,
                "При успехе сообщение должно прийти из строки backup_data_saved"
            )
        }

    @Test
    fun importBackup_when_open_input_stream_throws_file_not_found_then_error() =
        runTest {
            // Given
            observeUiState()
            every { contentResolver.openInputStream(uri) } throws FileNotFoundException("Файл удалён")

            // When
            viewModel.importBackup(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isError, "FileNotFoundException должен помечать состояние как ошибочное")
            assertEquals(
                "Не удалось найти файл: Файл удалён",
                state.resultMessage,
                "Сообщение должно сообщать, что файл не найден"
            )
            assertFalse(state.isImporting, "Индикатор импорта должен быть снят после ошибки")
        }

    @Test
    fun importBackup_when_open_input_stream_throws_io_exception_then_error() =
        runTest {
            // Given
            observeUiState()
            every { contentResolver.openInputStream(uri) } throws IOException("Файл повреждён")

            // When
            viewModel.importBackup(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isError, "IOException должен помечать состояние как ошибочное")
            assertEquals(
                "Не удалось прочитать файл: Файл повреждён",
                state.resultMessage,
                "Сообщение должно сообщать, что файл не прочитан"
            )
            assertFalse(state.isImporting, "Индикатор импорта должен быть снят после ошибки")
        }

    @Test
    fun importBackup_when_open_input_stream_returns_null_then_error() =
        runTest {
            // Given
            observeUiState()
            every { contentResolver.openInputStream(uri) } returns null

            // When
            viewModel.importBackup(uri)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            val state = viewModel.uiState.value
            assertTrue(state.isError, "null вместо InputStream должен считаться ошибкой")
            assertEquals(
                "Не удалось открыть InputStream для чтения",
                state.resultMessage,
                "Должно быть сообщение о невозможности открыть InputStream"
            )
            assertFalse(state.isImporting, "Индикатор импорта должен быть снят после ошибки")
        }

    companion object {
        private const val BACKUP_SAVED_TEXT = "Backup data saved"
    }
}
