# Project Conventions

Соглашения JetpackDays для unit-тестов. Часть из них —
требования AGENTS.md (обязательные), часть — устоявшаяся практика
(принятая, но гибкая).

## Обязательные (из AGENTS.md)

### Никакого `!!`

Даже в тестах. Используй `?.`, `?:`, `let`, `checkNotNull`:

```kotlin
// ПЛОХО
val item = repository.getById(id)!!
val reminder = successState.reminder!!

// ХОРОШО
val item = checkNotNull(repository.getById(id)) { "Item должен существовать" }
val reminder = successState.reminder
assertNotNull(reminder, "Reminder должен быть не null")
```

### `snake_case` для имён тестов, без обратных кавычек

Формат и допустимые вариации — `references/fundamentals.md`
(раздел «Именование методов»).

### Файлы тестов в `app/src/test/`, зеркалят `app/src/main/`

```
app/src/main/java/com/dayscounter/domain/usecase/CalculateDaysDifferenceUseCase.kt
app/src/test/java/com/dayscounter/domain/usecase/CalculateDaysDifferenceUseCaseTest.kt
```

### Имя тест-класса = `<Class>Test`

```
CalculateDaysDifferenceUseCase → CalculateDaysDifferenceUseCaseTest
MainScreenViewModel → MainScreenViewModelTest
```

### Импорты только из JUnit 5

`org.junit.jupiter.api.*`, не `org.junit.*`.

## Устоявшиеся (следуй для единообразия)

### Given / When / Then в каждом тесте

Три маркера-комментария. Даже если When и Then — одна строка.
Помогает глазу и как документация.

### Русские сообщения в `assert`

```kotlin
assertEquals(2, items.size, "Должно быть 2 элемента")
```

### `Logger` в тестах ViewModel

`com.dayscounter.util.NoOpLogger` — предпочтительный вариант:
реализация `Logger`, которая ничего не делает. Альтернативно —
`mockk(relaxed = true)` (relaxed-мок перехватывает любые вызовы
без падения; см. правило 11 SKILL.md и `EXAMPLE.md`). Оба подхода
живут в проекте: `MainScreenViewModelTest` использует
`mockk(relaxed = true)`, `DetailScreenViewModelTest` и
`CreateEditScreenViewModelTest` — `NoOpLogger()`. Смешивать
в одном тест-классе не нужно — выбери один.

```kotlin
// Предпочтительный вариант
viewModel = MainScreenViewModel(
    repository = repository,
    dataStore = dataStore,
    logger = NoOpLogger()
)

// Допустимая альтернатива
val logger: Logger = mockk(relaxed = true)
viewModel = MainScreenViewModel(
    repository = repository,
    dataStore = dataStore,
    logger = logger
)
```

### `StubResourceProvider` в тестах Use Case

`com.dayscounter.data.provider.StubResourceProvider` —
production-класс из `data/provider/`, специально сделанный
заглушкой для тестов. Не пиши свою:

```kotlin
private val resourceProvider: ResourceProvider = StubResourceProvider()
```

Он возвращает русские строки (`"Сегодня"`, `"осталось"`, `"прошло"`)
и сокращения (`"дн."`, `"мес."`, `"г."`).

### Fake предпочтительнее Mock для репозиториев с Flow

См. `references/fakes.md`. `FakeItemRepository` на `MutableStateFlow`
вместо `mockk() returns flowOf(...)`.

### Private nested class для Fake

Fake объявляется как `private class` внутри тест-класса; пороги
и политика выноса в общий файл — `references/fakes.md`
(«Где размещать Fakes»).

### Имя Fake-класса — `Fake<InterfaceName>` или `Fake<InterfaceName>With<Modifier>`

```kotlin
private class FakeItemRepository : ItemRepository
private class FakeItemRepositoryWithLoggingDisabled : ItemRepository
private class FakeReminderDao : ReminderDao
private class FakeReminderManager : ReminderManager
private class FakeReminderScheduler : ReminderScheduler
private class FakeResourceProvider : ResourceProvider
```

### `lateinit var` для всего, что создаётся в `@BeforeEach`

```kotlin
private lateinit var repository: FakeItemRepository
private lateinit var viewModel: MainScreenViewModel
private lateinit var testDispatcher: TestDispatcher
```

Immutable поля (например, `private val useCase = ...()`) — `val`.

### `@OptIn(ExperimentalCoroutinesApi::class)` на классе

Если используется `StandardTestDispatcher` / `TestScope` —
на уровне класса, не метода:

```kotlin
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
    // ...
}
```

### `runTest { ... }` как выражение

JUnit 5 + coroutines-test поддерживают `runTest` как тело функции:

```kotlin
@Test
fun whenAction_thenExpected() = runTest {
    // Given
    // When
    // Then
}
```

Либо `runTest` внутри тела — оба варианта встречаются в проекте.
Форма как выражение — короче.

### Тестовые JSON-ресурсы в `app/src/test/resources/`

Список файлов, конвенция имён и загрузка — `references/data-layer.md`
(раздел «JSON парсинг с реальными файлами»).

### KDoc на тест-классе

```kotlin
/**
 * Тесты для [CalculateDaysDifferenceUseCase].
 */
class CalculateDaysDifferenceUseCaseTest
```

```kotlin
/**
 * Unit-тесты для MainScreenViewModel.
 */
class MainScreenViewModelTest
```

## Запреты (из AGENTS.md)

### Запрет новых интеграционных тестов ViewModel

Причина (из AGENTS.md и опыта проекта):
- `runBlocking` конфликтует с `viewModelScope.launch`
- Flow репозитория не активируется в тестах
- Тесты зависают или падают
- Unit-тесты с MockK обеспечивают лучшее покрытие

В JetpackDays часть интеграционных тестов ViewModel была отключена
(`@Ignore`) именно из-за этих сложностей.

Существующие интеграционные тесты ViewModel (если они есть в
`androidTest/`) обслуживай как есть, но **не создавай новых**.

### Запрет Mockito / Mockito-Kotlin

Только MockK (`io.mockk.*`). Mockito не подключён в проект.

### Не тестируй через реальные I/O в unit-тестах

- Никаких реальных Room database (это integration).
- Никакого реального DataStore (это integration).
- Никаких реальных сетевых вызовов (проект офлайн, но всё равно).
- Никакого `Context.getString` — это integration; используй
  `StubResourceProvider` или `mockk<ResourceProvider>()`.

## Что гибко

- `@DisplayName` — опционально, не злоупотребляй.
- `@Nested` — не используется в проекте, но JUnit 5 поддерживает.
- `ParameterizedTest` — не используется (хотя `@CsvSource` доступен).
  Если появится повторяющаяся логика — переходи на параметризацию.
- Импорт `assertEquals` через `*` — не принят, но и не запрещён.
