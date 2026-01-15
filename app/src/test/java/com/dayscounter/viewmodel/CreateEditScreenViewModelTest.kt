package com.dayscounter.viewmodel

import androidx.lifecycle.SavedStateHandle
import com.dayscounter.data.formatter.ResourceIds
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.domain.exception.ItemException
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.NoOpLogger
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для CreateEditScreenViewModel.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CreateEditScreenViewModelTest {
    private lateinit var repository: FakeItemRepositoryWithLoggingDisabled
    private lateinit var resourceProvider: FakeResourceProvider
    private lateinit var viewModel: CreateEditScreenViewModel
    private lateinit var testDispatcher: TestDispatcher

    private val testItem =
        Item(
            id = 1L,
            title = "Тестовое событие",
            details = "Описание события",
            timestamp = System.currentTimeMillis(),
            colorTag = null,
            displayOption = DisplayOption.DAY,
        )

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepositoryWithLoggingDisabled()
        resourceProvider = FakeResourceProvider()

        // Создаем ViewModel без itemId (для создания)
        viewModel =
            CreateEditScreenViewModel(
                repository,
                resourceProvider,
                NoOpLogger(),
                savedStateHandle = SavedStateHandle(),
            )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenViewModelCreatedWithNoItemId_thenInitializesWithNewItem() {
        runTest {
            // Given - SavedStateHandle без itemId
            val savedStateHandle = SavedStateHandle()

            // When - Создаем ViewModel для нового элемента
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Состояние должно быть Success с пустым Item
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = newViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("", successState.item.title, "Название должно быть пустым")
            assertEquals("", successState.item.details, "Описание должно быть пустым")
            assertEquals(
                DisplayOption.DAY,
                successState.item.displayOption,
            )
            assertNull(newViewModel.originalItem.value, "Оригинальный элемент должен быть null")
            assertFalse(newViewModel.hasChanges.value, "Изменений не должно быть")
        }
    }

    @Test
    fun whenViewModelCreatedWithItemId_thenLoadsItem() {
        runTest {
            // Given - SavedStateHandle с itemId
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel для редактирования
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Элемент должен быть загружен
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = newViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Тестовое событие", successState.item.title, "Название должно совпадать")
            assertEquals("Описание события", successState.item.details, "Описание должно совпадать")
            assertEquals(
                testItem,
                newViewModel.originalItem.value,
                "Оригинальный элемент должен быть сохранен",
            )
            assertFalse(newViewModel.hasChanges.value, "Изменений не должно быть")
        }
    }

    @Test
    fun whenItemNotFound_thenShowsErrorState() {
        runTest {
            // Given - SavedStateHandle с несуществующим itemId
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Должно быть состояние Error
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = newViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Error, "Должно быть состояние Error")
            val errorState = uiState as CreateEditScreenState.Error
            assertEquals(
                "Событие не найдено",
                errorState.message,
                "Сообщение об ошибке должно быть корректным",
            )
        }
    }

    @Test
    fun whenCreateItemWithValidData_thenSavesItem() {
        runTest {
            // Given - ViewModel для создания
            val savedStateHandle = SavedStateHandle()
            val newItem =
                Item(
                    id = 0L,
                    title = "Новое событие",
                    details = "Новое описание",
                    timestamp = System.currentTimeMillis(),
                    colorTag = null,
                    displayOption = DisplayOption.MONTH_DAY,
                )

            // When - Создаем элемент
            val createViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            createViewModel.createItem(newItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть сохранен и состояние Success
            val uiState = createViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Новое событие", successState.item.title, "Название должно совпадать")
            assertEquals("Новое описание", successState.item.details, "Описание должно совпадать")
            assertEquals(
                DisplayOption.MONTH_DAY,
                successState.item.displayOption,
                "Опция отображения должна совпадать",
            )
            assertTrue(repository.insertItemCalled, "Метод insertItem должен быть вызван")
        }
    }

    @Test
    fun whenCreateItemFails_thenShowsErrorState() {
        runTest {
            // Given - Repository выбрасывает ошибку при вставке
            repository.shouldThrowOnInsert = true
            val savedStateHandle = SavedStateHandle()
            val errorItem =
                Item(
                    id = 0L,
                    title = "Событие с ошибкой",
                    details = "Описание",
                    timestamp = System.currentTimeMillis(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY,
                )

            // When - Пытаем создать элемент
            val createViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            createViewModel.createItem(errorItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Должно быть состояние Error
            val uiState = createViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Error, "Должно быть состояние Error")
            val errorState = uiState as CreateEditScreenState.Error
            assertTrue(
                errorState.message.contains("Ошибка создания события"),
                "Сообщение об ошибке должно содержать текст об ошибке",
            )
        }
    }

    @Test
    fun whenUpdateItemWithValidData_thenUpdatesItem() {
        runTest {
            // Given - Элемент в Repository и ViewModel для редактирования
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)
            val updatedItem =
                testItem.copy(
                    title = "Обновленное название",
                    details = "Обновленное описание",
                    timestamp = testItem.timestamp + 86400000L,
                    colorTag = 0xFFFF0000.toInt(),
                    displayOption = DisplayOption.YEAR_MONTH_DAY,
                )

            // When - Обновляем элемент
            val updateViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            updateViewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть обновлен и состояние Success
            val uiState = updateViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as CreateEditScreenState.Success
            assertEquals(
                "Обновленное название",
                successState.item.title,
                "Название должно быть обновлено",
            )
            assertEquals(
                "Обновленное описание",
                successState.item.details,
                "Описание должно быть обновлено",
            )
            assertEquals(
                DisplayOption.YEAR_MONTH_DAY,
                successState.item.displayOption,
                "Опция отображения должна быть обновлена",
            )
            assertTrue(repository.updateItemCalled, "Метод updateItem должен быть вызван")
        }
    }

    @Test
    fun whenUpdateItemFails_thenShowsErrorState() {
        runTest {
            // Given - Repository выбрасывает ошибку при обновлении
            repository.shouldThrowOnUpdate = true
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)
            val updatedItem = testItem.copy(title = "Обновленное название")

            // When - Пытаем обновить элемент
            val updateViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            updateViewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Должно быть состояние Error
            val uiState = updateViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Error, "Должно быть состояние Error")
            val errorState = uiState as CreateEditScreenState.Error
            assertTrue(
                errorState.message.contains("Ошибка обновления события"),
                "Сообщение об ошибке должно содержать текст об ошибке",
            )
        }
    }

    @Test
    fun whenLoadItemFails_thenShowsErrorState() {
        runTest {
            // Given - Repository выбрасывает ошибку при загрузке
            repository.shouldThrowOnGetById = true
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Должно быть состояние Error
            val uiState = newViewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Error, "Должно быть состояние Error")
            val errorState = uiState as CreateEditScreenState.Error
            assertTrue(
                errorState.message.contains("Ошибка загрузки события"),
                "Сообщение об ошибке должно содержать текст об ошибке",
            )
        }
    }

    @Test
    fun whenCheckHasChangesWithTitleChanged_thenHasChangesIsTrue() {
        runTest {
            // Given - ViewModel с загруженным элементом
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = "Измененное название",
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(newViewModel.hasChanges.value, "Изменения должны быть обнаружены")
        }
    }

    @Test
    fun whenCheckHasChangesWithDetailsChanged_thenHasChangesIsTrue() {
        runTest {
            // Given - ViewModel с загруженным элементом
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = testItem.title,
                details = "Измененное описание",
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(newViewModel.hasChanges.value, "Изменения должны быть обнаружены")
        }
    }

    @Test
    fun whenCheckHasChangesWithTimestampChanged_thenHasChangesIsTrue() {
        runTest {
            // Given - ViewModel с загруженным элементом
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = testItem.title,
                details = testItem.details,
                timestamp = testItem.timestamp + 86400000L,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(newViewModel.hasChanges.value, "Изменения должны быть обнаружены")
        }
    }

    @Test
    fun whenCheckHasChangesWithColorTagChanged_thenHasChangesIsTrue() {
        runTest {
            // Given - ViewModel с загруженным элементом без цвета
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = testItem.title,
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = 0xFFFF0000.toInt(),
                displayOption = testItem.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(newViewModel.hasChanges.value, "Изменения должны быть обнаружены")
        }
    }

    @Test
    fun whenCheckHasChangesWithDisplayOptionChanged_thenHasChangesIsTrue() {
        runTest {
            // Given - ViewModel с загруженным элементом
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = testItem.title,
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(newViewModel.hasChanges.value, "Изменения должны быть обнаружены")
        }
    }

    @Test
    fun whenCheckHasChangesWithNoChanges_thenHasChangesIsFalse() {
        runTest {
            // Given - ViewModel с загруженным элементом
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = testItem.title,
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )

            // Then - Изменений не должно быть
            assertFalse(newViewModel.hasChanges.value, "Изменений не должно быть")
        }
    }

    @Test
    fun whenCheckHasChangesWithSameTimestamp_thenHasChangesIsFalse() {
        runTest {
            // Given - ViewModel с загруженным элементом с конкретным timestamp
            val fixedTimestamp = 1704067200000L // 2024-01-01 00:00:00
            val itemWithFixedTimestamp =
                testItem.copy(timestamp = fixedTimestamp)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(itemWithFixedTimestamp)

            // When - Создаем ViewModel и проверяем тот же timestamp
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = itemWithFixedTimestamp.title,
                details = itemWithFixedTimestamp.details,
                timestamp = fixedTimestamp, // Тот же самый timestamp
                colorTag = itemWithFixedTimestamp.colorTag,
                displayOption = itemWithFixedTimestamp.displayOption,
            )

            // Then - Изменений не должно быть обнаружено
            assertFalse(
                newViewModel.hasChanges.value,
                "При том же timestamp изменения не должны быть обнаружены",
            )
        }
    }

    @Test
    fun whenCheckHasChangesWithTimestampOneDayLater_thenHasChangesIsTrue() {
        runTest {
            // Given - ViewModel с загруженным элементом с конкретным timestamp
            val fixedTimestamp = 1704067200000L // 2024-01-01 00:00:00
            val itemWithFixedTimestamp =
                testItem.copy(timestamp = fixedTimestamp)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(itemWithFixedTimestamp)

            // When - Создаем ViewModel и проверяем timestamp на 1 день позже
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            val nextDayTimestamp =
                fixedTimestamp + (24 * 60 * 60 * 1000) // +1 день (86400000 мс)
            newViewModel.checkHasChanges(
                title = itemWithFixedTimestamp.title,
                details = itemWithFixedTimestamp.details,
                timestamp = nextDayTimestamp, // На 1 день позже
                colorTag = itemWithFixedTimestamp.colorTag,
                displayOption = itemWithFixedTimestamp.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(
                newViewModel.hasChanges.value,
                "При изменении timestamp на 1 день изменения должны быть обнаружены",
            )
        }
    }

    @Test
    fun whenResetHasChanges_thenHasChangesIsFalse() {
        runTest {
            // Given - ViewModel с загруженным элементом и обнаруженными изменениями
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            testDispatcher.scheduler.advanceUntilIdle()
            newViewModel.checkHasChanges(
                title = "Измененное название",
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )
            assertTrue(newViewModel.hasChanges.value, "Изменения должны быть обнаружены")

            // When - Сбрасываем изменения
            newViewModel.resetHasChanges()

            // Then - Флаг изменений должен быть сброшен
            assertFalse(newViewModel.hasChanges.value, "Флаг изменений должен быть сброшен")
        }
    }

    @Test
    fun whenLoadingItem_thenSetsLoadingStateFirst() {
        runTest {
            // Given - SavedStateHandle с itemId и задержка в repository
            repository.loadingDelayMs = 100L
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 1L))
            repository.setItemForGetById(testItem)

            // When - Создаем ViewModel
            val newViewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Сначала должно быть состояние Loading
            var uiState = newViewModel.uiState.value
            assertTrue(
                uiState is CreateEditScreenState.Loading,
                "Сначала должно быть состояние Loading",
            )

            // After delay - должно стать Success
            testDispatcher.scheduler.advanceUntilIdle()
            uiState = newViewModel.uiState.value
            assertTrue(
                uiState is CreateEditScreenState.Success,
                "После загрузки должно быть состояние Success",
            )
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Тестовое событие", successState.item.title, "Название должно совпадать")
        }
    }

    /**
     * Fake repository с отключенным логированием для тестов.
     * НЕ использует методы Repository, которые вызывают логгер.
     */
    private class FakeItemRepositoryWithLoggingDisabled : ItemRepository {
        private val storedItem = MutableStateFlow<Item?>(null)

        var insertItemCalled = false
        var updateItemCalled = false
        var shouldThrowOnInsert = false
        var shouldThrowOnUpdate = false
        var shouldThrowOnGetById = false
        var loadingDelayMs: Long = 0L

        fun setItemForGetById(itemParam: Item) {
            storedItem.value = itemParam
        }

        override fun getAllItems(): Flow<List<Item>> = flowOf(emptyList())

        @Suppress("MaxLineLength")
        override fun getAllItems(sortOrder: com.dayscounter.domain.model.SortOrder): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun getItemById(id: Long): Item? {
            if (loadingDelayMs > 0) {
                kotlinx.coroutines.delay(loadingDelayMs)
            }
            if (shouldThrowOnGetById) {
                throw ItemException.LoadFailed("Ошибка загрузки")
            }
            return storedItem.value?.takeIf<Item> { it.id == id }
        }

        override fun getItemFlow(id: Long): Flow<Item?> =
            storedItem.map { itemValue ->
                itemValue?.takeIf<Item> { it.id == id }
            }

        override fun searchItems(query: String): Flow<List<Item>> = flowOf(emptyList())

        override suspend fun insertItem(item: Item): Long {
            insertItemCalled = true
            if (shouldThrowOnInsert) {
                throw ItemException.SaveFailed("Ошибка сохранения")
            }
            storedItem.value = item
            return item.id
        }

        override suspend fun updateItem(item: Item) {
            updateItemCalled = true
            if (shouldThrowOnUpdate) {
                throw ItemException.UpdateFailed("Ошибка обновления")
            }
        }

        override suspend fun deleteItem(item: Item) {
            if (storedItem.value?.id == item.id) {
                storedItem.value = null
            }
        }

        override suspend fun deleteAllItems() {
            storedItem.value = null
        }

        override suspend fun getItemsCount(): Int = if (storedItem.value != null) 1 else 0
    }

    /**
     * Fake ResourceProvider для тестов.
     */
    private class FakeResourceProvider : ResourceProvider {
        override fun getString(
            resId: Int,
            vararg formatArgs: Any,
        ): String =
            when (resId) {
                ResourceIds.EVENT_NOT_FOUND -> "Событие не найдено"
                ResourceIds.ERROR_CREATING_EVENT -> "Ошибка создания события: ${formatArgs.joinToString()}"
                ResourceIds.ERROR_UPDATING_EVENT -> "Ошибка обновления события: ${formatArgs.joinToString()}"
                ResourceIds.ERROR_LOADING_EVENT -> "Ошибка загрузки события: ${formatArgs.joinToString()}"
                else -> "Строка по умолчанию: $resId"
            }

        override fun getQuantityString(
            resId: Int,
            quantity: Int,
            vararg formatArgs: Any,
        ): String = "$quantity"

        override fun getYearsString(quantity: Int): String = "$quantity год"

        override fun getMonthsString(quantity: Int): String = "$quantity месяц"
    }
}
