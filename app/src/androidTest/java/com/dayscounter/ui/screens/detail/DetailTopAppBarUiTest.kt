package com.dayscounter.ui.screens.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.ui.viewmodel.DetailScreenState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты навигационной иконки «назад» в [DetailTopAppBar].
 *
 * Проверяет контракт спеки `detail-screen` (change fix-detail-screen-infinite-loading):
 * кнопка «назад» MUST отображаться и работать в состояниях загрузки и ошибки —
 * сценарии «Возврат назад из состояния загрузки» и «Возврат назад из состояния ошибки».
 */
@RunWith(AndroidJUnit4::class)
class DetailTopAppBarUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val backIconDescription = context.getString(R.string.close)

    @Test
    fun back_icon_when_loading_state_then_on_back_click_invoked() {
        // Given — TopAppBar в состоянии загрузки
        var backClickCount = 0
        composeTestRule.setContent {
            com.dayscounter.ui.theme.JetpackDaysTheme {
                DetailTopAppBar(
                    uiState = DetailScreenState.Loading,
                    onBackClick = { backClickCount += 1 },
                    onEditClick = {},
                    onDeleteClick = {},
                    itemId = 1L
                )
            }
        }

        // When — иконка отображается, клик по ней выполняется
        composeTestRule
            .onNodeWithContentDescription(backIconDescription)
            .assertIsDisplayed()
            .performClick()

        // Then — колбэк «назад» вызван ровно один раз
        composeTestRule.runOnIdle {
            assertEquals(
                "Стрелка назад должна отображаться и быть кликабельной в состоянии Loading",
                1,
                backClickCount
            )
        }
    }

    @Test
    fun back_icon_when_error_state_then_on_back_click_invoked() {
        // Given — TopAppBar в состоянии ошибки (событие не найдено)
        var backClickCount = 0
        composeTestRule.setContent {
            com.dayscounter.ui.theme.JetpackDaysTheme {
                DetailTopAppBar(
                    uiState = DetailScreenState.Error(message = "Событие не найдено"),
                    onBackClick = { backClickCount += 1 },
                    onEditClick = {},
                    onDeleteClick = {},
                    itemId = 999L
                )
            }
        }

        // When — иконка отображается, клик по ней выполняется
        composeTestRule
            .onNodeWithContentDescription(backIconDescription)
            .assertIsDisplayed()
            .performClick()

        // Then — колбэк «назад» вызван ровно один раз
        composeTestRule.runOnIdle {
            assertEquals(
                "Стрелка назад должна отображаться и быть кликабельной в состоянии Error",
                1,
                backClickCount
            )
        }
    }
}
