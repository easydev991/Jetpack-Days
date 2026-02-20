package com.dayscounter.viewmodel

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.test.MainDispatcherRule
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционные тесты для DetailScreenViewModel.
 * Тестируют взаимодействие ViewModel с реальным Repository и базой данных.
 */
@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailScreenViewModelIntegrationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepositoryImpl
    private lateinit var viewModel: DetailScreenViewModel
    private lateinit var context: Context

    private val testItemId = 1L
    private val testItem =
        Item(
            id = 0L,  // Используем id = 0L, чтобы Room генерировал новый ID
            title = "Тестовое событие",
            details = "Описание события",
            timestamp = System.currentTimeMillis(),
            colorTag = null,
            displayOption = DisplayOption.DAY,
        )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // Создаем новую in-memory базу для каждого теста
        database =
            Room
                .inMemoryDatabaseBuilder(
                    context,
                    DaysDatabase::class.java,
                ).allowMainThreadQueries()
                .build()

        repository = ItemRepositoryImpl(database.itemDao())

        // Очищаем базу данных перед каждым тестом
        database.clearAllTables()
    }

    @After
    fun tearDown() {
        // Закрываем базу данных после каждого теста
        database.close()
    }

    @Test
    fun whenItemExistsInDatabase_thenLoadsSuccessfully() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                // Проверяем начальное состояние Loading
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                // Проверяем состояние Success
                val successState = awaitItem()
                assertTrue(
                    "Состояние должно быть Success",
                    successState is DetailScreenState.Success
                )
                val success = successState as DetailScreenState.Success
                assertEquals("Тестовое событие", success.item.title)
                assertEquals("Описание события", success.item.details)
                assertEquals(insertedId, success.item.id)
            }
        }
    }

    @Test
    fun whenItemDoesNotExistInDatabase_thenRemainsInLoadingState() {
        runTest {
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Подписываемся на uiState для активации StateFlow
            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            // Ждем некоторое время
            advanceUntilIdle()

            // Состояние должно остаться Loading
            assertTrue(
                "Состояние должно быть Loading",
                viewModel.uiState.value is DetailScreenState.Loading,
            )
        }
    }

    @Test
    fun whenRequestDelete_thenShowsDeleteDialog() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
            backgroundScope.launch {
                viewModel.uiState.collect {}
            }

            viewModel.requestDelete()
            assertTrue(
                "Диалог удаления должен быть показан",
                viewModel.showDeleteDialog.value,
            )
        }
    }

    @Test
    fun whenCancelDelete_thenHidesDeleteDialogAndKeepsItem() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)
            backgroundScope.launch {
                viewModel.uiState.collect {}
            }
            viewModel.requestDelete()

            viewModel.cancelDelete()
            assertFalse(
                "Диалог удаления должен быть скрыт",
                viewModel.showDeleteDialog.value,
            )
            val item = repository.getItemById(insertedId)
            assertNotNull("Элемент должен остаться в базе данных", item)
            assertEquals("Тестовое событие", item!!.title)
        }
    }

    @Test
    fun whenItemWithColorTag_thenLoadsCorrectly() {
        runTest {
            val itemWithColorTag =
                testItem.copy(
                    colorTag = 0xFFFF00FF.toInt(),
                )
            val insertedId = repository.insertItem(itemWithColorTag)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                val successState = awaitItem()
                assertTrue(
                    "Состояние должно быть Success",
                    successState is DetailScreenState.Success
                )
                val success = successState as DetailScreenState.Success
                assertEquals(
                    0xFFFF00FF.toInt(),
                    success.item.colorTag,
                )
            }
        }
    }

    @Test
    fun whenFlowEmitsNewItem_thenViewModelUpdatesState() {
        runTest {
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                val initialState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Success",
                    initialState is DetailScreenState.Success,
                )
                val initialSuccess = initialState as DetailScreenState.Success
                assertEquals("Тестовое событие", initialSuccess.item.title)

                val updatedItem =
                    initialSuccess.item.copy(
                        title = "Обновленное событие",
                        details = "Обновленное описание",
                    )
                repository.updateItem(updatedItem)

                val updatedState = awaitItem()
                assertTrue(
                    "Обновленное состояние должно быть Success",
                    updatedState is DetailScreenState.Success,
                )
                val updatedSuccess = updatedState as DetailScreenState.Success
                assertEquals(
                    "Обновленное событие",
                    updatedSuccess.item.title,
                )
                assertEquals(
                    "Обновленное описание",
                    updatedSuccess.item.details,
                )
            }
        }
    }

    @Test
    fun whenMultipleItemsInDatabase_thenLoadsCorrectItemById() {
        runTest {
            val item1 =
                testItem.copy(title = "Событие 1", timestamp = 1000000000000L)
            val item2 =
                testItem.copy(title = "Событие 2", timestamp = 2000000000000L)
            repository.insertItem(item1)
            val id2 = repository.insertItem(item2)

            val savedStateHandle = SavedStateHandle(mapOf("itemId" to id2))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                val successState = awaitItem()
                assertTrue(
                    "Состояние должно быть Success",
                    successState is DetailScreenState.Success
                )
                val success = successState as DetailScreenState.Success
                assertEquals(
                    "Событие 2",
                    success.item.title,
                )
            }
        }
    }

    @Test
    fun whenItemWithEmptyDetails_thenLoadsCorrectly() {
        runTest {
            val itemWithEmptyDetails =
                testItem.copy(
                    details = "",
                )
            val insertedId = repository.insertItem(itemWithEmptyDetails)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                val successState = awaitItem()
                assertTrue(
                    "Состояние должно быть Success",
                    successState is DetailScreenState.Success
                )
                val success = successState as DetailScreenState.Success
                assertEquals("", success.item.details)
            }
        }
    }

    @Test
    fun whenItemWithSpecificTimestamp_thenLoadsCorrectly() {
        runTest {
            val specificTimestamp = 1234567890000L
            val itemWithTimestamp =
                testItem.copy(
                    timestamp = specificTimestamp,
                )
            val insertedId = repository.insertItem(itemWithTimestamp)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Тестируем эмиссии StateFlow с помощью Turbine
            viewModel.uiState.test {
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading,
                )

                val successState = awaitItem()
                assertTrue(
                    "Состояние должно быть Success",
                    successState is DetailScreenState.Success
                )
                val success = successState as DetailScreenState.Success
                assertEquals(
                    specificTimestamp,
                    success.item.timestamp,
                )
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
