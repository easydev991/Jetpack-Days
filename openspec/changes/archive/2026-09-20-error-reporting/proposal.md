# Proposal

## Why

Аудит 66 catch-сайтов в `app/src/main` показал: в Firebase Crashlytics попадают только ошибки репозиториев и `CalculateDaysDifferenceUseCase`. Остальные скрытые сбои — деградация доставки уведомлений, сбои смены иконки, молчаливые неудачи удаления — живут только в logcat: без Android Studio разработчик их никогда не увидит, а пользователь остаётся один на один с «ничего не произошло».

## What Changes

- Добавить `CrashlyticsHelper.logException(e, message)` в 6 точек отказа — всего 6 catch-блоков, сгруппированных в 6 пунктов (приоритет A аудита catch-сайтов, сент. 2026):
  - `AlarmReminderScheduler` — `SecurityException` (нет exact alarm) → fallback на inexact;
  - `GetFormattedDaysForItemUseCase` — сбой форматирования (fallback-текст на экране без причины);
  - `ExactAlarmPermissionViewModel` — сбой открытия настроек;
  - `DetailScreenViewModel` — `ItemException.DeleteFailed` (удаление молча не срабатывает);
  - `IconManager.changeIcon` — один общий catch при установке иконки (3 исходных узких catch сведены к одному по итогам ревью, throw-семантика сохранена);
  - `IconManager.disableComponent` — logException в его существующем общем catch при сбросе иконок (throw-семантика не меняется).
- Для каждого сайта — unit-тест Red/Green с `mockkObject(CrashlyticsHelper)` и verify `logException`.
- Поведение приложения не меняется: fallback-ветки, `Result.failure` и UI-состояния остаются как есть; добавляется только наблюдаемость (non-fatal в консоли).
- Канал существующий — новая абстракция не вводится.

## Capabilities

### New Capabilities

- `error-reporting`: отчёт о проглоченных (не доходящих до пользователя) ошибках в Firebase Crashlytics через существующий `CrashlyticsHelper.logException` — какие сбойные пути обязаны репортить non-fatal.

### Modified Capabilities

_(нет — изменений в существующих capabilities нет, добавляется новая)_

## Impact

- Код: `reminder/AlarmReminderScheduler.kt`, `domain/usecase/GetFormattedDaysForItemUseCase.kt`, `domain/usecase/IconManager.kt`, `ui/viewmodel/ExactAlarmPermissionViewModel.kt`, `ui/viewmodel/DetailScreenViewModel.kt`.
- Тесты: `AlarmReminderSchedulerTest`, `GetFormattedDaysForItemUseCaseTest`, `ExactAlarmPermissionViewModelTest`, `DetailScreenViewModelTest`, новый `IconManagerTest`.
- Зависимости: нет новых — `CrashlyticsHelper` уже в проекте (release-only, debug collection off).
