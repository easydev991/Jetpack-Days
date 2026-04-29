package com.dayscounter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.domain.exception.ItemException.DeleteFailed
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.reminder.NoOpReminderManager
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием экрана деталей события.
 */
class DetailScreenViewModel(
    private val repository: ItemRepository,
    private val logger: Logger = AndroidLogger(),
    savedStateHandle: SavedStateHandle,
    private val reminderManager: ReminderManager = NoOpReminderManager,
    private val currentTimeMillisProvider: () -> Long = { System.currentTimeMillis() }
) : ViewModel() {
    companion object {
        fun factory(
            repository: ItemRepository,
            reminderManager: ReminderManager = NoOpReminderManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DetailScreenViewModel(
                        repository = repository,
                        reminderManager = reminderManager,
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
                .filterNotNull()
                .collect { item ->
                    updateSuccessState(item = item)
                }
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
                val message = "Ошибка удаления события: ${e.message}"
                logger.e("DetailScreenViewModel", message, e)
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
