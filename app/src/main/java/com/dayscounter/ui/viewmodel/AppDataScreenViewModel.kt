package com.dayscounter.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.AppErrorOperation
import com.dayscounter.data.preferences.ReviewCountersDataStore
import com.dayscounter.di.AppModule
import com.dayscounter.domain.exception.BackupException
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ExportBackupUseCase
import com.dayscounter.domain.usecase.ImportBackupUseCase
import com.dayscounter.review.ReviewSessionHolder
import com.dayscounter.ui.state.AppDataUiState
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.sql.SQLException

private const val TAG = "AppDataScreenViewModel"
private const val STATE_TIMEOUT_MS = 5000L

/**
 * ViewModel для экрана App Data Screen. Управляет резервным копированием и восстановлением данных.
 *
 * @property deps Зависимости ViewModel, сгруппированные для конструктора
 */
class AppDataScreenViewModel(
    private val deps: Deps
) : ViewModel() {
    /**
     * Зависимости [AppDataScreenViewModel], сгруппированные для конструктора.
     */
    data class Deps(
        val repository: ItemRepository,
        val context: Application,
        val exportBackupUseCase: ExportBackupUseCase,
        val importBackupUseCase: ImportBackupUseCase,
        val analyticsService: AnalyticsService,
        val reviewCountersDataStore: ReviewCountersDataStore,
        val logger: Logger = AndroidLogger()
    )

    companion object {
        /**
         * Factory для создания AppDataScreenViewModel. Используется для ручного DI вместо Hilt.
         *
         * @param repository Repository для работы с данными
         * @param application Application контекст
         * @param analyticsService Optional AnalyticsService для логирования
         * @param reviewCountersDataStore Хранилище счётчиков review-триггеров
         * (сбрасывается при полном удалении данных, design.md D3)
         * @return Factory для создания ViewModel
         */
        fun factory(
            repository: ItemRepository,
            application: Application,
            analyticsService: AnalyticsService,
            reviewCountersDataStore: ReviewCountersDataStore
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AppDataScreenViewModel(
                        deps =
                            Deps(
                                repository = repository,
                                context = application,
                                exportBackupUseCase = ExportBackupUseCase(repository),
                                importBackupUseCase = ImportBackupUseCase(repository, logger = AppModule.createLogger()),
                                analyticsService = analyticsService,
                                reviewCountersDataStore = reviewCountersDataStore
                            )
                    )
                }
            }
    }

    /** Flow для отслеживания наличия записей в базе */
    private val hasItemsFlow: StateFlow<Boolean> =
        deps.repository
            .getAllItems()
            .map { items -> items.isNotEmpty() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
                initialValue = false
            )

    /** Мутабельный UI State для обновления состояния (кроме hasItems и showDeleteDialog) */
    private val _uiState = MutableStateFlow(AppDataUiState())

    /** Мутабельный StateFlow для управления показом диалога удаления */
    private val _showDeleteDialog = MutableStateFlow(false)

    /** Flow для управления показом диалога удаления */
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    /** UI State экрана. Обновляется при изменении данных в базе и локального состояния */
    val uiState: StateFlow<AppDataUiState> =
        combine(_uiState, hasItemsFlow) { state, hasItems ->
            state.copy(hasItems = hasItems)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = AppDataUiState()
        )

    /**
     * Создает резервную копию данных в JSON файл.
     * Использует FilePicker для выбора места сохранения.
     *
     * Стрим открывается через contentResolver здесь (UI-слой).
     *
     * @param uri URI файла для сохранения
     */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(isExporting = true, resultMessage = null, isError = false)

                val outputStream =
                    try {
                        deps.context.contentResolver.openOutputStream(uri)
                    } catch (e: IOException) {
                        throw BackupException("Не удалось экспортировать данные: ${e.message}", e)
                    } ?: throw BackupException("Не удалось открыть OutputStream для записи")

                val result = deps.exportBackupUseCase(outputStream)

                result
                    .onSuccess { count ->
                        deps.logger.d(TAG, "Экспорт выполнен успешно, экспортировано $count записей")
                        _uiState.value =
                            _uiState.value.copy(
                                isExporting = false,
                                resultMessage = deps.context.getString(R.string.backup_data_saved),
                                isError = false
                            )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(isExporting = false)
                        reportError("Ошибка при экспорте данных", AppErrorOperation.CREATE_BACKUP, e)
                    }
            } catch (e: BackupException) {
                _uiState.value = _uiState.value.copy(isExporting = false)
                reportError("Ошибка при экспорте данных", AppErrorOperation.CREATE_BACKUP, e)
            }
        }
    }

    /**
     * Восстанавливает данные из JSON файла.
     * Использует FilePicker для выбора файла.
     *
     * Стрим открывается через contentResolver здесь (UI-слой).
     *
     * @param uri URI файла для импорта
     */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value =
                    _uiState.value.copy(isImporting = true, resultMessage = null, isError = false)

                val inputStream =
                    openInputStreamOrNull(uri)
                        ?: throw BackupException("Не удалось открыть InputStream для чтения")

                val result = deps.importBackupUseCase(inputStream)

                result
                    .onSuccess { count ->
                        deps.logger.d(TAG, "Импорт выполнен успешно, импортировано $count записей")
                        _uiState.value =
                            _uiState.value.copy(
                                isImporting = false,
                                resultMessage = deps.context.getString(R.string.data_restored_from_backup),
                                isError = false
                            )
                    }.onFailure { e ->
                        _uiState.value = _uiState.value.copy(isImporting = false)
                        reportError("Ошибка при импорте данных", AppErrorOperation.RESTORE_BACKUP, e)
                    }
            } catch (e: BackupException) {
                _uiState.value = _uiState.value.copy(isImporting = false)
                reportError("Ошибка при импорте данных", AppErrorOperation.RESTORE_BACKUP, e)
            }
        }
    }

    /**
     * Общий хвост обработки ошибки экспорта/импорта: лог, событие аналитики,
     * сообщение пользователю и isError. Снятие busy-флага (isExporting/isImporting)
     * остаётся на месте вызова — это единственное различие между операциями.
     */
    private fun reportError(
        logMessage: String,
        operation: AppErrorOperation,
        e: Throwable
    ) {
        deps.logger.e(TAG, logMessage, e)
        deps.analyticsService.log(AnalyticsEvent.AppError(operation, e))
        _uiState.value =
            _uiState.value.copy(
                resultMessage = e.message ?: deps.context.getString(R.string.error),
                isError = true
            )
    }

    /** Открывает InputStream для импорта; FileNotFoundException/IOException мапятся в BackupException. */
    private fun openInputStreamOrNull(uri: Uri): InputStream? =
        try {
            deps.context.contentResolver.openInputStream(uri)
        } catch (e: FileNotFoundException) {
            throw BackupException("Не удалось найти файл: ${e.message}", e)
        } catch (e: IOException) {
            throw BackupException("Не удалось прочитать файл: ${e.message}", e)
        }

    /**
     * Удаляет все записи из базы данных.
     * Показывает диалог подтверждения перед удалением.
     */
    fun deleteAllData() {
        _showDeleteDialog.value = true
    }

    /**
     * Подтверждает удаление всех данных.
     */
    fun confirmDeleteAllData() {
        viewModelScope.launch {
            try {
                _showDeleteDialog.value = false
                _uiState.value =
                    _uiState.value.copy(isDeleting = true, resultMessage = null, isError = false)

                deps.repository.deleteAllItems()

                // Сброс счётчиков review-триггеров там же, где удаляются записи (design.md D3):
                // после полного удаления данных авто-запрос оценки начинается с нуля.
                // Ошибка сброса не отменяет уже выполненное удаление — логируем и продолжаем.
                try {
                    deps.reviewCountersDataStore.reset()
                } catch (e: IOException) {
                    deps.logger.e(TAG, "Не удалось сбросить счётчики review-триггеров", e)
                }

                // Флаг сессии сбрасывается вместе с триггерами: после полного
                // удаления данных следующая в этой же сессии созданная запись
                // снова может запросить оценку (чек-лист задачи 8.3)
                ReviewSessionHolder.reset()

                deps.logger.d(TAG, "Все данные успешно удалены")
                _uiState.value =
                    _uiState.value.copy(
                        isDeleting = false,
                        resultMessage = deps.context.getString(R.string.all_data_deleted),
                        isError = false
                    )
            } catch (e: SQLException) {
                deps.logger.e(TAG, "Ошибка при удалении данных", e)
                deps.analyticsService.log(AnalyticsEvent.AppError(AppErrorOperation.DELETE_ALL_DATA, e))
                _uiState.value =
                    _uiState.value.copy(
                        isDeleting = false,
                        resultMessage = e.message ?: deps.context.getString(R.string.error),
                        isError = true
                    )
            }
        }
    }

    /**
     * Отменяет удаление всех данных.
     */
    fun cancelDeleteAllData() {
        _showDeleteDialog.value = false
    }

    /**
     * Очищает сообщение о результате операции.
     * Вызывается после отображения Toast.
     */
    fun clearResultMessage() {
        _uiState.value = _uiState.value.copy(resultMessage = null, isError = false)
    }
}
