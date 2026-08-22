## ADDED Requirements

### Requirement: Гейт обработки reminder-intent в MainActivity.onCreate пропускает обработку при recreation Activity

`MainActivity.onCreate` MUST вызывать `handleReminderIntent(intent, reminderManager)` только когда `shouldHandleReminderIntent(savedInstanceState)` возвращает `true`. Pure-функция гейта `MainActivity.shouldHandleReminderIntent(savedInstanceState: Bundle?): Boolean` MUST возвращать `true` тогда и только тогда, когда `savedInstanceState == null` — это покрывает cold-start через launcher и cold-start через пуш-интент (новый процесс). Гейт MUST быть объявлен в `companion object`, помечен `@VisibleForTesting internal` и доступен для прямого вызова из JVM unit-тестов. При recreation Activity (rotation, theme change, configuration change, возврат из фона после смерти процесса) Android передаёт `savedInstanceState != null`, и гейт MUST вернуть `false` — обработка reminder-intent не выполняется повторно, `pendingOpenDetailItemId` не перезаписывается прежним значением, `LaunchedEffect` не пушит `ItemDetail` поверх текущего экрана навигации. `onNewIntent` (пуш при живом процессе) остаётся без изменений и обрабатывает reminder-intent независимо от гейта — Android вызывает его при новом пуш-интенте поверх существующей Activity.

#### Scenario: Cold-start через launcher обрабатывает reminder-intent

- **WHEN** пользователь запускает приложение через launcher (без пуш-интента), Android создаёт `MainActivity` с `savedInstanceState == null`
- **THEN** `shouldHandleReminderIntent(null)` возвращает `true`; если `intent` содержит `EXTRA_ITEM_ID` (например, при cold-start через пуш на убитое приложение), `handleReminderIntent` вызывается и `pendingOpenDetailItemId` устанавливается в `itemId`

#### Scenario: Recreation через rotation пропускает обработку reminder-intent

- **WHEN** Activity уже создана, пользователь поворачивает устройство, Android пересоздаёт `MainActivity` с `savedInstanceState != null` (Android сохраняет Bundle при configuration change)
- **THEN** `shouldHandleReminderIntent(savedInstanceState)` возвращает `false`; `handleReminderIntent` НЕ вызывается; `pendingOpenDetailItemId` остаётся в текущем значении (или `null`, если пуш ещё не приходил); `LaunchedEffect(pendingOpenDetailItemId)` в `RootScreen` НЕ пушит `ItemDetail` поверх текущего стека навигации

#### Scenario: Recreation через смену темы пропускает обработку reminder-intent

- **WHEN** пользователь меняет тему приложения (Light → Dark или наоборот) в `ThemeIconScreen`, Android пересоздаёт `MainActivity` с `savedInstanceState != null`
- **THEN** `shouldHandleReminderIntent(savedInstanceState)` возвращает `false`; `handleReminderIntent` НЕ вызывается; пользователь остаётся на `ThemeIconScreen`, а не улетает на `DetailScreen` ранее открытого события

#### Scenario: Пуш при живом процессе обрабатывается через onNewIntent независимо от гейта

- **WHEN** приложение запущено, приходит новый пуш → Android вызывает `onNewIntent(intent)` без `onCreate`
- **THEN** `handleReminderIntent` вызывается безусловно (без проверки гейта); `setIntent(intent)` обновляет текущий intent Activity; `pendingOpenDetailItemId` устанавливается в `itemId`; `consumeReminder` и `NotificationManagerCompat.cancel` срабатывают один раз на доставку пуша

### Requirement: Getter openDetailItemId для интеграционных тестов отдаёт текущее значение pendingOpenDetailItemId

`MainActivity` MUST предоставлять `@get:VisibleForTesting internal val openDetailItemId: Long?` который возвращает текущее значение `pendingOpenDetailItemId.value`. Getter MUST быть read-only (без setter'а) и MUST NOT изменять поведение Activity в release-сборке. Getter используется androidTest'ом для антирегрессионной проверки, что после `scenario.recreate()` без push-интента `openDetailItemId` остаётся `null`.

#### Scenario: Getter возвращает null на свежем старте без push-интента

- **WHEN** `MainActivity` запускается без push-интента (cold-start через launcher)
- **THEN** `activity.openDetailItemId` возвращает `null`

#### Scenario: Getter возвращает null после recreate без push-интента

- **WHEN** `MainActivity` запущена без push-интента, выполняется `scenario.recreate()` (программный эквивалент rotation)
- **THEN** после `moveToState(Lifecycle.State.RESUMED)` `activity.openDetailItemId` возвращает `null` — антирегрессия: гейт не дёргает `handleReminderIntent` при recreation

### Requirement: JVM unit-тест MainActivityReminderGatingTest покрывает гейт без Android-зависимостей

`MainActivityReminderGatingTest` (`app/src/test/java/com/dayscounter/reminder/`) MUST содержать минимум два сценария (Given/When/Then структура, JUnit 5, MockK не требуется): `cold_start_triggers_handle` (проверяет `shouldHandleReminderIntent(null) == true`) и `recreate_skips_handle` (проверяет `shouldHandleReminderIntent(Bundle()) == false`). Тесты MUST запускаться как pure-JVM unit-тесты — без `@RunWith(AndroidJUnit4::class)`, без Robolectric, без Compose Testing, без эмулятора. Каждый сценарий MUST содержать assert с информативным сообщением на русском языке (logs/user-facing convention проекта).

#### Scenario: Тест cold_start_triggers_handle проходит на JVM

- **WHEN** запускается `MainActivityReminderGatingTest.cold_start_triggers_handle` через `./gradlew test`
- **THEN** `assertTrue(shouldHandleReminderIntent(null))` проходит без ошибок; тест выполняется за <100ms как pure-JVM unit-тест

#### Scenario: Тест recreate_skips_handle проходит на JVM

- **WHEN** запускается `MainActivityReminderGatingTest.recreate_skips_handle` через `./gradlew test`
- **THEN** `assertFalse(shouldHandleReminderIntent(Bundle()))` проходит без ошибок; тест проверяет, что recreation (rotation, theme change) не обрабатывает reminder-intent повторно

### Requirement: AndroidTest MainActivityDeepLinkRotationUiTest проверяет антирегрессию через реальный ActivityScenario.recreate()

`MainActivityDeepLinkRotationUiTest` (`app/src/androidTest/java/com/dayscounter/navigation/`) MUST содержать сценарий `given_regular_launch_when_activity_recreated_then_open_detail_item_id_remains_null`: запускает `MainActivity` через `ActivityScenario.launch(MainActivity::class.java)`, проверяет что `activity.openDetailItemId == null` на свежем старте, вызывает `scenario.recreate()` (реальный recreate без push-интента), и проверяет что `activity.openDetailItemId` остаётся `null` после `moveToState(Lifecycle.State.RESUMED)`. Тест MUST использовать `@RunWith(AndroidJUnit4::class)` (JUnit 4 — стандарт для androidTest в проекте). Тест НЕ воспроизводит исходный баг напрямую (для воспроизведения нужен `setIntent` + recreate, что ломает `ActivityScenario.close()` / `recreate()` в instrumentation) — это антирегрессия на правильное начальное состояние и поведение `openDetailItemId` после recreation.

#### Scenario: Тест антирегрессии проходит на эмуляторе

- **WHEN** запускается `MainActivityDeepLinkRotationUiTest.given_regular_launch_when_activity_recreated_then_open_detail_item_id_remains_null` через `make android-test ANDROID_TEST_FILTER=MainActivityDeepLinkRotationUiTest`
- **THEN** на свежем старте `openDetailItemId == null`; после `scenario.recreate()` и `moveToState(Lifecycle.State.RESUMED)` `openDetailItemId` остаётся `null`; тест завершается без `IllegalStateException` и без зависания `ActivityScenario.close()`
