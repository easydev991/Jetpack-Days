package com.dayscounter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.domain.exception.ItemException
import com.dayscounter.domain.exception.ItemException.DeleteFailed
import com.dayscounter.domain.exception.ItemException.LoadFailed
import com.dayscounter.domain.exception.ItemException.UpdateFailed
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием главного экрана со списком событий.
 *
 * Отвечает за загрузку, управление и отображение списка событий.
 */
class MainScreenViewModel(
    private val repository: ItemRepository,
) : ViewModel() {
    companion object {
        fun factory(repository: ItemRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MainScreenViewModel(repository)
                }
            }
    }

    /**
     * Состояние экрана.
     */
    private val _uiState = MutableStateFlow<MainScreenState>(MainScreenState.Loading)
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    /**
     * Выбранная опция отображения дней по умолчанию.
     */
    private val _defaultDisplayOption = MutableStateFlow(DisplayOption.DAY)
    val defaultDisplayOption: StateFlow<DisplayOption> = _defaultDisplayOption.asStateFlow()

    /**
     * Загружает события при создании ViewModel.
     */
    init {
        loadItems()
        // Observe items and update UI state accordingly
        viewModelScope.launch {
            repository.getAllItems().collect { items ->
                if (_uiState.value !is MainScreenState.Error) {
                    _uiState.value = MainScreenState.Success(items)
                }
            }
        }
    }

    /**
     * Загружает события из репозитория.
     */
    private fun loadItems() {
        viewModelScope.launch {
            try {
                _uiState.value = MainScreenState.Loading
                val loadedItems = repository.getAllItems().first()
                _uiState.value = MainScreenState.Success(loadedItems)
            } catch (e: ItemException.LoadFailed) {
                val message = "Ошибка загрузки списка событий: ${e.message}"
                android.util.Log.e("MainScreenViewModel", message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }

    /**
     * Удаляет событие.
     *
     * @param item Событие для удаления
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                android.util.Log.d("MainScreenViewModel", "Событие удалено: ${item.title}")
            } catch (e: ItemException.DeleteFailed) {
                val message = "Ошибка удаления события: ${e.message}"
                android.util.Log.e("MainScreenViewModel", message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }

    /**
     * Обновляет событие.
     *
     * @param item Событие для обновления
     */
    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
                android.util.Log.d("MainScreenViewModel", "Событие обновлено: ${item.title}")
            } catch (e: ItemException.UpdateFailed) {
                val message = "Ошибка обновления события: ${e.message}"
                android.util.Log.e("MainScreenViewModel", message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }

    /**
     * Изменяет опцию отображения дней по умолчанию.
     *
     * @param displayOption Новая опция отображения
     */
    fun updateDefaultDisplayOption(displayOption: DisplayOption) {
        _defaultDisplayOption.value = displayOption
        android.util.Log.d("MainScreenViewModel", "Опция отображения обновлена: $displayOption")
    }

    /**
     * Сбрасывает состояние ошибки.
     */
    fun clearError() {
        if (_uiState.value is MainScreenState.Error) {
            viewModelScope.launch {
                val loadedItems = repository.getAllItems().first()
                _uiState.value = MainScreenState.Success(loadedItems)
            }
        }
    }

    /**
     * Обновляет состояние загрузки.
     */
    fun refresh() {
        loadItems()
    }
}

/**
 * Состояние главного экрана.
 */
sealed class MainScreenState {
    /** Загрузка данных */
    data object Loading : MainScreenState()

    /** Успешная загрузка */
    data class Success(val items: List<com.dayscounter.domain.model.Item>) : MainScreenState()

    /** Ошибка загрузки */
    data class Error(val message: String) : MainScreenState()
}
