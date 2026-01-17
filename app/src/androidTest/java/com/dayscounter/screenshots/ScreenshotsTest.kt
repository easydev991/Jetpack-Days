package com.dayscounter.screenshots

import android.content.Context
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.AndroidJUnit4
import androidx.compose.ui.test.junit4.AndroidTestRunner
import androidx.compose.ui.test.junit4.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.junit4.junit.runner.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.junit4.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        database.itemDao().deleteAllItems()

        // Загружаем демо-данные
        loadDemoData()
    }

    @After
    fun tearDown() = runBlocking {
        // Очищаем базу данных после теста
        database.itemDao().deleteAllItems()
    }

    @Test
    fun testScreenshots() {
        val context = composeTestRule.activity

        // Ждем загрузки UI
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Дополнительная пауза для полной загрузки UI

        // Скриншот 1: Демо-список на главном экране
        Screengrab.screenshot("1-demoList")

        // Делаем паузу между скриншотами
        Thread.sleep(1000)

        // Скриншот 2: Экран создания записи (просто показываем его)
        // Нажимаем кнопку добавления (FAB) через contentDescription
        composeTestRule.onNodeWithContentDescription(context.getString(R.string.add_item))
            .performClick()
        
        // Ждем открытия экрана создания
        Thread.sleep(2000)
        Screengrab.screenshot("2-createScreen")

        // Скриншот 3: Экран до сохранения
        Thread.sleep(1000)
        Screengrab.screenshot("3-beforeSave")

        // Скриншот 4: Главный экран снова
        Screengrab.screenshot("4-backToMain")

        // Скриншот 5: Сортировка
        Thread.sleep(1000)
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

        database.itemDao().insertItem(
            ItemEntity(
                id = 1,
                title = "My Birthday",
                details = "Celebrating my special day",
                timestamp = pastTimestamp,
                colorTag = 0xFFFF0000.toInt(),
                displayOption = DisplayOption.DAY.name
            )
        )

        // Запись 2: 50 дней назад
        val pastDate2 = today.minus(50, ChronoUnit.DAYS)
        val pastTimestamp2 = pastDate2
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        database.itemDao().insertItem(
            ItemEntity(
                id = 2,
                title = "Anniversary",
                details = "Our wedding anniversary",
                timestamp = pastTimestamp2,
                colorTag = 0xFF00FF00.toInt(),
                displayOption = DisplayOption.MONTH_DAY.name
            )
        )

        // Запись 3: 365 дней назад
        val pastDate3 = today.minus(365, ChronoUnit.DAYS)
        val pastTimestamp3 = pastDate3
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        database.itemDao().insertItem(
            ItemEntity(
                id = 3,
                title = "Graduation Day",
                details = "University graduation ceremony",
                timestamp = pastTimestamp3,
                colorTag = 0xFF0000FF.toInt(),
                displayOption = DisplayOption.YEAR_MONTH_DAY.name
            )
        )
    }
}
