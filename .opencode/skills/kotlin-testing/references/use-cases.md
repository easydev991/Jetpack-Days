# Use Cases

Use Case в проекте — это `class` с `operator fun invoke(...)` в
`com.dayscounter.domain.usecase`. Тесты лежат в этом же пакете.

## Три формы Use Case

### 1. Pure (без I/O)

`CalculateDaysDifferenceUseCase`, `CalculateDaysDifferenceUseCaseTest`:

```kotlin
class CalculateDaysDifferenceUseCaseTest {
    private val useCase = CalculateDaysDifferenceUseCase(NoopCrashReporter())

    @Test
    fun calculate_when_1_day_difference_then_returns_1_day() {
        // Given
        val currentDate = LocalDate.now()
        val eventDate = currentDate.minusDays(1)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.days, "Должен быть 1 день")
    }
}
```

Use Case создаётся один раз как `val` — он immutable. Если тестам
нужны разные зависимости (например, разные `Clock`) — допустимо
создавать его внутри `@Test` (как в `references/EXAMPLE.md` §1).

### 2. С зависимостями (MockK)

`FormatDaysTextUseCase` зависит от `DaysFormatter`. Тест
мокирует форматтер:

```kotlin
class FormatDaysTextUseCaseTest {
    private val daysFormatter: DaysFormatter = mockk()
    private val textProvider: DaysTextProvider = FakeDaysTextProvider()
    private val useCase = FormatDaysTextUseCase(daysFormatter = daysFormatter)

    @Test
    fun invoke_when_calculated_positive_days_and_showminus_true_then_shows_number_with_sign() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val difference = DaysDifference.Calculated(
            period = period, totalDays = 10, timestamp = 1234567890000L
        )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                textProvider = textProvider,
                totalDays = 10,
                showMinus = true
            )
        } returns "10 дней"

        // When
        val result = useCase.invoke(
            difference = difference,
            displayOption = DisplayOption.DAY,
            textProvider = textProvider,
            showMinus = true
        )

        // Then
        assertEquals("10 дней", result)
    }
}
```

`FakeDaysTextProvider` — общий тестовый fake из
`app/src/test/java/com/dayscounter/domain/usecase/`, не пиши
подделку на каждый тест. Возвращает русские строки для детального
экрана.

### 3. С `Result<T>` и валидацией

`BuildReminderUseCase` возвращает `Result<Reminder>` — ошибки для
невалидных входов. Тесты проверяют обе ветки. Канонический полный
класс — `references/EXAMPLE.md`, раздел «1. Use Case без
зависимостей + `Result<T>` + Clock»: happy path с `Clock.fixed`
и `invoke_whenAfterIntervalAmountIsZero_thenReturnsFailure`.

## `Result<T>` — assert-паттерны

```kotlin
// Happy path
val result = useCase(request)
assertTrue(result.isSuccess, "Результат должен быть успешным")
val value = result.getOrThrow()

// Failure — есть ошибка
val result = useCase(invalidRequest)
assertTrue(result.isFailure, "Должна быть ошибка валидации")

// Failure — конкретный тип ошибки
val ex = result.exceptionOrNull()
assertTrue(ex is MyException, "Тип ошибки должен быть MyException")
```

## `Clock.fixed` — обязательно для time-зависимых Use Case

Если Use Case читает `Clock.systemDefaultZone()` или
`LocalDate.now()`, тест становится **flaky по дню** — он пройдёт в
полдень, упадёт за полночь.

Решение — инжектить `Clock` и в тесте передавать `Clock.fixed(...)`:

```kotlin
private val fixedInstant = Instant.parse("2026-04-27T10:15:30Z")
private val zoneId = ZoneId.of("Europe/Moscow")
private val clock: Clock = Clock.fixed(fixedInstant, zoneId)

private val useCase = BuildReminderUseCase(clock = clock)
```

То же для `currentTimeMillisProvider: () -> Long` — инжектим как
параметр, в тесте — лямбда.

## Явная передача `currentDate`

`CalculateDaysDifferenceUseCase` принимает `currentDate: LocalDate`
опционально. В тесте **всегда передавай явно** — иначе flaky:

```kotlin
// ПЛОХО — неявный currentDate = LocalDate.now() внутри use case
@Test
fun calculate_when_30_days_difference_then_returns_30_days() {
    val currentDate = LocalDate.now()
    // ...
}

// ХОРОШО — фиксируем
@Test
fun calculate_when_30_days_difference_then_returns_30_days() {
    val currentDate = LocalDate.of(2024, 3, 31)  // детерминированно
    val eventDate = LocalDate.of(2024, 3, 1)
    // ...
    val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)
    // ...
}
```

Исключение — тесты, которые явно проверяют поведение "сегодня"
(например, `calculate_when_same_day_then_returns_today`).

## Use Case + I/O (поток)

`ExportBackupUseCase`, `ImportBackupUseCase` пишут и читают бэкап через
`java.io.OutputStream` / `InputStream`. Это suspend-функции с I/O —
мокируем `ItemRepository`, а поток подменяем на `ByteArrayOutputStream`:

```kotlin
class ExportBackupUseCaseTest {
    private val repository: ItemRepository = mockk()
    private lateinit var useCase: ExportBackupUseCase
    private val outputStream = ByteArrayOutputStream()

    @BeforeEach
    fun setup() {
        useCase = ExportBackupUseCase(repository)
    }

    @Test
    fun invoke_when_exporting_then_contains_format_android() = runBlocking {
        // Given
        val items = listOf(testItem)
        coEvery { repository.getAllItems() } returns flowOf(items)

        // When
        val result = useCase(outputStream)

        // Then
        assertTrue(result.isSuccess)
    }
}
```

`Context` и `ContentResolver` в use case'ах не участвуют — домен их не
знает. Выбор места записи (`Uri` из SAF, `File`, файл обмена) делает
вызывающий слой, в приложении это `AppDataScreenViewModel`; use case
получает уже готовый поток. Здесь `runBlocking` допустим — нет
`viewModelScope.launch`, нет диспетчеризации. Это обычная suspend-функция.

## Проверка исключений (не `Result<T>`)

Если функция **бросает** исключение (а не оборачивает в
`Result.failure`). Канон с примерами — `references/assertions.md`,
раздел «`assertThrows` для исключений»: для suspend-функций в
`runTest` — `runCatching` (лямбда `assertThrows` не suspend),
для не-suspend — `assertThrows(...::class.java)`.

## Частые ошибки

| Ошибка | Решение |
|---|---|
| Use Case читает `LocalDate.now()` | Инжектить `Clock`, в тесте `Clock.fixed(...)` |
| Use Case читает `System.currentTimeMillis()` | Инжектить `currentTimeMillisProvider: () -> Long` |
| `runBlocking` для suspend use case | Допустимо, если нет `viewModelScope`. Иначе — `runTest` |
| `assertEquals(Result.failure(...), result)` | Не сравнивай `Result` целиком. Используй `assertTrue(result.isFailure)` |
| Тест Use Case с `Context`/`ContentResolver` | Не делай так — домен работает с потоками/интерфейсами; `Context` остаётся в UI-слое (ViewModel) |
| `every` вместо `coEvery` для suspend-метода | Тест не сработает, ошибка в рантайме |
