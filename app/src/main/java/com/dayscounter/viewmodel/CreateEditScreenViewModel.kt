package com.dayscounter.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.data.formatter.ResourceIds
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.domain.exception.ItemException
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием экрана создания/редактирования события.
 *
 * @property repository Репозиторий для работы с данными
 * @property resourceProvider Провайдер строковых ресурсов для локализации
 * @property savedStateHandle SavedStateHandle для получения параметров навигации
 */
class CreateEditScreenViewModel(
    private val repository: ItemRepository,
    private val resourceProvider: ResourceProvider,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    companion object {
        fun factory(
            repository: ItemRepository,
            resourceProvider: ResourceProvider,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CreateEditScreenViewModel(
                        repository = repository,
                        resourceProvider = resourceProvider,
                        savedStateHandle =
                            checkNotNull(
                                createSavedStateHandle(),
                            ) {
                                "SavedStateHandle is required"
                            },
                    )
                }
            }
    }

    /**
     * Идентификатор события из параметров навигации (null для создания нового).
     */
    private val itemId: Long? = savedStateHandle["itemId"]

    /**
     * Состояние экрана.
     */
    private val _uiState = MutableStateFlow<CreateEditScreenState>(CreateEditScreenState.Loading)
    val uiState: StateFlow<CreateEditScreenState> = _uiState.asStateFlow()

    /**
     * Оригинальные данные события (для отслеживания изменений при редактировании).
     */
    private val _originalItem = MutableStateFlow<Item?>(null)
    val originalItem: StateFlow<Item?> = _originalItem.asStateFlow()

    /**
     * Признак наличия изменений по сравнению с оригинальными данными.
     */
    private val _hasChanges = MutableStateFlow(false)
    val hasChanges: StateFlow<Boolean> = _hasChanges.asStateFlow()

    init {
        if (itemId != null) {
            loadItem()
        } else {
            _uiState.value =
                CreateEditScreenState.Success(
                    Item(
                        id = 0L,
                        title = "",
                        details = "",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = com.dayscounter.domain.model.DisplayOption.DAY,
                    ),
                )
        }
    }

    /**
     * Проверяет наличие изменений по сравнению с оригинальными данными.
     */
    fun checkHasChanges(
        title: String,
        details: String,
        timestamp: Long,
        colorTag: Int?,
        displayOption: com.dayscounter.domain.model.DisplayOption,
    ) {
        val original = _originalItem.value ?: return
        val hasChanges =
            title != original.title ||
                details != original.details ||
                timestamp != original.timestamp ||
                colorTag != original.colorTag ||
                displayOption != original.displayOption
        _hasChanges.value = hasChanges
    }

    /**
     * Сбрасывает состояние изменений.
     */
    fun resetHasChanges() {
        _hasChanges.value = false
    }

    /**
     * Загружает событие из репозитория (для редактирования).
     */
    private fun loadItem() {
        viewModelScope.launch {
            try {
                _uiState.value = CreateEditScreenState.Loading
                val item = repository.getItemById(itemId!!)

                if (item != null) {
                    _uiState.value = CreateEditScreenState.Success(item)
                    _originalItem.value = item
                    _hasChanges.value = false
                    android.util.Log.d("CreateEditScreenViewModel", "Событие загружено: ${item.title}")
                } else {
                    _uiState.value =
                        CreateEditScreenState.Error(
                            resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND),
                        )
                    android.util.Log.w("CreateEditScreenViewModel", "Событие не найдено: $itemId")
                }
            } catch (e: ItemException.LoadFailed) {
                val message =
                    resourceProvider.getString(
                        ResourceIds.ERROR_LOADING_EVENT,
                        e.message,
                    )
                android.util.Log.e("CreateEditScreenViewModel", message, e)
                _uiState.value = CreateEditScreenState.Error(message)
            }
        }
    }

    /**
     * Создает новое событие.
     *
     * @param item Событие для создания
     */
    fun createItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.insertItem(item)
                _uiState.value = CreateEditScreenState.Success(item)
                android.util.Log.d("CreateEditScreenViewModel", "Событие создано: ${item.title}")
            } catch (e: ItemException.SaveFailed) {
                val message =
                    resourceProvider.getString(
                        ResourceIds.ERROR_CREATING_EVENT,
                        e.message,
                    )
                android.util.Log.e("CreateEditScreenViewModel", message, e)
                _uiState.value = CreateEditScreenState.Error(message)
            }
        }
    }

    /**
     * Обновляет существующее событие.
     *
     * @param item Событие для обновления
     */
    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
                _uiState.value = CreateEditScreenState.Success(item)
                _originalItem.value = item
                _hasChanges.value = false
                android.util.Log.d("CreateEditScreenViewModel", "Событие обновлено: ${item.title}")
            } catch (e: ItemException.UpdateFailed) {
                val message =
                    resourceProvider.getString(
                        ResourceIds.ERROR_UPDATING_EVENT,
                        e.message,
                    )
                android.util.Log.e("CreateEditScreenViewModel", message, e)
                _uiState.value = CreateEditScreenState.Error(message)
            }
        }
    }
}

/**
 * Состояние экрана создания/редактирования.
 */
sealed class CreateEditScreenState {
    /** Загрузка данных */
    data object Loading : CreateEditScreenState()

    /** Успешная загрузка */
    data class Success(
        val item: Item,
    ) : CreateEditScreenState()

    /** Ошибка загрузки */
    data class Error(
        val message: String,
    ) : CreateEditScreenState()
}
