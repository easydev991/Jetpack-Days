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
| Firebase Crashlytics | (через libs.versions.toml) |
| Firebase Analytics | (через libs.versions.toml) |
| Google Services plugin | 4.4.4 |
| Crashlytics Gradle plugin | 3.0.7 |

---

## google-services.json

- Файл `app/google-services.json` присутствует в проекте
- Добавлен в `.gitignore` — не хранится в репозитории
- Проект Firebase: `days-counter-5ee1f`
- Android App: `com.dayscounter`

Для локальной разработки достаточно поместить `google-services.json` в `app/`. Для CI/CD — передавать через защищённое хранилище.

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

Определены в `AnalyticsEvent` (sealed class):

| Событие | Триггер | Параметры |
|---------|---------|-----------|
| `ScreenView` | Композиция экрана | `screen` (AppScreen), `screenClass` (опционально) |
| `UserAction` | Действие пользователя | `action` (UserActionType), `iconName` (опционально) |
| `AppError` | Ошибка в приложении | `operation`, `throwable` |

### screen_view на экранах

Логирование `screen_view` происходит в `RootScreenComponents.kt` через `LaunchedEffect` при композиции каждого экрана:

| Экран | AppScreen | screenClass |
|-------|-----------|-------------|
| Список событий | `EVENTS` | `MainScreen` |
| Детали события | `DETAIL` | `DetailScreen` |
| Создание события | `CREATE_EVENT` | `CreateEventScreen` |
| Редактирование | `EDIT_EVENT` | `EditEventScreen` |
| Ещё (More) | `MORE` | `MoreScreen` |
| Тема/иконка | `THEME_ICON` | `ThemeIconScreen` |
| Данные приложения | `APP_DATA` | `AppDataScreen` |

### UserAction

Логируются действия: `EDIT`, `DELETE`, `DELETE_ALL`, `SORT_CHANGED`, `CONFIRM_DELETE`, `CANCEL_DELETE`, `ICON_SELECTED`, `ICON_CHANGED`, `BACKUP_CREATED`, `BACKUP_RESTORED`, `SHARE`, и другие.

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
