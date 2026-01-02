package com.dayscounter.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.formatter.ResourceIds
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.runner.RunWith
import java.io.File

/**
 * Интеграционные тесты для CreateEditScreenViewModel.
 * Тестируют взаимодействие ViewModel с реальным Repository и базой данных.
 */
@RunWith(AndroidJUnit4::class)
class CreateEditScreenViewModelIntegrationTest {
    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepositoryImpl
    private lateinit var resourceProvider: FakeResourceProvider
    private lateinit var viewModel: CreateEditScreenViewModel
    private lateinit var testDispatcher: TestDispatcher
    private lateinit var context: Context

    @TempDir
    lateinit var tempDir: File

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
        kotlinx.coroutines.Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext<Context>()

        // Создаем in-memory базу данных для тестов
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    DaysDatabase::class.java,
                ).build()

        repository = ItemRepositoryImpl(database.itemDao())
        resourceProvider = FakeResourceProvider()
    }

    @AfterEach
    fun tearDown() {
        database.close()
    }

    @Test
    fun whenCreateItem_thenItemSavedInDatabase() {
        runTest {
            // Given - ViewModel для создания
            val savedStateHandle = SavedStateHandle()
            val newItem =
                Item(
                    id = 0L,
                    title = "Новое событие",
                    details = "Новое описание",
                    timestamp = System.currentTimeMillis(),
                    colorTag = 0xFFFF0000.toInt(),
                    displayOption = DisplayOption.MONTH_DAY,
                )
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )

            // When - Создаем элемент
            viewModel.createItem(newItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть в базе данных
            val allItems = repository.getAllItems().first()
            assertEquals(1, allItems.size, "Должен быть один элемент в базе данных")
            assertEquals("Новое событие", allItems[0].title, "Название должно совпадать")
            assertEquals("Новое описание", allItems[0].details, "Описание должно совпадать")
            assertEquals(DisplayOption.MONTH_DAY, allItems[0].displayOption, "Опция отображения должна совпадать")
            assertNotNull(allItems[0].colorTag, "Цвет должен быть установлен")
        }
    }

    @Test
    fun whenUpdateItem_thenItemUpdatedInDatabase() {
        runTest {
            // Given - Создаем элемент в базе данных
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            val updatedItem =
                testItem.copy(
                    id = insertedId,
                    title = "Обновленное название",
                    details = "Обновленное описание",
                    timestamp = testItem.timestamp + 86400000L,
                    colorTag = 0xFF00FF00.toInt(),
                    displayOption = DisplayOption.YEAR_MONTH_DAY,
                )

            // When - Обновляем элемент через ViewModel
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть обновлен в базе данных
            val itemFromDb = repository.getItemById(insertedId)
            assertNotNull(itemFromDb, "Элемент должен существовать в базе данных")
            assertEquals("Обновленное название", itemFromDb!!.title, "Название должно быть обновлено")
            assertEquals("Обновленное описание", itemFromDb.details, "Описание должно быть обновлено")
            assertEquals(DisplayOption.YEAR_MONTH_DAY, itemFromDb.displayOption, "Опция отображения должна быть обновлена")
            assertEquals(0xFF00FF00.toInt(), itemFromDb.colorTag, "Цвет должен быть обновлен")
        }
    }

    @Test
    fun whenLoadExistingItem_thenItemLoadedFromDatabase() {
        runTest {
            // Given - Создаем элемент в базе данных
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            // When - Создаем ViewModel для редактирования
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )

            // Then - Элемент должен быть загружен
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = viewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Тестовое событие", successState.item.title, "Название должно совпадать")
            assertEquals("Описание события", successState.item.details, "Описание должно совпадать")
            assertEquals(testItem.timestamp, successState.item.timestamp, "Дата должна совпадать")
            assertEquals(DisplayOption.DAY, successState.item.displayOption, "Опция отображения должна совпадать")
        }
    }

    @Test
    fun whenLoadNonExistentItem_thenShowsError() {
        runTest {
            // Given - SavedStateHandle с несуществующим itemId
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))

            // When - Создаем ViewModel
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )

            // Then - Должно быть состояние Error
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = viewModel.uiState.value
            assertTrue(uiState is CreateEditScreenState.Error, "Должно быть состояние Error")
            val errorState = uiState as CreateEditScreenState.Error
            assertEquals("Событие не найдено", errorState.message, "Сообщение об ошибке должно быть корректным")
        }
    }

    @Test
    fun whenCheckHasChanges_thenCorrectlyDetectsChanges() {
        runTest {
            // Given - Создаем элемент в базе данных и загружаем его
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Изменяем название
            viewModel.checkHasChanges(
                title = "Новое название",
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(viewModel.hasChanges.value, "Изменения должны быть обнаружены")
        }
    }

    @Test
    fun whenUpdateMultipleFields_thenAllChangesPersistedInDatabase() {
        runTest {
            // Given - Создаем элемент в базе данных
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            // When - Обновляем все поля
            val updatedItem =
                testItem.copy(
                    id = insertedId,
                    title = "Полностью новое название",
                    details = "Полностью новое описание",
                    timestamp = testItem.timestamp + 172800000L, // +2 дня
                    colorTag = 0xFF0000FF.toInt(),
                    displayOption = DisplayOption.YEAR_MONTH_DAY,
                )

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Все изменения должны быть сохранены
            val itemFromDb = repository.getItemById(insertedId)
            assertNotNull(itemFromDb, "Элемент должен существовать")
            assertEquals("Полностью новое название", itemFromDb!!.title, "Название должно быть обновлено")
            assertEquals("Полностью новое описание", itemFromDb.details, "Описание должно быть обновлено")
            assertEquals(testItem.timestamp + 172800000L, itemFromDb.timestamp, "Дата должна быть обновлена")
            assertEquals(0xFF0000FF.toInt(), itemFromDb.colorTag, "Цвет должен быть обновлен")
            assertEquals(DisplayOption.YEAR_MONTH_DAY, itemFromDb.displayOption, "Опция отображения должна быть обновлена")
        }
    }

    @Test
    fun whenCreateMultipleItems_thenAllItemsSavedInDatabase() {
        runTest {
            // Given - ViewModel для создания
            val savedStateHandle = SavedStateHandle()
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )

            // When - Создаем несколько элементов
            val item1 =
                Item(
                    id = 0L,
                    title = "Событие 1",
                    details = "Описание 1",
                    timestamp = System.currentTimeMillis(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY,
                )
            val item2 =
                Item(
                    id = 0L,
                    title = "Событие 2",
                    details = "Описание 2",
                    timestamp = System.currentTimeMillis() - 86400000L,
                    colorTag = 0xFFFF0000.toInt(),
                    displayOption = DisplayOption.MONTH_DAY,
                )

            viewModel.createItem(item1)
            testDispatcher.scheduler.advanceUntilIdle()

            // Создаем новый ViewModel для второго элемента
            val viewModel2 =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            viewModel2.createItem(item2)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Все элементы должны быть в базе данных
            val allItems = repository.getAllItems().first()
            assertEquals(2, allItems.size, "Должны быть два элемента в базе данных")
            assertTrue(allItems.any { it.title == "Событие 1" }, "Первый элемент должен существовать")
            assertTrue(allItems.any { it.title == "Событие 2" }, "Второй элемент должен существовать")
        }
    }

    @Test
    fun whenUpdateAndThenLoadItem_thenChangesPersisted() {
        runTest {
            // Given - Создаем и обновляем элемент
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            val updatedItem =
                testItem.copy(
                    id = insertedId,
                    title = "Обновленное название",
                )

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Создаем новый ViewModel и загружаем тот же элемент
            val viewModel2 =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )

            // Then - Изменения должны быть сохранены
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = viewModel2.uiState.value
            assertTrue(uiState is CreateEditScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Обновленное название", successState.item.title, "Название должно быть обновленным")
            assertEquals(updatedItem, viewModel2.originalItem.value, "Оригинальный элемент должен быть обновленным")
            assertFalse(viewModel2.hasChanges.value, "Изменений не должно быть при загрузке обновленного элемента")
        }
    }

    @Test
    fun whenCheckHasChangesWithColorTagFromNullToValue_thenDetectsChanges() {
        runTest {
            // Given - Элемент без цвета
            val itemWithoutColor =
                testItem.copy(
                    colorTag = null,
                )
            val insertedId = repository.insertItem(itemWithoutColor)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Устанавливаем цвет
            viewModel.checkHasChanges(
                title = itemWithoutColor.title,
                details = itemWithoutColor.details,
                timestamp = itemWithoutColor.timestamp,
                colorTag = 0xFFFF0000.toInt(),
                displayOption = itemWithoutColor.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(viewModel.hasChanges.value, "Изменения должны быть обнаружены при установке цвета")
        }
    }

    @Test
    fun whenCheckHasChangesWithColorTagFromValueToNull_thenDetectsChanges() {
        runTest {
            // Given - Элемент с цветом
            val itemWithColor =
                testItem.copy(
                    colorTag = 0xFFFF0000.toInt(),
                )
            val insertedId = repository.insertItem(itemWithColor)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()

            // When - Убираем цвет
            viewModel.checkHasChanges(
                title = itemWithColor.title,
                details = itemWithColor.details,
                timestamp = itemWithColor.timestamp,
                colorTag = null,
                displayOption = itemWithColor.displayOption,
            )

            // Then - Изменения должны быть обнаружены
            assertTrue(viewModel.hasChanges.value, "Изменения должны быть обнаружены при удалении цвета")
        }
    }

    @Test
    fun whenResetHasChanges_thenChangesFlagReset() {
        runTest {
            // Given - Элемент с обнаруженными изменениями
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.checkHasChanges(
                title = "Измененное название",
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )
            assertTrue(viewModel.hasChanges.value, "Изменения должны быть обнаружены")

            // When - Сбрасываем изменения
            viewModel.resetHasChanges()

            // Then - Флаг изменений должен быть сброшен
            assertFalse(viewModel.hasChanges.value, "Флаг изменений должен быть сброшен")
        }
    }

    /**
     * Fake ResourceProvider для тестирования.
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
                else -> "Строка по умолчанию"
            }

        override fun getQuantityString(
            resId: Int,
            quantity: Int,
            vararg formatArgs: Any,
        ): String = "$quantity"
    }
}
