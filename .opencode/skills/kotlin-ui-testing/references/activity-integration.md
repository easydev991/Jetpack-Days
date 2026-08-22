# Activity integration — тесты с реальной MainActivity

Интеграционные UI-тесты запускают реальную `MainActivity` и полный стек:
Activity + Compose + реальный Room + ViewModel. Используются, когда
нужно проверить поведение экрана целиком: поиск, сортировка, ротация,
загрузка данных из БД.

Примеры: `ui/screens/events/MainScreenSearchVisibilityUiTest.kt`,
`ui/screens/events/MainScreenSortByTimeOfDayUiTest.kt`,
`ui/screens/createedit/CreateEditHasChangesRegressionUiTest.kt`.

## Скелет

```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenSomeBehaviorUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() {
        runBlocking { dao.deleteAllItems() }   // чистый стейт перед тестом
    }

    @After
    fun tearDown() {
        runBlocking { dao.deleteAllItems() }   // чистота после теста
    }

    @Test
    fun when_condition_then_result() {
        runBlocking { insertTestData() }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.search))
            .assertIsDisplayed()
    }

    private suspend fun insertTestData() {
        dao.insertItem(ItemEntity(...))
    }
}
```

Ключевое:
- `createAndroidComposeRule<MainActivity>()` — реальная Activity
- Вставка данных — `runBlocking { dao.insertItem(...) }`
- После вставки — `composeTestRule.waitForIdle()`, иначе UI не увидит данные
- Очистка БД в `@Before` и `@After` — изолированность тестов
- Обращение к Activity — `composeTestRule.activity`

## Кастомный launch Intent (push, deep link)

Когда Activity должна стартовать с нестандартным `Intent` (push-уведомление,
deep link, deeplink из `Intent.ACTION_VIEW`), недостаточно
`createAndroidComposeRule<MainActivity>()` — он стартует Activity с пустым
`Intent`. В v2 API **нет overload** `createAndroidComposeRule(intent)`
(см. `AndroidComposeTestRule.android.kt`: только `<A>` и `(activityClass)`).
Подмена `composeTestRule.activity.intent = pushIntent` тоже не сработает —
правило уже стартовало Activity, и при recreation Android читает
сохранённый `intent` из системы, а не текущее поле.

**Рабочий путь** — собрать `AndroidComposeTestRule` напрямую, передав
`ActivityScenarioRule(intent)` (НЕ `ActivityScenario.launch(intent)` —
последний не компилируется: `ActivityScenario` implements `AutoCloseable`,
**не** `TestRule`. `AndroidComposeTestRule<R : TestRule, A>` требует
именно `TestRule`). Используйте
`androidx.test.ext.junit.rules.ActivityScenarioRule` — он `extends
ExternalResource` (то есть `TestRule`). Внутренний helper
`getActivityFromTestRule(rule: ActivityScenarioRule<A>): A` помечен
`internal` и недоступен из androidTest module, поэтому нужен свой
`activityProvider` через `lateinit var`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MainActivityDeepLinkRotationUiTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val dao: ItemDao
        get() = DaysDatabase.getDatabase(context.applicationContext).itemDao()

    private val pushIntent =
        Intent(context, MainActivity::class.java).apply {
            action = ReminderIntentContract.ACTION_OPEN_FROM_REMINDER
            putExtra(ReminderIntentContract.EXTRA_ITEM_ID, TARGET_ID)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

    @get:Rule
    val composeTestRule =
        AndroidComposeTestRule(
            activityRule = ActivityScenarioRule(pushIntent),
            activityProvider = ::activityFromRule
        )

    @Before
    fun setUp() {
        runBlocking { dao.deleteAllItems() }
        // Запись создаётся ПОСЛЕ старта Activity — push-intent уже доставлен.
        // Для сценариев, где DetailScreen должен рендериться, добавь
        // runBlocking { dao.insertItem(...) } + composeTestRule.waitForIdle().
    }

    @After
    fun tearDown() {
        runBlocking { dao.deleteAllItems() }
    }

    @Test
    fun when_condition_then_result() {
        // Given: Activity стартовала с push-intent, push обработан
        composeTestRule.waitForIdle()

        // When / Then: ...
    }
}

// Top-level helper — не internal (internal недоступен из androidTest module).
private fun activityFromRule(rule: ActivityScenarioRule<MainActivity>): MainActivity {
    lateinit var activity: MainActivity
    rule.scenario.onActivity { activity = it }
    return activity
}
```

Ключевое:
- `ActivityScenarioRule(intent)` стартует Activity с нужным `Intent` —
  `onCreate` и `onNewIntent` отрабатывают ожидаемо
- `activityProvider = ::activityFromRule` отдаёт Compose-правилу ссылку
  на Activity для `composeTestRule.activity`
- Для теста без push-интента замените на
  `ActivityScenarioRule(MainActivity::class.java)` в `activityRule`

### Caveat: `setIntent` + recreation ломает cleanup

Если в тесте нужен recreation (recreate + setIntent для симуляции rotation
с push-intentом), **не делайте** этого через
`activity.setIntent(pushIntent)` + `scenario.recreate()`:
- `ActivityScenario.recreate()` после `setIntent` бросает
  `IllegalStateException: Requested a re-creation of Activity but didn't happen`
  (ломается внутреннее `mInstrumentationActivityResult`)
- `ActivityScenario.close()` в `@After` после `setRequestedOrientation`
  (rotation) зависает на таймауте `Activity never becomes DESTROYED`
  (45-90 сек)
- `LaunchedEffect`-ы в Compose Testing срабатывают раньше `NavHost`-графа
  → `Cannot navigate to item_detail/1001. Navigation graph has not been set
  for NavController` (race, в production не воспроизводится)

Вместо UI-flow recreation для тестирования **логики** recreation —
вынесите условие в `companion object` (`@VisibleForTesting`) и тестируйте
в pure-JVM unit-тесте, см. раздел «Pure-JVM gate для Activity-логики» ниже.
Для UI-flow без push-intent — `scenario.recreate()` без `setIntent`
работает чисто.

## Pure-JVM gate для Activity-логики

Когда логика в `Activity.onCreate`/`onNewIntent` сводится к условию
(`savedInstanceState == null`, проверка action/intent, флаг и т.п.) —
**выносите условие в `companion object` с `@VisibleForTesting`** и
тестируйте в `app/src/test/` (JUnit 5, без Android, без Compose, без
Robolectric, без эмулятора). Это:

- В **100x быстрее** androidTest (миллисекунды vs секунды)
- **Детерминированный** — нет race в `LaunchedEffect`, нет зависаний
  в `ActivityScenario.close()`
- **JUnit 5 friendly** — без конфликтов с существующим Jupiter-стеком
- **Проверяет ровно то, что фикс делает** — условие срабатывания

Реальный пример из проекта — фикс бага навигации при ротации после пуша
(см. `docs/Plan_2026-08-22_Fix_Rotation_Navigation_Bug.md`). В
`MainActivity.kt`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ...
        if (shouldHandleReminderIntent(savedInstanceState)) {
            handleReminderIntent(intent, reminderManager)
        }
        // ...
    }

    companion object {
        /**
         * Гейт обработки пуш-интента в [onCreate]. Возвращает `true` для
         * cold-start и process-restart, `false` для recreation при rotation
         * / theme change / возврате из фона.
         *
         * @see onCreate
         */
        @VisibleForTesting
        internal fun shouldHandleReminderIntent(savedInstanceState: Bundle?): Boolean =
            savedInstanceState == null
    }
}
```

JVM unit-тест в `app/src/test/java/com/dayscounter/reminder/MainActivityReminderGatingTest.kt`:

```kotlin
class MainActivityReminderGatingTest {
    @Test
    fun cold_start_triggers_handle() {
        // Given: cold-start — savedInstanceState == null
        // When: проверяем гейт
        val shouldHandle = MainActivity.shouldHandleReminderIntent(null)
        // Then: handleReminderIntent должен быть вызван
        assertTrue(
            shouldHandle,
            "Cold-start (savedInstanceState == null) должен обработать reminder-intent"
        )
    }

    @Test
    fun recreate_skips_handle() {
        // Given: recreation — Android сохранил Bundle
        // When: проверяем гейт
        val shouldHandle = MainActivity.shouldHandleReminderIntent(Bundle())
        // Then: handleReminderIntent НЕ должен вызываться повторно
        assertFalse(
            shouldHandle,
            "Recreation (savedInstanceState != null) НЕ должен обрабатывать " +
                "reminder-intent — иначе LaunchedEffect повторно пушнёт ItemDetail"
        )
    }
}
```

Дополнительно — androidTest антирегрессия через `ActivityScenario.recreate()`
**без `setIntent`** (recreate сам подсовывает исходный intent из
ActivityRecord, без дополнительных манипуляций). Это уже не тестирует
логику гейта (это JVM-тест), а проверяет, что `pendingOpenDetailItemId`
корректно отдаётся через `@get:VisibleForTesting` getter после реального
recreate lifecycle.

**Когда НЕ подходит pure-JVM gate:**
- Логика внутри `onCreate` обращается к системным сервисам (`NotificationManager`,
  `AlarmManager`), DI-графу, БД — тогда нужен instrumentation test.
- Тестируется сам Compose-граф (навигация, эффекты, side-effects на UI) —
  тогда `createAndroidComposeRule<MainActivity>()`, но без `setIntent`
  и без `setRequestedOrientation` (см. caveat выше).

## Реальный ViewModel с in-memory Room

`CreateEditHasChangesRegressionUiTest` — гибрид: реальная in-memory БД
+ ViewModel создаётся вручную с реальным репозиторием:

```kotlin
@Before
fun setUp() {
    database = Room
        .inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DaysDatabase::class.java
        )
        .allowMainThreadQueries()
        .build()
    repository = ItemRepositoryImpl(database.itemDao())
}

@Test
fun checkHasChanges_afterLoadItem_doesNotFalselyDetectTimeOfDayChange() {
    // Given: событие с известным временем суток (14:30)
    val timestamp = LocalDate.of(2026, 5, 1)
        .atTime(LocalTime.of(14, 30))
        .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val insertedId = runBlocking { repository.insertItem(item) }
    val viewModel = CreateEditScreenViewModel(
        repository = repository,
        resourceProvider = createTestResourceProvider(),
        logger = NoOpLogger(),
        savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId)),
        analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
    )

    // When: открываем экран редактирования без изменения данных
    composeTestRule.setContent {
        JetpackDaysTheme {
            CreateEditScreen(
                itemId = insertedId,
                viewModel = viewModel,
                analyticsService = AnalyticsService(listOf(NoopAnalyticsProvider()))
            )
        }
    }
    composeTestRule.waitForIdle()

    // Then: кнопка «Сохранить» неактивна
    composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
}
```

## Ротация (recreation Activity)

Полный тест — `references/EXAMPLE.md` (раздел «Интеграционный тест
с MainActivity», метод `when_device_rotates_then_search_field_remains_displayed`).
Паттерн: вставить данные → `waitForIdle()` → сменить ориентацию →
assert → вернуть ориентацию:

```kotlin
val activity = composeTestRule.activity
val originalOrientation = activity.requestedOrientation
activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
composeTestRule.waitForIdle()
// ... asserts ...
activity.requestedOrientation = originalOrientation
```

**Caveat:** `setRequestedOrientation` ломает `ActivityScenario.close()`
в `@After`, если в тесте также есть `activity.setIntent(...)` — cleanup
зависает на 45-90 сек в `Activity never becomes DESTROYED`. Для логики
recreation (например, gate `savedInstanceState == null`) предпочитайте
pure-JVM gate через `companion object` — см. раздел выше.

## Проверка сортировки через boundsInRoot

```kotlin
@Test
fun sameDateDifferentTimeOfDay_ascOldFirst_thenEarlierTimeIsAboveLaterTime() {
    composeTestRule.waitForIdle()

    composeTestRule
        .onNodeWithContentDescription(context.getString(R.string.sort))
        .performClick()
    composeTestRule
        .onNodeWithText(context.getString(R.string.old_first))
        .performClick()
    composeTestRule.waitForIdle()

    val nodeA = composeTestRule.onNodeWithText(titleA)
    val nodeB = composeTestRule.onNodeWithText(titleB)
    nodeA.assertIsDisplayed()
    nodeB.assertIsDisplayed()

    val topA = nodeA.fetchSemanticsNode("nodeA").boundsInRoot.top
    val topB = nodeB.fetchSemanticsNode("nodeB").boundsInRoot.top
    assertTrue(
        "A (09:00) должен быть выше B (18:00) при сортировке «сначала старые», " +
            "но topA=$topA, topB=$topB",
        topA < topB
    )
}
```

## Поиск и скрытие элементов

Полные тесты — `references/EXAMPLE.md` (раздел «Интеграционный тест
с MainActivity»). Суть: поле поиска скрыто при <= 4 элементах
(`assertDoesNotExist`), появляется при 5+ (`assertIsDisplayed`),
остаётся видимым при введённом запросе даже после удаления элементов.
