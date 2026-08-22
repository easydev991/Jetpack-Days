# Tasks: SCHEDULE_EXACT_ALARM permission handling

## Этап 1: Модель состояния (Domain Layer, TDD) — DONE

- [x] Создан `app/src/main/java/com/dayscounter/domain/model/ExactAlarmPermissionState.kt` — sealed (`Allowed` / `Denied(canRequest: Boolean)`) с KDoc на class и оба case'а (почему `Boolean`).

## Этап 2: ~~Политика~~ — удалено по Ponytail-ревью

- (skipped) `ExactAlarmPermissionPolicy.kt` + `ExactAlarmPermissionPolicyTest.kt` (3 теста, 43 строки) удалены — `decideExactAlarmBanner` не вызывается из production, типы `ExactAlarmBannerKind`/`Decision`/`messageResId` мёртвые. Логика покрыта `when (state) { is Allowed -> Unit; is Denied -> DeniedCard(...) }` в `ExactAlarmPermissionInlineSection`.

## Этап 3: Helper (Reminder Layer, TDD) — DONE

- [x] Создан `app/src/main/java/com/dayscounter/reminder/ExactAlarmPermissionHelper.kt` — interface (`getState`, `requestSettings`) + `AndroidExactAlarmPermissionHelper`. Pre-31 guard (на API < 31 system always grants, `Denied(canRequest=false)`). `requestSettings` бросает `ActivityNotFoundException` на кастомных прошивках (MIUI/HyperOS).

## Этап 4: ViewModel (UI Layer, TDD) — DONE

- [x] `app/src/main/java/com/dayscounter/ui/viewmodel/ExactAlarmPermissionViewModel.kt`: `_state: MutableStateFlow` от `helper.getState()`, `state: StateFlow`, `refresh()`, `onRequestPermission()` (try/catch + `logger.w` + refresh в любом случае). `companion object { fun factory(helper) }` — конвенция проекта (`CreateEditScreenViewModel.factory`). `logger: Logger = AndroidLogger()` — instance, не тип.
- [x] `app/src/test/java/com/dayscounter/ui/viewmodel/ExactAlarmPermissionViewModelTest.kt` — 5 JVM unit-тестов: initial state × 2, refresh, onRequestPermission с логом throwable, onRequestPermission с success. Fake helper на `var currentState` + `AtomicInteger`; синхронные JUnit 5 проверки.

**Авто-обновление при возврате на экран:** в `ExactAlarmPermissionInlineSection` добавлен `DisposableEffect(lifecycleOwner) { val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh() }; lifecycleOwner.lifecycle.addObserver(observer); onDispose { lifecycleOwner.lifecycle.removeObserver(observer) } }`. Без этого баннер не исчезнет после возврата из системных настроек `SCHEDULE_EXACT_ALARM` (шторка уведомлений / авто-диалог ОС).

## Этап 5: Логирование в AlarmReminderScheduler (минимальный фикс) — DONE

- [x] `app/src/main/java/com/dayscounter/reminder/AlarmReminderScheduler.kt`: `SecurityException` логируется через `logger.w(TAG, "...fallback на setAndAllowWhileIdle...", securityException)` + fallback на `setAndAllowWhileIdle`.
- [x] `app/src/test/java/com/dayscounter/reminder/AlarmReminderSchedulerTest.kt`: тест `schedule_when_security_exception_then_fallback_invoked_and_logged` (MockK для `AlarmManager`).

## Этап 6: DI интеграция (без TDD) — DONE

- [x] `app/src/main/java/com/dayscounter/di/AppModule.kt`: `createExactAlarmPermissionHelper(context)` + `createExactAlarmPermissionViewModelFactory(context): ViewModelProvider.Factory` (тонкая обёртка над `ExactAlarmPermissionViewModel.factory(helper)`, удобна для `viewModel(factory = ...)` в UI).

## Этап 7: UI компонент баннера (UI Layer, прямые шаги) — DONE

- [x] 7.1 `app/src/main/java/com/dayscounter/ui/screens/createedit/ExactAlarmPermissionBanner.kt`: `DeniedCard(canRequest, onRequestPermission, modifier)` (internal) + `ExactAlarmPermissionInlineSection()` (создаёт VM, `when (val s = state)`, `DisposableEffect` на ON_RESUME). **Уточнение по Ponytail-ревью:** stateless wrapper `ExactAlarmPermissionBanner` удалён — единственный production-вызывающий (InlineSection) делает `when (state)` напрямую, 3 превью бессмысленны.
- [x] 7.2 Два визуальных состояния: `canRequest=true` → `OutlinedCard` с кнопкой «Включить»; `canRequest=false` → `OutlinedCard` с предупреждением; `Allowed` → `Unit`.
- [x] 7.3 InlineSection создаёт VM через `viewModel(factory = AppModule.createExactAlarmPermissionViewModelFactory(context))` (manual DI, без Hilt).
- [x] 7.4 Подключён в `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditFormContent.kt` после `ReminderSettingsSection`, только если `isReminderEnabled` в `CreateEditReminderState`.

**Отступление от плана (зафиксировано):** подключение выполнено в `CreateEditFormContent.kt`, а не `CreateEditScreen.kt` — `Screen` оборачивает форму в `Scaffold`, баннер относится к форме, а не к chrome. Вместо `LifecycleEventEffect(ON_RESUME)` — `DisposableEffect` + `LifecycleEventObserver` (конвенция проекта, см. `CreateEditReminderEffects.kt:23` / `ObserveReminderStateOnResume`). Семантика идентична: refresh на `ON_RESUME`. Inline-блок `ExactAlarmPermissionInlineSection` вынесен в `ExactAlarmPermissionBanner.kt` — иначе `CreateEditFormContent.kt` упирается в лимит detekt `TooManyFunctions` (11 функций/файл).

**Локализация** (4 строки в `values/strings.xml` + `values-ru/strings.xml`): `exact_alarm_request_title`, `exact_alarm_request_text`, `exact_alarm_request_button`, `exact_alarm_unavailable_text`.

## Этап 8: AndroidManifest и lifecycle (минимальный) — DONE

- [x] 8.1 В `app/src/main/AndroidManifest.xml` — `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` после `POST_NOTIFICATIONS`.
- [x] 8.2 `androidx.lifecycle:lifecycle-runtime-compose:2.11.0` подтягивается транзитивно (через `lifecycle-runtime-ktx` / `lifecycle-viewmodel-compose`). Правок в `libs.versions.toml` / `app/build.gradle.kts` не требуется. Артефакт в проекте сейчас не используется напрямую (выбран `DisposableEffect` — см. отступление 7.4), но версия зафиксирована в classpath.
- [x] 8.3 androidTest пропущен сознательно. Визуальные состояния покрыты 2 Compose-превью (`Denied` × 2; `Allowed → Unit` — превью не нужно), поведение `ExactAlarmPermissionViewModel` — 5 JVM unit-тестами. Реальный цикл «запрос permission → возврат → баннер пропадает» тестируется вручную (Этап 10). Дополнительный androidTest не добавляет покрытия.

## Этап 9: Проверки — DONE

- [x] 9.1 `make test` — **466/466 успешных** (58 unit-классов). Арифметика: 462 (исходных) + 5 (ViewModel) + 2 (AlarmScheduler) − 3 (удалённые тесты политики) = 466.
- [x] 9.2 `make android-test` — пропущено сознательно (см. 8.3).
- [x] 9.3 `make format` / `make lint` / `make check` — все `BUILD SUCCESSFUL` (ktlintFormat + detekt autoCorrect, ktlintCheck + app:detekt + markdownlint, assembleDebug + test).

## Этап 10: Ручная приёмка (на устройстве/эмуляторе) — [ ]

- [x] 10.1 Эмулятор (Pixel, API 33+): создать событие, настроить reminder через 1 минуту, свернуть. Дождаться уведомления — должно прийти в пределах ±10 секунд.
- [ ] 10.2 Эмулятор (Pixel, API 31): то же самое — без диалога запроса permission (он granted по умолчанию на эмуляторе), но уведомление должно прийти вовремя.
- [ ] 10.3 Реальное устройство: проверить что:
  - При первом создании reminder после установки — диалог «Allow exact alarms» НЕ появляется автоматически (система сама решает, когда показать; если у пользователя отключено в батарейных настройках — система сама покажет позже).
  - Баннер «Включить точные напоминания» отображается на экране создания reminder, **если reminder включён** (`isReminderEnabled = true` в `CreateEditReminderState`; см. §7.4).
  - При нажатии кнопки «Включить» открывается системный экран `SCHEDULE_EXACT_ALARM`.
  - После включения permission и возврата в приложение баннер пропадает.
  - **Антирегрессия:** открыть настройки `SCHEDULE_EXACT_ALARM` через системную шторку (не через кнопку баннера), включить permission, вернуться в приложение → баннер должен пропасть (без явного нажатия кнопки). Это подтверждает работу refresh на `ON_RESUME` через `DisposableEffect` + `LifecycleEventObserver` (см. отступление 7.4).
- [ ] 10.4 Проверить `adb shell dumpsys alarm | grep -A 2 com.dayscounter` — в выводе должно быть `Exact` вместо `Allow-While-Idle` для наших алармов.
- [ ] 10.5 Антирегрессия: на устройстве с отключённым exact alarm permission — уведомление всё равно приходит (с задержкой через `setAndAllowWhileIdle`), баннер информирует пользователя.

## Что выходит за рамки этого плана

- `USE_EXACT_ALARM` permission — Play Console фильтрует, оставляем на потом если понадобится.
- Manufacturer battery savers (Xiaomi/Huawei/Samsung) — это баг ОЕМ, не наш. Документируем в README, но не фиксим кодом.
- App Standby Buckets — лечится открытием приложения, что и так происходит при настройке reminder.
- One-time vs recurring reminders — текущая модель `Reminder` поддерживает только одноразовые (нет поля repeat). Это за рамками.