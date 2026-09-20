# Examples — канонические примеры

Полные образцы тестов из реального кода JetpackDays. Используй как
референс при написании новых.

---

## 1. Use Case без зависимостей + `Result<T>` + Clock

`BuildReminderUseCaseTest` — clock-зависимый use case с валидацией.

```kotlin
package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

class BuildReminderUseCaseTest {
    private val fixedInstant = Instant.parse("2026-04-27T10:15:30Z")
    private val zoneId = ZoneId.of("Europe/Moscow")
    private val clock: Clock = Clock.fixed(fixedInstant, zoneId)

    @Test
    fun invoke_whenAfterIntervalDays_thenKeepsCurrentTimeOfDayAndAddsDays() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val request = ReminderRequest(
            itemId = 42L,
            mode = ReminderMode.AFTER_INTERVAL,
            afterAmount = 3,
            afterUnit = ReminderIntervalUnit.DAY
        )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess, "Результат должен быть успешным")
        val reminder = result.getOrThrow()
        val expected = ZonedDateTime.ofInstant(fixedInstant, zoneId).plusDays(3)
        assertEquals(expected.toInstant().toEpochMilli(), reminder.targetEpochMillis)
        assertEquals(3, reminder.intervalAmount)
        assertEquals(ReminderIntervalUnit.DAY, reminder.intervalUnit)
    }

    @Test
    fun invoke_whenAfterIntervalAmountIsZero_thenReturnsFailure() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val request = ReminderRequest(
            itemId = 42L,
            mode = ReminderMode.AFTER_INTERVAL,
            afterAmount = 0,
            afterUnit = ReminderIntervalUnit.WEEK
        )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure, "Должна быть ошибка валидации")
    }
}
```

---

## 2. ViewModel с FakeItemRepository + DataStore

`MainScreenViewModelTest` (фрагмент). Полный файл — в
`app/src/test/java/com/dayscounter/ui/viewmodel/MainScreenViewModelTest.kt`.

```kotlin
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

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
    private lateinit var repository: FakeItemRepository
    private lateinit var sortOrderFlow: MutableStateFlow<SortOrder>
    private lateinit var dataStore: AppSettingsDataStore
    private lateinit var logger: Logger
    private lateinit var viewModel: MainScreenViewModel
    private lateinit var testDispatcher: TestDispatcher
    private val sortOrderSlot = slot<SortOrder>()

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
        sortOrderFlow = MutableStateFlow(SortOrder.DESCENDING)
        dataStore = mockk(relaxed = true)
        logger = mockk(relaxed = true)

        every { dataStore.sortOrder } returns sortOrderFlow
        coEvery { dataStore.setSortOrder(capture(sortOrderSlot)) } answers {
            sortOrderFlow.value = sortOrderSlot.captured
        }

        viewModel = MainScreenViewModel(repository, dataStore, logger)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenSortOrderChanged_thenSortsItems() = runTest {
        // Given
        val items = listOf(
            Item(id = 1L, title = "Событие 1", details = "Детали 1",
                 timestamp = System.currentTimeMillis() - 86400000,
                 colorTag = null, displayOption = DisplayOption.DAY),
            Item(id = 2L, title = "Событие 2", details = "Детали 2",
                 timestamp = System.currentTimeMillis(),
                 colorTag = null, displayOption = DisplayOption.DAY)
        )
        repository.setItems(items)

        // When
        viewModel.updateSortOrder(SortOrder.ASCENDING)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MainScreenState.Success, "Должно быть состояние Success")
        val successState = uiState as MainScreenState.Success
        assertEquals(2, successState.items.size)
        assertEquals(1L, successState.items[0].id, "Первый элемент должен быть старым")
        assertEquals(SortOrder.ASCENDING, viewModel.sortOrder.value)
        coVerify { dataStore.setSortOrder(SortOrder.ASCENDING) }
    }

    // Канонический FakeItemRepository (setItems/containsItem + все
    // методы ItemRepository) — references/fakes.md,
    // раздел «Канонический FakeItemRepository».
    private class FakeItemRepository : ItemRepository { /* см. fakes.md */ }
}
```

---

## 3. Mapper (entity ↔ domain)

Канонический пример — `references/data-layer.md`, раздел
«Mapper (entity ↔ domain)»: три теста, включая
`roundTripConversion_thenPreservesAllFields` со структурным
сравнением `assertEquals(original, converted)`.

---

## 4. JSON-парсинг с реальными файлами

Канонический пример — `references/data-layer.md`, раздел
«JSON парсинг с реальными файлами»: два теста, `loadResource`
и список тестовых JSON-ресурсов.

---

## Где искать ещё

- `app/src/test/java/com/dayscounter/domain/usecase/CalculateDaysDifferenceUseCaseTest.kt` — pure use case, временные кейсы, явный `currentDate`.
- `app/src/test/java/com/dayscounter/domain/usecase/FormatDaysTextUseCaseTest.kt` — MockK для одного интерфейса + `StubResourceProvider`.
- `app/src/test/java/com/dayscounter/domain/model/DaysDifferenceTest.kt` — sealed-class проверки.
- `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt` — `SavedStateHandle`, `currentTimeMillisProvider`, `FakeReminderManager`.
- `app/src/test/java/com/dayscounter/data/repository/ReminderRepositoryImplTest.kt` — `FakeReminderDao` для repository.
- `app/src/test/java/com/dayscounter/reminder/DefaultReminderManagerTest.kt` — несколько Fake'ов одновременно.
- `app/src/test/java/com/dayscounter/data/database/DisplayOptionConverterTest.kt` — converter + round-trip.
- `app/src/test/java/com/dayscounter/ui/state/DetailScreenStateTest.kt` — sealed-state equality.
