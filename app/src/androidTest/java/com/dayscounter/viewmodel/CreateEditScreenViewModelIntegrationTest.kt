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
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

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

    private val testItem =
        Item(
            id = 1L,
            title = "Тестовое событие",
            details = "Описание события",
            timestamp = System.currentTimeMillis(),
            colorTag = null,
            displayOption = DisplayOption.DAY,
        )

    @Before
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

    @After
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
                    NoOpLogger(),
                    savedStateHandle,
                )

            // When - Создаем элемент
            viewModel.createItem(newItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть в базе данных
            val allItems = repository.getAllItems().first()
            assertEquals("Должен быть один элемент в базе данных", 1, allItems.size)
            assertEquals("Название должно совпадать", "Новое событие", allItems[0].title)
            assertEquals("Описание должно совпадать", "Новое описание", allItems[0].details)
            assertEquals("Опция отображения должна совпадать", DisplayOption.MONTH_DAY, allItems[0].displayOption)
            assertNotNull("Цвет должен быть установлен", allItems[0].colorTag)
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
                    NoOpLogger(),
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Элемент должен быть обновлен в базе данных
            val itemFromDb = repository.getItemById(insertedId)
            assertNotNull("Элемент должен существовать в базе данных", itemFromDb)
            assertEquals("Название должно быть обновлено", "Обновленное название", itemFromDb!!.title)
            assertEquals("Описание должно быть обновлено", "Обновленное описание", itemFromDb.details)
            assertEquals("Опция отображения должна быть обновлена", DisplayOption.YEAR_MONTH_DAY, itemFromDb.displayOption)
            assertEquals("Цвет должен быть обновлен", 0xFF00FF00.toInt(), itemFromDb.colorTag)
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
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Элемент должен быть загружен
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = viewModel.uiState.value
            assertTrue("Должно быть состояние Success", uiState is CreateEditScreenState.Success)
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Название должно совпадать", "Тестовое событие", successState.item.title)
            assertEquals("Описание должно совпадать", "Описание события", successState.item.details)
            assertEquals("Дата должна совпадать", testItem.timestamp, successState.item.timestamp)
            assertEquals("Опция отображения должна совпадать", DisplayOption.DAY, successState.item.displayOption)
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
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Должно быть состояние Error
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = viewModel.uiState.value
            assertTrue("Должно быть состояние Error", uiState is CreateEditScreenState.Error)
            val errorState = uiState as CreateEditScreenState.Error
            assertEquals("Сообщение об ошибке должно быть корректным", "Событие не найдено", errorState.message)
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
                    NoOpLogger(),
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
            assertTrue("Изменения должны быть обнаружены", viewModel.hasChanges.value)
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
                    NoOpLogger(),
                    savedStateHandle,
                )
            testDispatcher.scheduler.advanceUntilIdle()
            viewModel.updateItem(updatedItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Все изменения должны быть сохранены
            val itemFromDb = repository.getItemById(insertedId)
            assertNotNull("Элемент должен существовать", itemFromDb)
            assertEquals("Название должно быть обновлено", "Полностью новое название", itemFromDb!!.title)
            assertEquals("Описание должно быть обновлено", "Полностью новое описание", itemFromDb.details)
            assertEquals("Дата должна быть обновлена", testItem.timestamp + 172800000L, itemFromDb.timestamp)
            assertEquals("Цвет должен быть обновлен", 0xFF0000FF.toInt(), itemFromDb.colorTag)
            assertEquals("Опция отображения должна быть обновлена", DisplayOption.YEAR_MONTH_DAY, itemFromDb.displayOption)
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
                    NoOpLogger(),
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
                    NoOpLogger(),
                    savedStateHandle,
                )
            viewModel2.createItem(item2)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - Все элементы должны быть в базе данных
            val allItems = repository.getAllItems().first()
            assertEquals("Должны быть два элемента в базе данных", 2, allItems.size)
            assertTrue("Первый элемент должен существовать", allItems.any { it.title == "Событие 1" })
            assertTrue("Второй элемент должен существовать", allItems.any { it.title == "Событие 2" })
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
                    NoOpLogger(),
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
                    NoOpLogger(),
                    savedStateHandle,
                )

            // Then - Изменения должны быть сохранены
            testDispatcher.scheduler.advanceUntilIdle()
            val uiState = viewModel2.uiState.value
            assertTrue("Должно быть состояние Success", uiState is CreateEditScreenState.Success)
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Название должно быть обновленным", "Обновленное название", successState.item.title)
            assertEquals("Оригинальный элемент должен быть обновленным", updatedItem, viewModel2.originalItem.value)
            assertFalse("Изменений не должно быть при загрузке обновленного элемента", viewModel2.hasChanges.value)
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
                    NoOpLogger(),
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
            assertTrue("Изменения должны быть обнаружены при установке цвета", viewModel.hasChanges.value)
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
                    NoOpLogger(),
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
            assertTrue("Изменения должны быть обнаружены при удалении цвета", viewModel.hasChanges.value)
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
                    NoOpLogger(),
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
            assertTrue("Изменения должны быть обнаружены", viewModel.hasChanges.value)

            // When - Сбрасываем изменения
            viewModel.resetHasChanges()

            // Then - Флаг изменений должен быть сброшен
            assertFalse("Флаг изменений должен быть сброшен", viewModel.hasChanges.value)
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
