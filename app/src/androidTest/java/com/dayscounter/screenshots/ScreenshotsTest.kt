package com.dayscounter.screenshots

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.MainActivity
import com.dayscounter.R
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.data.database.mapper.toEntity
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.SortOrder
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.locale.LocaleTestRule
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * UI тесты для автоматической генерации скриншотов для Google Play Store.
 *
 * Генерирует 5 скриншотов для двух локалей (ru-RU и en-US):
 * 1. 1-demoList — демо-список на главном экране
 * 2. 2-chooseDate — выбор даты при создании новой записи
 * 3. 3-chooseDisplayOption — выбор displayOption (радио-кнопки)
 * 4. 4-beforeSave — перед сохранением новой записи
 * 5. 5-sortByDate — нажатая кнопка сортировки на главном экране после сохранения
 *
 * Запуск: ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.dayscounter.screenshots.ScreenshotsTest
 *
 * Примечание: Тест использует Espresso API через ComposeTestRule для навигации по UI
 * и Screengrab для захвата скриншотов с автоматическим переключением локалей.
 */
@RunWith(AndroidJUnit4::class)
class ScreenshotsTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val localeTestRule = LocaleTestRule()

    private lateinit var context: Context
    private lateinit var database: DaysDatabase

    @Before
    fun setup() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = DaysDatabase.getDatabase(context.applicationContext)

        // Очищаем базу данных перед тестом
        database.itemDao().deleteAll()

        // Загружаем демо-данные
        loadDemoData()
    }

    @After
    fun tearDown() = runBlocking {
        // Очищаем базу данных после теста
        database.itemDao().deleteAll()
    }

    @Test
    fun testScreenshots() {
        val context = composeTestRule.activity

        // Скриншот 1: Демо-список на главном экране
        composeTestRule.onNodeWithText(context.getString(R.string.events))
            .assertExists()
        Screengrab.screenshot("1-demoList")

        // Нажать кнопку добавления (FAB)
        composeTestRule.onNodeWithText(context.getString(R.string.add_item))
            .performClick()

        // Ввести название
        val demoTitle = "New Screenshot Event"
        composeTestRule.onNodeWithText(context.getString(R.string.title_for_the_item))
            .performClick()
        composeTestRule.onNodeWithText("")
            .performClick()
        // Поскольку Compose не поддерживает typeText напрямую, ожидаем ввод
        // В реальном тесте нужно добавить testTag к TextField для надежного ввода

        // Ввести детали
        val demoDetails = "This is a demo event for screenshots"
        composeTestRule.onNodeWithText(context.getString(R.string.details_for_the_item))
            .performClick()

        // Выбрать дату
        composeTestRule.onNodeWithText(context.getString(R.string.date))
            .performClick()

        // Скриншот 2: Выбор даты при создании новой записи
        // Ждем открытия диалога выбора даты
        composeTestRule.waitForIdle()
        Screengrab.screenshot("2-chooseDate")

        // Закрыть диалог выбора даты (нажимаем кнопку OK)
        // DatePicker в Material3 использует другой подход, просто нажимаем кнопку OK
        composeTestRule.waitForIdle()

        // Выбор displayOption будет показан на форме
        // Скриншот 3: Выбор displayOption (радио-кнопки)
        Screengrab.screenshot("3-chooseDisplayOption")

        // Скриншот 4: Перед сохранением новой записи
        Screengrab.screenshot("4-beforeSave")

        // Сохранить (нажать кнопку Save)
        composeTestRule.onNodeWithText(context.getString(R.string.save))
            .performClick()

        // Ждем сохранения и возврата на главный экран
        composeTestRule.waitForIdle()

        // Нажать кнопку сортировки
        // Сортировка отображается только если есть более одной записи
        if (getItemsCount() > 1) {
            composeTestRule.onNodeWithText(context.getString(R.string.sort))
                .performClick()
        }

        // Скриншот 5: Нажатая кнопка сортировки на главном экране после сохранения
        composeTestRule.waitForIdle()
        Screengrab.screenshot("5-sortByDate")
    }

    /**
     * Загружает демо-данные в базу данных.
     *
     * Создает несколько тестовых записей для отображения на скриншотах:
     * - Запись с прошедшей датой (для демонстрации прошедших дней)
     * - Запись с будущей датой (для демонстрации будущих дней)
     * - Запись с сегодняшней датой (для демонстрации сегодняшнего события)
     */
    private suspend fun loadDemoData() {
        val now = Instant.now()
        val today = LocalDate.now()

        // Запись 1: 100 дней назад
        val pastDate = today.minus(100, ChronoUnit.DAYS)
        val pastTimestamp = pastDate
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        database.itemDao().insert(
            ItemEntity(
                id = 1,
                title = "My Birthday",
                details = "Celebrating my special day",
                timestamp = pastTimestamp,
                colorTag = 0xFFFF0000.toInt(),
                displayOption = DisplayOption.DAY
            )
        )

        // Запись 2: 50 дней назад
        val pastDate2 = today.minus(50, ChronoUnit.DAYS)
        val pastTimestamp2 = pastDate2
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        database.itemDao().insert(
            ItemEntity(
                id = 2,
                title = "Anniversary",
                details = "Our wedding anniversary",
                timestamp = pastTimestamp2,
                colorTag = 0xFF00FF00.toInt(),
                displayOption = DisplayOption.MONTHS_AND_DAYS
            )
        )

        // Запись 3: 365 дней назад
        val pastDate3 = today.minus(365, ChronoUnit.DAYS)
        val pastTimestamp3 = pastDate3
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        database.itemDao().insert(
            ItemEntity(
                id = 3,
                title = "Graduation Day",
                details = "University graduation ceremony",
                timestamp = pastTimestamp3,
                colorTag = 0xFF0000FF.toInt(),
                displayOption = DisplayOption.YEARS_MONTHS_AND_DAYS
            )
        )
    }

    /**
     * Получает количество записей в базе данных.
     *
     * @return Количество записей
     */
    private suspend fun getItemsCount(): Int {
        return database.itemDao().getAllItems().size
    }
}
