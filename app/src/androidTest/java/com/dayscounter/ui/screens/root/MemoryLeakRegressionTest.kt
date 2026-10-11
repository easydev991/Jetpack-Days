package com.dayscounter.ui.screens.root

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.NoopAnalyticsProvider
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import kotlinx.coroutines.runBlocking
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Регрессионный тест утечек памяти (LeakCanary) для корневой навигации:
 * циклы по нижним вкладкам и открытие/закрытие детального экрана.
 *
 * Правило [noLeaksRule] объявлено с `order = 1` (внешнее), [composeTestRule] —
 * с `order = 2` (внутреннее): в JUnit 4 «rules with a higher value are inner»,
 * поэтому проверка утечек выполняется строго после teardown composeTestRule,
 * когда Activity и композиция уже уничтожены — иначе ловить нечего.
 *
 * NavHostController нельзя хранить в поле теста: он удерживает уничтоженную
 * Activity, и LeakCanary справедливо посчитает это утечкой, созданной тестом.
 * RootScreen создаёт контроллер внутри себя, поэтому навигация идёт только
 * через UI-клики (строка списка → back-button в DetailAppBar).
 *
 * Запуск: `make memory-check` (нужен подключённый эмулятор).
 */
@RunWith(AndroidJUnit4::class)
class MemoryLeakRegressionTest {
    @get:Rule(order = 1)
    val noLeaksRule = DetectLeaksAfterTestSuccess()

    @get:Rule(order = 2)
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val analyticsService = AnalyticsService(providers = listOf(NoopAnalyticsProvider()))

    private val tabTexts =
        listOf(
            context.getString(R.string.events),
            context.getString(R.string.more)
        )

    @Before
    fun seedItem() {
        runBlocking {
            DaysDatabase.getDatabase(context.applicationContext).itemDao().insertItem(
                ItemEntity(
                    id = 1L,
                    title = SEED_ITEM_TITLE,
                    details = "",
                    timestamp = System.currentTimeMillis(),
                    colorTag = null,
                    displayOption = DisplayOption.DAY.name
                )
            )
        }
    }

    @After
    fun cleanupItem() {
        runBlocking {
            // БД — process-wide синглтон: чистим целиком,
            // чтобы сид-строка не протекла в другие тесты прогона
            DaysDatabase.getDatabase(context.applicationContext).clearAllTables()
        }
    }

    @Test
    fun eventsScreen_whenCyclingBottomNavTabs_thenNoLeaks() {
        // Given: корневой экран с нижней навигацией (Events, More)
        composeTestRule.setContent {
            RootScreen(analyticsService = analyticsService)
        }

        // When: циклы по нижним вкладкам
        repeat(CYCLES) {
            tabTexts.forEach { tabText ->
                // hasText + hasClickAction: строка вкладки есть и в top bar
                // (не кликабельный), и в NavigationBarItem (кликабельный) —
                // без фильтра по click-action найдутся 2 ноды
                composeTestRule
                    .onNode(hasText(tabText) and hasClickAction())
                    .performClick()
            }
        }

        // Then: утечки проверяет внешнее правило noLeaksRule
    }

    @Test
    fun eventsScreen_whenOpeningItemDetailAndGoingBack_thenNoLeaks() {
        // Given: корневой экран со сид-строкой в списке событий
        composeTestRule.setContent {
            RootScreen(analyticsService = analyticsService)
        }
        composeTestRule.waitForIdle()

        // When: циклы «строка списка → ItemDetail → back» через UI,
        // не через navController (он создаётся внутри RootScreen)
        repeat(CYCLES) { cycle ->
            composeTestRule
                .onNodeWithText(SEED_ITEM_TITLE)
                .performClick()
            if (cycle == 0) {
                // Детальный экран должен реально дойти до контента,
                // а не зависнуть в Loading (иначе тест зелёный всегда,
                // а заявленный сценарий не проверяется)
                composeTestRule.waitUntil(timeoutMillis = LOADING_TIMEOUT_MS) {
                    composeTestRule
                        .onAllNodesWithText(context.getString(R.string.loading))
                        .fetchSemanticsNodes()
                        .isEmpty()
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule
                .onNodeWithContentDescription(context.getString(R.string.close))
                .performClick()
            composeTestRule.waitForIdle()
        }

        // Then: утечки проверяет внешнее правило noLeaksRule
    }

    private companion object {
        const val CYCLES = 15
        const val SEED_ITEM_TITLE = "SeedItem"
        const val LOADING_TIMEOUT_MS = 5_000L
    }
}
