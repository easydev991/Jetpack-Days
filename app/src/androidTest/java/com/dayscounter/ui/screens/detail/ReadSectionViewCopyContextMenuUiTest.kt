package com.dayscounter.ui.screens.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты контекстного меню копирования в [ReadSectionView].
 *
 * Проверяет контракт:
 * - при ненулевом [ReadSectionView] долгое нажатие открывает меню,
 *   клик по пункту «Скопировать» вызывает [ReadSectionView.onCopy] ровно один раз;
 * - при `onCopy == null` долгое нажатие НЕ открывает меню.
 */
@RunWith(AndroidJUnit4::class)
class ReadSectionViewCopyContextMenuUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val copyMenuText = context.getString(R.string.context_menu_copy)
    private val bodyText = "Какой-то title для проверки контекстного меню"

    @Test
    fun on_copy_when_menu_item_clicked_then_count_is_one() {
        // Given
        var invocationCount = 0
        composeTestRule.setContent {
            com.dayscounter.ui.theme.JetpackDaysTheme {
                ReadSectionView(
                    headerText = context.getString(R.string.title),
                    bodyText = bodyText,
                    onCopy = { invocationCount += 1 }
                )
            }
        }

        // When: долгое нажатие по телу секции открывает меню
        composeTestRule
            .onNodeWithText(bodyText)
            .performTouchInput { longClick() }
        composeTestRule.waitUntil(timeoutMillis = 2_000) {
            composeTestRule
                .onAllNodesWithText(copyMenuText)
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeTestRule
            .onNodeWithText(copyMenuText)
            .assertIsDisplayed()
            .performClick()

        // Then: колбэк вызван ровно один раз
        composeTestRule.runOnIdle {
            org.junit.Assert.assertEquals(
                "onCopy должен вызываться ровно один раз после клика по пункту меню",
                1,
                invocationCount
            )
        }
    }

    @Test
    fun long_press_ignored_when_on_copy_is_null_then_no_menu_shown() {
        // Given
        composeTestRule.setContent {
            com.dayscounter.ui.theme.JetpackDaysTheme {
                ReadSectionView(
                    headerText = context.getString(R.string.reminder_settings),
                    bodyText = bodyText,
                    onCopy = null
                )
            }
        }

        // When: долгое нажатие по телу секции
        composeTestRule
            .onNodeWithText(bodyText)
            .performTouchInput { longClick() }
        composeTestRule.waitForIdle()

        // Then: пункт меню «Скопировать» не должен появляться
        composeTestRule
            .onNodeWithText(copyMenuText)
            .assertDoesNotExist()
    }
}
