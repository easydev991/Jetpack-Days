## Why

Reminder-уведомления приходят с задержкой в минуты (эмулятор) / часы (устройство), часто — только после ручного открытия приложения. Корень — `AndroidManifest.xml` не объявляет `SCHEDULE_EXACT_ALARM`, и на API 31+ `setExactAndAllowWhileIdle` бросает `SecurityException`, который глотается в `AlarmReminderScheduler`, откатываясь на `setAndAllowWhileIdle` (неточный alarm, Doze задерживает до выхода устройства из сна). Пользователь должен иметь возможность увидеть, что exact-alarm отключён в системе, и быстро перейти в настройки, чтобы вернуть точные напоминания.

## What Changes

- `AndroidManifest.xml`: `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` после `POST_NOTIFICATIONS`.
- Новый доменный тип `ExactAlarmPermissionState` (sealed: `Allowed` / `Denied(canRequest: Boolean)`).
- Новый `ExactAlarmPermissionHelper` (interface) + `AndroidExactAlarmPermissionHelper` (co-located с `AlarmReminderScheduler`): pre-31 → `Allowed`; post-31 → `AlarmManager.canScheduleExactAlarms()`; `requestSettings()` бросает `ActivityNotFoundException` на MIUI/HyperOS.
- Новый `ExactAlarmPermissionViewModel` с `companion object { fun factory(helper) }`, `StateFlow`, `refresh()`, `onRequestPermission()` (try/catch → `logger.w` → refresh в любом случае).
- Новый `ExactAlarmPermissionInlineSection` Composable (создаёт VM, `when (state)`, `DisposableEffect` + `LifecycleEventObserver` на `ON_RESUME`) + `DeniedCard` (internal) + 2 Compose-превью в `JetpackDaysTheme`.
- Интеграция в `CreateEditFormContent.kt` после `ReminderSettingsSection`, гейт `isReminderEnabled`.
- `AlarmReminderScheduler.kt`: `SecurityException` логируется через `logger.w` (не глотать), fallback на `setAndAllowWhileIdle` сохраняется.
- `AppModule.kt`: `createExactAlarmPermissionHelper(context)` + `createExactAlarmPermissionViewModelFactory(context)`.
- Локализация: 4 строки (`exact_alarm_request_title`, `exact_alarm_request_text`, `exact_alarm_request_button`, `exact_alarm_unavailable_text`) в `values/strings.xml` + `values-ru/strings.xml`.
- Тесты: `ExactAlarmPermissionViewModelTest` (5 JVM unit, JUnit 5, Fake helper, синхронные проверки) + новый кейс `schedule_when_security_exception_then_fallback_invoked_and_logged` в `AlarmReminderSchedulerTest` (MockK для `AlarmManager`).

## Capabilities

### New Capabilities

- `exact-alarm-permission`: политика доступа к `SCHEDULE_EXACT_ALARM`: доменная модель состояния, helper для проверки/запроса permission, ViewModel с авто-обновлением на `ON_RESUME`, UI-баннер в форме создания/редактирования reminder'а, локализация на en/ru, корректное логирование `SecurityException` в `AlarmReminderScheduler`.

### Modified Capabilities

- (нет — корневой дефект не в spec'd поведении; новая capability ортогональна)

## Impact

- **Файлы новые:** 6 (`ExactAlarmPermissionState.kt`, `ExactAlarmPermissionHelper.kt`, `ExactAlarmPermissionViewModel.kt`, `ExactAlarmPermissionBanner.kt`, `ExactAlarmPermissionBannerPreviews.kt`, `ExactAlarmPermissionViewModelTest.kt`).
- **Файлы изменяемые:** 7 (`AndroidManifest.xml`, `AlarmReminderScheduler.kt`, `AppModule.kt`, `CreateEditFormContent.kt`, `values/strings.xml`, `values-ru/strings.xml`, `AlarmReminderSchedulerTest.kt`).
- **Тесты:** +5 JVM unit (`ExactAlarmPermissionViewModelTest`), +1 JVM unit (`AlarmReminderSchedulerTest` SecurityException case); androidTest сознательно пропущен — визуальные состояния покрыты 2 Compose-превью, поведение VM — 5 JVM unit.
- **Зависимости:** нет новых (`androidx.lifecycle:lifecycle-runtime-compose:2.11.0` подтягивается транзитивно через `lifecycle-runtime-ktx` / `lifecycle-viewmodel-compose`).
- **API breaking:** нет — баннер аддитивен, манифест-permission — opt-in для пользователя.
- **Ponytail-упрощения в процессе:** удалён stateless wrapper `ExactAlarmPermissionBanner` + `ExactAlarmPermissionPolicy` + 3 теста политики (типы `ExactAlarmBannerKind`/`Decision`/`messageResId` оказались мёртвыми).
- **Без изменений:** `RootScreen.kt`, `Screen.kt`, `ReminderAlarmReceiver.kt`, `ReminderManager`/`DefaultReminderManager`, `data/database/`.