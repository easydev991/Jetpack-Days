## 1. Гейт обработки reminder-intent в MainActivity

- [x] 1.1 Добавить `companion object` в `MainActivity.kt` с `@VisibleForTesting internal fun shouldHandleReminderIntent(savedInstanceState: Bundle?): Boolean = savedInstanceState == null` и KDoc с описанием контракта (cold-start vs recreation).
- [x] 1.2 Добавить `@get:VisibleForTesting internal val openDetailItemId: Long?` в `MainActivity`, делегирующий на `pendingOpenDetailItemId.value`, для интеграционных тестов.
- [x] 1.3 Обернуть вызов `handleReminderIntent(intent, reminderManager)` в `onCreate` в `if (shouldHandleReminderIntent(savedInstanceState))`.
- [x] 1.4 Не трогать `onNewIntent` — пуш при живом процессе уже обрабатывается отдельным методом без гейта.

## 2. JVM unit-тест гейта (pure-JVM, без Android)

- [x] 2.1 Создать `app/src/test/java/com/dayscounter/reminder/MainActivityReminderGatingTest.kt` с JUnit 5 (`@Test`, `org.junit.jupiter.api.Assertions`).
- [x] 2.2 Добавить сценарий `cold_start_triggers_handle`: `shouldHandleReminderIntent(null) == true`, информативный assert-сообщение на русском.
- [x] 2.3 Добавить сценарий `recreate_skips_handle`: `shouldHandleReminderIntent(Bundle()) == false`, информативный assert-сообщение на русском.
- [x] 2.4 KDoc на класс с описанием корневого бага и связи с androidTest антирегрессией.

## 3. AndroidTest антирегрессия

- [x] 3.1 Создать `app/src/androidTest/java/com/dayscounter/navigation/MainActivityDeepLinkRotationUiTest.kt` с `@RunWith(AndroidJUnit4::class)` и JUnit 4.
- [x] 3.2 Реализовать сценарий `given_regular_launch_when_activity_recreated_then_open_detail_item_id_remains_null`: `ActivityScenario.launch(MainActivity::class.java)` → assert `openDetailItemId == null` → `scenario.recreate()` → `moveToState(Lifecycle.State.RESUMED)` → assert `openDetailItemId == null`.
- [x] 3.3 KDoc на класс с пояснением, что тест НЕ воспроизводит исходный баг (это антирегрессия на корректное начальное состояние), а полное покрытие — JVM unit-тест гейта + ручная приёмка.

## 4. Build / tooling

- [x] 4.1 Добавить переменную `ANDROID_TEST_FILTER` в `Makefile` для запуска подмножества androidTest (например, `make android-test ANDROID_TEST_FILTER=MainActivityDeepLinkRotationUiTest`).

## 5. Документация и скиллы

- [x] 5.1 Обновить `.opencode/skills/kotlin-ui-testing/SKILL.md`: исправить сниппет `ActivityScenario.launch` → `ActivityScenarioRule`.
- [x] 5.2 Добавить раздел «Pure-JVM gate для Activity-логики» в `.opencode/skills/kotlin-ui-testing/references/activity-integration.md` с примером gate-функции в `companion object`.
- [x] 5.3 Добавить Caveat в `activity-integration.md` про `setIntent` + `recreate` ломающие `ActivityScenario.close()` (link на этот фикс как на источник).
- [x] 5.4 Удалить документ `docs/Plan_2026-08-22_Fix_Rotation_Navigation_Bug.md` после создания всех OpenSpec-артефактов (план был временным).

## 6. Проверки

- [x] 6.1 `make test` — 460/460 проходит, включая новые JVM unit-тесты гейта.
- [x] 6.2 `make android-test` — 104/104 проходит, включая `MainActivityDeepLinkRotationUiTest`.
- [x] 6.3 `make format` — ktlint + detekt + markdownlint зелёные.
- [x] 6.4 `make lint` — ktlintCheck + detekt зелёные.
- [x] 6.5 `make check` — все проверки зелёные.

## 7. Ручная приёмка (Этап 6 плана)

- [x] 7.1 Создать событие, настроить напоминание на ближайшую минуту, свернуть приложение.
- [x] 7.2 Дождаться пуша, тапнуть → открылся `DetailScreen`.
- [x] 7.3 Назад → вкладка **More** → «Оформление приложения».
- [x] 7.4 Поворот в горизонтальную ориентацию → остаёмся на «Оформление приложения» (не улетаем на `DetailScreen`).
- [x] 7.5 Поворот обратно в вертикаль → остаёмся на «Оформление приложения».
- [ ] 7.6 Антирегрессия: пуш в свёрнутое приложение → `DetailScreen` открывается как раньше.
- [ ] 7.7 Антирегрессия: пуш в убитое приложение (force-stop) → `DetailScreen` открывается как раньше.
