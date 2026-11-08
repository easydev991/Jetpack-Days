package com.dayscounter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.data.provider.ResourceIds
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.domain.exception.ItemException.DeleteFailed
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.reminder.NoOpReminderManager
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.CrashReporter
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием экрана деталей события.
 *
 * 7 DI-параметров — следствие wiring resourceProvider; ручной DI без Hilt,
 * сокращение параметров вне рамок текущего change.
 */
@Suppress("LongParameterList")
class DetailScreenViewModel(
    private val repository: ItemRepository,
    private val logger: Logger = AndroidLogger(),
    savedStateHandle: SavedStateHandle,
    private val reminderManager: ReminderManager = NoOpReminderManager,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() },
    private val crashReporter: CrashReporter,
    private val resourceProvider: ResourceProvider
) : ViewModel() {
    companion object {
        fun factory(
            repository: ItemRepository,
            reminderManager: ReminderManager = NoOpReminderManager,
            crashReporter: CrashReporter,
            resourceProvider: ResourceProvider
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DetailScreenViewModel(
                        repository = repository,
                        reminderManager = reminderManager,
                        crashReporter = crashReporter,
                        resourceProvider = resourceProvider,
                        savedStateHandle =
                            checkNotNull(createSavedStateHandle()) {
                                "SavedStateHandle is required"
                            }
                    )
                }
            }
    }

    private val itemId: Long =
        checkNotNull(savedStateHandle["itemId"]) {
            "ItemId parameter is required"
        }

    private val _uiState = MutableStateFlow<DetailScreenState>(DetailScreenState.Loading)
    val uiState: StateFlow<DetailScreenState> = _uiState.asStateFlow()

    private val _showDeleteDialog = MutableStateFlow(false)
    val showDeleteDialog: StateFlow<Boolean> = _showDeleteDialog.asStateFlow()

    init {
        observeItem()
    }

    private fun observeItem() {
        viewModelScope.launch {
            repository
                .getItemFlow(itemId)
                .collect { item ->
                    if (item != null) {
                        updateSuccessState(item = item)
                    } else {
                        updateErrorState()
                    }
                }
        }
    }

    /**
     * Переход в Error только из Loading (guard D2): повторные null-эмиссии
     * после удаления события не затирают ни Success, ни уже установленный Error.
     */
    private fun updateErrorState() {
        if (_uiState.value is DetailScreenState.Loading) {
            _uiState.value =
                DetailScreenState.Error(
                    message = resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND)
                )
            logger.w("DetailScreenViewModel", "Событие не найдено: $itemId")
        }
    }

    fun refreshReminder() {
        val currentState = _uiState.value as? DetailScreenState.Success ?: return

        viewModelScope.launch {
            updateSuccessState(item = currentState.item)
        }
    }

    private suspend fun updateSuccessState(item: Item) {
        val upcomingReminder =
            reminderManager
                .getActiveReminder(itemId)
                ?.takeIf { it.targetEpochMillis > currentTimeMillisProvider() }
        val nextState = DetailScreenState.Success(item = item, reminder = upcomingReminder)
        if (_uiState.value != nextState) {
            _uiState.value = nextState
        }
    }

    fun deleteItem() {
        viewModelScope.launch {
            try {
                val item = repository.getItemById(itemId)
                if (item != null) {
                    reminderManager.clearReminder(itemId)
                    repository.deleteItem(item)
                    logger.d(
                        "DetailScreenViewModel",
                        "Событие удалено: ${item.title} (id=${item.id})"
                    )
                }
            } catch (e: DeleteFailed) {
                // Идентификатор в сообщении — по паттерну репозиториев (design: Goals),
                // чтобы диагностировать сбой без отладчика
                val message = "Ошибка удаления события itemId=$itemId: ${e.message}"
                logger.e("DetailScreenViewModel", message, e)
                // Удаление не сработало, а UI об этом не знает — отчёт обязателен
                crashReporter.logException(e, message)
            }
        }
    }

    fun requestDelete() {
        _showDeleteDialog.value = true
        logger.d("DetailScreenViewModel", "Запрос на удаление")
    }

    fun confirmDelete() {
        deleteItem()
        _showDeleteDialog.value = false
        logger.d("DetailScreenViewModel", "Удаление подтверждено")
    }

    fun cancelDelete() {
        _showDeleteDialog.value = false
        logger.d("DetailScreenViewModel", "Удаление отменено")
    }
}

sealed class DetailScreenState {
    data object Loading : DetailScreenState()

    data class Success(
        val item: Item,
        val reminder: Reminder? = null
    ) : DetailScreenState()

    data class Error(
        val message: String
    ) : DetailScreenState()
}
