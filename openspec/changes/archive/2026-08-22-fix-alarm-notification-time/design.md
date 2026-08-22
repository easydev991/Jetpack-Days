# Design: SCHEDULE_EXACT_ALARM permission handling

## Architecture

Слоистая структура поверх существующего `AlarmReminderScheduler`:

```
Domain Layer:        ExactAlarmPermissionState (sealed)
Reminder Layer:      ExactAlarmPermissionHelper (interface) + AndroidExactAlarmPermissionHelper
UI Layer:            ExactAlarmPermissionViewModel → ExactAlarmPermissionInlineSection → DeniedCard
DI (manual):         AppModule.createExactAlarmPermissionHelper / createExactAlarmPermissionViewModelFactory
Diagnostic:          AlarmReminderScheduler.logger.w(SecurityException) — fallback на setAndAllowWhileIdle
```

Helper co-located с `AlarmReminderScheduler.kt` — оба оборачивают один `AlarmManager`, pass-through use case без трансформации, отдельный repository не нужен (прецедент: `ReminderNotificationPermissionPolicy.kt` использует `NotificationManagerCompat` напрямую).

## Decisions

### Decision 1: SCHEDULE_EXACT_ALARM, не USE_EXACT_ALARM

`SCHEDULE_EXACT_ALARM` безопасен на API 31+, требует user prompt через `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`. `USE_EXACT_ALARM` (API 33+) — без user prompt, но Play Console может отклонить приложение. Берём первый.

### Decision 2: Pre-31 → Allowed; post-31 → AlarmManager.canScheduleExactAlarms()

На API ≤ 30 `SCHEDULE_EXACT_ALARM` не требуется (манифест-permission игнорируется системой, `setExactAndAllowWhileIdle` всегда разрешён). Helper возвращает `Allowed` без обращения к `AlarmManager`. На API ≥ 31 helper обращается к `AlarmManager.canScheduleExactAlarms()` — это authoritative runtime check (манифест-permission ещё не означает, что пользователь не отозвал его в настройках).

### Decision 3: DisposableEffect + LifecycleEventObserver на ON_RESUME

Баннер должен исчезнуть после возврата из системных настроек `SCHEDULE_EXACT_ALARM` (включая путь через системную шторку, не через кнопку баннера). Реализация через `DisposableEffect(lifecycleOwner) { val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }; lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) } }`. `DisposableEffect` вместо `LifecycleEventEffect` — конвенция проекта (`CreateEditReminderEffects.kt:23` / `ObserveReminderStateOnResume`). Семантика идентична: refresh на `ON_RESUME`.

### Decision 4: InlineSection в CreateEditFormContent.kt, не CreateEditScreen.kt

`Screen` оборачивает форму в `Scaffold` — баннер относится к форме, а не к chrome. Подключение в `CreateEditFormContent.kt` после `ReminderSettingsSection` с гейтом `isReminderEnabled` (баннер не показывается, если reminder выключен — это шум).

### Decision 5: Inline-блок ExactAlarmPermissionInlineSection вынесен в ExactAlarmPermissionBanner.kt

Без выноса `CreateEditFormContent.kt` упирается в лимит detekt `TooManyFunctions` (11 функций/файл).

### Decision 6: Manual DI factory, без Hilt

`AppModule.createExactAlarmPermissionViewModelFactory(context): ViewModelProvider.Factory` — тонкая обёртка над `ExactAlarmPermissionViewModel.factory(helper)`, удобна для `viewModel(factory = ...)` в UI. Соответствует конвенции проекта (см. `CreateEditScreenViewModel.factory`).

### Decision 7: Compose-превью для визуальных состояний, androidTest сознательно пропущен

Превью покрывают `Denied(canRequest=true)` и `Denied(canRequest=false)` в `JetpackDaysTheme` — обе визуальные ветки. `Allowed → Unit` — превью не нужно (нет UI). Реальный цикл «запрос permission → возврат → баннер пропадает» тестируется вручную (Этап 10). Дополнительный androidTest не добавляет покрытия.

### Decision 8: onRequestPermission логирует throwable и всегда refresh

`try { helper.requestSettings() } catch (e: ActivityNotFoundException) { logger.w(TAG, "...", e) } finally { refresh() }`. На кастомных прошивках (MIUI/HyperOS) `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` может отсутствовать — `ActivityNotFoundException` не должен крашить UI и должен логироваться как диагностический сигнал, а не как ошибка пользователя.

### Decision 9: Companion factory на ExactAlarmPermissionViewModel

`companion object { fun factory(helper): ViewModelProvider.Factory }`. Конвенция проекта: `CreateEditScreenViewModel.factory`, `ThemeIconViewModel.factory`.

### Decision 10: Logger как instance, не тип

`private val logger: Logger = AndroidLogger()` в `ExactAlarmPermissionViewModel` и `AlarmReminderScheduler` — инстанс-поле, не передаётся через конструктор и не инжектится. Конвенция проекта для trivial классов.

## Risks

- **ActivityNotFoundException на MIUI/HyperOS.** `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` может отсутствовать. Caught + logged + refresh continues. Баннер показывает `Denied(canRequest=false)` с текстом `exact_alarm_unavailable_text` после неудачного `requestSettings()` (state всё равно обновится на `Denied`). Mitigated.
- **Denied(canRequest=false) без кнопки «Включить».** На устройствах, где system screen для exact-alarm отсутствует, показывается предупреждение без action — пользователь не может решить проблему из баннера. Документируем в README (manufacturer battery savers), но не фиксим кодом.
- **Doze/fallback путь.** Если exact-alarm недоступен — `setAndAllowWhileIdle` сохраняется, уведомление всё равно приходит (с задержкой). Антирегрессия не нужна: баннер информирует, scheduler корректно фоллбэчит.
- **Race condition между refresh и onRequestPermission.** При нажатии «Включить» пользователь уходит в system settings. Возврат → ON_RESUME → refresh → state = Allowed → баннер исчезает. Защита от race: `refresh()` всегда читает текущее состояние через `AlarmManager.canScheduleExactAlarms()`, не из локального кеша. Race невозможен.
- **Lifecycle observer leak.** `onDispose` всегда вызывается в Compose — утечки observer нет.
- **App Standby Buckets** лечатся открытием приложения, что и так происходит при настройке reminder'а. Не блокирует.

## Patterns

- Sealed класс для UI state: `when (state) { is Allowed -> Unit; is Denied -> DeniedCard(...) }` — exhaustive, без `else`.
- VM `companion object factory` — прецедент `CreateEditScreenViewModel.factory`.
- `DisposableEffect` + `LifecycleEventObserver` — прецедент `CreateEditReminderEffects.kt`.
- `logger: Logger = AndroidLogger()` instance — прецедент во всём reminder-слое.
- Локализация: ключи snake_case `exact_alarm_*` с суффиксом `_title`/`_text`/`_button`.
- Helper co-located с consumer (`AlarmReminderScheduler.kt` + `ExactAlarmPermissionHelper.kt`) — прецедент `ReminderNotificationPermissionPolicy.kt` + `NotificationManagerCompat`.

## Open questions

- (нет — план стабилизирован, реализация в коммите c3b444dc)
