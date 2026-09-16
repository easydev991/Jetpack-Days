# Firebase Integration в Jetpack Days

## Обзор

Firebase интегрирован в проект для сбора крашей в production сборках. Используются два сервиса:

- **Firebase Crashlytics** — сбор критических ошибок с полными стек-трейсами
- **Firebase Analytics** — breadcrumb logs для контекста действий пользователя перед крашем

---

## Конфигурация сборки

### Build types

Сбор ошибок **включен только в release** и **отключён в debug**:

```kotlin
// app/build.gradle.kts
buildTypes {
    debug {
        manifestPlaceholders["crashlyticsCollectionEnabled"] = false
    }
    release {
        manifestPlaceholders["crashlyticsCollectionEnabled"] = true
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

### AndroidManifest

Оба флага (`crashlytics`, `analytics`) управляются через один placeholder:

```xml
<meta-data
    android:name="firebase_crashlytics_collection_enabled"
    android:value="${crashlyticsCollectionEnabled}" />
<meta-data
    android:name="firebase_analytics_collection_enabled"
    android:value="${crashlyticsCollectionEnabled}" />
```

### Зависимости

Управляются через Firebase BOM в `gradle/libs.versions.toml`:

| Компонент | Версия |
|-----------|--------|
| Firebase BOM | (через libs.versions.toml) |
| Firebase Crashlytics | (через BOM) |
| Firebase Analytics | (через BOM) |
| Google Services plugin | 4.5.0 |
| Crashlytics Gradle plugin | 3.0.8 |

---

## google-services.json

- Файл `app/google-services.json` **не коммитится** (добавлен в `.gitignore`) — исходник хранится в приватном репозитории `easydev991/android-secrets` (каталог `jetpackdays/google-services.json`)
- Скачивается вместе с остальными секретами по SSH (доступ настраивается через `make setup_ssh`): цель `_load_secrets` в `Makefile` клонирует репозиторий и копирует файл в `app/`
- Загрузка триггерится обёрткой `_ensure_secrets` — только если `.secrets/` или `app/google-services.json` отсутствуют, а не перед каждой gradle-сборкой
- Один файл используется обоими `productFlavors` (`rustore` и `github` — оба работают с одним Firebase-проектом)
- Проект Firebase: `days-counter-5ee1f`
- Android App: `com.dayscounter`

На fresh checkout без SSH-доступа `make build` / `make apk` упадут с понятным сообщением об ошибке от `_load_secrets` (см. «Секреты для подписи» в [docs/deployment.md](deployment.md#секреты-для-подписи)).

---

## CrashlyticsHelper

Безопасная обёртка для логирования ошибок в Crashlytics (файл: `app/.../crash/CrashlyticsHelper.kt`):

```kotlin
object CrashlyticsHelper {
    fun logException(exception: Throwable, message: String? = null) {
        try {
            Firebase.crashlytics.apply {
                message?.let { setCustomKey("error_message", it) }
                recordException(exception)
            }
        } catch (e: Exception) {
            // Игнорируем ошибки Crashlytics, чтобы избежать бесконечного цикла
        }
    }
}
```

**Особенности:**
- Безопасный вызов — ошибки в самом Crashlytics не приводят к рекурсии
- Дополнительное сообщение записывается в custom key `error_message`
- Используется общий перехват `Exception` намеренно (см. Suppress-аннотацию в файле)

### Интеграция с кодом

`CrashlyticsHelper.logException()` вызывается во всех ключевых точках:

- **ItemRepositoryImpl** — ошибки при CRUD-операциях с элементами
- **ReminderRepositoryImpl** — ошибки при работе с напоминаниями
- **CalculateDaysDifferenceUseCase** — ошибки при вычислении разницы дат

---

## Analytics (breadcrumb logs)

### Архитектура

События аналитики проходят через цепочку:

```
Composable Screen
    ↓ (вызов)
AnalyticsService.log(event)
    ↓ (делегирование)
FirebaseAnalyticsProvider.log(event)  → Firebase Analytics
```

**`AnalyticsService`** (`app/.../analytics/AnalyticsService.kt`) — принимает список провайдеров и делегирует события каждому.

**`AnalyticsProvider`** (`app/.../analytics/AnalyticsProvider.kt`) — интерфейс для провайдеров аналитики.

### Типы событий

Определены в `AnalyticsEvent` (sealed interface):

| Событие | Триггер | Параметры |
|---------|---------|-----------|
| `ScreenView` | Композиция экрана | `screen` (AppScreen), `screenClass` (опционально) |
| `UserAction` | Действие пользователя | `action` (UserActionType), `iconName` (опционально) |
| `AppError` | Ошибка в приложении | `operation`, `throwable` |

### screen_view на экранах

Логирование `screen_view` происходит в `ui/screens/common/RootScreenComponents.kt` через `LaunchedEffect` при композиции каждого экрана. Создание и редактирование — одна композиция `CreateEditScreen` (разные маршруты навигации):

| Экран | AppScreen | screenClass |
|-------|-----------|-------------|
| Список событий | `EVENTS` | `MainScreen` |
| Детали события | `DETAIL` | `DetailScreen` |
| Создание события | `CREATE_EDIT` | `CreateEditScreen` |
| Редактирование | `CREATE_EDIT` | `CreateEditScreen` |
| Ещё (More) | `MORE` | `MoreScreen` |
| Тема/иконка | `THEME_ICON` | `ThemeIconScreen` |
| Данные приложения | `APP_DATA` | `AppDataScreen` |

### UserAction

Типы действий определены в enum `UserActionType`: `CREATE`, `EDIT`, `DELETE`, `SORT`, `OPEN_FILTER`, `ITEM_SAVED`, `ICON_SELECTED`, `CREATE_BACKUP`, `RESTORE_BACKUP`, `DELETE_ALL_DATA`.

---

## Тестирование

Unit-тесты для проверки безопасности и корректности логирования:

| Тест | Файл |
|------|------|
| CrashlyticsHelper — различные типы исключений с/без сообщения | `app/src/test/.../crash/CrashlyticsHelperTest.kt` |
| AnalyticsService — делегирование всем провайдерам | `app/src/test/.../analytics/AnalyticsServiceTest.kt` |
| AnalyticsEvent — создание событий всех типов | `app/src/test/.../analytics/AnalyticsEventTest.kt` |
| NoopAnalyticsProvider — безопасность при пустом провайдере | `app/src/test/.../analytics/NoopAnalyticsProviderTest.kt` |

---

## Безопасность и конфиденциальность

**✅ ДОПУСТИМО:** логировать типы исключений, названия экранов, контекст действий без персональных данных, breadcrumb logs.

**❌ НЕДОПУСТИМО:** логировать персональную информацию, пароли, токены, ключи шифрования, содержимое пользовательских данных, location данные, уникальные идентификаторы.

---

## Возможные проблемы

### Конфликты зависимостей

Использовать Firebase BOM для управления версиями. Проверить: `./gradlew :app:dependencies`.

### Crashlytics не собирает краши в release

- Проверить `crashlyticsCollectionEnabled = true` в release build type
- Проверить `AndroidManifest.xml`
- Проверить настройки ProGuard (не вырезаны ли Firebase классы)
- Убедиться, что устройство имеет интернет-соединение

### Увеличение размера APK

- Включить `isMinifyEnabled = true` в release
- Использовать APK Analyzer при необходимости

### Ошибки в debug попадают в Crashlytics

- Убедиться, что `crashlyticsCollectionEnabled = false` в debug
- Пересобрать проект после изменений

---

## Ссылки

- [Firebase Android Setup](https://firebase.google.com/docs/android/setup)
- [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics)
- [Customize crash reports for Android](https://firebase.google.com/docs/crashlytics/android/customize-crash-reports#get-breadcrumb-logs)
- [Firebase BOM](https://firebase.google.com/docs/android/learn-more#bom)
- [Firebase Security Guidelines](https://firebase.google.com/support/guides/security)
