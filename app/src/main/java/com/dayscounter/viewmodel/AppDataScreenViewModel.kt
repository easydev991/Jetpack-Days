package com.dayscounter.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.R
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.BackupException
import com.dayscounter.domain.usecase.ExportBackupUseCase
import com.dayscounter.domain.usecase.ImportBackupUseCase
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
import java.sql.SQLException

private const val TAG = "AppDataScreenViewModel"
private const val STATE_TIMEOUT_MS = 5000L

/**
 * ViewModel для экрана App Data Screen. Управляет резервным копированием и восстановлением данных.
 *
 * @property repository Repository для работы с данными
 * @property context Контекст приложения
 * @property exportBackupUseCase Use Case для экспорта данных
 * @property importBackupUseCase Use Case для импорта данных
 * @property logger Logger для логирования (по умолчанию AndroidLogger)
 */
class AppDataScreenViewModel(
    private val repository: ItemRepository,
    private val context: Context,
    private val exportBackupUseCase: ExportBackupUseCase,
    private val importBackupUseCase: ImportBackupUseCase,
    private val logger: Logger = AndroidLogger(),
) : ViewModel() {
    companion object {
        /**
         * Factory для создания AppDataScreenViewModel. Используется для ручного DI вместо Hilt.
         *
         * @param repository Repository для работы с данными
         * @param application Application контекст
         * @return Factory для создания ViewModel
         */
        fun factory(
            repository: ItemRepository,
            application: Application,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    AppDataScreenViewModel(
                        repository = repository,
                        context = application,
                        exportBackupUseCase = ExportBackupUseCase(repository, application),
                        importBackupUseCase = ImportBackupUseCase(repository, application),
                    )
                }
            }
    }

    /** Flow для отслеживания наличия записей в базе */
    private val hasItemsFlow: StateFlow<Boolean> =
        repository
            .getAllItems()
            .map { items -> items.isNotEmpty() }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
                initialValue = false,
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
            initialValue = AppDataUiState(),
        )

    /**
     * Создает резервную копию данных в JSON файл.
     * Использует FilePicker для выбора места сохранения.
     *
     * @param uri URI файла для сохранения
     */
    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isExporting = true, resultMessage = null, isError = false)

                val result = exportBackupUseCase(uri)

                result
                    .onSuccess { count ->
                        logger.d(TAG, "Экспорт выполнен успешно, экспортировано $count записей")
                        _uiState.value =
                            _uiState.value.copy(
                                isExporting = false,
                                resultMessage = context.getString(R.string.backup_data_saved),
                                isError = false,
                            )
                    }.onFailure { e ->
                        logger.e(TAG, "Ошибка при экспорте данных", e)
                        _uiState.value =
                            _uiState.value.copy(
                                isExporting = false,
                                resultMessage =
                                    e.message
                                        ?: context.getString(R.string.error),
                                isError = true,
                            )
                    }
            } catch (e: BackupException) {
                logger.e(TAG, "Ошибка при экспорте данных", e)
                _uiState.value =
                    _uiState.value.copy(
                        isExporting = false,
                        resultMessage = e.message ?: context.getString(R.string.error),
                        isError = true,
                    )
            }
        }
    }

    /**
     * Восстанавливает данные из JSON файла.
     * Использует FilePicker для выбора файла.
     *
     * @param uri URI файла для импорта
     */
    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isImporting = true, resultMessage = null, isError = false)

                val result = importBackupUseCase(uri)

                result
                    .onSuccess { count ->
                        logger.d(TAG, "Импорт выполнен успешно, импортировано $count записей")
                        _uiState.value =
                            _uiState.value.copy(
                                isImporting = false,
                                resultMessage = context.getString(R.string.data_restored_from_backup),
                                isError = false,
                            )
                    }.onFailure { e ->
                        logger.e(TAG, "Ошибка при импорте данных", e)
                        _uiState.value =
                            _uiState.value.copy(
                                isImporting = false,
                                resultMessage =
                                    e.message
                                        ?: context.getString(R.string.error),
                                isError = true,
                            )
                    }
            } catch (e: BackupException) {
                logger.e(TAG, "Ошибка при импорте данных", e)
                _uiState.value =
                    _uiState.value.copy(
                        isImporting = false,
                        resultMessage = e.message ?: context.getString(R.string.error),
                        isError = true,
                    )
            }
        }
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
                _uiState.value = _uiState.value.copy(isDeleting = true, resultMessage = null, isError = false)

                repository.deleteAllItems()

                logger.d(TAG, "Все данные успешно удалены")
                _uiState.value =
                    _uiState.value.copy(
                        isDeleting = false,
                        resultMessage = context.getString(R.string.all_data_deleted),
                        isError = false,
                    )
            } catch (e: SQLException) {
                logger.e(TAG, "Ошибка при удалении данных", e)
                _uiState.value =
                    _uiState.value.copy(
                        isDeleting = false,
                        resultMessage = e.message ?: context.getString(R.string.error),
                        isError = true,
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
