package com.dayscounter.ui.ds

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

/**
 * Компонентные тесты для [DaysCountText].
 *
 * Проверяет корректность отображения текста, применение стилей
 * и работу с различными текстовыми строками.
 */
class DaysCountTextTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Проверяет, что компонент корректно отображает переданный текст.
     */
    @Test
    fun daysCountText_whenDisplayed_thenShowsCorrectText() {
        // Given
        val testText = "5 дней"

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    /**
     * Проверяет, что компонент корректно отображает "Сегодня".
     */
    @Test
    fun daysCountText_whenToday_thenShowsToday() {
        // Given
        val testText = "Сегодня"

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    /**
     * Проверяет, что компонент корректно отображает составной формат.
     */
    @Test
    fun daysCountText_whenCompositeFormat_thenShowsCorrectText() {
        // Given
        val testText = "1 год 2 месяца 5 дней"

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    /**
     * Проверяет, что компонент корректно отображает сокращенный формат.
     */
    @Test
    fun daysCountText_whenAbbreviatedFormat_thenShowsCorrectText() {
        // Given
        val testText = "2 мес. 5 дн."

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    /**
     * Проверяет, что компонент корректно работает с английским текстом.
     */
    @Test
    fun daysCountText_whenEnglishText_thenShowsCorrectText() {
        // Given
        val testText = "Today"

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    /**
     * Проверяет, что компонент корректно работает с составным английским форматом.
     */
    @Test
    fun daysCountText_whenEnglishComposite_thenShowsCorrectText() {
        // Given
        val testText = "1 year 2 months 5 days"

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
            .assertTextEquals(testText)
    }

    /**
     * Проверяет, что компонент корректно обрабатывает пустую строку.
     */
    @Test
    fun daysCountText_whenEmptyText_thenShowsEmpty() {
        // Given
        val testText = ""

        // When
        composeTestRule.setContent {
            DaysCountText(
                formattedText = testText,
            )
        }

        // Then
        composeTestRule
            .onNodeWithText(testText)
            .assertExists()
    }
}
