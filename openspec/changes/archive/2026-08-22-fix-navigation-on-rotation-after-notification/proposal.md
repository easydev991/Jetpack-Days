## Why

`MainActivity.onCreate` безусловно вызывает `handleReminderIntent`, и Android при recreation Activity (rotation, смена темы, восстановление процесса) подсовывает сохранённый `Intent` пуша с `EXTRA_ITEM_ID`. В результате `pendingOpenDetailItemId` перезаписывается, и `LaunchedEffect` в `RootScreen` повторно пушит `ItemDetail` в стек навигации поверх текущего экрана (например, `ThemeIconScreen` на вкладке **More**). Баг воспроизводится стабильно: после поворота из `ThemeIconScreen` пользователь оказывается на `DetailScreen` уже открытого ранее события. Нужно обрабатывать reminder-intent ровно один раз на доставку пуша и не реагировать на него при recreation.

## What Changes

- В `MainActivity.onCreate` вызов `handleReminderIntent` обёрнут в гейт `shouldHandleReminderIntent(savedInstanceState)`, который возвращает `true` только при cold-start (`savedInstanceState == null`).
- В `MainActivity` добавлен `companion object` с `internal fun shouldHandleReminderIntent(savedInstanceState: Bundle?): Boolean` (помечен `@VisibleForTesting`) и `@get:VisibleForTesting internal val openDetailItemId` для интеграционных тестов.
- Добавлен JVM unit-тест `MainActivityReminderGatingTest` (2 сценария: cold-start, recreate) без Android-зависимостей — pure-JVM gate.
- Добавлен androidTest `MainActivityDeepLinkRotationUiTest` — антирегрессия: после `scenario.recreate()` без push `openDetailItemId` остаётся `null`.
- В `Makefile` добавлена переменная `ANDROID_TEST_FILTER` для быстрой итерации над androidTest.
- Скилл `.opencode/skills/kotlin-ui-testing` обновлён: исправлен сниппет `ActivityScenario.launch` → `ActivityScenarioRule`, добавлен раздел «Pure-JVM gate для Activity-логики» + Caveat про `setIntent` + recreation.

## Capabilities

### New Capabilities
- `main-activity-reminder-gating`: гейт обработки reminder-intent в `MainActivity.onCreate`, который срабатывает только при cold-start и пропускается при recreation (rotation / theme change / возврат из фона).

### Modified Capabilities
- _none_

## Impact

**Production code:**
- `app/src/main/java/com/dayscounter/MainActivity.kt` — добавлен gate + companion + getter.

**Tests:**
- `app/src/test/java/com/dayscounter/reminder/MainActivityReminderGatingTest.kt` — новый JVM unit-тест (2 сценария).
- `app/src/androidTest/java/com/dayscounter/navigation/MainActivityDeepLinkRotationUiTest.kt` — новый androidTest (1 сценарий антирегрессии).

**Build / tooling:**
- `Makefile` — добавлен `ANDROID_TEST_FILTER`.

**Docs / skills:**
- `.opencode/skills/kotlin-ui-testing/SKILL.md` — обновлены сниппеты.
- `.opencode/skills/kotlin-ui-testing/references/activity-integration.md` — добавлен раздел «Pure-JVM gate для Activity-логики» + Caveat.

**Не затрагивается (по результатам анализа):**
- `RootScreen.kt` — `LaunchedEffect(pendingOpenDetailItemId)` корректен, фикс только в условии срабатывания.
- `Screen.kt` — маршруты непричастны.
- `ReminderAlarmReceiver.kt`, `ReminderIntentParser.kt` — без изменений.
- `onNewIntent` — пуш при живом процессе уже обрабатывается отдельным методом.
