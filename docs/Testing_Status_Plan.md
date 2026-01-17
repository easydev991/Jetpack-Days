# Статус и план тестирования

## Обзор

В проекте реализованы тесты на современном стеке (JUnit 5) для всех уровней приложения. Документ описывает текущий статус тестов и план по исправлению проблемных тестов.

## Архитектура тестирования

### Типы тестов

- **Unit-тесты**: бизнес-логика изолированно, MockK для зависимостей, AAA паттерн
- **Instrumented-тесты**: тесты, требующие Android окружения (androidTest)
  - **Интеграционные тесты DAO/Repository**: взаимодействие с реальной БД
  - **UI-тесты**: Compose Testing для компонентов
  - **Интеграционные тесты ViewModels**: ограниченная поддержка (только DetailScreenViewModel)

### Инструменты

- **JUnit 5** (`org.junit.jupiter:junit-jupiter-api`, `org.junit.jupiter:junit-jupiter-engine`)
- **MockK** для мокирования в unit-тестах
- **Compose Testing** для UI-тестов
- **Room Testing** для интеграционных тестов БД
- **kotlinx-coroutines-test** для тестирования корутин
- **Turbine** для тестирования Flow/StateFlow (app.cash.turbine:turbine:1.1.0)

### Структура

- `app/src/test/` — unit-тесты (ViewModels, Use Cases, Domain models)
- `app/src/androidTest/` — integration/UI тесты (DAO, Repository, UI компоненты)

---

## Текущий статус тестов

### Unit-тесты (app/src/test/)

**Статус:** ✅ Все активны и работают

| Категория | Всего тестовых методов | Статус |
|-----------|----------------------|--------|
| Domain Layer (Use Cases) | ~30 | ✅ Завершено |
| Domain Layer (Models) | ~25 | ✅ Завершено |
| Data Layer (Entities, Mappers, Converters) | ~40 | ✅ Завершено |
| Data Layer (Formatters, Preferences) | ~45 | ✅ Завершено |
| ViewModel Layer (unit с MockK) | ~80 | ✅ Завершено |
| UI Layer (Navigation, UI State, Components) | ~44 | ✅ Завершено |
| **Итого** | **264** | **✅ Завершено** |

**Файлов тестов:** 31 файл

#### Выполненные миграции на JUnit 5

Все 264 unit-теста перенесены на JUnit 5 (Use Cases, Domain Models, Data Layer, ViewModels, UI Layer) ✅

### Instrumented-тесты (app/src/androidTest/)

**Статус:** ✅ Работающие тесты — активны, ⚠️ Частично исправленные тесты

| Категория | Тесты | Статус |
|-----------|-------|--------|
| Базовый тест | ExampleInstrumentedTest (1) | ✅ Работает |
| DAO интеграционные тесты | ItemDaoTest (11) | ✅ Работает |
| Repository интеграционные тесты | ItemRepositoryIntegrationTest (8) | ✅ Работает |
| База данных | DaysDatabaseTest (2) | ✅ Работает |
| UI-тесты Compose | DaysCountTextTest (7) | ✅ Работает |
| UI-тесты Compose | MoreScreenTest (7) | ✅ Работают |
| UI-тесты Compose | ThemeIconScreenTest (12) | ✅ Работают |
| ViewModel интеграционные тесты | DetailScreenViewModelIntegrationTest (9) | ✅ Работают |
| ViewModel интеграционные тесты | CreateEditScreenViewModelIntegrationTest (1 активный, 10 отключенных) | ⚠️ Требуют решения |
| **Итого (активные)** | **57** | **✅ Работают** |
| **Итого (отключенные)** | **10** | **⚠️ Требуют решения** |

**Всего тестовых методов (@Test):** 67

#### Исправленные тесты (DetailScreenViewModelIntegrationTest)

**DetailScreenViewModelIntegrationTest (9 тестов работают):**

Метод `deleteItem()` рефакторингован для прямого получения элемента из репозитория. Это обеспечивает более надежную логику и устраняет зависимость от кешированного состояния.

**Исправленные тесты используют:** `runTest`, `MainDispatcherRule`, `Turbine` для StateFlow эмиссий, `advanceUntilIdle()`.

**Работающие тесты (9 из 11):** загрузка элемента, обработка отсутствующего элемента, диалог удаления, отмена удаления, обновление через Flow, цветовая метка, несколько элементов, пустые детали, конкретный timestamp.

#### Отключенные тесты (требуют решения)

**CreateEditScreenViewModelIntegrationTest (10 тестов отключены):** все тесты, кроме первого, отключены через `@Ignore("Тест написан с ошибками")`, причина: конфликт между `runBlocking` и `viewModelScope.launch`

**Список отключенных тестов:**

1. `whenUpdateItem_thenItemUpdatedInDatabase`
2. `whenLoadExistingItem_thenItemLoadedFromDatabase`
3. `whenLoadNonExistentItem_thenShowsError`
4. `whenCheckHasChanges_thenCorrectlyDetectsChanges`
5. `whenUpdateMultipleFields_thenAllChangesPersistedInDatabase`
6. `whenCreateMultipleItems_thenAllItemsSavedInDatabase`
7. `whenUpdateAndThenLoadItem_thenChangesPersisted`
8. `whenCheckHasChangesWithColorTagFromValueToNull_thenDetectsChanges`
9. `whenCheckHasChangesWithColorTagFromValueToNull_thenDetectsChanges2`
10. `whenResetHasChanges_thenChangesFlagReset`

---

## Рабочий подход к тестированию ViewModels

### Правила тестирования ViewModels

**ВАЖНО:**

- ❌ **Запрещено:** Создавать новые интеграционные тесты с ViewModels
- ⚠️ **Временное решение:** Для существующих тестов использовать `runTest`, `MainDispatcherRule` и `Turbine`
- ✅ **Рекомендуется:** Тестировать ViewModels через unit-тесты с MockK

**Причина:**

- Конфликт между `runBlocking` и `viewModelScope.launch`
- Flow репозитория не активируется корректно в тестах
- Тесты зависают бесконечно или падают
- Unit-тесты с MockK покрывают всю бизнес-логику
- Интеграционные тесты DAO/Repository покрывают взаимодействие с БД

**Рабочий подход для существующих тестов ViewModels:**

- ✅ Использовать `runTest` вместо `runBlocking`
- ✅ Использовать `MainDispatcherRule` для замены `Dispatchers.Main`
- ✅ Использовать `Turbine` для тестирования StateFlow эмиссий
- ✅ Использовать `advanceUntilIdle()` для ожидания завершения корутин

### Инфраструктура

#### Зависимости

**gradle/libs.versions.toml:**

```toml
[versions]
turbine = "1.1.0"

[libraries]
turbine = { module = "app.cash.turbine:turbine", version.ref = "turbine" }
```

**app/build.gradle.kts:**

```kotlin
androidTestImplementation(libs.turbine)
androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
```

#### MainDispatcherRule

**app/src/androidTest/java/com/dayscounter/test/MainDispatcherRule.kt:**

```kotlin
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    private val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
```

---

## Тесты удаления

### Статус

**✅ Все тесты удаления работают!**

**Выполнено:**

1. ✅ Рефакторинг `DetailScreenViewModel.deleteItem()` — прямое получение элемента из репозитория
2. ✅ Добавлены unit-тесты для методов удаления в `DetailScreenViewModelTest`
3. ✅ Отключены избыточные интеграционные тесты удаления — покрыто в `ItemRepositoryIntegrationTest` и `ItemDaoTest`

### Доказательство работы удаления

**ItemRepositoryIntegrationTest:** проверка удаления элемента и подтверждение его отсутствия

**ItemDaoTest:** проверка удаления через DAO и подтверждение отсутствия в базе данных

---

## Покрытие кода тестами

### Метрики покрытия

**Текущее покрытие:** ~60%
**Целевое покрытие:** 70%+
**Порог CI/CD:** Требуется 60%+ для прохождения проверок

### Рекомендации по улучшению покрытия

1. **Добавить недостающие тесты:**
   - Тесты для всех Use Cases (текущее покрытие ~30/50 методов)
   - Тесты для сложных сценариев (ошибки, исключения)
   - Тесты для UI компонентов (покрыто базовые сценарии)

2. **Улучшить существующие тесты:**
   - Добавить assertions для edge cases
   - Тесты для обработки ошибок
   - Тесты для параллельного выполнения

---

## Интеграция с CI/CD

### Требования к CI/CD

1. **Unit-тесты должны проходить:**
   - Все тесты из `app/src/test/` должны проходить
   - Команда: `./gradlew test`

2. **Instrumented-тесты должны проходить:**
   - Все активные тесты из `app/src/androidTest/` должны проходить
   - Команда: `./gradlew connectedAndroidTest`

3. **Линтинг:**
   - `./gradlew ktlintCheck` — проверка стиля кода
   - `./gradlew detekt` — проверка качества кода
   - Все замечания должны быть исправлены

---

## Требования к новым тестам

### Общие правила

1. **Именование тестов:**
   - Формат: `whenCondition_thenExpectedResult`
   - Пример: `whenItemExistsInDatabase_thenLoadsSuccessfully`

2. **Структура теста (AAA паттерн):**

   ```kotlin
   @Test
   fun testName() {
       // Arrange (Given)
       val input = testData

       // Act (When)
       val result = viewModel.execute(input)

       // Assert (Then)
       assertEquals(expected, result)
   }
   ```

3. **Использование корутин:**
   - Тесты корутин должны использовать `runTest`
   - Замена `Dispatchers.Main` на тестовый диспетчер
   - Ожидание завершения корутин с `advanceUntilIdle()`

### JUnit 5 Migration

**Требования:**

- Использовать аннотации JUnit 5 (`@Test`, `@Before`, `@After`)
- Использовать assertions JUnit 5 (`assertEquals`, `assertTrue`, `assertNull`)
- Избежать аннотаций JUnit 4 (`@org.junit.Test`, `@BeforeClass`, `@AfterClass`)

---

## Заключение

### Достижения

- ✅ Миграция всех unit-тестов на JUnit 5 (264 теста)
- ✅ Исправление интеграционных тестов ViewModels с использованием Turbine
- ✅ Создание рабочей инфраструктуры для тестирования корутин
- ✅ Рефакторинг `DetailScreenViewModel.deleteItem()`
- ✅ Все активные тесты проходят (57 instrumented тестов, 264 unit-теста)
- ✅ Добавлены UI-тесты для экранов MoreScreen (7) и ThemeIconScreen (12)

### Покрытие кода тестами

**Всего тестов:** 321 (264 unit + 57 instrumented)

**Unit-тесты (264):**

- ✅ ViewModels, Use Cases, Domain Models, Data Layer
- ✅ Бизнес-логика покрыта unit-тестами с MockK

**Instrumented-тесты (57 активных):**

- ✅ DAO, Repository, Database: 22 теста
- ✅ UI-тесты Compose: 26 тестов (DaysCountText, MoreScreen, ThemeIconScreen)
- ✅ Интеграционные тесты ViewModels: 9 тестов (DetailScreenViewModel)
- ✅ Взаимодействие с БД покрыто интеграционными тестами

**Отключенные тесты:**

- ⚠️ CreateEditScreenViewModelIntegrationTest: 10 тестов (требуют решения)

### Следующие шаги

1. Принять решение по CreateEditScreenViewModelIntegrationTest (исправить или удалить)
2. Улучшить покрытие кода тестами до 70%+
3. Добавить тесты для дополнительных UI компонентов
4. Рассмотреть добавление скриншот-тестов для визуальной регрессии
