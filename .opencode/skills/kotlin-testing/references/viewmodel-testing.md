# ViewModel Testing

Это самый частый кейс в проекте: тест ViewModel через
`FakeItemRepository` + `StandardTestDispatcher`. Интеграционные
тесты ViewModel (с реальным Room) **запрещены** AGENTS.md.

## Полный шаблон

```kotlin
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MyViewModelTest {
    private lateinit var repository: FakeItemRepository
    private lateinit var viewModel: MyViewModel
    private lateinit var testDispatcher: TestDispatcher

    @BeforeEach
    fun setUp() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)

        repository = FakeItemRepository()
        viewModel = MyViewModel(
            repository = repository,
            logger = NoOpLogger(),                    // предпочтительно (или mockk(relaxed = true))
            savedStateHandle = SavedStateHandle()     // если нужен itemId
        )
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun whenAction_thenExpectedResult() = runTest {
        // Given
        repository.setItems(listOf(testItem))

        // When
        viewModel.someAction()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertTrue(uiState is MyUiState.Success)
    }
}
```

## Шесть обязательных элементов

1. `@OptIn(ExperimentalCoroutinesApi::class)` на классе.
2. `testDispatcher = StandardTestDispatcher()` в `@BeforeEach`.
3. `Dispatchers.setMain(testDispatcher)` — иначе `viewModelScope`
   по умолчанию использует `Main`, который не контролируется тестом.
4. `Dispatchers.resetMain()` в `@AfterEach` — обязательно,
   иначе следующий тест в том же процессе JVM получит грязный
   диспетчер.
5. `NoOpLogger()` (или `mockk(relaxed = true)` для `Logger`) —
   если ViewModel принимает `Logger`. Без замены
   `android.util.Log` падает в unit-тесте.
6. `testDispatcher.scheduler.advanceUntilIdle()` после каждого
   действия ViewModel — корутины не запустятся сами.

## SavedStateHandle — для навигационных аргументов

`DetailScreenViewModel`, `CreateEditScreenViewModel` читают `itemId`
из `SavedStateHandle`. В тесте передаём вручную:

```kotlin
val savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId))
viewModel = DetailScreenViewModel(
    repository = repository,
    logger = NoOpLogger(),
    savedStateHandle = savedStateHandle,
    reminderManager = reminderManager
)
```

`mapOf("itemId" to testItemId)` — это имитация аргументов навигации
(`"item_detail/$testItemId"`), которые в проде Compose Navigation
кладёт в handle.

## `currentTimeMillisProvider` — для clock-зависимых ViewModel

`DetailScreenViewModel` принимает `currentTimeMillisProvider: () -> Long`
для тестирования "будущих" и "прошедших" напоминаний:

```kotlin
var nowMillis = 1_800_000_000_000L
viewModel = DetailScreenViewModel(
    repository = repository,
    logger = NoOpLogger(),
    savedStateHandle = SavedStateHandle(mapOf("itemId" to testItemId)),
    reminderManager = reminderManager,
    currentTimeMillisProvider = { nowMillis }   // фиксируем время
)

// ... действие, advanceUntilIdle
nowMillis += 120_000L                            // "сдвиг" часов
viewModel.refreshReminder()
testDispatcher.scheduler.advanceUntilIdle()
// теперь проверяем, что прошедшее напоминание скрыто
```

## Полный пример: `DetailScreenViewModelTest`

```kotlin
@Test
fun when_item_not_found_then_remains_in_loading_state() = runTest {
    // Given - Repository не содержит элемент
    val savedStateHandle = SavedStateHandle(mapOf("itemId" to 999L))
    viewModel = DetailScreenViewModel(
        repository = repository,
        logger = NoOpLogger(),
        savedStateHandle = savedStateHandle,
        reminderManager = reminderManager
    )

    // When - Пытаемся загрузить несуществующий элемент
    testDispatcher.scheduler.advanceUntilIdle()

    // Then - Должно остаться в состоянии Loading
    val currentState = viewModel.uiState.value
    assertTrue(currentState is DetailScreenState.Loading, "Должно остаться в состоянии Loading")
}
```

## Полный пример: `MainScreenViewModelTest` (с DataStore + slot)

```kotlin
private val sortOrderSlot = slot<SortOrder>()
private lateinit var sortOrderFlow: MutableStateFlow<SortOrder>

@BeforeEach
fun setUp() {
    testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)

    repository = FakeItemRepository()
    sortOrderFlow = MutableStateFlow(SortOrder.DESCENDING)
    val dataStore = mockk<AppSettingsDataStore>(relaxed = true)

    every { dataStore.sortOrder } returns sortOrderFlow
    coEvery { dataStore.setSortOrder(capture(sortOrderSlot)) } answers {
        sortOrderFlow.value = sortOrderSlot.captured
    }

    viewModel = MainScreenViewModel(repository, dataStore, mockk(relaxed = true))
}

@Test
fun whenSortOrderChanged_thenSortsItems() = runTest {
    // Given
    repository.setItems(listOf(testItem1, testItem2))

    // When
    viewModel.updateSortOrder(SortOrder.ASCENDING)
    testDispatcher.scheduler.advanceUntilIdle()

    // Then
    val state = viewModel.uiState.value as MainScreenState.Success
    assertEquals(2, state.items.size)
    assertEquals(SortOrder.ASCENDING, viewModel.sortOrder.value)
    coVerify { dataStore.setSortOrder(SortOrder.ASCENDING) }
}
```

`MutableStateFlow` для `sortOrderFlow` имитирует persistence —
вызов `setSortOrder` обновляет `sortOrderFlow.value`, и ViewModel
получает эмиссию обратно через `dataStore.sortOrder`.

## Почему НЕ `runBlocking` для ViewModel

```kotlin
// НЕПРАВИЛЬНО — конфликтует с viewModelScope.launch
@Test
fun bad() = runBlocking {
    viewModel.someAction()   // запускает viewModelScope.launch на Main
    // viewModelScope.launch НЕ выполнится — диспетчер Main не под контролем
}
```

Симптомы: тест зависает, `UncompletedCoroutinesError` после, flaky
failures.

**Правильно** — `runTest` (он контролирует `TestScope`) +
`StandardTestDispatcher` + `advanceUntilIdle()`.

## Когда ViewModel использует Turbine

**Никогда в unit-тестах этого проекта.** Turbine подключён только в
`androidTest/` (см. `app/build.gradle.kts` — `androidTestImplementation(libs.turbine)`).
В unit-тестах Flow проверяется через `viewModel.uiState.value` после
`advanceUntilIdle()`. Turbine в unit-тестах не нужен и не используется.

## StateFlow с `WhileSubscribed`

Часть state собирается через `stateIn(..., WhileSubscribed(5000), ...)`
(`AppDataScreenViewModel`, `ThemeIconViewModel`,
`MainScreenViewModel.sortOrder`): upstream стартует только при
подписчике, поэтому чтение `.value` без подписки вернёт initialValue.

В проекте чтение `.value` после `advanceUntilIdle()` обычно работает —
Flow подписан внутри самого ViewModel через `combine` в
`viewModelScope`. Если тестируете изолированный Flow без внутренней
подписки — подпишитесь до чтения:

```kotlin
backgroundScope.launch { viewModel.sortOrder.collect { } }
testDispatcher.scheduler.advanceUntilIdle()
```

## Проверка нескольких эмиссий без Turbine

```kotlin
@Test
fun whenItemReemits_thenStateUpdates() = runTest {
    repository.setItems(listOf(testItem))
    testDispatcher.scheduler.advanceUntilIdle()
    val stateAfterFirstLoad = viewModel.uiState.value as DetailScreenState.Success

    // Имитируем re-emit (например, после сохранения из Edit screen)
    repository.setItems(listOf(testItem.copy(timestamp = testItem.timestamp + 1)))
    testDispatcher.scheduler.advanceUntilIdle()

    val stateAfterReemit = viewModel.uiState.value as DetailScreenState.Success
    assertEquals(stateAfterFirstLoad.item.id, stateAfterReemit.item.id)
}
```

Каждый `setItems` + `advanceUntilIdle()` = один «снимок» Flow.
Промежуточные состояния смотри через `viewModel.uiState.value`.

## Типичные ошибки

| Ошибка | Симптом | Решение |
|---|---|---|
| Забыл `setMain` | `viewModelScope.launch` висит навечно | Добавь `Dispatchers.setMain(testDispatcher)` в `@BeforeEach` |
| Забыл `resetMain` | Следующий тест в JVM получает грязный Main, flaky failures | Добавь в `@AfterEach` |
| Забыл `advanceUntilIdle()` | `uiState.value` — `Loading`, хотя ждёшь `Success` | Вставь после действия ViewModel |
| `@OptIn` пропущен | Компилятор ругается на `StandardTestDispatcher` | Добавь `@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)` |
| `mockk()` вместо `Fake` для `ItemRepository` | `mockRepo.getAllItems() returns flowOf(...)` не эмитится в `combine` | Замени на `FakeItemRepository` (см. `references/fakes.md`) |
