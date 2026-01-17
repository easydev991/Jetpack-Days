package com.dayscounter.screenshots


import android.content.Context
import android.util.Log
import java.util.Locale
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.MainActivity
import com.dayscounter.R
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tools.fastlane.screengrab.Screengrab
import tools.fastlane.screengrab.UiAutomatorScreenshotStrategy
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

    @Rule
    @JvmField
    val localeTestRule = LocaleTestRule()

    private lateinit var context: Context
    private lateinit var database: DaysDatabase

    @Before
    fun setup() = runBlocking {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        database = DaysDatabase.getDatabase(context.applicationContext)

// Устанавливаем стратегию скриншотов для Compose
        Screengrab.setDefaultScreenshotStrategy(UiAutomatorScreenshotStrategy())

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
        val activity = composeTestRule.activity


        // Ждем загрузки UI
        composeTestRule.waitForIdle()
        Thread.sleep(2000) // Дополнительная пауза для полной загрузки UI

        Log.d("ScreenshotsTest", "Снимаем скриншот 1: Демо-список")

        // Скриншот 1: Демо-список на главном экране
        Screengrab.screenshot("1-demoList")

        Thread.sleep(1000)

        Log.d(
            "ScreenshotsTest",
            "Нажимаем на FAB для создания записи"
        ) // Скриншот 2: Экран создания записи с открытым DatePicker

        // Нажимаем кнопку добавления (FAB) через contentDescription
        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.add_item))
            .performClick()


        // Ждем открытия экрана создания
        composeTestRule.waitForIdle()

        Thread.sleep(1000)


        Log.d("ScreenshotsTest", "Проверяем, что экран создания открыт")
        // Проверяем, что экран создания открыт
        composeTestRule.onNodeWithText(activity.getString(R.string.new_item)).assertExists()

        Log.d("ScreenshotsTest", "Открываем DatePicker")
        // Нажимаем на иконку календаря для открытия DatePicker
        composeTestRule
            .onNodeWithContentDescription(activity.getString(R.string.select_date))
            .performClick()

        // Ждем открытия DatePicker
        composeTestRule.waitForIdle()

        Thread.sleep(1000)

        Log.d("ScreenshotsTest", "Выбираем дату в DatePicker")

        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        // Material 3 DatePicker автоматически выбирает текущую дату при открытии
        // Но для надежности попробуем нажать на дату "15"
        // Используем onAllNodesWithText, так как может быть несколько вхождений
        try {
            composeTestRule.onAllNodesWithText("15", substring = true)
                .onFirst()
                .performClick()
            Log.d("ScreenshotsTest", "Успешно нажали на дату 15")
        } catch (e: Exception) {
            Log.w("ScreenshotsTest", "Не удалось нажать на дату 15: ${e.message}")
            // Продолжаем - Material 3 DatePicker может уже иметь выбранную дату
        }

        Log.d("ScreenshotsTest", "Снимаем скриншот 2: DatePicker открыт")

        Screengrab.screenshot("2-chooseDate")

        // Ждем выбора даты
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        Log.d("ScreenshotsTest", "Закрываем DatePicker")
        // Закрываем DatePicker нажатием OK
        composeTestRule.onNodeWithText(activity.getString(R.string.ok)).performClick()

        // Ждем закрытия DatePicker
        composeTestRule.waitForIdle()

        Thread.sleep(500)

//        Log.d("ScreenshotsTest", "Выбираем опцию отображения")
//        // Скриншот 3: Экран создания с выбранным displayOption
//        // Выбираем опцию отображения "Months and days"
//        composeTestRule
//            .onAllNodesWithText(activity.getString(R.string.months_and_days))
//            .onFirst()
//            .performClick()
//
//        // Ждем выбора
//        composeTestRule.waitForIdle()
//
//        Thread.sleep(500)

        Log.d("ScreenshotsTest", "Снимаем скриншот 3: Выбрана опция отображения")

        Screengrab.screenshot("3-chooseDisplayOption")

        // Скриншот 4: Заполненная форма перед сохранением
        Log.d("ScreenshotsTest", "Заполняем поле заголовка")
        // Заполняем поле заголовка в зависимости от текущей локали
        val currentLocale = Locale.getDefault()
        val testTitle = if (currentLocale.language == "ru") {
            "Слетали на море"
        } else {
            "Travelled to the seaside"
        }

        composeTestRule.onNodeWithText(activity.getString(R.string.title)).performClick()

        composeTestRule.onNodeWithText(activity.getString(R.string.title))
            .performTextInput(testTitle)

        // Ждем ввода
        composeTestRule.waitForIdle()

        Thread.sleep(500)

        Log.d("ScreenshotsTest", "Заполняем поле деталей")
        // Заполняем поле деталей в зависимости от текущей локали
        val testDetails = if (currentLocale.language == "ru") {
            "Отдыхали у теплого моря, купались, загорали и катались на велосипедах"
        } else {
            "Relaxed by the warm sea, swam, sunbathed and rode bicycles"
        }

        composeTestRule.onNodeWithText(activity.getString(R.string.details)).performClick()

        composeTestRule
            .onNodeWithText(activity.getString(R.string.details))
            .performTextInput(testDetails)

        // Ждем ввода
        composeTestRule.waitForIdle()

        Thread.sleep(500)

        Log.d("ScreenshotsTest", "Снимаем скриншот 4: Заполненная форма")

        Screengrab.screenshot("4-beforeSave")


        Log.d("ScreenshotsTest", "Сохраняем запись")
        // Сохраняем запись
        try {
            // Проверяем, что кнопка доступна перед нажатием
            // Используем onFirst, чтобы выбрать конкретную кнопку, если их несколько
            composeTestRule.onAllNodesWithText(activity.getString(R.string.save))
                .onFirst()
                .assertExists()
                .assertIsEnabled()

            Log.d("ScreenshotsTest", "Кнопка сохранения доступна, нажимаем")
            composeTestRule.onAllNodesWithText(activity.getString(R.string.save))
                .onFirst()
                .performClick()
        } catch (e: AssertionError) {
            Log.e("ScreenshotsTest", "Кнопка сохранения недоступна: ${e.message}")
            Log.e("ScreenshotsTest", "Это означает, что дата не выбрана. Тест будет остановлен.")
            throw e
        }

        // Ждем возврата на главный экран
        composeTestRule.waitForIdle()

        Thread.sleep(1000)

        // Шаг 1: Открываем меню сортировки и меняем на "сначала старые"
        Log.d("ScreenshotsTest", "Открываем меню сортировки для изменения порядка")

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.sort))
            .performClick()

        composeTestRule.waitForIdle()

        Thread.sleep(500)

//        Log.d("ScreenshotsTest", "Выбираем 'сначала старые' для изменения порядка")

//        composeTestRule.onNodeWithText(activity.getString(R.string.old_first)).performClick()

//        // Ждем применения новой сортировки
//        composeTestRule.waitForIdle()

//        Thread.sleep(500)

        // Шаг 2: Снова открываем меню сортировки для скриншота
        Log.d("ScreenshotsTest", "Открываем меню сортировки для скриншота")

        composeTestRule.onNodeWithContentDescription(activity.getString(R.string.sort))
            .performClick()

        // Ждем открытия меню
        composeTestRule.waitForIdle()
        Thread.sleep(500)

        Log.d(
            "ScreenshotsTest",
            "Снимаем скриншот 5: Главный экран с открытым меню сортировки (выбрано 'сначала старые')"

        )

        Screengrab.screenshot("5-sortByDate")
    }

    /**
     * Загружает демо-данные в базу данных.
     *
     * Создает тестовые записи с локализацией.
     * Заголовки и детали локализуются в зависимости от текущей локали (ru-RU или en-US).
     */
    private suspend fun loadDemoData() {
        val currentLocale = Locale.getDefault()
        val isEnglish = currentLocale.language == "en"
        val demoItems = listOf(
            DemoItem(
                titleRu = "Новые кроссовки",
                titleEn = "New Sneakers",
                detailsRu = "Купили спортивную обувь для утренних пробежек",
                detailsEn = "Purchased sports shoes for morning runs",
                timestampSeconds = 1_672_531_200, // 2023-01-01
                colorTag = null
            ),
            DemoItem(
                titleRu = "Ремонт окна",
                titleEn = "Window Repair",
                detailsRu = "Замена старой рамы на энергосберегающую конструкцию",
                detailsEn = "Replacing old frame with energy-saving structure",
                timestampSeconds = 1_641_081_600, // 2022-01-02
                colorTag = null
            ),
            DemoItem(
                titleRu = "Торжественное мероприятие",
                titleEn = "Celebratory Event",
                detailsRu = "Посещение праздничного вечера с друзьями",
                detailsEn = "Attending a festive evening with friends",
                timestampSeconds = 1_633_046_400, // 2021-10-01
                colorTag = 0xFFFFA500.toInt() // Orange
            ),
            DemoItem(
                titleRu = "Приобретение автомобиля",
                titleEn = "Car Purchase",
                detailsRu = "Оформление кредита на новый внедорожник",
                detailsEn = "Arranging a loan for a new SUV",
                timestampSeconds = 1_654_041_600, // 2022-06-01
                colorTag = android.graphics.Color.RED // Red
            ),
            DemoItem(
                titleRu = "Стоматологический осмотр",
                titleEn = "Dental Checkup",
                detailsRu = "Установка пломбы на коренной зуб",
                detailsEn = "Filling placement on a molar tooth",
                timestampSeconds = 1_664_582_400, // 2022-10-01
                colorTag = null
            ),
            DemoItem(
                titleRu = "Зарубежное путешествие",
                titleEn = "Overseas Trip",
                detailsRu = "Тур по историческим достопримечательностям",
                detailsEn = "Tour of historical landmarks",
                timestampSeconds = 1_598_918_400, // 2020-09-01
                colorTag = 0xFF800080.toInt() // Purple
            ),
            DemoItem(
                titleRu = "Защита проекта",
                titleEn = "Project Defense",
                detailsRu = "Успешная презентация годового исследования",
                detailsEn = "Successful presentation of annual research",
                timestampSeconds = 1_561_939_200, // 2019-07-01
                colorTag = android.graphics.Color.YELLOW // Yellow
            ),
            DemoItem(
                titleRu = "Смена адреса",
                titleEn = "Address Change",
                detailsRu = "Переезд в новую квартиру с улучшенной планировкой",
                detailsEn = "Moving to a new apartment with better layout",
                timestampSeconds = 1_585_699_200, // 2020-04-02
                colorTag = null
            ),
            DemoItem(
                titleRu = "Водительский экзамен",
                titleEn = "Driving Exam",
                detailsRu = "Успешная сдача теста в ГИБДД с первого раза",
                detailsEn = "Passed driving test on first attempt",
                timestampSeconds = 1_483_228_800, // 2017-01-01
                colorTag = 0xFF98FF98.toInt() // Mint
            ),
            DemoItem(
                titleRu = "Медицинская операция",
                titleEn = "Medical Surgery",
                detailsRu = "Плановое хирургическое вмешательство в клинике",
                detailsEn = "Scheduled surgical procedure at clinic",
                timestampSeconds = 1_538_352_000, // 2018-10-01
                colorTag = null
            )
        )

        // Вставляем записи в базу данных
        demoItems.forEachIndexed { index, demoItem ->
            database.itemDao().insertItem(
                ItemEntity(
                    id = (index + 1).toLong(),
                    title = if (isEnglish) demoItem.titleEn else demoItem.titleRu,
                    details = if (isEnglish) demoItem.detailsEn else demoItem.detailsRu,
                    timestamp = demoItem.timestampSeconds * 1000, // Конвертируем в миллисекунды
                    colorTag = demoItem.colorTag,
                    displayOption = DisplayOption.YEAR_MONTH_DAY.name
                )
            )
        }
    }

    /**
     * Вспомогательный класс для демо-данных.
     */
    private data class DemoItem(
        val titleRu: String,
        val titleEn: String,
        val detailsRu: String,
        val detailsEn: String,
        val timestampSeconds: Long,
        val colorTag: Int?
    )
}
