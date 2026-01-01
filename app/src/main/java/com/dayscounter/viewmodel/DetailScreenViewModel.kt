package com.dayscounter.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.domain.exception.ItemException
import com.dayscounter.domain.exception.ItemException.DeleteFailed
import com.dayscounter.domain.exception.ItemException.UpdateFailed
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel для управления состоянием экрана деталей события.
 *
 * @property repository Репозиторий для работы с данными
 * @property savedStateHandle SavedStateHandle для получения параметров навигации
 */
class DetailScreenViewModel(
    private val repository: ItemRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    companion object {
        fun factory(repository: ItemRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    DetailScreenViewModel(
                        repository = repository,
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
     * Идентификатор события из параметров навигации.
     */
    private val itemId: Long =
        checkNotNull(savedStateHandle["itemId"]) {
            "ItemId parameter is required"
        }

    /**
     * Состояние экрана.
     */
    private val _uiState = MutableStateFlow<DetailScreenState>(DetailScreenState.Loading)
    val uiState: StateFlow<DetailScreenState> = _uiState.asStateFlow()

    init {
        loadItem()
    }

    /**
     * Загружает событие из репозитория.
     */
    private fun loadItem() {
        viewModelScope.launch {
            try {
                _uiState.value = DetailScreenState.Loading
                val item = repository.getItemById(itemId)

                if (item != null) {
                    _uiState.value = DetailScreenState.Success(item)
                    android.util.Log.d("DetailScreenViewModel", "Событие загружено: ${item.title}")
                } else {
                    _uiState.value = DetailScreenState.Error("Событие не найдено")
                    android.util.Log.w("DetailScreenViewModel", "Событие не найдено: $itemId")
                }
            } catch (e: ItemException) {
                val message = "Ошибка загрузки события: ${e.message}"
                android.util.Log.e("DetailScreenViewModel", message, e)
                _uiState.value = DetailScreenState.Error(message)
            }
        }
    }

    /**
     * Удаляет событие.
     */
    fun deleteItem() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                if (currentState is DetailScreenState.Success) {
                    repository.deleteItem(currentState.item)
                    _uiState.value = DetailScreenState.Deleted
                    android.util.Log.d("DetailScreenViewModel", "Событие удалено: ${currentState.item.title}")
                }
            } catch (e: ItemException.DeleteFailed) {
                val message = "Ошибка удаления события: ${e.message}"
                android.util.Log.e("DetailScreenViewModel", message, e)
                _uiState.value = DetailScreenState.Error(message)
            }
        }
    }

    /**
     * Обновляет событие.
     *
     * @param item Обновленное событие
     */
    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
                _uiState.value = DetailScreenState.Success(item)
                android.util.Log.d("DetailScreenViewModel", "Событие обновлено: ${item.title}")
            } catch (e: ItemException.UpdateFailed) {
                val message = "Ошибка обновления события: ${e.message}"
                android.util.Log.e("DetailScreenViewModel", message, e)
                _uiState.value = DetailScreenState.Error(message)
            }
        }
    }
}

/**
 * Состояние экрана деталей.
 */
sealed class DetailScreenState {
    /** Загрузка данных */
    data object Loading : DetailScreenState()

    /** Успешная загрузка */
    data class Success(
        val item: Item,
    ) : DetailScreenState()

    /** Ошибка загрузки */
    data class Error(
        val message: String,
    ) : DetailScreenState()

    /** Событие удалено */
    data object Deleted : DetailScreenState()
}
