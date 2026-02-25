package com.dayscounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.domain.exception.ItemException.DeleteFailed
import com.dayscounter.domain.exception.ItemException.UpdateFailed
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATE_TIMEOUT_MS = 5000L
private const val TAG = "MainScreenViewModel"

/**
 * ViewModel для управления состоянием главного экрана со списком событий.
 *
 * Отвечает за загрузку, управление и отображение списка событий.
 * Порядок сортировки сохраняется в DataStore для сохранения выбора пользователя.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainScreenViewModel(
    private val repository: ItemRepository,
    private val dataStore: AppSettingsDataStore,
    private val logger: Logger = AndroidLogger(),
) : ViewModel() {
    companion object {
        fun factory(
            repository: ItemRepository,
            dataStore: AppSettingsDataStore,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MainScreenViewModel(repository, dataStore)
                }
            }
    }

    /**
     * Состояние экрана.
     */
    private val _uiState = MutableStateFlow<MainScreenState>(MainScreenState.Loading)
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    /**
     * Поисковый запрос.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Порядок сортировки (читается из DataStore).
     */
    val sortOrder: StateFlow<SortOrder> =
        dataStore.sortOrder.stateIn(
            scope = viewModelScope,
            started =
                kotlinx.coroutines.flow.SharingStarted
                    .WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = SortOrder.DESCENDING,
        )

    /**
     * Количество элементов (для отображения кнопки сортировки).
     */
    private val _itemsCount = MutableStateFlow(0)
    val itemsCount: StateFlow<Int> = _itemsCount.asStateFlow()

    /**
     * Элемент для удаления в диалоговом окне.
     */
    private val _showDeleteDialog = MutableStateFlow<Item?>(null)
    val showDeleteDialog: StateFlow<Item?> = _showDeleteDialog.asStateFlow()

    /**
     * Загружает события при создании ViewModel.
     */
    init {
        // Observe items and update UI state accordingly
        observeItems()
    }

    /**
     * Наблюдает за изменением элементов и обновляет состояние UI.
     */
    private fun observeItems() {
        viewModelScope.launch {
            combine(
                sortOrder.flatMapLatest { order -> repository.getAllItems(order) },
                _searchQuery,
            ) { items, query ->
                logger.d(
                    TAG,
                    "Обновление списка: количество элементов=${items.size}, запрос поиска=\'$query\'"
                )
                if (query.isEmpty()) {
                    items
                } else {
                    val filteredItems =
                        items.filter { item ->
                            val titleContains = item.title.contains(query, ignoreCase = true)
                            val detailsContains = item.details.contains(query, ignoreCase = true)
                            logger.d(
                                TAG,
                                "Фильтрация: элемент=\'${item.title}\', " +
                                    "детали=\'${item.details}\', " +
                                    "запрос=\'$query\', " +
                                    "titleContains=$titleContains, " +
                                    "detailsContains=$detailsContains",
                            )
                            titleContains || detailsContains
                        }
                    logger.d(TAG, "После фильтрации: ${filteredItems.size} элементов")
                    filteredItems
                }
            }.collect { items ->
                logger.d(TAG, "Обновление UI: количество элементов=${items.size}")
                _itemsCount.value = items.size
                _uiState.value = MainScreenState.Success(items)
            }
        }
    }

    /**
     * Обновляет поисковый запрос.
     *
     * @param query Новый поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Обновляет порядок сортировки.
     *
     * @param order Новый порядок сортировки
     */
    fun updateSortOrder(order: SortOrder) {
        viewModelScope.launch {
            dataStore.setSortOrder(order)
            logger.d(TAG, "Порядок сортировки обновлен и сохранен: $order")
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
                logger.d(TAG, "Событие удалено: ${item.title}")
            } catch (e: DeleteFailed) {
                val message = "Ошибка удаления события: ${e.message}"
                logger.e(TAG, message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }

    /**
     * Запрашивает подтверждение удаления записи.
     *
     * @param item Запись для удаления
     */
    fun requestDelete(item: Item) {
        _showDeleteDialog.value = item
        logger.d(TAG, "Запрос на удаление: ${item.title}")
    }

    /**
     * Подтверждает удаление записи.
     */
    fun confirmDelete() {
        _showDeleteDialog.value?.let { item ->
            deleteItem(item)
            logger.d(TAG, "Удаление подтверждено: ${item.title}")
        }
        _showDeleteDialog.value = null
    }

    /**
     * Отменяет удаление записи.
     */
    fun cancelDelete() {
        _showDeleteDialog.value = null
        logger.d(TAG, "Удаление отменено")
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
                logger.d(TAG, "Событие обновлено: ${item.title}")
            } catch (e: UpdateFailed) {
                val message = "Ошибка обновления события: ${e.message}"
                logger.e(TAG, message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }
}

/**
 * Состояние главного экрана.
 */
sealed class MainScreenState {
    /** Загрузка данных */
    data object Loading : MainScreenState()

    /** Успешная загрузка */
    data class Success(
        val items: List<Item>,
    ) : MainScreenState()

    /** Ошибка загрузки */
    data class Error(
        val message: String,
    ) : MainScreenState()
}
