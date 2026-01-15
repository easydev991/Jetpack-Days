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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для CreateEditScreenViewModel.
 * Тестируют взаимодействие ViewModel с реальным Repository и базой данных.
 * Большинство тестов падают, потому что не дожидаются асинхронных задач.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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
        Dispatchers.setMain(testDispatcher)

        context = ApplicationProvider.getApplicationContext()

        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    DaysDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        repository = ItemRepositoryImpl(database.itemDao())
        resourceProvider = FakeResourceProvider()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenCreateItem_thenItemSavedInDatabase() {
        runTest {
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

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.createItem(newItem)

            val allItems = repository.getAllItems().first()
            assertEquals(1, allItems.size)
            assertEquals("Новое событие", allItems[0].title)
            assertEquals("Новое описание", allItems[0].details)
            assertEquals(
                DisplayOption.MONTH_DAY,
                allItems[0].displayOption,
            )
            assertNotNull("Цвет должен быть установлен", allItems[0].colorTag)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenUpdateItem_thenItemUpdatedInDatabase() {
        runTest {
            val uniqueTestItem = testItem.copy(timestamp = System.currentTimeMillis())
            val insertedId = repository.insertItem(uniqueTestItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            val updatedItem =
                uniqueTestItem.copy(
                    id = insertedId,
                    title = "Обновленное название",
                    details = "Обновленное описание",
                    timestamp = uniqueTestItem.timestamp + 86400000L,
                    colorTag = 0xFF00FF00.toInt(),
                    displayOption = DisplayOption.YEAR_MONTH_DAY,
                )

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            val uiState = viewModel.uiState.value
            assertTrue("Элемент должен быть загружен", uiState is CreateEditScreenState.Success)
            viewModel.updateItem(updatedItem)

            val itemFromDb = repository.getItemById(insertedId)
            assertNotNull("Элемент должен существовать в базе данных", itemFromDb)
            assertEquals(
                "Обновленное название",
                itemFromDb!!.title,
            )
            assertEquals(
                "Обновленное описание",
                itemFromDb.details,
                "Описание должно быть обновлено",
            )
            assertEquals(
                DisplayOption.YEAR_MONTH_DAY,
                itemFromDb.displayOption,
            )
            assertEquals(0xFF00FF00.toInt(), itemFromDb.colorTag)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenLoadExistingItem_thenItemLoadedFromDatabase() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            val uiState = viewModel.uiState.value
            assertTrue("Должно быть состояние Success", uiState is CreateEditScreenState.Success)
            val successState = uiState as CreateEditScreenState.Success
            assertEquals("Тестовое событие", successState.item.title)
            assertEquals("Описание события", successState.item.details)
            assertEquals(testItem.timestamp, successState.item.timestamp)
            assertEquals(DisplayOption.DAY, successState.item.displayOption)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenLoadNonExistentItem_thenShowsError() {
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            val uiState = viewModel.uiState.value
            assertTrue("Должно быть состояние Error", uiState is CreateEditScreenState.Error)
            val errorState = uiState as CreateEditScreenState.Error
            assertEquals("Событие не найдено", errorState.message)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenCheckHasChanges_thenCorrectlyDetectsChanges() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.checkHasChanges(
                title = "Новое название",
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )

            assertTrue("Изменения должны быть обнаружены", viewModel.hasChanges.value)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenUpdateMultipleFields_thenAllChangesPersistedInDatabase() {
        runTest {
            val uniqueTestItem = testItem.copy(timestamp = System.currentTimeMillis())
            val insertedId = repository.insertItem(uniqueTestItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            val updatedItem =
                uniqueTestItem.copy(
                    id = insertedId,
                    title = "Полностью новое название",
                    details = "Полностью новое описание",
                    timestamp = uniqueTestItem.timestamp + 172800000L,
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

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.updateItem(updatedItem)

            val itemFromDb = repository.getItemById(insertedId)
            assertNotNull("Элемент должен существовать", itemFromDb)
            assertEquals(
                "Полностью новое название",
                itemFromDb!!.title,
            )
            assertEquals(
                "Полностью новое описание",
                itemFromDb.details,
            )
            assertEquals(
                testItem.timestamp + 172800000L,
                itemFromDb.timestamp,
            )
            assertEquals(0xFF0000FF.toInt(), itemFromDb.colorTag)
            assertEquals(
                DisplayOption.YEAR_MONTH_DAY,
                itemFromDb.displayOption,
            )
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenCreateMultipleItems_thenAllItemsSavedInDatabase() {
        runTest {
            val savedStateHandle = SavedStateHandle()
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

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.createItem(item1)

            val savedStateHandle2 = SavedStateHandle()
            val viewModel2 =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle2,
                )

            backgroundScope.launch {
                viewModel2.uiState.collect {}
            }

            viewModel2.createItem(item2)

            val allItems = repository.getAllItems().first()
            assertEquals(2, allItems.size)
            assertTrue(
                "Первый элемент должен существовать",
                allItems.any { it.title == "Событие 1" },
            )
            assertTrue(
                "Второй элемент должен существовать",
                allItems.any { it.title == "Событие 2" },
            )
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenUpdateAndThenLoadItem_thenChangesPersisted() {
        runTest {
            val uniqueTestItem = testItem.copy(timestamp = System.currentTimeMillis())
            val insertedId = repository.insertItem(uniqueTestItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            val updatedItem = uniqueTestItem.copy(id = insertedId, title = "Обновленное название")

            val viewModel1 =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel1.uiState.collect {}
            }

            viewModel1.updateItem(updatedItem)

            val viewModel2 =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel2.uiState.collect {}
            }

            val uiState = viewModel2.uiState.value
            assertTrue("Должно быть состояние Success", uiState is CreateEditScreenState.Success)
            val successState = uiState as CreateEditScreenState.Success
            assertEquals(
                "Обновленное название",
                successState.item.title,
            )
            assertEquals(
                updatedItem,
                viewModel2.originalItem.value,
            )
            assertFalse("Изменений не должно быть", viewModel2.hasChanges.value)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenCheckHasChangesWithColorTagFromValueToNull_thenDetectsChanges() {
        runTest {
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

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.checkHasChanges(
                title = itemWithoutColor.title,
                details = itemWithoutColor.details,
                timestamp = itemWithoutColor.timestamp,
                colorTag = 0xFFFF0000.toInt(),
                displayOption = itemWithoutColor.displayOption,
            )

            assertTrue("Изменения должны быть обнаружены", viewModel.hasChanges.value)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenCheckHasChangesWithColorTagFromValueToNull_thenDetectsChanges2() {
        runTest {
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

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.checkHasChanges(
                title = itemWithColor.title,
                details = itemWithColor.details,
                timestamp = itemWithColor.timestamp,
                colorTag = null,
                displayOption = itemWithColor.displayOption,
            )

            assertTrue("Изменения должны быть обнаружены", viewModel.hasChanges.value)
        }
    }

    @Ignore("Тест написан с ошибками")
    @Test
    fun whenResetHasChanges_thenChangesFlagReset() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            viewModel =
                CreateEditScreenViewModel(
                    repository,
                    resourceProvider,
                    NoOpLogger(),
                    savedStateHandle,
                )

            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.checkHasChanges(
                title = "Измененное название",
                details = testItem.details,
                timestamp = testItem.timestamp,
                colorTag = testItem.colorTag,
                displayOption = testItem.displayOption,
            )
            assertTrue("Изменения должны быть обнаружены", viewModel.hasChanges.value)

            viewModel.resetHasChanges()

            assertFalse("Флаг изменений должен быть сброшен", viewModel.hasChanges.value)
        }
    }

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

        override fun getYearsString(quantity: Int): String = "$quantity лет"

        override fun getMonthsString(quantity: Int): String = "$quantity месяцев"
    }
}
