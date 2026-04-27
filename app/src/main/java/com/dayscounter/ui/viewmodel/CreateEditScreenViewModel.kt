package com.dayscounter.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.AppErrorOperation
import com.dayscounter.data.provider.ResourceIds
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.domain.exception.ItemException
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ReminderRequest
import com.dayscounter.reminder.NoOpReminderManager
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.ui.screens.createedit.toChangeFingerprint
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием экрана создания/редактирования события.
 */
class CreateEditScreenViewModel(
    private val repository: ItemRepository,
    private val resourceProvider: ResourceProvider,
    private val logger: Logger = AndroidLogger(),
    savedStateHandle: SavedStateHandle,
    private val analyticsService: AnalyticsService,
    private val reminderManager: ReminderManager = NoOpReminderManager
) : ViewModel() {
    companion object {
        fun factory(
            repository: ItemRepository,
            resourceProvider: ResourceProvider,
            analyticsService: AnalyticsService,
            reminderManager: ReminderManager = NoOpReminderManager
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CreateEditScreenViewModel(
                        repository = repository,
                        resourceProvider = resourceProvider,
                        savedStateHandle =
                            checkNotNull(createSavedStateHandle()) {
                                "SavedStateHandle is required"
                            },
                        analyticsService = analyticsService,
                        reminderManager = reminderManager
                    )
                }
            }
    }

    private val itemId: Long? = savedStateHandle["itemId"]

    private val _uiState = MutableStateFlow<CreateEditScreenState>(CreateEditScreenState.Loading)
    val uiState: StateFlow<CreateEditScreenState> = _uiState.asStateFlow()

    private val _originalItem = MutableStateFlow<Item?>(null)
    val originalItem: StateFlow<Item?> = _originalItem.asStateFlow()

    private val originalReminderFingerprint = MutableStateFlow<String?>(null)

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    init {
        if (itemId != null) {
            loadItem()
        } else {
            _uiState.value =
                CreateEditScreenState.Success(
                    item =
                        Item(
                            id = 0L,
                            title = "",
                            details = "",
                            timestamp = System.currentTimeMillis(),
                            colorTag = null,
                            displayOption = com.dayscounter.domain.model.DisplayOption.DAY
                        ),
                    reminder = null
                )
        }
    }

    @Suppress("LongParameterList")
    fun checkHasChanges(
        title: String,
        details: String,
        timestamp: Long,
        colorTag: Int?,
        displayOption: com.dayscounter.domain.model.DisplayOption,
        reminderFingerprint: String? = null
    ) {
        val original = _originalItem.value ?: return
        val hasChanges =
            title != original.title ||
                details != original.details ||
                timestamp != original.timestamp ||
                colorTag != original.colorTag ||
                displayOption != original.displayOption ||
                reminderFingerprint != originalReminderFingerprint.value
        _hasChanges.value = hasChanges
    }

    fun resetHasChanges() {
        _hasChanges.value = false
    }

    private fun loadItem() {
        val nonNullItemId = checkNotNull(itemId)

        viewModelScope.launch {
            try {
                _uiState.value = CreateEditScreenState.Loading
                val item = repository.getItemById(nonNullItemId)

                if (item != null) {
                    val reminder = reminderManager.getActiveReminder(nonNullItemId)
                    _uiState.value = CreateEditScreenState.Success(item, reminder)
                    _originalItem.value = item
                    originalReminderFingerprint.value = reminder.toChangeFingerprint()
                    _hasChanges.value = false
                    logger.d("CreateEditScreenViewModel", "Событие загружено: ${item.title}")
                } else {
                    _uiState.value =
                        CreateEditScreenState.Error(
                            resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND)
                        )
                    logger.w("CreateEditScreenViewModel", "Событие не найдено: $itemId")
                }
            } catch (e: ItemException.LoadFailed) {
                val message =
                    resourceProvider.getString(
                        ResourceIds.ERROR_LOADING_EVENT,
                        e.message
                    )
                logger.e("CreateEditScreenViewModel", message, e)
                _uiState.value = CreateEditScreenState.Error(message)
            }
        }
    }

    @Suppress("LongMethod")
    fun saveItem(
        item: Item,
        reminderRequest: ReminderRequest?,
        onSaved: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val persistedItem =
                    if (itemId != null) {
                        val updatedItem = item.copy(id = itemId)
                        repository.updateItem(updatedItem)
                        updatedItem
                    } else {
                        val insertedId = repository.insertItem(item.copy(id = 0L))
                        item.copy(id = insertedId)
                    }

                if (reminderRequest != null) {
                    val reminderResult =
                        reminderManager.saveReminder(
                            request = reminderRequest.copy(itemId = persistedItem.id),
                            itemTitle = persistedItem.title
                        )

                    if (reminderResult.isFailure) {
                        val error = reminderResult.exceptionOrNull()
                        _uiState.value =
                            CreateEditScreenState.Error(
                                error?.message
                                    ?: resourceProvider.getString(
                                        ResourceIds.ERROR_UPDATING_EVENT,
                                        "Reminder save failed"
                                    )
                            )
                        return@launch
                    }
                } else {
                    reminderManager.clearReminder(persistedItem.id)
                }

                val activeReminder = reminderManager.getActiveReminder(persistedItem.id)

                _uiState.value = CreateEditScreenState.Success(persistedItem, activeReminder)
                _originalItem.value = persistedItem
                originalReminderFingerprint.value = activeReminder.toChangeFingerprint()
                _hasChanges.value = false
                onSaved()
            } catch (e: ItemException.SaveFailed) {
                val message =
                    resourceProvider.getString(
                        ResourceIds.ERROR_CREATING_EVENT,
                        e.message
                    )
                logger.e("CreateEditScreenViewModel", message, e)
                analyticsService.log(AnalyticsEvent.AppError(AppErrorOperation.CREATE_ITEM, e))
                _uiState.value = CreateEditScreenState.Error(message)
            } catch (e: ItemException.UpdateFailed) {
                val message =
                    resourceProvider.getString(
                        ResourceIds.ERROR_UPDATING_EVENT,
                        e.message
                    )
                logger.e("CreateEditScreenViewModel", message, e)
                analyticsService.log(AnalyticsEvent.AppError(AppErrorOperation.UPDATE_ITEM, e))
                _uiState.value = CreateEditScreenState.Error(message)
            }
        }
    }
}

/**
 * Состояние экрана создания/редактирования.
 */
sealed class CreateEditScreenState {
    data object Loading : CreateEditScreenState()

    data class Success(
        val item: Item,
        val reminder: Reminder? = null
    ) : CreateEditScreenState()

    data class Error(
        val message: String
    ) : CreateEditScreenState()
}
