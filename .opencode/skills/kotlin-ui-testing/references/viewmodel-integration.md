# ViewModel integration — MainDispatcherRule, Turbine

Единственный интеграционный ViewModel-тест в проекте —
`DetailScreenViewModelIntegrationTest.kt`
(`app/src/androidTest/java/com/dayscounter/ui/viewmodel/`).
Он тестирует ViewModel с реальной in-memory Room БД и реальным
репозиторием, в отличие от unit-тестов с фейками.

> Новые интеграционные ViewModel-тесты не писать — только unit-тесты
> с Fake-репозиторием (см. навык `kotlin-testing`, правило 7).
> Существующий — обслуживать как есть.

## MainDispatcherRule

Общий хелпер — `app/src/androidTest/java/com/dayscounter/test/MainDispatcherRule.kt`:

```kotlin
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

Использование:

```kotlin
@get:Rule
val mainDispatcherRule = MainDispatcherRule()
```

Заменяет `Dispatchers.Main` на `StandardTestDispatcher` до теста
и восстанавливает после. Без него `viewModelScope.launch` использует
реальный Main, который не контролируется тестом.

## Скелет ViewModel-интеграции

Полный код — `references/EXAMPLE.md` (раздел «ViewModel-интеграция
(Turbine)»). Ключевые элементы:

```kotlin
@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailScreenViewModelIntegrationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        // in-memory Room + ItemRepositoryImpl + database.clearAllTables()
    }
}
```

База строится в `@Before` и закрывается в `@After` — при таком per-test
паттерне `clearAllTables()` не нужен, данные между тестами не живут.
Он обязателен только для process-wide базы через синглтон
`DaysDatabase.getDatabase(...)` (используется в activity-тестах) —
там данные остаются между тестами.

## Тест через Turbine

Паттерн (полный тест — в `references/EXAMPLE.md`):

```kotlin
viewModel.uiState.test {
    val loadingState = awaitItem()   // начальное состояние — Loading
    val successState = awaitItem()   // после загрузки — Success
}
```

Ключевое:
- `runTest { }` + `viewModel.uiState.test { awaitItem() }` (Turbine)
- Первый `awaitItem()` — начальное состояние (`Loading`)
- Второй `awaitItem()` — состояние после загрузки (`Success`)
- Доступ к полям — после `assertTrue(state is ...)`, без `!!`
- `SavedStateHandle(mapOf("itemId" to insertedId))` — реальный handle

## Альтернативы Turbine

Когда `.test { }` не подходит (например, нужен `advanceUntilIdle`):

```kotlin
runTest {
    // advanceUntilIdle + сбор через backgroundScope
    val collectedStates = mutableListOf<DetailScreenState>()
    backgroundScope.launch {
        viewModel.uiState.collect { collectedStates += it }
    }
    mainDispatcherRule.testDispatcher.scheduler.advanceUntilIdle()
}
```

Проверка реактивности — обновление БД между эмиссиями:

```kotlin
viewModel.uiState.test {
    val first = awaitItem()
    repository.updateItem(updatedItem)   // изменение в БД
    val second = awaitItem()             // ViewModel переэмитит
    assertEquals("Обновлённое событие", (second as DetailScreenState.Success).item.title)
}
```

## Различия unit vs integration для ViewModel

| Аспект | Unit-тест (`app/src/test/`) | Интеграционный (`androidTest/`) |
|---|---|---|
| Репозиторий | Fake на MutableStateFlow | Реальный `ItemRepositoryImpl` |
| База | — | Room in-memory |
| Диспетчер | `StandardTestDispatcher` + setMain/resetMain | `MainDispatcherRule` |
| State-проверки | `runTest` + collect | Turbine `.test { awaitItem() }` |
| SavedStateHandle | Фейковый | Реальный с `mapOf(...)` |
