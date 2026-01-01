package com.dayscounter.viewmodel

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для MainScreenViewModel.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
    private lateinit var repository: FakeItemRepository
    private lateinit var viewModel: MainScreenViewModel
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
        viewModel = MainScreenViewModel(repository)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenViewModelCreated_thenLoadsAllItems() {
        runTest {
            // Given - Repository содержит 2 элемента
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Событие 1",
                        details = "Детали 1",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - ViewModel создан
            // Дожидаемся завершения инициализации
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Состояние обновляется с загруженными элементами
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(2, successState.items.size, "Должно быть 2 элемента")
        }
    }

    @Test
    fun whenSearchQueryChanged_thenFiltersItems() {
        runTest {
            // Given - Repository содержит элементы
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "День рождения",
                        details = "Праздничный день",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                    Item(
                        id = 2L,
                        title = "Новый год",
                        details = "Праздник",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Вводим поисковый запрос
            viewModel.updateSearchQuery("день")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элементы фильтруются
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(1, successState.items.size, "Должен быть 1 элемент")
            assertEquals("День рождения", successState.items[0].title, "Должен быть правильный элемент")
        }
    }

    @Test
    fun whenSearchQueryEmpty_thenShowsAllItems() {
        runTest {
            // Given - Repository содержит элементы
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "День рождения",
                        details = "Праздничный день",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                    Item(
                        id = 2L,
                        title = "Новый год",
                        details = "Праздник",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Поисковый запрос очищен
            viewModel.updateSearchQuery("день")
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.updateSearchQuery("")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Все элементы отображаются
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(2, successState.items.size, "Должны быть все 2 элемента")
        }
    }

    @Test
    fun whenSortOrderChanged_thenSortsItems() {
        runTest {
            // Given - Repository содержит элементы
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Событие 1",
                        details = "Детали 1",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Меняем порядок сортировки на старые первые
            viewModel.updateSortOrder(SortOrder.ASCENDING)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элементы сортируются
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(2, successState.items.size, "Должно быть 2 элемента")
            assertEquals(1L, successState.items[0].id, "Первый элемент должен быть старым")
            assertEquals(SortOrder.ASCENDING, viewModel.sortOrder.value, "Порядок сортировки должен обновиться")
        }
    }

    @Test
    fun whenSortOrderDescending_thenShowsNewestFirst() {
        runTest {
            // Given - Repository содержит элементы
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Событие 1",
                        details = "Детали 1",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Порядок сортировки новые первые (по умолчанию)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элементы сортируются от новых к старым
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(2, successState.items.size, "Должно быть 2 элемента")
            assertEquals(2L, successState.items[0].id, "Первый элемент должен быть новым")
        }
    }

    @Test
    fun whenItemDeleted_thenRemovesFromList() {
        runTest {
            // Given - Repository содержит элемент
            val item =
                Item(
                    id = 1L,
                    title = "Событие для удаления",
                    details = "Детали",
                    timestamp = System.currentTimeMillis(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY,
                )
            repository.setItems(listOf(item))

            // When - Удаляем элемент
            viewModel.deleteItem(item)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент удален из списка
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(0, successState.items.size, "Список должен быть пустым")
            assertEquals(0, viewModel.itemsCount.value, "Количество элементов в ViewModel должно быть 0")
        }
    }

    @Test
    fun whenNoItems_thenShowsEmptyState() {
        runTest {
            // Given - Repository пустой
            repository.setItems(emptyList())

            // When - ViewModel создан
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Состояние Success с пустым списком
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(0, successState.items.size, "Список должен быть пустым")
            assertEquals(0, viewModel.itemsCount.value, "Количество элементов должно быть 0")
        }
    }

    @Test
    fun whenSearchInDetails_thenFindsItem() {
        runTest {
            // Given - Repository содержит элемент с текстом в деталях
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Событие",
                        details = "Праздничный день рождения",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Ищем по тексту в деталях
            viewModel.updateSearchQuery("рождения")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент найден
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(1, successState.items.size, "Должен быть 1 элемент")
        }
    }

    @Test
    fun whenSearchCaseInsensitive_thenFindsItem() {
        runTest {
            // Given - Repository содержит элемент
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "День рождения",
                        details = "Детали",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Ищем с разным регистром
            viewModel.updateSearchQuery("ДЕНЬ")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент найден
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(1, successState.items.size, "Должен быть 1 элемент")
        }
    }

    @Test
    fun whenSearchWithNoResults_thenShowsEmptyList() {
        runTest {
            // Given - Repository содержит элементы
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "День рождения",
                        details = "Детали",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY,
                    ),
                )
            repository.setItems(items)

            // When - Ищем то, чего нет
            viewModel.updateSearchQuery("Несуществующий текст")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Пустой список
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(0, successState.items.size, "Список должен быть пустым")
        }
    }

    /**
     * Fake repository для тестирования.
     */
    private class FakeItemRepository : ItemRepository {
        private val _items = MutableStateFlow<List<Item>>(emptyList())

        fun setItems(items: List<Item>) {
            _items.value = items
        }

        override fun getAllItems(): Flow<List<Item>> = _items

        override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> =
            _items.map { items ->
                when (sortOrder) {
                    SortOrder.ASCENDING -> items.sortedBy { it.timestamp }
                    SortOrder.DESCENDING -> items.sortedByDescending { it.timestamp }
                }
            }

        override suspend fun getItemById(id: Long): Item? = _items.value.find { it.id == id }

        override fun searchItems(query: String): Flow<List<Item>> {
            val filteredItems =
                _items.value.filter { item ->
                    item.title.contains(query, ignoreCase = true) ||
                        item.details.contains(query, ignoreCase = true)
                }
            return flowOf(filteredItems)
        }

        override suspend fun insertItem(item: Item): Long {
            val newId = (_items.value.maxOfOrNull { it.id } ?: 0) + 1
            val newItem = item.copy(id = newId)
            _items.value = _items.value + newItem
            return newId
        }

        override suspend fun updateItem(item: Item) {
            _items.value = _items.value.map { if (it.id == item.id) item else it }
        }

        override suspend fun deleteItem(item: Item) {
            _items.value = _items.value.filterNot { it.id == item.id }
        }

        override suspend fun deleteAllItems() {
            _items.value = emptyList()
        }

        override suspend fun getItemsCount(): Int = _items.value.size
    }
}
