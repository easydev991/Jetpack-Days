package com.dayscounter.ui.screens.more

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.BuildConfig
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Компонентные тесты для [MoreScreen].
 *
 * Проверяет наличие всех кнопок на экране, корректность отображения
 * версии приложения и функциональность кнопок-заглушек.
 */
@RunWith(AndroidJUnit4::class)
class MoreScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Проверяет, что кнопка "Тема и иконка" отображается на экране.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsThemeAndIconButton() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_theme_and_icon))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопка "Данные приложения" отображается на экране.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsAppDataButton() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_data))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопка "Отправить отзыв" отображается на экране.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsSendFeedbackButton() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.send_feedback))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопка "Оценить приложение" отображается на экране.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsRateAppButton() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.rate_the_app))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопка "Поделиться приложением" отображается на экране.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsShareAppButton() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.share_the_app))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопка "Страница на GitHub" отображается на экране.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsGitHubPageButton() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.github_page))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что версия приложения отображается внизу экрана.
     */
    @Test
    fun moreScreen_whenDisplayed_thenShowsAppVersion() {
        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                MoreScreen()
            }
        }

        // Then
        val expectedVersionText = BuildConfig.VERSION_NAME
        composeTestRule
            .onNodeWithText(
                context.getString(
                    R.string.app_version,
                    expectedVersionText,
                ),
            ).assertIsDisplayed()
    }
}
