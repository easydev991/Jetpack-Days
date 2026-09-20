---
name: kotlin-ui-testing
description: >
  Экспертное руководство по инструментированным (androidTest) тестам
  в Android-проекте JetpackDays: JUnit 4, Compose Testing
  (createComposeRule / createAndroidComposeRule), Room in-memory,
  Turbine для ViewModel, тесты AlarmManager/Receiver, TestViewModel
  и фейковые репозитории, запуск через make android-test.
  Использовать при написании новых UI- и интеграционных тестов,
  отладке flaky androidTest и улучшении покрытия.
---

# Kotlin UI Testing — kotlin-ui-testing skill

## Overview

Этот skill — для инструментированных тестов JetpackDays из
`app/src/androidTest/`. Стек: **JUnit 4** (`org.junit.*`),
**Compose Testing** (`createComposeRule`, `createAndroidComposeRule`),
**Room in-memory**, **Turbine**, **kotlinx-coroutines-test**
(`runTest`, `MainDispatcherRule`).

Два типа тестов в `androidTest/`:

- **Компонентные UI-тесты** — `createComposeRule()` (v2 API,
  `androidx.compose.ui.test.junit4.v2`), изолированный
  Compose-компонент, зависимости подменяются фейками
  (см. `references/component-testing.md`).
- **Интеграционные тесты** — `createAndroidComposeRule<MainActivity>()`
  (полный стек с реальной Activity), либо без Activity: Room DAO /
  репозиторий / ViewModel / Alarm (см. `references/activity-integration.md`,
  `references/room-repository.md`, `references/viewmodel-integration.md`).
  Если Activity должна стартовать с кастомным `Intent` (push, deep link) —
  `AndroidComposeTestRule(activityRule = ActivityScenarioRule(intent), ...)`
  в том же `references/activity-integration.md` (раздел «Кастомный launch Intent»).
  **Для тестируемой Activity-логики предпочитайте pure-JVM gate через
  `companion object` + `@VisibleForTesting`** (см. тот же reference,
  раздел «Pure-JVM gate для Activity-логики») — никакого Compose, никакого
  эмулятора.

> Unit-тесты — это `app/src/test/` (JUnit 5, MockK). Их правила описаны
> в навыке `kotlin-testing`. Этот skill про `app/src/androidTest/`.

## Agent behavior contract (следуй этим правилам)

1. **JUnit 4 — фреймворк androidTest.** Импорты из `org.junit.*`:
   `@Test`, `@Before`, `@After`. JUnit 5
   (`org.junit.jupiter.api.*`) — только в unit-тестах `app/src/test/`.
   `@RunWith(AndroidJUnit4::class)` — в интеграционных тестах
   (Activity, data/, reminder/) и в большинстве компонентных
   Compose-тестов. Без него — DaysCountTextTest,
   ColorSelectorUiTest, ColorTagFilterDialogTest (с v2 API
   не требуется).
2. **ComposeTestRule — v2 API (`androidx.compose.ui.test.junit4.v2`).**
   Компонентный тест — `createComposeRule()`; тест с реальной
   MainActivity — `createAndroidComposeRule<MainActivity>()`. Правило
   объявляется как `@get:Rule val composeTestRule = ...`.
   Для старта Activity с кастомным `Intent` (push, deep link) —
   следуй разделу «Кастомный launch Intent» в
   `references/activity-integration.md`: там канонический рецепт и все
   подводные камни v2 API.
   Если логика выносима в pure-функцию — используйте **pure-JVM gate**
   (companion-объект + `@VisibleForTesting`), см. тот же reference.
3. **snake_case для имён тестов, без обратных кавычек.** Форматы:
   `subject_whenCondition_thenResult`, `when_condition_then_result`,
   составной `subject_verb_suffix[_whenCondition]`. Примеры —
   `references/fundamentals.md` (раздел «Именование тестов»).
4. **Given / When / Then в каждом тесте.** Разделяй тело тремя
   комментариями-маркерами.
5. **Никакого `!!`.** Даже в тестах. Используй `?.`, `?:`, `let`,
   `checkNotNull`, `assertNotNull(...)` перед доступом к полям.
6. **Сообщения `assert` — на русском**, с контекстом и фактическими
   значениями. Образец — `references/fundamentals.md` (раздел «Правила»).
7. **Строки UI — через ресурсы, не хардкод.** `context.getString(R.string.save)`
   вместо «Сохранить». `context` — из `InstrumentationRegistry.getInstrumentation().targetContext`
   или `ApplicationProvider.getApplicationContext()`.
8. **Компонентный тест изолирует зависимости.** `setContent`
   оборачивает компонент в `JetpackDaysTheme` (в большинстве тестов;
   DaysCountTextTest — без темы, если компонент не зависит от темы).
   ViewModel создаётся factory-функцией с фейковыми зависимостями
   (см. `references/component-testing.md` — `TestViewModel.kt`).
9. **Интеграционный тест с данными — реальный Room in-memory.**
   `Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()`,
   `@After` закрывает базу (`database.close()`) — при per-test базе
   cleanup данных не нужен; `clearAllTables()` обязателен только для
   process-wide базы через синглтон `getDatabase()` (activity-тесты).
   Вставка данных — через `runBlocking`.
10. **`waitForIdle()` после вставки данных.** Любой `runBlocking { ... }`
    с изменением БД перед assert обязан сопровождаться
    `composeTestRule.waitForIdle()`.
11. **Никакого `Thread.sleep`.** Для ожидания условий —
    `waitUntil(timeoutMillis = ...) { ... }` или `waitForIdle()`.
    `Thread.sleep` в существующих тестах не используется.
12. **Turbine — только для ViewModel-интеграции** в `androidTest/`
    (`viewModel.uiState.test { awaitItem() }` внутри `runTest`).
    В unit-тестах Turbine запрещён.
13. **Асинхронность ViewModel — через `MainDispatcherRule`.**
    `@get:Rule val mainDispatcherRule = MainDispatcherRule()` — заменяет
    `Dispatchers.Main` на `StandardTestDispatcher`. Без него
    `viewModelScope.launch` не контролируется тестом.
14. **Один `setContent` на тест (sealed v2 API).** Повторный
    `composeTestRule.setContent` в том же тесте бросает
    `IllegalStateException: ... has already set content`. «Второй экран»
    в рамках теста — подмена состояния через `mutableStateOf` внутри
    первого `setContent` (по образцу `ThemeIconScreenTest`: свой
    хелпер `setContent(...)` с параметрами вместо повторных вызовов).

## Запуск тестов

- **Все androidTest**: `make android-test` — запуск
  `./gradlew connected<Flavor>DebugAndroidTest` (для дефолта `FLAVOR=github` —
  `connectedGithubDebugAndroidTest`) + человекочитаемый отчёт через
  `scripts/android_test_report.py`.
- **Отчёт в браузере**: `make android-test-report` — открывает index.html
  из `app/build/reports/androidTests/connected/`. Путь зависит от
  варианта (AGP 9 с flavors): `connected/<buildType>/flavors/<flavor>/` —
  для дефолтного `make android-test` (`FLAVOR=github`) это
  `connected/debug/flavors/github/index.html`.
- **Все тесты (unit + интеграционные)**: `make test-all`.
- Отчёт `android_test_report.py`: exit code 0 при успехе, 1 при упавших
  тестах или ошибке Gradle. Подробности — `references/running-tests.md`.

## First 60 seconds (triage template)

Прежде чем писать код, собери факты:

- **Цель**: новые UI-тесты, миграция, flaky failures, покрытие
  экрана / компонента / слоя данных?
- **Факты**:
  - Что тестируем: отдельный Compose-компонент (компонентный тест,
    `createComposeRule`) или целый экран в MainActivity
    (интеграционный, `createAndroidComposeRule<MainActivity>`) или слой
    данных (DAO / Repository / ViewModel / Alarm)?
  - Нужны ли реальные данные? Если да — Room in-memory +
    `runBlocking`-вставки. Если нет — фейковый `ItemRepository`
    (`object : ItemRepository`) из `TestViewModel.kt`.
  - Есть ли ViewModel? Тогда `MainDispatcherRule` (+ Turbine для
    интеграции).
  - Есть ли системные сервисы (AlarmManager, NotificationManager)?
    Тогда `InstrumentationRegistry` + shell-пермишены для API 33+.
  - Нужен ли реальный стейт Activity (ротация, интенты)? Тогда
    `createAndroidComposeRule<MainActivity>()`.

## Routing map (читай нужный reference быстро)

- Анатомия теста, именование, Given/When/Then, JUnit 4 →
  `references/fundamentals.md`
- Compose: селекторы, действия, assertions, `waitForIdle` /
  `waitUntil` / `runOnIdle`, bounds → `references/compose-selectors.md`
- Компонентные UI-тесты: `createComposeRule`, `JetpackDaysTheme`,
  `TestViewModel` → `references/component-testing.md`
- Интеграция с MainActivity: in-memory Room, ротация, очистка БД →
  `references/activity-integration.md`
- Room DAO / Repository: in-memory, `runBlocking`, `.first()`, CRUD,
  маппинг → `references/room-repository.md`
- ViewModel-интеграция: `MainDispatcherRule`, Turbine, `SavedStateHandle` →
  `references/viewmodel-integration.md`
- AlarmManager / Receiver: `PendingIntent`, пермишены API 33+ →
  `references/alarm-reminder.md`
- Запуск `make android-test`, отчёт, screenshot-tests →
  `references/running-tests.md`
- Полные канонические примеры → `references/EXAMPLE.md`

## Common pitfalls → next best move

| Проблема | Решение |
|---|---|
| Тест с MainActivity падает «no compose hierarchy found» | Используй `createAndroidComposeRule<MainActivity>()`, а не `createComposeRule()`, если тестируешь реальный экран |
| `onNodeWithText` не находит строку | Используй `context.getString(R.string.xxx)` — строка может быть в ресурсах, а не хардкодом; проверь, что вызван `setContent` (тема `JetpackDaysTheme` — если компонент её использует) |
| Данные вставил, а UI их не видит | После `runBlocking { ... }` вызови `composeTestRule.waitForIdle()` |
| `onAllNodesWithContentDescription` без проверки количества | Добавь `.assertCountEquals(n)` — иначе тест проходит при 0 нод |
| `onAllNodesWithTag` / `onNodeWithText` находит 0 нод, хотя нода есть | Нода внутри контейнера с `mergeDescendants = true` (строка списка, кнопка, диалог) не видна в merged-дереве — ищи с `useUnmergedTree = true` |
| `performClick` на недоступной (disabled) ноде падает | У disabled-ноды нет click-action — паттерн «тап по недоступному → ничего не произошло» не работает. Проверяй `assertIsNotEnabled` или подсчёт disabled-нод |
| `Unresolved reference 'click'` в `performTouchInput` | `click`/`longClick`/`doubleClick` — extension-функции `TouchInjectionScope`, каждой нужен свой импорт: `import androidx.compose.ui.test.click` |
| `onNodeWithText("15")` не находит день в M3 `DatePicker` | В material3 1.4+ номер дня вычищен из семантики ячейки (доступен только локализованный description даты). Актуально для `DatePickerDialogSection` в CreateEdit — проверяй через `context.getString` description или тестировать колбэки, а не тексты ячеек |
| Нод в диалоге больше, чем ожидал `assertCountEquals` | `onAllNodes(...)` видит форму под диалогом (DatePickerDialog в CreateEdit) — сужай матчер или учитывай фон |
| `navController.navigate(...)` бросает `IllegalArgumentException: destination not found` | Компонентный тест без графа навигации: оберни экран в `NavHost`-харнесс с заглушкой destination (`NavHost(navController, startDestination = ...) { composable(...) { ТестируемыйЭкран(...) } }`) |
| `navController` нужен снаружи `setContent` | `lateinit var navController; composeTestRule.setContent { rememberNavController().also { navController = it } }` |
| Второй `setContent` бросает `IllegalStateException: has already set content` | Sealed v2 API — один `setContent` на тест; смену экрана/состояния делай через `mutableStateOf` внутри первого `setContent` (правило 14) |
| `assertIsDisplayed()` падает, хотя нода есть вне экрана | Проверяй позицию через `fetchSemanticsNode().boundsInRoot` или проскролль до ноды |
| ViewModel в тесте не реагирует на действия | Добавь `MainDispatcherRule` (`Dispatchers.setMain`), иначе `viewModelScope` использует реальный Main |
| Тест Room падает «cannot access database on the main thread» | `.allowMainThreadQueries()` при создании in-memory базы |
| Тест ViewModel зависает навсегда | Не используй `runBlocking` с `viewModelScope.launch`. Замени на `runTest { ... }` + Turbine `.test { awaitItem() }` (см. `references/viewmodel-integration.md`) |
| `assertTrue(x is Y)` без проверки sealed перед `as Y` | Сначала `assertTrue(state is DetailScreenState.Success)`, потом `as` — иначе ClassCastException |
| Alarm-тест не видит PendingIntent | Ищи через `PendingIntent.getBroadcast(...)` с `FLAG_NO_CREATE or FLAG_IMMUTABLE` (см. `references/alarm-reminder.md`) |
| Тест уведомлений падает на API 33+ | Оберни в `runWithNotificationPermission { ... }` — `adoptShellPermissionIdentity(POST_NOTIFICATIONS)` (см. `references/alarm-reminder.md`) |
| Ротация не работает в тесте | `composeTestRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE` + `waitForIdle()` (см. `references/activity-integration.md`). **Caveat:** `setRequestedOrientation` + `Activity.setIntent(intent)` ломают `ActivityScenario.close()` в `@After` — см. «Caveat: `setIntent` + recreation ломает cleanup» там же. |
| Нужно передать кастомный Intent на старте (push, deep link) | В v2 нет `createAndroidComposeRule(intent)`; используй `AndroidComposeTestRule(activityRule = ActivityScenarioRule(intent), activityProvider = ::activityFromRule)` — детали и подводные камни в `references/activity-integration.md` (раздел «Кастомный launch Intent»). |

## Verification checklist

- [ ] Тест на JUnit 4: импорты из `org.junit.*`;
      `@RunWith(AndroidJUnit4::class)` где требуется (см. правило 1)
- [ ] `@get:Rule val composeTestRule` — `createComposeRule()` (компонент)
      или `createAndroidComposeRule<MainActivity>()` (экран); для кастомного
      launch-intent — `AndroidComposeTestRule(activityRule = ActivityScenarioRule(intent), activityProvider = ::activityFromRule)`
- [ ] Имя метода в `snake_case` без обратных кавычек
- [ ] Комментарии `// Given`, `// When`, `// Then` в каждом тесте
- [ ] Нет `!!` — `?.`, `?:`, `let`, `checkNotNull`, `assertNotNull`
- [ ] Сообщения `assert` на русском
- [ ] Строки UI через `context.getString(R.string.xxx)`, не хардкод
- [ ] Компонентный тест — изоляция через `TestViewModel` / фейковые
      зависимости, обёртка в `JetpackDaysTheme` (по необходимости)
- [ ] Интеграционный тест — Room in-memory с `allowMainThreadQueries()`,
      `database.close()` в `@After` (cleanup данных — только для
      process-wide базы, см. правило 9), вставки через `runBlocking` +
      `waitForIdle()`
- [ ] ViewModel-тест — `MainDispatcherRule`; Turbine только в `androidTest/`
- [ ] Нет `Thread.sleep` — `waitUntil(timeoutMillis = ...)` / `waitForIdle()`
- [ ] Один `setContent` на тест; смена экрана — через `mutableStateOf`
      внутри первого `setContent`
- [ ] Поиск нод внутри merged-контейнеров (строки списка, диалоги,
      тест-теги) — с `useUnmergedTree = true`, где нужно
- [ ] Файлы в `app/src/androidTest/` зеркалят структуру `app/src/main/`
- [ ] Имя класса оканчивается на `Test` (или `UiTest` / `IntegrationTest` /
      `InstrumentedTest`)
- [ ] Запуск — `make android-test`, все тесты зелёные

## References

- `references/fundamentals.md`
- `references/compose-selectors.md`
- `references/component-testing.md`
- `references/activity-integration.md`
- `references/room-repository.md`
- `references/viewmodel-integration.md`
- `references/alarm-reminder.md`
- `references/running-tests.md`
- `references/EXAMPLE.md`
