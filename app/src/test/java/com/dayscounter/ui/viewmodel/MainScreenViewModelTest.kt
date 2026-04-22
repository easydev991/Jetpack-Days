package com.dayscounter.ui.viewmodel

import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
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
    private lateinit var sortOrderFlow: MutableStateFlow<SortOrder>
    private lateinit var dataStore: AppSettingsDataStore
    private lateinit var logger: Logger
    private lateinit var viewModel: MainScreenViewModel
    private lateinit var testDispatcher: TestDispatcher
    private val sortOrderSlot = slot<SortOrder>()
    private lateinit var colorTagFilterFlow: MutableStateFlow<Int?>

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
        sortOrderFlow = MutableStateFlow(SortOrder.DESCENDING)
        colorTagFilterFlow = MutableStateFlow(null)
        dataStore = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        // Настраиваем dataStore для возврата и обновления sortOrder
        every { dataStore.sortOrder } returns sortOrderFlow
        coEvery { dataStore.setSortOrder(capture(sortOrderSlot)) } answers {
            sortOrderFlow.value = sortOrderSlot.captured
        }
        // Настраиваем dataStore для colorTagFilter
        every { dataStore.mainScreenColorTagFilter } returns colorTagFilterFlow
        coEvery { dataStore.setMainScreenColorTagFilter(any()) } answers {
            colorTagFilterFlow.value = firstArg()
        }

        viewModel = MainScreenViewModel(repository, dataStore, logger)
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
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
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
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Новый год",
                        details = "Праздник",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
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
            assertEquals(
                "День рождения",
                successState.items[0].title,
                "Должен быть правильный элемент"
            )
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
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Новый год",
                        details = "Праздник",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
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
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
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
            assertEquals(
                SortOrder.ASCENDING,
                viewModel.sortOrder.value,
                "Порядок сортировки должен обновиться"
            )
            coVerify { dataStore.setSortOrder(SortOrder.ASCENDING) }
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
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis(),
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
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
                    displayOption = DisplayOption.DAY
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
            assertEquals(
                0,
                viewModel.itemsCount.value,
                "Количество элементов в ViewModel должно быть 0"
            )
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
                        displayOption = DisplayOption.DAY
                    )
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
                        displayOption = DisplayOption.DAY
                    )
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
                        displayOption = DisplayOption.DAY
                    )
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

    // ========================================================================
    // TDD-тесты для фильтрации по цветовым тегам (Этап 1)
    // ========================================================================

    @Test
    fun `when color filter disabled then shows all items`() {
        runTest {
            // Given - элементы с разными colorTag
            val purple = 0xFF570CF0.toInt()
            val blue = 0xFF2196F3.toInt()
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Событие 1",
                        details = "Детали 1",
                        timestamp = System.currentTimeMillis(),
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали 2",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = blue,
                        displayOption = DisplayOption.DAY
                    )
                )
            repository.setItems(items)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - фильтр по цвету не установлен (по умолчанию null)
            // Then - отображаются все записи
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(2, successState.items.size, "Должны отображаться все 2 элемента")
        }
    }

    @Test
    fun `when color filter selected then shows only matching items`() {
        runTest {
            // Given - элементы с разными colorTag
            val purple = 0xFF570CF0.toInt()
            val blue = 0xFF2196F3.toInt()
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Фиолетовое событие",
                        details = "Детали",
                        timestamp = System.currentTimeMillis(),
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Синее событие",
                        details = "Детали",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = blue,
                        displayOption = DisplayOption.DAY
                    )
                )
            repository.setItems(items)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - выбираем фильтр по фиолетовому цвету
            viewModel.updateSelectedColorTag(purple)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - отображается только элемент с purple
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(1, successState.items.size, "Должен быть 1 элемент")
            assertEquals(purple, successState.items[0].colorTag, "Элемент должен иметь фиолетовый тег")
        }
    }

    @Test
    fun `when color filter AND search query then applies both filters`() {
        runTest {
            // Given - элементы с разными цветами и разными названиями
            val purple = 0xFF570CF0.toInt()
            val blue = 0xFF2196F3.toInt()
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Праздник",
                        details = "Важное событие",
                        timestamp = System.currentTimeMillis(),
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Встреча",
                        details = "Рабочая встреча",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = blue,
                        displayOption = DisplayOption.DAY
                    )
                )
            repository.setItems(items)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - выбираем фильтр по цвету + поисковый запрос
            viewModel.updateSelectedColorTag(purple)
            viewModel.updateSearchQuery("праздник")
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - применяется пересечение фильтров (AND)
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(1, successState.items.size, "Должен быть 1 элемент")
            assertEquals("Праздник", successState.items[0].title)
            assertEquals(purple, successState.items[0].colorTag)
        }
    }

    @Test
    fun `when items without colorTag then they do not appear in color filter results`() {
        runTest {
            // Given - элементы с colorTag и без
            val purple = 0xFF570CF0.toInt()
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "С тегом",
                        details = "Детали",
                        timestamp = System.currentTimeMillis(),
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Без тега",
                        details = "Детали",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
                )
            repository.setItems(items)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - выбираем фильтр по цвету
            viewModel.updateSelectedColorTag(purple)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - только элемент с colorTag
            val uiState = viewModel.uiState.value
            assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
            val successState = uiState as MainScreenState.Success
            assertEquals(1, successState.items.size, "Только элемент с colorTag")
            assertEquals(purple, successState.items[0].colorTag)
        }
    }

    @Test
    fun `availableColorTags contains only unique colors from items`() {
        runTest {
            // Given - элементы с повторяющимися цветами
            val purple = 0xFF570CF0.toInt()
            val blue = 0xFF2196F3.toInt()
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Событие 1",
                        details = "Детали",
                        timestamp = System.currentTimeMillis(),
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Событие 2",
                        details = "Детали",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 3L,
                        title = "Событие 3",
                        details = "Детали",
                        timestamp = System.currentTimeMillis() - 2 * 86400000,
                        colorTag = blue,
                        displayOption = DisplayOption.DAY
                    )
                )
            repository.setItems(items)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - получаем доступные цвета
            val availableColors = viewModel.availableColorTags.value

            // Then - только уникальные цвета без повторений
            assertEquals(2, availableColors.size, "Должны быть 2 уникальных цвета")
            assertTrue(availableColors.contains(purple), "Должен содержать фиолетовый")
            assertTrue(availableColors.contains(blue), "Должен содержать синий")
        }
    }

    @Test
    fun `when last item with color deleted then availableColors updates`() {
        runTest {
            // Given - два элемента с разными цветами
            val purple = 0xFF570CF0.toInt()
            val blue = 0xFF2196F3.toInt()
            val purpleItem =
                Item(
                    id = 1L,
                    title = "Фиолетовое",
                    details = "Детали",
                    timestamp = System.currentTimeMillis(),
                    colorTag = purple,
                    displayOption = DisplayOption.DAY
                )
            val blueItem =
                Item(
                    id = 2L,
                    title = "Синее",
                    details = "Детали",
                    timestamp = System.currentTimeMillis() - 86400000,
                    colorTag = blue,
                    displayOption = DisplayOption.DAY
                )
            repository.setItems(listOf(purpleItem, blueItem))
            testDispatcher.scheduler.advanceUntilIdle()

            // Verify initial state - two colors available
            assertEquals(2, viewModel.availableColorTags.value.size, "Изначально 2 цвета")

            // When - удаляем фиолетовый элемент
            viewModel.deleteItem(purpleItem)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - доступные цвета обновляются
            val availableColors = viewModel.availableColorTags.value
            assertEquals(1, availableColors.size, "После удаления должен остаться 1 цвет")
            assertTrue(availableColors.contains(blue), "Должен остаться только синий")
        }
    }

    @Test
    fun `when selected color disappears then filter auto-resets`() {
        runTest {
            // Given - элемент с уникальным цветом
            val purple = 0xFF570CF0.toInt()
            val item =
                Item(
                    id = 1L,
                    title = "Уникальное",
                    details = "Детали",
                    timestamp = System.currentTimeMillis(),
                    colorTag = purple,
                    displayOption = DisplayOption.DAY
                )
            repository.setItems(listOf(item))
            testDispatcher.scheduler.advanceUntilIdle()

            // Select the filter
            viewModel.updateSelectedColorTag(purple)
            testDispatcher.scheduler.advanceUntilIdle()
            assertEquals(purple, viewModel.selectedColorTag.value, "Фильтр установлен")

            // When - удаляем элемент с этим цветом
            viewModel.deleteItem(item)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - фильтр автоматически сбрасывается на null
            assertEquals(
                null,
                viewModel.selectedColorTag.value,
                "Фильтр должен сброситься на null когда выбранный цвет исчезает"
            )
        }
    }

    @Test
    fun `clearColorTagFilter resets selected color to null`() {
        runTest {
            // Given - элемент с цветом
            val purple = 0xFF570CF0.toInt()
            val item =
                Item(
                    id = 1L,
                    title = "Событие",
                    details = "Детали",
                    timestamp = System.currentTimeMillis(),
                    colorTag = purple,
                    displayOption = DisplayOption.DAY
                )
            repository.setItems(listOf(item))
            testDispatcher.scheduler.advanceUntilIdle()

            // Select filter
            viewModel.updateSelectedColorTag(purple)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - очищаем фильтр
            viewModel.clearColorTagFilter()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - фильтр сброшен
            assertEquals(null, viewModel.selectedColorTag.value, "Фильтр должен быть null")
        }
    }

    // ========================================================================
    // TDD-тесты для персистентности фильтра (Этап 2)
    // ========================================================================

    @Test
    fun `when color filter saved in DataStore then restored after ViewModel init`() {
        runTest {
            // Given - элемент с цветом и пред установленный фильтр в DataStore
            val purple = 0xFF570CF0.toInt()
            val item =
                Item(
                    id = 1L,
                    title = "Фиолетовое",
                    details = "Детали",
                    timestamp = System.currentTimeMillis(),
                    colorTag = purple,
                    displayOption = DisplayOption.DAY
                )
            repository.setItems(listOf(item))
            colorTagFilterFlow.value = purple // Эмулируем сохранённый фильтр

            // When - создаём ViewModel (имитирует перезапуск приложения)
            val viewModel2 = MainScreenViewModel(repository, dataStore, logger)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - фильтр восстановлен из DataStore
            assertEquals(purple, viewModel2.selectedColorTag.value, "Фильтр должен восстановиться из DataStore")
        }
    }

    @Test
    fun `when color filter changed then saved to DataStore`() {
        runTest {
            // Given - элемент с цветом
            val purple = 0xFF570CF0.toInt()
            val blue = 0xFF2196F3.toInt()
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Фиолетовое",
                        details = "Детали",
                        timestamp = System.currentTimeMillis(),
                        colorTag = purple,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Синее",
                        details = "Детали",
                        timestamp = System.currentTimeMillis() - 86400000,
                        colorTag = blue,
                        displayOption = DisplayOption.DAY
                    )
                )
            repository.setItems(items)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - меняем фильтр
            viewModel.updateSelectedColorTag(blue)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - фильтр сохранён в DataStore
            coVerify { dataStore.setMainScreenColorTagFilter(blue) }
        }
    }

    @Test
    fun `when clearColorTagFilter called then null saved to DataStore`() {
        runTest {
            // Given - элемент с цветом и установленный фильтр
            val purple = 0xFF570CF0.toInt()
            val item =
                Item(
                    id = 1L,
                    title = "Фиолетовое",
                    details = "Детали",
                    timestamp = System.currentTimeMillis(),
                    colorTag = purple,
                    displayOption = DisplayOption.DAY
                )
            repository.setItems(listOf(item))
            viewModel.updateSelectedColorTag(purple)
            testDispatcher.scheduler.advanceUntilIdle()

            // When - очищаем фильтр
            viewModel.clearColorTagFilter()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then - null сохранён в DataStore
            coVerify { dataStore.setMainScreenColorTagFilter(null) }
        }
    }

    /**
     * Fake repository для тестирования.
     */
    private class FakeItemRepository : ItemRepository {
        private val _items = MutableStateFlow<List<Item>>(emptyList())
        val items: Flow<List<Item>> = _items

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

        override fun getItemFlow(id: Long): Flow<Item?> =
            _items.map { items ->
                items.find { it.id == id }
            }

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
