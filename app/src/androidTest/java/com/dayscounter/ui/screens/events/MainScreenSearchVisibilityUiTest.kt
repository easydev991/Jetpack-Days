package com.dayscounter.ui.screens.events

import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.MainActivity
import com.dayscounter.R
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * UI-тесты для V2: видимость [SearchBar] на главном экране
 * в зависимости от количества элементов и активного поискового запроса.
 *
 * Удаление элементов выполняется через DAO напрямую, чтобы не зависеть от long-press +
 * контекстного меню (этот путь покрывается ручным прогоном).
 *
 * Селектор поля — `contentDescription` leadingIcon (`R.string.search`): нода присутствует
 * только когда родительский [androidx.compose.animation.AnimatedVisibility] имеет
 * `visible = true`, что и проверяется.
 */
@RunWith(AndroidJUnit4::class)
class MainScreenSearchVisibilityUiTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dao: ItemDao

    private val searchDescription: String
        get() = context.getString(R.string.search)

    private val closeDescription: String
        get() = context.getString(R.string.close)

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
        runBlocking { insertItems(4) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertDoesNotExist()
    }

    @Test
    fun when_items_count_5_then_search_field_displayed() {
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()
    }

    @Test
    fun when_items_count_drops_from_5_to_4_then_search_field_animates_out() {
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()

        runBlocking { deleteFirstItem() }

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription(searchDescription)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    @Test
    fun when_user_typed_query_with_3_items_then_search_field_stays_visible() {
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        // Вводим запрос при 5 элементах: поле видно через ветку itemsCount >= MIN_ITEMS_FOR_SEARCH.
        // "Item" матчит все заголовки, после удаления 2 элементов останется 3.
        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .performTextInput("Item")

        runBlocking { deleteFirstItem() }
        runBlocking { deleteFirstItem() }
        composeTestRule.waitForIdle()

        // Поле остаётся видимым благодаря ветке searchQuery.isNotEmpty().
        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()
    }

    @Test
    fun when_user_clears_query_with_4_items_then_search_field_animates_out() {
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .performTextInput("Item")

        // 5 -> 4, query не пуст, поле остаётся.
        runBlocking { deleteFirstItem() }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()

        // Очищаем запрос через Close-кнопку: 4 + "" -> поле должно уйти.
        composeTestRule
            .onNodeWithContentDescription(closeDescription)
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = 1000) {
            composeTestRule
                .onAllNodesWithContentDescription(searchDescription)
                .fetchSemanticsNodes()
                .isEmpty()
        }
    }

    @Test
    fun when_search_query_empty_then_clear_button_not_displayed() {
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(closeDescription)
            .assertDoesNotExist()
    }

    @Test
    fun when_device_rotates_then_search_field_remains_displayed() {
        runBlocking { insertItems(5) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()

        // После rotation SearchField остаётся видимым (Activity recreate, state сохраняется).
        val activity = composeTestRule.activity
        val originalOrientation = activity.requestedOrientation
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .assertIsDisplayed()

        // R5: клик по SearchField после rotation не падает — BasicTextField остаётся кликабельным.
        composeTestRule
            .onNodeWithContentDescription(searchDescription)
            .performClick()

        activity.requestedOrientation = originalOrientation
    }

    private suspend fun insertItems(count: Int) {
        val timestamp =
            LocalDate
                .of(2026, 7, 26)
                .atTime(9, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        repeat(count) { index ->
            dao.insertItem(
                ItemEntity(
                    title = "Item ${index + 1}",
                    details = "",
                    timestamp = timestamp,
                    colorTag = null,
                    displayOption = DisplayOption.DAY.name
                )
            )
        }
    }

    private suspend fun deleteFirstItem() {
        val first = dao.getAllItems().first().firstOrNull() ?: return
        dao.deleteItem(first)
    }
}
