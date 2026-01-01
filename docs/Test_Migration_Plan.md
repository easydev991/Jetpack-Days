# План миграции старых тестов на JUnit 5

## Обзор

В проекте есть 17 тестовых файлов с расширением `.disabled`, которые требуют миграции на современный стек (JUnit 5) и активации.

### Статус тестов

| Категория | Всего файлов | Можно удалить | Требуют миграции |
|-----------|--------------|----------------|------------------|
| Unit-тесты | 14 | 0 | 14 |
| ViewModel-тесты | 2 | 0 | 2 |
| Navigation-тесты | 1 | 0 | 1 |
| **Итого** | **17** | **0** | **17** |

**Примечание:** После предыдущего анализа, файл `ItemRepositoryImplTest.kt.disabled` также требует миграции, так как интеграционный тест не покрывает все сценарии unit-тестирования.

---

## Структура миграции по слоям

### Приоритет 1: Domain Layer (Use Cases)

Эти тесты критически важны для бизнес-логики приложения.

#### 1. `CalculateDaysDifferenceUseCaseTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/domain/usecase/CalculateDaysDifferenceUseCaseTest.kt.disabled`

**Текущее покрытие:**
- Используется косвенно через `DaysCalculatorViewModelTest` (создание реальных экземпляров)
- Нет прямого unit-теста

**Необходимые тесты:**
- Вычисление для "Сегодня" (Today)
- Вычисление для 1 дня
- Вычисление для нескольких дней
- Вычисление для месяцев
- Вычисление для лет
- Комплексные вычисления (годы + месяцы + дни)

**Критерии готовности:**
- ✅ Миграция на JUnit 5 (`org.junit.jupiter.api`)
- ✅ Использование `kotlin.test.assert...` вместо `kotlin.test.assert...`
- ✅ Все тесты проходят
- ✅ Покрытие > 90% use case

---

#### 2. `FormatDaysTextUseCaseTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/domain/usecase/FormatDaysTextUseCaseTest.kt.disabled`

**Текущее покрытие:**
- Используется косвенно через `DaysCalculatorViewModelTest`
- Нет прямого unit-теста

**Необходимые тесты:**
- Форматирование для "Сегодня" (русский)
- Форматирование для "Today" (английский)
- Форматирование для DAY опции с разным количеством дней
- Форматирование для MONTH_DAY опции (сокращения)
- Форматирование для YEAR_MONTH_DAY опции
- Локализация множественного числа

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ MockK для мокирования зависимостей
- ✅ Тесты для всех DisplayOption
- ✅ Тесты локализации (ru/en)

---

#### 3. `GetFormattedDaysForItemUseCaseTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/domain/usecase/GetFormattedDaysForItemUseCaseTest.kt.disabled`

**Текущее покрытие:**
- Используется в `DetailScreen`
- Тест уже на JUnit 5, но отключен

**Требуемые действия:**
- Переименовать файл из `.kt.disabled` в `.kt`
- Проверить зависимости (Mockito → MockK)
- Проверить корректность всех тестов
- Добавить недостающие сценарии

**Критерии готовности:**
- ✅ Тест активирован (без .disabled)
- ✅ Использование MockK вместо Mockito
- ✅ Все тесты проходят
- ✅ Покрытие всех DisplayOption

---

### Приоритет 2: Domain Layer (Models)

#### 4. `TimePeriodTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/domain/model/TimePeriodTest.kt.disabled`

**Необходимые тесты:**
- Проверка `isEmpty()` при всех нулях
- Проверка `isEmpty()` при ненулевых значениях
- Корректность вычислений
- Пограничные значения

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ Тесты всех методов

---

#### 5. `ItemTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/domain/model/ItemTest.kt.disabled`

**Необходимые тесты:**
- Инициализация с обязательными полями
- Инициализация с colorTag
- Инициализация с displayOption
- Инициализация с colorTag и displayOption
- Метод `backupItem` с цветовым тегом
- Метод `backupItem` без цветового тега
- Проверка значений по умолчанию

**Критерии готовности:**
- ✅ Тест активирован
- ✅ Покрытие всех полей и методов

---

#### 6. `DisplayOptionTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/domain/model/DisplayOptionTest.kt.disabled`

**Необходимые тесты:**
- Тест значений enum (DAY, MONTH_DAY, YEAR_MONTH_DAY)
- Тест значения по умолчанию (DEFAULT)
- Тест строкового представления
- Тест парсинга из строки (case-insensitive)
- Тест обработки неизвестных значений
- Совместимость с iOS форматом (camelCase)

**Критерии готовности:**
- ✅ Тест активирован
- ✅ Тесты всех enum значений
- ✅ Тесты методов `fromString()` и `toJsonString()`

---

#### 7. `DaysDifferenceTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/domain/model/DaysDifferenceTest.kt.disabled`

**Необходимые тесты:**
- Сравнение `Today` случаев
- Сравнение `Calculated` случаев
- Проверка TimePeriod в Calculated
- Проверка totalDays
- Пограничные значения

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ Тесты всех sealed классов

---

### Приоритет 3: Data Layer (Database)

#### 8. `ItemEntityTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/data/database/entity/ItemEntityTest.kt.disabled`

**Необходимые тесты:**
- Создание Entity с обязательными полями
- Создание Entity с colorTag
- Создание Entity с displayOption
- Создание Entity с displayOption и colorTag
- Проверка значений по умолчанию
- Проверка автоинкремента id

**Критерии готовности:**
- ✅ Тест активирован
- ✅ Покрытие всех полей Entity

---

#### 9. `DisplayOptionConverterTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/data/database/converters/DisplayOptionConverterTest.kt.disabled`

**Необходимые тесты:**
- Конвертация DisplayOption → String (DAY)
- Конвертация DisplayOption → String (MONTH_DAY)
- Конвертация DisplayOption → String (YEAR_MONTH_DAY)
- Конвертация String → DisplayOption (DAY)
- Конвертация String → DisplayOption (MONTH_DAY)
- Конвертация String → DisplayOption (YEAR_MONTH_DAY)
- Обработка неизвестных строк (возврат DEFAULT)
- Round-trip конвертация (сохранение значения)

**Критерии готовности:**
- ✅ Тест активирован
- ✅ Конвертация в обе стороны
- ✅ Обработка ошибок

---

#### 10. `ItemMapperTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/data/database/mapper/ItemMapperTest.kt.disabled`

**Необходимые тесты:**
- Конвертация Entity → Domain со всеми полями
- Конвертация Domain → Entity со всеми полями
- Конвертация с null значениями (colorTag)
- Конвертация с разными DisplayOption
- Round-trip конвертация (сохранение всех полей)
- Проверка id сохранения

**Критерии готовности:**
- ✅ Тест активирован
- ✅ Конвертация в обе стороны
- ✅ Обработка null значений

---

#### 11. `ItemRepositoryImplTest` (текущий: JUnit 5)

**Файл:** `app/src/test/java/com/dayscounter/data/repository/ItemRepositoryImplTest.kt.disabled`

**Необходимые тесты:**
- `getAllItems()` - возвращает Flow с элементами
- `getItemById()` - находит существующий элемент
- `getItemById()` - возвращает null для несуществующего
- `searchItems()` - фильтрация по запросу
- `insertItem()` - добавление нового элемента
- `updateItem()` - обновление существующего
- `deleteItem()` - удаление по id
- `deleteAllItems()` - очистка всех
- `getItemsCount()` - подсчет элементов
- Конвертация Entity → Domain в методах репозитория
- Обработка null значений

**Критерии готовности:**
- ✅ Тест активирован
- ✅ MockK для мокирования ItemDao
- ✅ Тесты всех методов интерфейса
- ✅ Проверка конвертации моделей

---

### Приоритет 4: ViewModel Layer

#### 12. `RootScreenViewModelTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/viewmodel/RootScreenViewModelTest.kt.disabled`

**Необходимые тесты:**
- При создании ViewModel активная вкладка по умолчанию
- Проверка `isEventsTabSelected()` для разных вкладок
- Проверка `isMoreTabSelected()` для разных вкладок
- Проверка списка вкладок (tabs)
- Переключение между вкладками
- Состояние currentTab

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ Использование UnconfinedTestDispatcher
- ✅ Тесты всех методов ViewModel

---

#### 13. `RootScreenViewModelIntegrationTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/viewmodel/RootScreenViewModelIntegrationTest.kt.disabled`

**Необходимые тесты:**
- Интеграция с Repository
- Проверка Flow обновлений
- Переключение вкладок с реальными зависимостями
- Комплексные сценарии использования

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ Интеграционное тестирование
- ✅ Проверка реактивности

---

### Приоритет 5: UI Layer

#### 14. `RootScreenStateTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/ui/state/RootScreenStateTest.kt.disabled`

**Необходимые тесты:**
- Создание state с параметрами
- Значения по умолчанию
- Сериализация/десериализация (если применимо)
- Равенство состояний
- Проверка всех полей data class

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ Тесты всех полей

---

#### 15. `ScreenTest` (текущий: JUnit 4)

**Файл:** `app/src/test/java/com/dayscounter/navigation/ScreenTest.kt.disabled`

**Необходимые тесты:**
- Проверка route для всех экранов
- Проверка методов `createRoute()` для экранов с параметрами
- Валидация формата маршрутов
- Совместимость с навигацией

**Критерии готовности:**
- ✅ Миграция на JUnit 5
- ✅ Тесты всех Screen объектов

---

### Приоритет 6: Примеры и удаление

#### 16. `ExampleUnitTest.kt.disabled`

**Файл:** `app/src/test/java/com/example/jetpackdays/ExampleUnitTest.kt.disabled`

**Статус:** Пример теста из шаблона Android
**Действие:** Удалить (не нужен)

---

#### 17. `DaysFormatterImplTest.kt.disabled`

**Файл:** `app/src/test/java/com/dayscounter/data/formatter/DaysFormatterImplTest.kt.disabled`

**Статус:** Есть активная версия на JUnit 5
**Действие:** Удалить (активная версия уже работает)

---

## Порядок выполнения

### Фаза 1: Критические Use Cases (1-2 недели)

1. `CalculateDaysDifferenceUseCaseTest`
2. `FormatDaysTextUseCaseTest`
3. `GetFormattedDaysForItemUseCaseTest` (активация)

**Результат:** Покрытие бизнес-логики на уровне Use Cases

---

### Фаза 2: Domain Models (1 неделя)

4. `TimePeriodTest`
5. `ItemTest` (активация)
6. `DisplayOptionTest` (активация)
7. `DaysDifferenceTest`

**Результат:** Полное покрытие доменных моделей

---

### Фаза 3: Data Layer (1-2 недели)

8. `ItemEntityTest` (активация)
9. `DisplayOptionConverterTest` (активация)
10. `ItemMapperTest` (активация)
11. `ItemRepositoryImplTest` (активация)

**Результат:** Покрытие слоя данных

---

### Фаза 4: ViewModel и UI (1 неделя)

12. `RootScreenViewModelTest`
13. `RootScreenViewModelIntegrationTest`
14. `RootScreenStateTest`
15. `ScreenTest`

**Результат:** Покрытие ViewModel и UI State

---

### Фаза 5: Очистка (1 день)

16. Удалить `ExampleUnitTest.kt.disabled`
17. Удалить `DaysFormatterImplTest.kt.disabled`

**Результат:** Чистый проект без устаревших файлов

---

## Технические требования

### Шаблон миграции с JUnit 4 на JUnit 5

#### Замена импортов:

```kotlin
// Было (JUnit 4)
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// Стало (JUnit 5)
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
```

#### Замена аннотаций:

```kotlin
// Было
@Before
fun setUp() { ... }

@After
fun tearDown() { ... }

// Стало
@BeforeEach
fun setUp() { ... }

@AfterEach
fun tearDown() { ... }
```

#### Работа с корутинами в ViewModel:

```kotlin
@Test
fun `some test`() = runTest {
    val testDispatcher = UnconfinedTestDispatcher()
    Dispatchers.setMain(testDispatcher)
    
    // Создание ViewModel внутри теста
    val viewModel = MyViewModel(...)
    
    // Тестовые действия
    
    // Очистка
    Dispatchers.resetMain()
}
```

---

## Инструменты и зависимости

### Текущий стек:

- **JUnit 5** (`org.junit.jupiter:junit-jupiter-api`, `org.junit.jupiter:junit-jupiter-engine`)
- **MockK** для мокирования
- **kotlinx-coroutines-test** для тестирования корутин

### Миграция с Mockito на MockK:

```kotlin
// Было (Mockito)
@ExtendWith(MockitoExtension::class)
class MyTest {
    @Mock
    private lateinit var dependency: Dependency
    
    @BeforeEach
    fun setUp() {
        whenever(dependency.someMethod()).thenReturn(value)
    }
}

// Стало (MockK)
class MyTest {
    private val dependency = mockk<Dependency>()
    
    @BeforeEach
    fun setUp() {
        every { dependency.someMethod() } returns value
    }
}
```

---

## Метрики успеха

### Покрытие кода:

| Слой | Целевое покрытие | Текущее |
|------|-----------------|----------|
| Domain (Use Cases) | >90% | ~60% |
| Domain (Models) | >80% | ~30% |
| Data (Entities) | >80% | ~20% |
| Data (Repositories) | >80% | ~40% |
| ViewModel | >70% | ~30% |
| **Итого** | **>80%** | **~40%** |

### Статус по завершению:

- ✅ Все тесты на JUnit 5
- ✅ Все тесты проходят (100%)
- ✅ Покрытие >80%
- ✅ Нет `.disabled` файлов
- ✅ CI/CD пайплайн зеленый

---

## Риски и mitigation

### Риск 1: Сложности с корутинами в ViewModel

**Описание:** Проблемы с `viewModelScope` в тестах, аналогичные `DaysCalculatorViewModelTest`

**Mitigation:**
- Использовать `UnconfinedTestDispatcher`
- Создавать ViewModel внутри `runTest`
- Следовать паттерну из `DaysCalculatorViewModelTest`

### Риск 2: Зависимости от Android SDK в unit-тестах

**Описание:** Некоторые тесты могут требовать `android.util.Log` или другие Android API

**Mitigation:**
- Использовать MockK для мокирования Android зависимостей
- Переместить такие тесты в `androidTest` (интеграционные)
- Использовать Robolectric для unit-тестов с Android зависимостями

### Риск 3: Недостаточное покрытие после миграции

**Описание:** Мигрированные тесты могут не покрывать все сценарии

**Mitigation:**
- Анализ покрытия перед миграцией
- Добавление недостающих сценариев
- Проверка покрытия после каждой фазы

---

## Документация

### Обновляемые документы:

1. **`ANDROID_DEVELOPMENT_PLAN.md`** — обновить статус тестирования
2. **`BUG_FIX_PLAN.md`** — отметить миграцию тестов как выполненную
3. **`JUNIT_6_MIGRATION_FIX.md`** — добавить информацию о миграции unit-тестов
4. **`README.md`** — обновить секцию тестирования

### Создание новых документов:

1. **`TEST_COVERAGE_REPORT.md`** — отчет о покрытии до и после миграции
2. **`TESTING_BEST_PRACTICES.md`** — лучшие практики тестирования в проекте

---

## Заключение

Миграция 15 тестовых файлов обеспечит полное покрытие бизнес-логики, слоя данных и ViewModel. Оценка времени: **5-6 недель** при работе одного разработчика.

После завершения миграции проект будет иметь:
- ✅ Современный стек тестирования (JUnit 5)
- ✅ Высокое покрытие (>80%)
- ✅ Устойчивый CI/CD пайплайн
- ✅ Чистый код без `.disabled` файлов
