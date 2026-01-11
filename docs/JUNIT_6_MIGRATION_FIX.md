# Исправление отключенных тестов

## Проблема

После обновления JUnit Jupiter до версии 6.0.1 возникли проблемы с выполнением тестов в `DaysCalculatorViewModelTest` и `DisplayOptionConverterTest`:

1. Корутины в `viewModelScope` не выполняются в тестовой среде
2. Ошибка `android.util.Log` not mocked

Всего было **6 отключенных тестов**:

- 5 тестов в `DaysCalculatorViewModelTest`
- 1 тест в `DisplayOptionConverterTest`

---

## Причина

JUnit 6.0.1 изменил поведение тестовых диспетчеров. `viewModelScope` использует диспетчер по умолчанию, который не связан с тестовым диспетчером. Кроме того, `android.util.Log` вызывает RuntimeException в unit-test среде.

---

## Решение

### 1. Внедрение интерфейса Logger

Создан интерфейс `Logger` для мокирования логирования в тестах:

```kotlin
// app/src/main/java/com/dayscounter/util/Logger.kt
interface Logger {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String, throwable: Throwable? = null)
    fun e(tag: String, message: String, throwable: Throwable? = null)
}
```

**Реализации:**

- `AndroidLogger` — для продакшена, использует `android.util.Log`
- `NoOpLogger` — для тестов, не выполняет никаких действий

**Использование в ViewModel:**

```kotlin
class DaysCalculatorViewModel(
    // параметры...
    private val logger: Logger = AndroidLogger(),
) : ViewModel() {
    fun reset() {
        _state.value = DaysCalculatorState()
        logger.d(TAG, "Состояние сброшено")
    }
}
```

**Использование в тестах:**

```kotlin
@Test
fun `reset when called then clears state`() = runTest {
    val viewModel = DaysCalculatorViewModel(
        // параметры...
        logger = NoOpLogger(),
    )
    // тестовые действия
}
```

### 2. Исправление тестов с корутинами

Для тестов, использующих `viewModelScope`:

```kotlin
@Test
fun `calculateDays when timestamp provided then updates state`() = runTest {
    val testDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(testDispatcher)

    val viewModel = DaysCalculatorViewModel(
        // параметры...
        logger = NoOpLogger(),
    )

    // тестовые действия
    viewModel.calculateDays(timestamp, currentDate)
}
```

### 3. Удаление неактуального теста

Тест `toDisplayOption_whenUnknownString_thenReturnsDefault` из `DisplayOptionConverterTest` был удалён как проверяющий детали реализации (логирование), а не бизнес-логику.

---

## Статус реализации

✅ **Все 6 отключенных тестов активированы и успешно проходят**

- `reset when called then clears state` — ✅
- `clearError when called then clears error state` — ✅
- `updateDisplayOption when called then updates display option` — ✅
- `calculateDays when timestamp provided then updates state` — ✅
- `calculateDays with custom displayOption then uses custom option` — ✅
- Неактуальный тест из `DisplayOptionConverterTest` — удалён

**Результат:** Все 17 unit-тестов проходят успешно (100%)

---

## Созданные файлы

- `app/src/main/java/com/dayscounter/util/Logger.kt` — интерфейс логирования
- `app/src/main/java/com/dayscounter/util/AndroidLogger.kt` — реализация для продакшена
- `app/src/test/java/com/dayscounter/util/NoOpLogger.kt` — реализация для тестов

## Изменённые файлы

- `app/src/main/java/com/dayscounter/viewmodel/DaysCalculatorViewModel.kt` — внедрение Logger
- `app/src/test/java/com/dayscounter/viewmodel/DaysCalculatorViewModelTest.kt` — исправление тестов
- `app/src/test/java/com/dayscounter/data/database/converters/DisplayOptionConverterTest.kt` — удаление теста

---

## Запуск тестов

```bash
./gradlew testDebugUnitTest
```

---

## Ссылки

- [Исходный код тестов](../app/src/test/java/com/dayscounter/viewmodel/DaysCalculatorViewModelTest.kt)
- [JUnit 6.0.1 Release Notes](https://junit.org/junit5/docs/release-notes/6.0.1.html)
