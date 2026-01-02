package com.dayscounter.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.dayscounter.domain.model.Item
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для DetailScreenViewModel.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailScreenViewModelTest {
    private lateinit var repository: FakeItemRepository
    private lateinit var viewModel: DetailScreenViewModel
    private lateinit var testDispatcher: TestDispatcher

    private val testItemId = 1L

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `whenViewModelCreated_thenStartsWithLoadingState`() {
        runTest {
            // Given - ViewModel создан без элемента
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
            viewModel = DetailScreenViewModel(repository, savedStateHandle)

            // Then - Начальное состояние должно быть Loading
            val initialState = viewModel.uiState.value
            assertTrue(
                initialState is DetailScreenState.Loading,
                "Начальное состояние должно быть Loading",
            )
        }
    }

    @Test
    fun `whenItemNotFound_thenRemainsInLoadingState`() {
        runTest {
            // Given - Repository не содержит элемент
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))
            viewModel = DetailScreenViewModel(repository, savedStateHandle)

            // When - Пытаемся загрузить несуществующий элемент
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Состояние должно остаться Loading (так как filterNotNull не пропускает null)
            val uiState = viewModel.uiState.value
            assertTrue(
                uiState is DetailScreenState.Loading,
                "При несуществующем элементе состояние должно быть Loading",
            )
        }
    }

    /**
     * Fake repository для тестирования.
     */
    private class FakeItemRepository : ItemRepository {
        private val itemsFlow = MutableStateFlow<List<Item>>(emptyList())

        fun setItem(item: Item) {
            itemsFlow.value = itemsFlow.value + item
        }

        override fun getAllItems(): Flow<List<Item>> = itemsFlow

        override fun getAllItems(sortOrder: com.dayscounter.domain.model.SortOrder): Flow<List<Item>> =
            itemsFlow.map { itemList ->
                when (sortOrder) {
                    com.dayscounter.domain.model.SortOrder.ASCENDING -> itemList.sortedBy { it.timestamp }
                    com.dayscounter.domain.model.SortOrder.DESCENDING ->
                        itemList.sortedByDescending { it.timestamp }
                }
            }

        override suspend fun getItemById(id: Long): Item? = itemsFlow.value.find { it.id == id }

        override fun getItemFlow(id: Long): Flow<Item?> =
            itemsFlow.map { itemList ->
                itemList.find { it.id == id }
            }

        override fun searchItems(query: String): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun insertItem(item: Item): Long {
            val newId = (itemsFlow.value.maxOfOrNull { it.id } ?: 0) + 1
            val newItem = item.copy(id = newId)
            itemsFlow.value = itemsFlow.value + newItem
            return newId
        }

        override suspend fun updateItem(item: Item) {
            itemsFlow.value = itemsFlow.value.map { if (it.id == item.id) item else it }
        }

        override suspend fun deleteItem(item: Item) {
            itemsFlow.value = itemsFlow.value.filterNot { it.id == item.id }
        }

        override suspend fun deleteAllItems() {
            itemsFlow.value = emptyList()
        }

        override suspend fun getItemsCount(): Int = itemsFlow.value.size
    }
}
