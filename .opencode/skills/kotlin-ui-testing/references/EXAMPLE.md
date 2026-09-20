# EXAMPLE — канонические примеры

Полные примеры тестов из реального кода JetpackDays.
Файлы: `app/src/androidTest/java/com/dayscounter/...`

## Компонентный UI-тест (без Activity)

`ui/ds/DaysCountTextTest.kt` — компонент `DaysCountText`, без темы и
без `@RunWith` (v2 API):

```kotlin
import androidx.compose.ui.test.junit4.v2.createComposeRule

class DaysCountTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun daysCountText_whenDisplayed_thenShowsCorrectText() {
        // Given
        val testText = "5 дней"

        // When
        composeTestRule.setContent {
            DaysCountText(formattedText = testText)
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    @Test
    fun daysCountText_whenEmptyText_thenShowsEmpty() {
        // Given
        val emptyText = ""

        // When
        composeTestRule.setContent {
            DaysCountText(formattedText = emptyText)
        }

        // Then
        composeTestRule.onNodeWithText(emptyText).assertExists()
    }
}
```

## Компонентный тест с колбэками (диалог)

`ui/screens/events/ColorTagFilterDialogTest.kt` — без `@RunWith`,
в `JetpackDaysTheme`, строки из ресурсов:

```kotlin
import androidx.compose.ui.test.junit4.v2.createComposeRule

class ColorTagFilterDialogTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val colorDescription = context.getString(R.string.color)
    private val resetText = context.getString(R.string.reset)
    private val applyText = context.getString(R.string.apply)
    private val titleText = context.getString(R.string.filter_by_color)

    @Test
    fun dialog_whenSameColorClicked_thenDeselects() {
        // Given - фильтр не установлен
        val currentFilter: Int? = null

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = currentFilter,
                    onApply = {},
                    onDismiss = {}
                )
            }
        }

        // When: кликаем по красному цвету дважды — выделение снимается
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[0]
            .performClick()
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[0]
            .performClick()

        // Then: нет фильтра и нет черновика — кнопка «Сбросить» неактивна
        composeTestRule
            .onNodeWithText(resetText)
            .assertIsNotEnabled()
    }

    @Test
    fun dialog_whenResetWithoutCurrentFilterButWithDraft_thenDialogStaysOpen() {
        // Given
        var onApplyCalled = false
        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorTagFilterDialog(
                    availableColors = availableColors,
                    currentFilter = null,
                    onApply = { onApplyCalled = true },
                    onDismiss = {}
                )
            }
        }

        // When: сброс без активного фильтра
        composeTestRule.onAllNodesWithContentDescription(colorDescription)[0].performClick()
        composeTestRule.onNodeWithText(resetText).performClick()

        // Then: диалог остаётся открытым, onApply не вызван
        composeTestRule.onNodeWithText(titleText).assertIsDisplayed()
        composeTestRule.runOnIdle {
            org.junit.Assert.assertFalse(
                "При сбросе черновика без фильтра onApply не должен вызываться",
                onApplyCalled
            )
        }
    }
}
```

## Интеграционный тест с MainActivity

`ui/screens/events/MainScreenSearchVisibilityUiTest.kt` — v2 API
`createAndroidComposeRule`, `@RunWith(AndroidJUnit4::class)`, реальный
DAO, вставка через `runBlocking`, `waitForIdle`:

```kotlin
@RunWith(AndroidJUnit4::class)
class MainScreenSearchVisibilityUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dao: ItemDao

    private val searchDescription: String
        get() = context.getString(R.string.search)

    @Before
    fun setUp() {
        runBlocking {
            dao = DaysDatabase.getDatabase(context.applicationContext).itemDao()
            dao.deleteAllItems()
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            dao.deleteAllItems()
        }
    }

    @Test
    fun when_items_count_4_then_search_field_not_displayed() {
        // Given
        runBlocking { insertItems(4) }
        composeTestRule.waitForIdle()

        // Then: поле поиска скрыто
        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertDoesNotExist()
    }

    @Test
    fun when_user_typed_query_with_3_items_then_search_field_stays_visible() {
        // Given
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .performTextInput("Item")

        // And: удаляем элементы, пока введён запрос
        runBlocking { deleteFirstItem() }
        runBlocking { deleteFirstItem() }
        composeTestRule.waitForIdle()

        // Then: поле поиска остаётся видимым
        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()
    }

    @Test
    fun when_device_rotates_then_search_field_remains_displayed() {
        // Given
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription(searchDescription).assertIsDisplayed()

        // When: поворот устройства
        val activity = composeTestRule.activity
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeTestRule.waitForIdle()

        // Then: поле поиска осталось
        composeTestRule.onNodeWithContentDescription(searchDescription).assertIsDisplayed()

        // Антирегрессия R5: клик по SearchField после rotation не падает
        composeTestRule.onNodeWithContentDescription(searchDescription).performClick()
        activity.requestedOrientation = originalOrientation
    }
}
```

## DAO-тест (Room in-memory)

`data/database/dao/ItemDaoTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class ItemDaoTest {
    private lateinit var database: DaysDatabase
    private lateinit var itemDao: ItemDao

    @Before
    fun setup() {
        database = Room
            .inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                DaysDatabase::class.java
            )
            .allowMainThreadQueries()
            .build()
        itemDao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertItem_thenRetrieveIt() =
        runBlocking {
            // Given
            val item = ItemEntity(title = "Тестовое событие", ...)

            // When
            val insertedId = itemDao.insertItem(item)
            val retrieved = itemDao.getItemById(insertedId)

            // Then
            assertNotNull(retrieved)
            assertEquals("Тестовое событие", retrieved?.title)
        }

    @Test
    fun getAllItems_whenSameTimestamp_thenSortedByIdDesc() =
        runBlocking {
            // Given
            itemDao.insertItem(itemA.copy(timestamp = 1000L))
            itemDao.insertItem(itemB.copy(timestamp = 1000L))

            // When
            val all = itemDao.getAllItems().first()

            // Then
            assertEquals(2, all.size)
            assertTrue(
                "При равном timestamp первым должен быть больший id",
                all[0].id > all[1].id
            )
        }
}
```

## ViewModel-интеграция (Turbine)

`ui/viewmodel/DetailScreenViewModelIntegrationTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DetailScreenViewModelIntegrationTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var database: DaysDatabase
    private lateinit var repository: ItemRepositoryImpl
    private lateinit var viewModel: DetailScreenViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, DaysDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = ItemRepositoryImpl(database.itemDao())
        database.clearAllTables()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun whenItemExistsInDatabase_thenLoadsSuccessfully() =
        runTest {
            // Given
            val insertedId = repository.insertItem(testItem)
            val savedStateHandle = SavedStateHandle(mapOf("itemId" to insertedId))

            // When
            viewModel = DetailScreenViewModel(repository, NoOpLogger(), savedStateHandle)

            // Then
            viewModel.uiState.test {
                val loadingState = awaitItem()
                assertTrue(
                    "Начальное состояние должно быть Loading",
                    loadingState is DetailScreenState.Loading
                )
                val successState = awaitItem()
                assertTrue(
                    "После загрузки — Success",
                    successState is DetailScreenState.Success
                )
                assertEquals(
                    "Тестовое событие",
                    (successState as DetailScreenState.Success).item.title
                )
            }
        }
}
```

## Alarm-тест (PendingIntent + пермишены)

`reminder/AlarmReminderSchedulerInstrumentedTest.kt`:

```kotlin
@RunWith(AndroidJUnit4::class)
class AlarmReminderSchedulerInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun cancel_whenScheduled_thenPendingIntentIsRemoved() {
        // Given
        val scheduler = AlarmReminderScheduler(context)
        val itemId = 777L
        val reminder = Reminder(itemId = itemId, ...)

        // When
        scheduler.schedule(reminder, "title")

        // Then
        assertNotNull(findReminderPendingIntent(itemId))

        // When
        scheduler.cancel(itemId)

        // Then
        assertNull(findReminderPendingIntent(itemId))
    }

    private fun findReminderPendingIntent(itemId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            ReminderIntentContract.requestCodeForItem(itemId),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderIntentContract.ACTION_FIRE_REMINDER
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
}
```
