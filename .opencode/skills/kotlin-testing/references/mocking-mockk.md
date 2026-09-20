# Mocking — MockK

MockK (`io.mockk.*`) — основной фреймворк моков в проекте.
Mockito / Mockito-Kotlin не используется.

## Создание мока

```kotlin
// Строгий — все вызовы без стаба бросают MockKException
private val logger: Logger = mockk()

// Relaxed — для не-stub-нутых методов возвращает дефолт (null/0/false)
private val logger: Logger = mockk(relaxed = true)
```

`relaxed = true` удобен для `Logger`, `DataStore`, простых интерфейсов,
где методов много и они не центральные. **Не используй `relaxed` для
репозиториев с Flow** — там нужны Fake (см. `references/fakes.md`).

## `every` / `coEvery`

```kotlin
// Синхронный вызов
every { mockResourceProvider.getString(R.string.today) } returns "Сегодня"

// С аргументами-свойствами (named args в блоке every тоже допустимы)
every {
    daysFormatter.formatComposite(
        period = period,
        displayOption = DisplayOption.DAY,
        resourceProvider = resourceProvider,
        totalDays = 10,
        showMinus = true
    )
} returns "10 дней"

// Suspend-функция — ТОЛЬКО coEvery
coEvery { mockRepository.deleteItem(testItem) } returns Unit

// Бросить исключение
every { mockProvider.getName() } throws IllegalStateException("not set")

// Suspend, бросить
coEvery { mockRepository.insertItem(any()) } throws SQLException("db error")
```

## `verify` / `coVerify`

```kotlin
// Был вызван хотя бы раз
verify { mockLogger.d("Tag", "msg") }

// Suspend
coVerify { mockRepository.deleteItem(testItem) }

// Ровно N раз
coVerify(exactly = 1) { mockRepository.insertItem(any()) }
coVerify(exactly = 0) { mockRepository.insertItem(any()) } // не было
```

## `slot` + `capture` (для перехвата аргументов)

Когда нужно проверить, **с чем** вызвали мок:

```kotlin
private val sortOrderSlot = slot<SortOrder>()

@BeforeEach
fun setUp() {
    coEvery { dataStore.setSortOrder(capture(sortOrderSlot)) } answers {
        sortOrderFlow.value = sortOrderSlot.captured
    }
}

@Test
fun whenSortOrderChanged_thenSortsItems() = runTest {
    viewModel.updateSortOrder(SortOrder.ASCENDING)
    testDispatcher.scheduler.advanceUntilIdle()
    // ...
    assertEquals(SortOrder.ASCENDING, sortOrderSlot.captured)
}
```

`capture(slot)` — захватывает первый аргумент. `slot.captured` —
последнее захваченное значение.

## `answers` (для side-effect при вызове)

Когда вызов мока должен менять состояние (например, записать в
`MutableStateFlow`):

```kotlin
coEvery { dataStore.setMainScreenColorTagFilter(any()) } answers {
    colorTagFilterFlow.value = firstArg()
}
```

`firstArg()`, `secondArg()` — доступ к аргументам по позиции.

## Типичные ошибки

### `every` для suspend-функции — NPE или молча игнорирует

```kotlin
// НЕПРАВИЛЬНО — без coEvery не сработает для suspend
every { mockRepo.insertItem(item) } returns 1L

// ПРАВИЛЬНО
coEvery { mockRepo.insertItem(item) } returns 1L
```

### `verify` для suspend-функции

```kotlin
// НЕПРАВИЛЬНО
verify { mockRepo.insertItem(item) }

// ПРАВИЛЬНО
coVerify { mockRepo.insertItem(item) }
```

### Flow-возвращающий метод — `every`, не `coEvery`

`fun getAllItems(): Flow<List<Item>>` — не suspend, поэтому `coEvery`
на нём не компилируется:

```kotlin
// НЕПРАВИЛЬНО — метод не suspend
coEvery { mockRepo.getAllItems() } returns flowOf(items)

// ПРАВИЛЬНО
every { mockRepo.getAllItems() } returns flowOf(items)
```

### Мок Flow не эмитится в `StateFlow`

`every { mockRepo.getAllItems() } returns flowOf(items)` создаёт
**холодный** Flow, который запускается только при `collect`. Если
ViewModel слушает через `combine` / `flatMapLatest` на TestDispatcher,
эмиссия может произойти, а может и нет — race-condition.

**Решение**: для `ItemRepository`, `ReminderRepository` — пиши Fake
на `MutableStateFlow` (см. `references/fakes.md`). MockK — только
для `Logger`, `DataStore`, `ResourceProvider` и подобных stateless
интерфейсов.

## Полный пример: `FormatDaysTextUseCaseTest`

Канонический полный класс — `references/use-cases.md`, §2
«С зависимостями (MockK)» (там же пояснение про
`StubResourceProvider`).

Ключевая деталь примера: `mockk()` без `relaxed`, потому что
единственный метод форматтера стабится в каждом тесте.
