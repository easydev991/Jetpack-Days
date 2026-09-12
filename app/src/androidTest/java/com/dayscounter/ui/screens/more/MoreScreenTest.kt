package com.dayscounter.ui.screens.more

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.BuildConfig
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Assume
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
    fun more_screen_when_displayed_then_shows_theme_and_icon_button() {
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
    fun more_screen_when_displayed_then_shows_app_data_button() {
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
    fun more_screen_when_displayed_then_shows_send_feedback_button() {
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
     * Проверяет, что кнопки "Оценить приложение" и "Поделиться приложением"
     * отображаются на экране при `BuildConfig.RUSTORE_FEATURES = true` (flavor rustore).
     * На flavor github тест пропускается через Assume.
     */
    @Test
    fun moreScreen_when_rustore_features_true_then_shows_rate_and_share_buttons() {
        // SKIPPED на flavor github
        Assume.assumeTrue(BuildConfig.RUSTORE_FEATURES)

        // Given: экран MoreScreen отрендерен при BuildConfig.RUSTORE_FEATURES=true (flavor rustore)
        composeTestRule.setContent { JetpackDaysTheme { MoreScreen() } }

        // Then: rate и share кнопки видны
        composeTestRule
            .onNodeWithText(context.getString(R.string.rate_the_app))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(context.getString(R.string.share_the_app))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопки "Оценить приложение" и "Поделиться приложением"
     * скрыты на экране при `BuildConfig.RUSTORE_FEATURES = false` (flavor github).
     * На flavor rustore тест пропускается через Assume.
     */
    @Test
    fun moreScreen_when_rustore_features_false_then_hides_rate_and_share_buttons() {
        // SKIPPED на flavor rustore
        Assume.assumeFalse(BuildConfig.RUSTORE_FEATURES)

        // Given: экран MoreScreen отрендерен при BuildConfig.RUSTORE_FEATURES=false (flavor github)
        composeTestRule.setContent { JetpackDaysTheme { MoreScreen() } }

        // Then: rate и share кнопки скрыты
        composeTestRule
            .onNodeWithText(context.getString(R.string.rate_the_app))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(context.getString(R.string.share_the_app))
            .assertDoesNotExist()
    }

    /**
     * Проверяет, что кнопка "Страница на GitHub" отображается на экране.
     */
    @Test
    fun more_screen_when_displayed_then_shows_github_page_button() {
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
     * Проверяет, что кнопка "Проверить обновления" отображается на экране
     * при `BuildConfig.RUSTORE_FEATURES = false` (flavor github).
     * На flavor rustore тест пропускается через Assume.
     */
    @Test
    fun checkForUpdatesButton_visible_when_github_flavor() {
        // SKIPPED на flavor rustore
        Assume.assumeFalse(BuildConfig.RUSTORE_FEATURES)

        // Given: экран MoreScreen отрендерен при BuildConfig.RUSTORE_FEATURES=false (flavor github)
        composeTestRule.setContent { JetpackDaysTheme { MoreScreen() } }

        // Then: кнопка проверки обновлений видна
        composeTestRule
            .onNodeWithText(context.getString(R.string.check_for_updates))
            .assertIsDisplayed()
    }

    /**
     * Проверяет, что кнопка "Проверить обновления" скрыта на экране
     * при `BuildConfig.RUSTORE_FEATURES = true` (flavor rustore).
     * На flavor github тест пропускается через Assume.
     */
    @Test
    fun checkForUpdatesButton_hidden_when_rustore_flavor() {
        // SKIPPED на flavor github
        Assume.assumeTrue(BuildConfig.RUSTORE_FEATURES)

        // Given: экран MoreScreen отрендерен при BuildConfig.RUSTORE_FEATURES=true (flavor rustore)
        composeTestRule.setContent { JetpackDaysTheme { MoreScreen() } }

        // Then: кнопка проверки обновлений скрыта
        composeTestRule
            .onNodeWithText(context.getString(R.string.check_for_updates))
            .assertDoesNotExist()
    }

    /**
     * Проверяет, что версия приложения отображается внизу экрана.
     */
    @Test
    fun more_screen_when_displayed_then_shows_app_version() {
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
                    expectedVersionText
                )
            ).assertIsDisplayed()
    }
}
