# KODA.md — Инструкции для работы с проектом Days Counter

## 1. Обзор проекта

**Название:** Days Counter (Дней с момента события)

**Назначение:** Мобильное Android-приложение для отслеживания количества дней с момента значимых событий. Пользователь создаёт события с датой, названием и опциями отображения, а приложение показывает прошедшее время.

**Режим работы:** Полностью офлайн. Отсутствуют сетевые зависимости и удалённые источники данных.

**Целевые платформы:** Android (minSdk 26, targetSdk 35, compileSdk 36)

---

## 2. Технологический стек

### Язык и среда выполнения

- **Kotlin** 2.3.0
- **Android Gradle Plugin** 9.0.0
- **KSP** 2.3.2 (Kotlin Symbol Processing)

### Основные библиотеки

| Категория | Библиотека | Версия |
|-----------|------------|--------|
| UI | Jetpack Compose BOM | 2026.01.00 |
| UI | Material 3 | 1.7.8 |
| Навигация | Navigation Compose | 2.9.6 |
| Жизненный цикл | Lifecycle ViewModel Compose | 2.10.0 |
| База данных | Room | 2.8.4 |
| Хранилище | DataStore Preferences | 1.2.0 |
| Асинхронность | Kotlin Coroutines | 1.10.2 |
| Сериализация | Kotlin Serialization | 1.9.0 |
| Тестирование (unit) | JUnit 5 + MockK | 6.0.2 / 1.14.7 |
| Тестирование (дополнительно) | Turbine + Robolectric | 1.1.0 / 4.16 |
| Тестирование (UI) | Espresso + Compose Testing | 3.7.0 |
| Firebase | Firebase BOM | 34.8.0 |
| Firebase | Crashlytics + Analytics | 34.8.0 |
| Автоматизация | Screengrab (fastlane) | 2.1.1 |

### Управление зависимостями

Версии библиотек централизованно определены в файле:

```
gradle/libs.versions.toml
```

---

## 3. Архитектура проекта

### Общая структура (Clean Architecture + MVVM)

```
app/src/main/java/com/dayscounter/
├── analytics/               # Firebase Analytics для breadcrumb logs
├── crash/                   # Firebase Crashlytics для обработки крашей
├── data/                    # Слой данных
│   ├── database/            # Room: entities, DAO, миграции
│   ├── repository/          # Реализации репозиториев
│   ├── formatter/           # Форматирование текста
│   └── preferences/         # DataStore для настроек
├── domain/                  # Слой домена (бизнес-логика)
│   ├── model/               # Сущности предметной области
│   ├── repository/          # Интерфейсы репозиториев
│   ├── usecase/             # Сценарии использования
│   └── exception/           # Пользовательские исключения
├── di/                      # Dependency Injection (ручной)
├── navigation/              # Маршруты навигации
├── presentation/            # Слой представления
│   ├── ui/
│   │   ├── screen/          # Экраны Compose
│   │   ├── component/       # Переиспользуемые компоненты
│   │   ├── state/           # UI state для ViewModels
│   │   └── theme/           # Тема оформления
│   └── viewmodel/           # ViewModels + UI state
└── util/                    # Утилиты (логирование, константы)
```

### Принципы архитектуры

- **Однонаправленный поток данных:** UI → ViewModel → Use Case → Repository → Data Source
- **Разделение ответственности:** каждый слой отвечает за свой уровень абстракции
- **Отсутствие сетевого слоя:** все данные хранятся локально (Room, DataStore)

### Экраны приложения

| Экран | Маршрут | Назначение |
|-------|---------|------------|
| RootScreen | — | Контейнер с навигацией (Events, More) |
| EventsScreen (MainScreen) | events | Список событий (новый маршрут) |
| MoreScreen | more | Дополнительные функции |
| DetailScreen | item_detail/{itemId} | Детали события + обратный отсчёт |
| CreateItemScreen | create_item | Создание события |
| EditItemScreen | edit_item/{itemId} | Редактирование события |
| ThemeIconScreen | theme_icon | Выбор темы и иконки приложения |
| AppDataScreen | app_data | Управление данными приложения (бэкап, восстановление) |

---

## 4. Сборка и запуск

### Основные команды (Makefile)

| Команда | Описание |
|---------|----------|
| `make build` | Сборка APK для отладки (`./gradlew assembleDebug`) |
| `make clean` | Очистка кеша проекта |
| `make test` | Запуск unit-тестов (JVM, без устройства) |
| `make android-test` | Запуск интеграционных тестов на Android-устройстве |
| `make test-all` | Запуск всех тестов (unit + интеграционные) |
| `make lint` | Проверка стиля кода (ktlint + detekt + markdownlint) |
| `make format` | Автоформатирование кода (ktlint + detekt с исправлениями + markdown) |
| `make check` | Полная проверка: сборка + тесты + линтер |
| `make install` | Установка APK на подключённое устройство/эмулятор |
| `make all` | Полный цикл: check + install |
| `make screenshots` | Генерация скриншотов для всех локалей |
| `make screenshots-ru` | Генерация скриншотов только на русском |
| `make screenshots-en` | Генерация скриншотов только на английском |
| `make setup` | Настройка окружения (rbenv, Ruby, fastlane, markdownlint) |
| `make release` | Создание подписанной AAB-сборки для публикации |
| `make help` | Показать справочное сообщение |

### Прямые команды Gradle

```bash
# Сборка
./gradlew assembleDebug        # Debug APK
./gradlew assembleRelease      # Release APK (требует подписи)

# Тестирование
./gradlew test                 # Unit-тесты
./gradlew connectedDebugAndroidTest  # Интеграционные тесты

# Анализ кода
./gradlew ktlintCheck          # Проверка стиля ktlint
./gradlew ktlintFormat         # Автоисправление ktlint
./gradlew detekt               # Статический анализ detekt

# Установка
./gradlew installDebug         # Установка debug APK
```

### Сборка APK

- **Debug:** `app/build/outputs/apk/debug/app-debug.apk`
- **Release:** Требует настройки signing config в `app/build.gradle.kts`

---

## 5. Правила разработки

### Стиль кода

| Элемент | Соглашение |
|---------|------------|
| Классы | `PascalCase` |
| Функции, переменные | `camelCase` |
| Константы | `UPPER_SNAKE_CASE` |
| Пакеты | `lowercase.with.dots` |
| Тесты | `FunctionName_whenCondition_thenExpectedResult` |

### Комментирование

- **KDoc** для публичных API и Use Cases
- **Объяснять «почему»**, не «что»
- **Логи на русском языке** по умолчанию
- Избегать избыточных комментариев для очевидного кода

### Безопасность кода

- **НЕ использовать оператор `!!`** — безопасно разворачивать опционалы
- Всегда обрабатывать `null` явным образом
- Валидировать пользовательский ввод

### Структура тестов

Тесты располагаются зеркально структуре кода:

```
app/src/test/java/com/dayscounter/     # Unit-тесты
app/src/androidTest/java/com/dayscounter/  # Integration/UI-тесты
```

### Подход к разработке (TDD)

**Критически важный порядок:**

1. **Тесты** → 2. **Логика** → 3. **UI**

1. Писать модульные тесты для бизнес-логики перед реализацией
1. Реализовывать функциональность слоя домена и данных перед UI
1. Создавать Compose UI только после тестирования логики

**Пирамида тестирования:**

- **Unit-тесты (70%)** — изолированное тестирование функций и классов
- **Интеграционные тесты (20%)** — взаимодействие между компонентами
- **UI-тесты (10%)** — критические пользовательские сценарии

### Именование тестов (AAA-паттерн)

```kotlin
@Test
fun calculateDaysDifference_whenSameDay_thenReturnsZero() {
    // Given — подготовка
    val date = LocalDate.now()
    
    // When — действие
    val result = calculateDaysDifference(date, date)
    
    // Then — проверка
    assertEquals(0, result)
}
```

---

## 6. Качество кода

### Инструменты анализа

| Инструмент | Назначение | Команда |
|------------|------------|---------|
| **ktlint** | Проверка стиля кода | `./gradlew ktlintCheck` |
| **ktlint** | Автоисправление | `./gradlew ktlintFormat` |
| **detekt** | Статический анализ | `./gradlew detekt` |
| **detekt** | С автоисправлением | `./gradlew detekt -Pdetekt.autoCorrect=true` |

### Конфигурация detekt

Файл конфигурации: `config/detekt/detekt.yml`

**Специфические настройки проекта:**

- `maxIssues: 10` — допускается до 10 ошибок перед падением сборки
- Для Composable функций разрешён `camelCase` (`functionPattern: '[a-z][a-zA-Z0-9]*'`)

### Требования к качеству

- Все замечания устраняются перед коммитом
- Автоисправления применяются регулярно
- Новый код не должен добавлять новых проблем

---

## 7. Локализация

### Поддерживаемые языки

- **Русский (ru)** — `res/values-ru/strings.xml`
- **Английский (en)** — `res/values-en/strings.xml`

### Правила локализации

- Использовать описательные ключи для строк
- Поддерживать множественное число (plural resources)
- Строки форматирования — через ресурсы, не хардкод
- Поддерживать аббревиатуры для дней/месяцев/лет (d, mo, y)

### Форматирование дат

- Используйте `LocalDate` для работы с датами (без `java.util.Date`)
- Форматирование текста учитывает `DisplayOption` (дни, месяцы+дни, годы+месяцы+дни)
- Локализованные формы: полное и сокращённое написание (days/d, months/mo, years/y)

---

## 8. Firebase интеграция

### Назначение Firebase

Проект использует Firebase для production-сборки:

- **Crashlytics** — сбор и анализ крашей в release-сборках
- **Analytics** — breadcrumb logs для отладки причин крашей

### Crashlytics

**Класс:** `com.dayscounter.crash.CrashlyticsHelper`

**Функции:**

- Автоматический сбор стек-трейсов исключений
- Метрики устройства (модель, версия Android, разрешение экрана)
- Безопасное логирование без риска бесконечного цикла

**Пример использования:**

```kotlin
CrashlyticsHelper.logException(exception, "Ошибка при загрузке данных")
```

### Analytics

**Класс:** `com.dayscounter.analytics.FirebaseAnalyticsHelper`

**Функции:**

- Логирование `screen_view` событий для breadcrumb logs в Crashlytics
- Логирование пользовательских событий для контекста действий
- Безопасное логирование — не ломает приложение при ошибках

**Пример использования:**

```kotlin
FirebaseAnalyticsHelper.logScreenView(context, "EventsScreen", "com.dayscounter.EventsScreen")
FirebaseAnalyticsHelper.logEvent(context, "item_created", Bundle().apply {
    putString("item_id", itemId.toString())
})
```

**Настройка:**

- Активируется только в release-сборках
- Конфигурация в `app/build.gradle.kts` через `google-services` и `firebase-crashlytics` плагины
- Google Services файл (`google-services.json`) должен быть в репозитории android-secrets

---

## 9. Резервное копирование и синхронизация

### Формат JSON (совместим с iOS)

```json
[
  {
    "title": "Название события",
    "details": "Описание",
    "timestamp": 1234567890000,
    "colorTag": "#FFFF00",
    "displayOption": "day"
  }
]
```

### Правила импорта/экспорта

- **Экспорт:** JSON, метаданные версии, обработка ошибок
- **Импорт:** валидация JSON, проверка версии, предотвращение дубликатов
- **Совместимость:** формат идентичен iOS-приложению

---

## 10. Производительность

### Рекомендации

- **Compose:** использовать `remember`/`rememberSaveable` для состояния
- **Списки:** применять `key()` для стабильной идентификации, Lazy loading
- **База данных:** создавать индексы для часто запрашиваемых полей
- **Память:** избегать утечек, правильная привязка к lifecycle
- **Firebase:** отключен в debug-сборках для быстрой отладки

---

## 11. Ключевые файлы проекта

| Файл | Описание |
|------|----------|
| `Makefile` | Команды для сборки, тестирования, линтинга, публикации |
| `gradle/libs.versions.toml` | Централизованное управление версиями зависимостей |
| `QWEN.md` | Лучшие практики разработки (дополнительная информация) |
| `app/build.gradle.kts` | Конфигурация модуля app |
| `config/detekt/detekt.yml` | Конфигурация detekt |
| `app/src/main/AndroidManifest.xml` | Манифест приложения |
| `app/src/main/java/com/dayscounter/analytics/FirebaseAnalyticsHelper.kt` | Firebase Analytics интеграция |
| `app/src/main/java/com/dayscounter/crash/CrashlyticsHelper.kt` | Firebase Crashlytics интеграция |
| `fastlane/Fastfile` | Автоматизация публикации и скриншотов |

---

## 12. Работа с проектом

### Начало работы

1. Клонировать репозиторий
2. Открыть в Android Studio
3. Дождаться синхронизации Gradle
4. Запустить `./gradlew build` для проверки

### Добавление новой функциональности

1. Создать/обновить тесты (unit → integration)
2. Реализовать доменную логику (Use Cases, Entities)
3. Реализовать слой данных (Repository, Data Source)
4. Создать UI (Compose Screen, ViewModel)
5. Добавить навигацию в `navigation/Screen.kt`
6. Добавить локализацию в `res/values/strings.xml` и `res/values-ru/strings.xml`
7. Запустить тесты: `make test`
8. Проверить стиль: `make lint`
9. Собрать проект: `make build`

### Типичные сценарии

| Задача | Действие |
|--------|----------|
| Добавить новый экран | Создать папку в `ui/screen/`, добавить маршрут в `navigation/Screen.kt` |
| Добавить бизнес-логику | Создать Use Case в `domain/usecase/` |
| Работа с БД | Обновить Entity в `data/database/entity/`, добавить DAO |
| Изменить тему | Редактировать `ui/theme/Theme.kt`, добавить иконки в `res/mipmap-anydpi-v26/` |
| Добавить новую иконку приложения | Создать drawable в `res/drawable/`, добавить ресурсы иконок в `res/mipmap-anydpi-v26/`, добавить alias в `AndroidManifest.xml` |
| Добавить локализацию | Обновить строки в `res/values/strings.xml` и `res/values-ru/strings.xml` |

---

## 13. Firebase и краш-репорты

### Отладка крашей

При возникновении крашей в production:

1. Откройте Firebase Console → Crashlytics
2. Breadcrumb logs показывают последовательность экранов (из Analytics)
3. Метрики устройства (модель, версия Android) помогают воспроизвести проблему
4. Стек-трейсы указывают на точное место ошибки

### Локальное тестирование

- Debug-сборки не отправляют данные в Firebase
- Используйте unit-тесты для проверки логики
- Интеграционные тесты: `make android-test`

---

## 14. Тестирование

### Типы тестов

| Тип | Расположение | Инструменты | Доля |
|-----|--------------|-------------|------|
| Unit | `app/src/test/` | JUnit 5, MockK | 70% |
| Integration | `app/src/androidTest/` | Espresso, Room | 20% |
| UI | `app/src/androidTest/` | Compose Testing | 10% |

### Запуск тестов

```bash
make test              # Только unit-тесты
make android-test      # Только интеграционные
make test-all          # Все тесты
```

---

## 15. Краткая справка по командам

```bash
# Быстрый старт
make build             # Сборка
make format            # Форматирование

# Полная проверка перед коммитом
make check             # Сборка + тесты + линтер

# Установка
make install           # На устройство/эмулятор
```

---

*Документ обновлён: 17 января 2026 г.*
