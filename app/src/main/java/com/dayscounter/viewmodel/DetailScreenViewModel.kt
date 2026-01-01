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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val uiState: StateFlow<DetailScreenState> =
        repository
            .getItemFlow(itemId)
            .filterNotNull()
            .map { item ->
                DetailScreenState.Success(item)
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = DetailScreenState.Loading,
            )

    /**
     * Удаляет событие.
     */
    fun deleteItem() {
        viewModelScope.launch {
            try {
                val currentState = uiState.value
                if (currentState is DetailScreenState.Success) {
                    repository.deleteItem(currentState.item)
                    android.util.Log.d("DetailScreenViewModel", "Событие удалено: ${currentState.item.title}")
                }
            } catch (e: ItemException.DeleteFailed) {
                val message = "Ошибка удаления события: ${e.message}"
                android.util.Log.e("DetailScreenViewModel", message, e)
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
}
