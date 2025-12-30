package com.dayscounter.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
 * @property savedStateHandle SavedStateHandle для получения параметров навигации
 */
class CreateEditScreenViewModel(
    private val repository: ItemRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    /**
     * Идентификатор события из параметров навигации (null для создания нового).
     */
    private val itemId: Long? = savedStateHandle["itemId"]

    /**
     * Состояние экрана.
     */
    private val _uiState = MutableStateFlow<CreateEditScreenState>(CreateEditScreenState.Loading)
    val uiState: StateFlow<CreateEditScreenState> = _uiState.asStateFlow()

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
     * Загружает событие из репозитория (для редактирования).
     */
    private fun loadItem() {
        viewModelScope.launch {
            try {
                _uiState.value = CreateEditScreenState.Loading
                val item = repository.getItemById(itemId!!)

                if (item != null) {
                    _uiState.value = CreateEditScreenState.Success(item)
                    android.util.Log.d("CreateEditScreenViewModel", "Событие загружено: ${item.title}")
                } else {
                    _uiState.value = CreateEditScreenState.Error("Событие не найдено")
                    android.util.Log.w("CreateEditScreenViewModel", "Событие не найдено: $itemId")
                }
            } catch (e: ItemException.LoadFailed) {
                val message = "Ошибка загрузки события: ${e.message}"
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
                val message = "Ошибка создания события: ${e.message}"
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
                android.util.Log.d("CreateEditScreenViewModel", "Событие обновлено: ${item.title}")
            } catch (e: ItemException.UpdateFailed) {
                val message = "Ошибка обновления события: ${e.message}"
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
    data class Success(val item: Item) : CreateEditScreenState()

    /** Ошибка загрузки */
    data class Error(val message: String) : CreateEditScreenState()
}
