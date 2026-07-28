package com.dayscounter.ui.screens.events

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.MainActivity
import com.dayscounter.R
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * UI-тест для §6.1: проверка визуального порядка элементов на главном экране
 * при одинаковой дате, но разном time-of-day.
 *
 * Вставляет два события (09:00 и 18:00) на одну дату и проверяет:
 * - оба элемента отображаются
 * - при сортировке «сначала старые» (ASC) 09:00 выше 18:00
 * - при сортировке «сначала новые» (DESC) 18:00 выше 09:00
 */
@RunWith(AndroidJUnit4::class)
class MainScreenSortByTimeOfDayUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var database: DaysDatabase

    private val titleA = "Событие A (09:00)"
    private val titleB = "Событие B (18:00)"

    @Before
    fun setUp() {
        runBlocking {
            database = DaysDatabase.getDatabase(context.applicationContext)
            database.itemDao().deleteAllItems()

            val date = LocalDate.of(2026, 7, 26)

            database.itemDao().insertItem(
                ItemEntity(
                    id = 0L,
                    title = titleA,
                    details = "",
                    timestamp =
                        date
                            .atTime(9, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY.name
                )
            )

            database.itemDao().insertItem(
                ItemEntity(
                    id = 0L,
                    title = titleB,
                    details = "",
                    timestamp =
                        date
                            .atTime(18, 0)
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY.name
                )
            )
        }
    }

    @After
    fun tearDown() {
        runBlocking {
            database.itemDao().deleteAllItems()
        }
    }

    @Test
    fun sameDateDifferentTimeOfDay_ascOldFirst_thenEarlierTimeIsAboveLaterTime() {
        composeTestRule.waitForIdle()

        // Открываем меню сортировки и выбираем «сначала старые» (ASC)
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.sort))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.old_first))
            .performClick()
        composeTestRule.waitForIdle()

        // Оба элемента видны
        val nodeA = composeTestRule.onNodeWithText(titleA)
        val nodeB = composeTestRule.onNodeWithText(titleB)
        nodeA.assertIsDisplayed()
        nodeB.assertIsDisplayed()

        // A (09:00) должен быть выше B (18:00)
        val topA = nodeA.fetchSemanticsNode("nodeA").boundsInRoot.top
        val topB = nodeB.fetchSemanticsNode("nodeB").boundsInRoot.top
        assertTrue(
            "A (09:00) должен быть выше B (18:00) при сортировке «сначала старые», " +
                "но topA=$topA, topB=$topB",
            topA < topB
        )
    }

    @Test
    fun sameDateDifferentTimeOfDay_descNewFirst_thenLaterTimeIsAboveEarlierTime() {
        composeTestRule.waitForIdle()

        // По умолчанию DESC (новые первые) — открываем меню, чтобы убедиться
        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.sort))
            .performClick()
        composeTestRule
            .onNodeWithText(context.getString(R.string.new_first))
            .performClick()
        composeTestRule.waitForIdle()

        // Оба элемента видны
        val nodeA = composeTestRule.onNodeWithText(titleA)
        val nodeB = composeTestRule.onNodeWithText(titleB)
        nodeA.assertIsDisplayed()
        nodeB.assertIsDisplayed()

        // B (18:00) должен быть выше A (09:00)
        val topA = nodeA.fetchSemanticsNode("nodeA").boundsInRoot.top
        val topB = nodeB.fetchSemanticsNode("nodeB").boundsInRoot.top
        assertTrue(
            "B (18:00) должен быть выше A (09:00) при сортировке «сначала новые», " +
                "но topB=$topB, topA=$topA",
            topB < topA
        )
    }
}
