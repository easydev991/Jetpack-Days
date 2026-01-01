# Исправление тестов после миграции JUnit Jupiter 5.x на 6.0.1

## Проблема

После обновления JUnit Jupiter до версии 6.0.1 возникли проблемы с выполнением тестов в `DaysCalculatorViewModelTest`:

1. Корутины в `viewModelScope` не выполняются в тестовой среде
2. Ошибка `android.util.Log` not mocked

### Падающие тесты

- `calculateDays when timestamp provided then updates state()`
- `calculateDays with custom displayOption then uses custom option()`
- `reset when called then clears state()`
- `updateDisplayOption when called then updates display option()`

### Текущая версия

```
kotlinx-coroutines-test = 1.10.2
```

## Причина проблемы

JUnit 6.0.1 внес изменения в поведение тестовых диспетчеров. Основная проблема: `viewModelScope` использует диспетчер по умолчанию, который не связан с тестовым диспетчером, установленным через `Dispatchers.setMain()`.

Ранее предпринятые попытки с `StandardTestDispatcher` + `advanceUntilIdle()` и с `UnconfinedTestDispatcher` без `runTest` не увенчались успехом.

## Успешное решение

Ключевое решение — создание ViewModel **ВНУТРИ** каждого теста с использованием `runTest` и `UnconfinedTestDispatcher`:

```kotlin
@Test
fun testSomething() = runTest {
    val testDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(testDispatcher)

    val viewModel = DaysCalculatorViewModel(
        calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
        formatDaysTextUseCase = formatDaysTextUseCase,
        resourceProvider = StubResourceProvider(),
        defaultDisplayOption = DisplayOption.DAY,
    )

    // тестовые действия
}
```

### Почему это работает

1. `runTest` создает контекст выполнения теста с тестовым диспетчером
2. `Dispatchers.setMain(testDispatcher)` устанавливает тестовый диспетчер как Main
3. `UnconfinedTestDispatcher` обеспечивает немедленное выполнение корутин
4. ViewModel создается после установки диспетчера, поэтому `viewModelScope` использует правильный диспетчер

### Статус

**BUILD SUCCESSFUL** — все 17 тестов проходят (100%)

## Возможные решения для будущего

### Решение 1: Мокирование `android.util.Log` с MockK

**Преимущества:**
- Минимальные изменения в коде
- Не требует изменения архитектуры

**Недостатки:**
- Не решает проблему с корутинами в `viewModelScope`

### Решение 2: Инъекция диспетчера в ViewModel (рекомендуется)

**Преимущества:**
- Полный контроль над диспетчером корутин в ViewModel
- Можно передавать тестовый диспетчер в тестах

**Недостатки:**
- Требует изменений архитектуры ViewModel
- Нужно обновить все места создания ViewModel

**Реализация:**

```kotlin
class DaysCalculatorViewModel(
    // существующие параметры
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main,
) : ViewModel() {
    private val viewModelScope = CoroutineScope(SupervisorJob() + coroutineDispatcher)
}
```

### Решение 3: Откатиться на JUnit Jupiter 5.x

**Преимущества:**
- Гарантирует работу существующих тестов
- Минимальные изменения

**Недостатки:**
- Не использует новые возможности JUnit 6.x

## Вывод

Текущее решение обеспечивает:
- ✅ Все тесты работают корректно (17 из 17)
- ✅ CI/CD пайплайн не падает
- ✅ 100% покрытие тестов

Для долгосрочной архитектуры рекомендуется реализовать **Решение 2** (инъекция диспетчера в ViewModel).

## Ссылки

- [Исходный код тестов](../app/src/test/java/com/dayscounter/viewmodel/DaysCalculatorViewModelTest.kt)
- [JUnit 6.0.1 Release Notes](https://junit.org/junit5/docs/release-notes/6.0.1.html)
