# Firebase Analytics Architecture

Документ фиксирует фактическую архитектуру и рабочие правила аналитики в `JetpackDays` на текущий момент.

## 1. Назначение

- Аналитика используется как технические breadcrumbs для диагностики проблем в пользовательских сценариях.
- Цель: видеть последовательность действий (нажатия и переходы по экранам) и ошибки (`app_error`) перед сбоем/ошибкой.
- Продуктовая персонализация и сбор персональных данных не являются целью.

## 2. Архитектура

- Единая модель событий: `AnalyticsEvent` (`sealed interface`)
  - `ScreenView(screen: AppScreen, screenClass: String?)`
  - `UserAction(action: UserActionType, iconName: String?)`
  - `AppError(operation: AppErrorOperation, throwable: Throwable)`
- Провайдерная схема:
  - `AnalyticsProvider` — контракт провайдера
  - `FirebaseAnalyticsProvider` — отправка в Firebase Analytics
  - `NoopAnalyticsProvider` — заглушка (debug/test)
- `AnalyticsService` делает fan-out события по всем подключенным провайдерам.
- Ошибка одного провайдера не ломает остальных: исключения ловятся внутри `AnalyticsService` и пишутся через `Logger`.

## 3. DI и жизненный цикл

- `AnalyticsService` создается в `AppModule.createAnalyticsService(context)` и кэшируется как singleton.
- Выбор провайдера зависит от типа сборки:
  - `BuildConfig.DEBUG = true` -> `NoopAnalyticsProvider`
  - иначе -> `FirebaseAnalyticsProvider`
- Сервис передается явно через параметры в экраны/ViewModel (ручной DI).
- Прямые вызовы Firebase SDK из экранов и ViewModel не используются.

## 4. Модель данных событий

### 4.1 ScreenView

`AppScreen`:
- `EVENTS` (`EventsScreen`)
- `DETAIL` (`DetailScreen`)
- `CREATE_EDIT` (`CreateEditScreen`)
- `MORE` (`MoreScreen`)
- `THEME_ICON` (`ThemeIconScreen`)
- `APP_DATA` (`AppDataScreen`)

### 4.2 UserAction

`UserActionType`:
- `CREATE`
- `EDIT`
- `DELETE`
- `SORT`
- `ITEM_SAVED`
- `ICON_SELECTED`
- `CREATE_BACKUP`
- `RESTORE_BACKUP`
- `DELETE_ALL_DATA`

`iconName` заполняется только для `ICON_SELECTED`.

### 4.3 AppError

`AppErrorOperation`:
- `SET_ICON`
- `CREATE_BACKUP`
- `RESTORE_BACKUP`
- `DELETE_ALL_DATA`
- `CREATE_ITEM`
- `UPDATE_ITEM`

В Firebase отправляются параметры:
- `operation`
- `error_domain` (имя класса исключения)
- `error_code` (hashCode исключения)

## 5. Правила трекинга (фактически реализовано)

- `screen_view` логируется при входе на destination в `NavHost` через `LaunchedEffect`.
- `user_action` логируется в точке нажатия пользователя (до выполнения долгих/потенциально падающих операций).
- Ключевые кнопки навигации и действий логируются как `user_action`:
  - создание, редактирование, сортировка, удаление, сохранение
  - экспорт/импорт бэкапа, подтверждение удаления всех данных
  - выбор иконки приложения
- Ошибки операций логируются как `app_error` в `catch`/`onFailure` ветках ViewModel.

## 6. Отправка в Firebase

`FirebaseAnalyticsProvider` использует:
- `FirebaseAnalytics.Event.SCREEN_VIEW` для `ScreenView`
- `user_action` для `UserAction`
- `app_error` для `AppError`

Дополнительно провайдер пишет debug-лог через абстракцию `Logger`.

Важно для отладки:
- В debug-сборке по умолчанию подключен `NoopAnalyticsProvider`, поэтому события не отправляются в Firebase.
- Для проверки отправки в Firebase нужна не-debug конфигурация с активным `FirebaseAnalyticsProvider`.

## 7. Приватность и ограничения

- В события запрещено передавать PII.
- Не передаются `user_id`, email, телефон, пользовательский текст, координаты и другие персональные данные.
- Допускаются технические и сценарные значения, не идентифицирующие пользователя напрямую (например, `iconName` выбранной иконки).

## 8. Текущее покрытие

- `ScreenView`: все основные экраны root-навигации покрыты.
- `UserAction`: покрыты ключевые действия на `Main`, `Create/Edit`, `ThemeIcon`, `AppData`.
- `AppError`: покрыты критичные ветки ошибок в `CreateEditScreenViewModel`, `ThemeIconViewModel`, `AppDataScreenViewModel`.

## 9. Ключевые файлы

- `app/src/main/java/com/dayscounter/analytics/AnalyticsEvent.kt`
- `app/src/main/java/com/dayscounter/analytics/AnalyticsProvider.kt`
- `app/src/main/java/com/dayscounter/analytics/AnalyticsService.kt`
- `app/src/main/java/com/dayscounter/analytics/FirebaseAnalyticsProvider.kt`
- `app/src/main/java/com/dayscounter/analytics/NoopAnalyticsProvider.kt`
- `app/src/main/java/com/dayscounter/di/AppModule.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/events/MainScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/appdata/AppDataScreen.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/ThemeIconViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/AppDataScreenViewModel.kt`
