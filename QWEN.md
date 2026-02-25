# Лучшие практики разработки для Android (Qwen Code)

Этот документ описывает лучшие практики разработки Android для этого проекта, следуя принципам TDD и используя современный стек технологий с минимальными зависимостями.

## Обзор проекта

Приложение "Days Counter" для отслеживания дней с момента событий. Работает полностью офлайн.

**Ключевые ограничения:**

- Офлайн режим: без сетевых функций
- Совместимость: модель резервной копии идентична iOS-приложению
- Логи: на русском языке по умолчанию
- Безопасность: не использовать `!!`, безопасно разворачивать опционалы

## Технологический стек

### Основные зависимости

- **Jetpack Compose**: UI
- **Navigation Compose**: навигация
- **Material3 Adaptive Navigation Suite**: адаптивная навигация
- **ViewModel**: управление состоянием UI
- **Room**: локальная БД
- **DataStore**: простое хранение настроек
- **Coroutines**: асинхронность
- **Kotlinx Serialization**: JSON сериализация
- **JUnit 5**: unit-тесты
- **MockK**: мокирование
- **Espresso**: UI тесты
- **Compose Testing**: тестирование Compose UI
- **Turbine**: тестирование Flow
- **Robolectric**: локальные Android тесты
- **Screengrab**: автоматизация скриншотов
- **Firebase Crashlytics**: сбор ошибок (только release)
- **Firebase Analytics**: breadcrumb logs для отладки крашей

**ВАЖНО:** Сетевые библиотеки (Retrofit, OkHttp, Ktor) НЕ используются.

### Зависимости проекта

Полный список зависимостей проекта находится в файле `gradle/libs.versions.toml`. Для получения актуальной информации о версиях библиотек и плагинов обращайтесь к этому файлу.

### Обновленная информация о проекте

- **AGP (Android Gradle Plugin)**: 9.0.0
- **Kotlin**: 2.3.0
- **Compile SDK**: 36
- **Target SDK**: 35
- **Min SDK**: 26
- **KSP (Kotlin Symbol Processing)**: 2.3.2 (KSP2)

## Architecture

### MVVM Pattern

- **Model**: Data layer (Room, repositories)
- **View**: Compose UI
- **ViewModel**: UI state management

### Clean Architecture

```
Presentation (UI, ViewModel)
├── Domain (Use Cases, entities)
└── Data (Repositories, только локальные источники)
```

**ВАЖНО:** Нет сетевых источников данных.

## Подход TDD

### Порядок разработки: Сначала тесты и логика, потом UI

**КРИТИЧЕСКИ ВАЖНО:** Строго соблюдать порядок:

1. Тесты → 2. Логика → 3. UI

1. Писать модульные тесты для бизнес-логики перед реализацией
1. Реализовывать функциональность слоя домена и данных перед UI
1. Проверять бизнес-логику тестами перед разработкой UI
1. Создавать Compose UI только после тестирования логики

### Пирамида тестирования

- **Модульные тесты (70%)**: Тестирование отдельных функций и классов
- **Интеграционные тесты (20%)**: Тестирование взаимодействия между компонентами
- **UI тесты (10%)**: Тестирование пользовательских сценариев и критических путей

### Соглашение об именовании тестов

```kotlin
@Test
fun functionName_whenCondition_thenExpectedResult() {
    // Given
    // When
    // Then
}
```

### Пример модульного теста

```kotlin
class DaysCalculatorTest {
    @Test
    fun calculateDaysDifference_whenSameDay_thenReturnsZero() {
        // Given
        val date = LocalDate.now()

        // When
        val result = DaysCalculator.calculateDaysDifference(date, date)

        // Then
        assertEquals(0, result)
    }
}
```

### Цикл TDD

1. **Red**: Написать падающий тест
2. **Green**: Написать минимальный код для прохождения теста
3. **Refactor**: Улучшить код, сохраняя тесты зелеными

### Принципы

- Тесты пишутся первыми
- Один тест - одна проверка
- Тесты независимы друг от друга
- Тесты быстрые и читаемые

## Лучшие практики Compose

### Управление состоянием

- Использовать `State`/`MutableState` для UI состояния
- Следовать однонаправленному потоку данных
- Использовать `ViewModel` для состояния UI

### Composition Local

- Использовать `CompositionLocal` только для темы/глобальной конфигурации
- Избегать чрезмерного использования `CompositionLocal`

### Аннотации Preview

```kotlin
@Preview
@Composable
fun ComponentPreview() {
    MyAppTheme {
        Component()
    }
}
```

### Комментарии

- Объяснять "почему", не "что"
- Логи на русском

## Слой данных

### База данных Room

- Определять сущности с правильными аннотациями
- Использовать DAO для операций с базой данных
- Правильно обрабатывать миграции

### Паттерн Repository

- Абстрагировать источники данных
- Обрабатывать только локальные источники данных (без удаленных/сетевых источников)
- Предоставлять чистый API для use cases
- Управлять локальными операциями с базой данных и операциями импорта/экспорта файлов

## Слой домена

### Use Cases

- Инкапсулировать бизнес-логику
- Следовать принципу единственной ответственности
- Принимать параметры и возвращать результаты

### Сущности

- Простые Kotlin data классы
- Инкапсуляция бизнес-логики
- Неизменяемые по умолчанию

## Слой представления

### Руководящие принципы ViewModel

- Хранить состояние UI
- Обрабатывать пользовательские события
- Делегировать бизнес-логику use cases
- Использовать `viewModelScope` для корутин

### Состояние UI

- Представлять состояние UI в виде data классов
- Обрабатывать состояния загрузки, успеха и ошибки
- Использовать sealed классы для представления состояния

## Навигация

### Нижняя навигация

- Использовать Navigation Compose для навигации на основе вкладок
- Определять маршруты как sealed классы
- Правильно обрабатывать глубокие ссылки

### Пример структуры навигации

```kotlin
sealed class Screen(
    val route: String,
    val icon: ImageVector? = null,
    val titleResId: Int? = null,
) {
    object Events : Screen(
        route = "events",
        icon = Icons.AutoMirrored.Filled.List,
        titleResId = R.string.events,
    )

    object More : Screen(
        route = "more",
        icon = Icons.Filled.MoreVert,
        titleResId = R.string.more,
    )

    object ThemeIcon : Screen(
        route = "theme_icon",
        titleResId = R.string.app_theme_and_icon,
    )

    object ItemDetail : Screen(
        route = "item_detail/{itemId}",
    ) {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }

    object CreateItem : Screen(
        route = "create_item",
    )

    object EditItem : Screen(
        route = "edit_item/{itemId}",
    ) {
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }

    object AppData : Screen(
        route = "app_data",
        titleResId = R.string.app_data,
    )
}
```

## Обработка ошибок

### Глобальная обработка ошибок

- Использовать sealed классы для типов ошибок
- Обрабатывать ошибки в ViewModel
- Отображать понятные пользователю сообщения об ошибках

### Пример sealed класса для ошибок

```kotlin
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}
```

## Локализация

### Ресурсы строк

- Поддерживать русский (ru) и английский (en)
- Использовать описательные ключи для строк
- Правильно обрабатывать множественное число

### Пример ресурса строк

```xml
<!-- res/values/strings.xml -->
<string name="days_count_format">%d дней</string>
<string name="today">Сегодня</string>
```

## Резервное копирование и восстановление

### Формат JSON

Совместим с iOS-приложением:

```json
[
  {
    "title": "Название события",
    "details": "Описание события",
    "timestamp": 1234567890000,
    "colorTag": "#FFFF00",
    "displayOption": "day"
  }
]
```

### Правила

- Экспорт: JSON формат, метаданные версии, обработка ошибок
- Импорт: валидация JSON, проверка версии, предотвращение дубликатов, обработка ошибок
- Совместимость: формат идентичен iOS, поля совпадают по названию и типу

## Производительность и безопасность

### Производительность

#### Память

- Избегать утечек, правильная привязка к lifecycle
- `remember`/`rememberSaveable` в Compose

#### БД

- Индексы, оптимизация запросов, пагинация для больших списков

#### UI

- Избегать лишних рекомпозиций
- `key()` для стабильной идентификации элементов списков
- Lazy loading для списков

### Безопасность

#### Данные

- Шифрование конфиденциальных данных
- Проверка пользовательского ввода
- Не хранить чувствительные данные открыто

#### Файлы

- Валидация содержимого импортируемых файлов
- Ограничение размера файлов
- Безопасная обработка ошибок

## Качество кода

### Линтинг

- ktlint: `./gradlew ktlintCheck`, `./gradlew ktlintFormat`
- detekt: `./gradlew detekt` (конфиг: `config/detekt/detekt.yml`)
  - Разрешено до 10 ошибок перед падением сборки (maxIssues: 10)
  - Для Composable функций разрешен camelCase (functionPattern: '[a-z][a-zA-Z0-9]*')

### Требования

- Все замечания устранены перед коммитом
- Автоисправления применяются регулярно
- Новый код не добавляет проблем

### Документация

- KDoc для публичных API
- Осмысленные имена
- Комментарии для сложной логики
- Документация актуальна

## Структура проекта

```
app/src/main/java/com/dayscounter/
├── MainActivity.kt
├── DaysCounterApplication.kt
├── navigation/              # Навигация
│   └── Screen.kt           # Определения экранов
├── di/                     # Dependency Injection (ручной)
│   ├── AppModule.kt
│   └── FormatterModule.kt  # Factory методы для DI
├── data/                   # Data layer
│   ├── database/
│   │   ├── DaysDatabase.kt
│   │   ├── dao/
│   │   ├── entity/
│   │   ├── ItemMapper.kt
│   │   └── DisplayOptionConverter.kt
│   ├── provider/           # Форматирование дней
│   ├── repository/
│   │   └── ItemRepositoryImpl.kt
│   ├── preferences/        # DataStore настройки
│   └── local/
├── domain/                 # Domain layer
│   ├── model/             # Domain entities
│   ├── repository/        # Repository interfaces
│   ├── usecase/           # Use cases
│   └── exception/         # Исключения
├── ui/                    # UI layer (Compose)
│   ├── ds/                # Design System компоненты
│   ├── screens/           # Экраны приложения
│   │   ├── appdata/       # AppDataScreen
│   │   ├── common/        # Общие компоненты экранов
│   │   ├── createedit/    # CreateEditScreen + состояние
│   │   ├── detail/        # DetailScreen
│   │   ├── events/        # MainScreen (список событий)
│   │   ├── more/          # MoreScreen
│   │   ├── root/          # RootScreen
│   │   └── themeicon/     # ThemeIconScreen
│   ├── state/             # UI state классы
│   ├── theme/             # App theme
│   └── viewmodel/         # ViewModels
├── analytics/             # Firebase Analytics
├── crash/                 # Firebase Crashlytics
└── util/                  # Утилиты
```

## Правила

- Экран → файл в `ui/screens/<screen>/`
- UI компоненты → файл в `ui/ds/` (Design System)
- ViewModel → файл в `ui/viewmodel/`
- DI → factory методы в `di/FormatterModule.kt`
- Тесты → `test/` (unit) и `androidTest/` (integration/UI)
- Структура тестов зеркалит структуру кода

## Тестирование

### Типы тестов

- **Unit**: бизнес-логика изолированно, MockK для зависимостей, AAA паттерн
- **Integration**: взаимодействие слоев (DAO, Repository), реальные реализации БД
- **UI**: критические сценарии, Compose Testing для компонентов

### Инструменты

- JUnit 5 - unit-тесты
- MockK - мокирование
- Robolectric - локальные Android тесты
- Compose Testing - Compose компоненты
- Turbine - тестирование Flow
- Screengrab - автоматизация скриншотов

### Структура

- `app/src/test/` - unit-тесты (ViewModels, Use Cases, Domain models)
- `app/src/androidTest/` - integration/UI тесты (DAO, Repository, UI компоненты)
- Структура зеркалит код
- Имена классов: `*Test`

### Best Practices

**Важно:** Не писать интеграционные тесты с ViewModels

- ❌ **Запрещено:** Создавать интеграционные тесты с ViewModels (CreateEditScreenViewModelIntegrationTest, DetailScreenViewModelIntegrationTest)
- ✅ **Допустимо:** Тестировать ViewModels только через unit-тесты с MockK
- ✅ **Допустимо:** Тестировать DAO и Repository через интеграционные тесты без ViewModels

**Причина:**

- Конфликт между `runBlocking` и `viewModelScope.launch`
- Flow репозитория не активируется корректно в тестах
- Тесты зависают бесконечно или падают

**Рабочий подход к тестированию:**

**Unit-тесты ViewModels (с MockK):**

```kotlin
@Test
fun loadItems_whenRepositoryReturnsData_thenSuccessState() {
    // Given
    val mockRepository = mockk<ItemRepository>()
    every { mockRepository.getAllItems() } returns flowOf(listOf(item))
    val viewModel = MainScreenViewModel(mockRepository)

    // When
    viewModel.loadItems()

    // Then
    assertEquals(MainScreenState.Success(listOf(item)), viewModel.uiState.value)
}
```

**Интеграционные тесты DAO и Repository:**

```kotlin
@Test
fun test() {
    runBlocking {
        repository.insertItem(item)
        val result = repository.getItemById(id)
        assertNotNull(result)
    }
}
```

- ✅ Прямые вызовы DAO/Repository
- ✅ Синхронные операции с БД
- ✅ Блокируют поток до завершения корутины
- ✅ Не используют ViewModel
- ✅ Используют только repository

**UI-тесты Compose компонентов:**

```kotlin
@Test
fun daysCountText_whenToday_thenShowsToday() {
    composeTestRule.setContent {
        DaysCountText(item)
    }
    composeTestRule.onNodeWithText("Сегодня").assertIsDisplayed()
}
```

- ✅ Тестируют UI компоненты в изоляции
- ✅ Используют Compose Testing
- ✅ Быстрые и надежные
- ✅ Не зависят от ViewModel

**Общие практики:**

- Быстрые и независимые тесты
- Описательные имена
- Один тест - одна проверка
- Тестировать поведение, не реализацию
- Интеграционные тесты только для DAO и Repository
- Unit-тесты для ViewModels с моками
- UI-тесты для Compose компонентов без бизнес-логики

**Unit-тесты ViewModels (с MockK):**

## Рассмотрение CI/CD

### Процесс сборки

- Использовать Gradle для автоматизации сборки
- Реализовать покрытие кода тестами
- Запускать тесты при каждом коммите

### Контроль качества

- Поддерживать пороги покрытия кода
- Запускать инструменты статического анализа
- Выполнять сканирование безопасности

## Makefile

Для удобства работы с проектом создан Makefile с командами:

### Основные команды

- `make build` - сборка APK для отладки
- `make clean` - очистка кэша проекта
- `make test` - запуск unit-тестов (JVM, без устройства) с отображением результатов
- `make android-test` - запуск интеграционных тестов на Android устройстве
- `make test-all` - запуск всех тестов (unit + интеграционные)
- `make lint` - запуск ktlint, detekt и markdownlint (проверка без исправлений)
- `make format` - форматирование кода (ktlint + detekt с исправлениями) и Markdown-файлов
- `make check` - полная проверка (сборка + тесты + линтер)
- `make install` - установка APK на устройство
- `make all` - полная проверка и установка приложения на устройство

### Команды для публикации

- `make apk` - создание подписанного APK для релизной конфигурации (без повышения версии). Файл: dayscounter{VERSION_CODE}.apk
- `make release` - создание подписанной AAB-сборки для публикации (аналог testflight в iOS). Файл: dayscounter{VERSION_CODE}.aab

### Команды для скриншотов

- `make screenshots` - генерировать скриншоты для всех локалей через fastlane
- `make screenshots-ru` - генерировать скриншоты только на русском
- `make screenshots-en` - генерировать скриншоты только на английском
- `make update_readme` - обновить таблицу со скриншотами в README.md
- `make android-test-report` - открыть HTML отчет интеграционных тестов в браузере

### Команды для настройки окружения

- `make setup` - установка и настройка инструментов для локальной разработки (rbenv, Ruby, fastlane, markdownlint-cli)
- `make fastlane` - запустить меню команд fastlane
- `make update_fastlane` - проверить и установить обновления fastlane

Для использования команд Makefile просто выполните их в корне проекта, например:

```
make test
```

или

```
make check
```

Эти команды автоматизируют рутинные операции и упрощают процесс разработки.
