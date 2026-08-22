# Exact Alarm Permission

## Purpose

Определить политику доступа к `SCHEDULE_EXACT_ALARM` permission: доменная модель состояния, helper для проверки и запроса permission, ViewModel с авто-обновлением состояния при возврате на экран, UI-баннер в форме создания/редактирования reminder'а, корректное логирование `SecurityException` в `AlarmReminderScheduler` с сохранением fallback на `setAndAllowWhileIdle`.

## Requirements

### Requirement: SCHEDULE_EXACT_ALARM объявлен в AndroidManifest

`AndroidManifest.xml` MUST содержать `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` после `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`. Без этого permission на API 31+ `setExactAndAllowWhileIdle` бросает `SecurityException` и приложение остаётся на неточных alarm'ах.

#### Scenario: Manifest содержит SCHEDULE_EXACT_ALARM после POST_NOTIFICATIONS

- **WHEN** разработчик открывает `app/src/main/AndroidManifest.xml`
- **THEN** в блоке `<uses-permission>` присутствует `android.permission.SCHEDULE_EXACT_ALARM` (объявлен после `POST_NOTIFICATIONS`).

### Requirement: ExactAlarmPermissionState sealed-модель с KDoc

`ExactAlarmPermissionState` MUST быть sealed-классом в `app/src/main/java/com/dayscounter/domain/model/` с двумя case'ами: `Allowed` (data object) и `Denied(val canRequest: Boolean)`. Параметр `canRequest` отличает состояние «permission отозван пользователем, можно запросить системный экран» от «permission технически недоступен на этом устройстве (custom firmware / MIUI/HyperOS)».

#### Scenario: API ≤ 30 → Allowed

- **WHEN** `Build.VERSION.SDK_INT < Build.VERSION_CODES.S`
- **THEN** `ExactAlarmPermissionHelper.getState()` возвращает `Allowed` без обращения к `AlarmManager` (на pre-31 system всегда grants).

#### Scenario: API ≥ 31 и AlarmManager.canScheduleExactAlarms() == true → Allowed

- **WHEN** `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` и `AlarmManager.canScheduleExactAlarms() == true`
- **THEN** `getState()` возвращает `Allowed`.

#### Scenario: API ≥ 31 и canScheduleExactAlarms() == false → Denied(canRequest=true)

- **WHEN** `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` и `AlarmManager.canScheduleExactAlarms() == false`
- **THEN** `getState()` возвращает `Denied(canRequest=true)` — можно показать кнопку «Включить», которая откроет системный экран `SCHEDULE_EXACT_ALARM`.

### Requirement: ExactAlarmPermissionHelper интерфейс + AndroidExactAlarmPermissionHelper реализация

`ExactAlarmPermissionHelper` MUST быть interface'ом в `app/src/main/java/com/dayscounter/reminder/` с методами `getState(): ExactAlarmPermissionState` и `requestSettings()` (бросает `ActivityNotFoundException` на кастомных прошивках, где системный экран `SCHEDULE_EXACT_ALARM` отсутствует). `AndroidExactAlarmPermissionHelper` MUST быть production-реализацией, использовать `AlarmManager` через `context.getSystemService(AlarmManager::class.java)`, и на pre-31 всегда возвращать `Allowed`.

#### Scenario: getState() на API ≤ 30 возвращает Allowed без обращения к AlarmManager

- **WHEN** SDK < 31
- **THEN** `getState()` возвращает `Allowed` напрямую (без вызова `AlarmManager.canScheduleExactAlarms()`).

#### Scenario: requestSettings() бросает ActivityNotFoundException на кастомных прошивках

- **WHEN** `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` не зарегистрирован ни одним `Activity` на устройстве (MIUI/HyperOS без system screen)
- **THEN** `requestSettings()` бросает `ActivityNotFoundException`. ViewModel перехватывает, логирует через `logger.w`, и обновляет state через `refresh()`.

### Requirement: ExactAlarmPermissionViewModel с авто-обновлением состояния

`ExactAlarmPermissionViewModel` MUST предоставлять публичный API: `state: StateFlow<ExactAlarmPermissionState>`, `refresh()` (читает текущее состояние из helper), `onRequestPermission()` (try/catch вокруг `helper.requestSettings()`, логирует throwable через `logger.w`, вызывает `refresh()` в finally-блоке). VM MUST иметь `companion object { fun factory(helper): ViewModelProvider.Factory }` (конвенция проекта). Logger MUST быть instance-полем `private val logger: Logger = AndroidLogger()`.

#### Scenario: Initial state — VM читает state из helper при создании

- **WHEN** `ExactAlarmPermissionViewModel.factory(helper).invoke(...)` создаёт VM
- **THEN** `state.value == helper.getState()` (без `IllegalStateException`).

#### Scenario: refresh() обновляет state из helper

- **WHEN** helper.getState() возвращает `Denied(true)`, вызывается `refresh()` после того, как пользователь включил permission в системе
- **THEN** `state.value` становится `Allowed` после завершения `refresh()`.

#### Scenario: onRequestPermission() логирует throwable и обновляет state в любом случае

- **WHEN** `helper.requestSettings()` бросает `ActivityNotFoundException`
- **THEN** `logger.w(TAG, message, throwable)` вызывается один раз с `throwable` параметром, и `state.value` всё равно обновляется через `refresh()` (не зависит от успеха/неудачи `requestSettings`).

### Requirement: ExactAlarmPermissionInlineSection интегрирован в CreateEditFormContent

`ExactAlarmPermissionInlineSection` MUST быть composable-функцией в `app/src/main/java/com/dayscounter/ui/screens/createedit/ExactAlarmPermissionBanner.kt`. MUST создавать VM через `viewModel(factory = AppModule.createExactAlarmPermissionViewModelFactory(LocalContext.current))`. MUST использовать `DisposableEffect(lifecycleOwner)` с `LifecycleEventObserver` на `Lifecycle.Event.ON_RESUME` для вызова `viewModel.refresh()`. MUST рендерить `DeniedCard` для `is Denied` (с разным текстом в зависимости от `canRequest`), и `Unit` для `is Allowed`. `DeniedCard` MUST быть `internal` composable с параметрами `canRequest: Boolean, onRequestPermission: () -> Unit, modifier: Modifier = Modifier`.

Секция MUST быть подключена в `CreateEditFormContent.kt` после `ReminderSettingsSection`, гейтом `if (params.uiStates.reminder.isEnabled)` — баннер показывается только когда reminder включён.

#### Scenario: isReminderEnabled=true и state=Denied(canRequest=true) → баннер с кнопкой «Включить»

- **WHEN** пользователь на экране создания/редактирования reminder'а, `isReminderEnabled=true`, permission отозван в `Settings.action`
- **THEN** `DeniedCard(canRequest=true, ...)` рендерится с текстом `exact_alarm_request_title` + `exact_alarm_request_text` и кнопкой `exact_alarm_request_button`.

#### Scenario: isReminderEnabled=true и state=Denied(canRequest=false) → баннер без кнопки

- **WHEN** `state=Denied(canRequest=false)` (кастомная прошивка без system screen)
- **THEN** `DeniedCard(canRequest=false, ...)` рендерится с текстом `exact_alarm_unavailable_text` без кнопки.

#### Scenario: isReminderEnabled=true и state=Allowed → баннер не рендерится

- **WHEN** `state=Allowed`
- **THEN** `ExactAlarmPermissionInlineSection` рендерит `Unit` (не показывает ничего).

#### Scenario: isReminderEnabled=false → баннер не рендерится

- **WHEN** `params.uiStates.reminder.isEnabled == false` (пользователь ещё не включил reminder)
- **THEN** гейт `if (params.uiStates.reminder.isEnabled)` в `CreateEditFormContent.kt` не вызывает `ExactAlarmPermissionInlineSection()` — баннер не появляется, даже если permission отозван.

#### Scenario: возврат из системных настроек SCHEDULE_EXACT_ALARM → баннер пропадает через ON_RESUME refresh

- **WHEN** пользователь включил permission в системном экране `SCHEDULE_EXACT_ALARM` (через кнопку баннера ИЛИ через системную шторку) и вернулся в приложение
- **THEN** `LifecycleEventObserver` вызывает `viewModel.refresh()` при `ON_RESUME`; `state` становится `Allowed`; баннер исчезает без явного пользовательского действия.

### Requirement: AlarmReminderScheduler логирует SecurityException и фоллбэчит на setAndAllowWhileIdle

`AlarmReminderScheduler.schedule(...)` MUST оборачивать вызов `setExactAndAllowWhileIdle` в try/catch по `SecurityException`. При перехвате MUST вызвать `logger.w(TAG, message, securityException)` с информативным сообщением на русском (включающим упоминание `SCHEDULE_EXACT_ALARM` и `setAndAllowWhileIdle`) и MUST фоллбэчить на `setAndAllowWhileIdle` — пользователь всё равно получает уведомление (с задержкой через Doze), но без краша.

#### Scenario: SecurityException при отсутствии permission → fallback на setAndAllowWhileIdle + logger.w

- **WHEN** `setExactAndAllowWhileIdle` бросает `SecurityException` (на API 31+ без SCHEDULE_EXACT_ALARM permission)
- **THEN** `logger.w` вызывается один раз с сообщением, содержащим «SCHEDULE_EXACT_ALARM» и «setAndAllowWhileIdle», и `throwable: SecurityException` параметром; `setAndAllowWhileIdle` вызывается как fallback.

### Requirement: DI фабрики в AppModule

`AppModule` MUST предоставлять `fun createExactAlarmPermissionHelper(context: Context): ExactAlarmPermissionHelper` (возвращает `AndroidExactAlarmPermissionHelper(context.applicationContext)`) и `fun createExactAlarmPermissionViewModelFactory(context: Context): ViewModelProvider.Factory` (делегирует `ExactAlarmPermissionViewModel.factory(createExactAlarmPermissionHelper(context))`). Manual DI, без Hilt.

#### Scenario: createExactAlarmPermissionHelper возвращает helper на основе applicationContext

- **WHEN** `createExactAlarmPermissionHelper(context)` вызывается с любым `Context`
- **THEN** возвращается `AndroidExactAlarmPermissionHelper(context.applicationContext)` — используется applicationContext во избежание утечки Activity.

#### Scenario: createExactAlarmPermissionViewModelFactory делегирует ExactAlarmPermissionViewModel.factory

- **WHEN** `createExactAlarmPermissionViewModelFactory(context)` вызывается
- **THEN** возвращается `ExactAlarmPermissionViewModel.factory(createExactAlarmPermissionHelper(context))` — стандартный `ViewModelProvider.Factory`, пригодный для `viewModel(factory = ...)`.

### Requirement: Локализация на en/ru

MUST быть добавлены 4 строки в `app/src/main/res/values/strings.xml` И `app/src/main/res/values-ru/strings.xml`:

| Ключ | English | Русский |
|---|---|---|
| `exact_alarm_request_title` | Exact reminders are off | Точные напоминания отключены |
| `exact_alarm_request_text` | Without this permission, notifications may arrive late | Без разрешения уведомления могут приходить с задержкой |
| `exact_alarm_request_button` | Enable | Включить |
| `exact_alarm_unavailable_text` | Exact reminders are not available on this device | Точные напоминания недоступны на этом устройстве |

#### Scenario: 4 строки присутствуют в обоих locales

- **WHEN** разработчик открывает `values/strings.xml` и `values-ru/strings.xml`
- **THEN** оба файла содержат все 4 ключа с указанными значениями.

### Requirement: JVM unit-тесты покрывают гейт и поведение VM

`app/src/test/java/com/dayscounter/ui/viewmodel/ExactAlarmPermissionViewModelTest.kt` MUST содержать минимум 5 `@Test` методов (JUnit 5, без `@RunWith(AndroidJUnit4::class)`, без Robolectric, без Compose Testing): `initial_state_when_helper_allowed_then_state_allowed`, `initial_state_when_helper_denied_then_state_denied`, `refresh_when_called_then_state_updated_from_helper`, `onRequestPermission_when_helper_throws_then_logged_with_throwable`, `onRequestPermission_when_helper_succeeds_then_state_refreshed`. Fake helper MUST использовать `var currentState: ExactAlarmPermissionState` + `AtomicInteger` для трекинга вызовов `requestSettings()`.

`app/src/test/java/com/dayscounter/reminder/AlarmReminderSchedulerTest.kt` MUST содержать тест `schedule_when_security_exception_then_fallback_invoked_and_logged` (MockK для `AlarmManager`), проверяющий что `setAndAllowWhileIdle` вызывается один раз и `logger.w` вызывается один раз с правильным сообщением и `SecurityException` параметром.

#### Scenario: 5 @Test методов проходят как pure-JVM unit-тесты

- **WHEN** запускается `./gradlew test --tests "*ExactAlarmPermissionViewModelTest"`
- **THEN** все 5 тестов проходят за <100ms каждый без Android-зависимостей.

#### Scenario: AlarmReminderSchedulerTest логирует SecurityException

- **WHEN** запускается `AlarmReminderSchedulerTest.schedule_when_security_exception_then_fallback_invoked_and_logged`
- **THEN** тест проходит: `setAndAllowWhileIdle` вызван, `logger.w` вызван с правильным сообщением.