package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI тесты для ColorSelector.
 *
 * Проверяет отображение кастомного цвета и взаимодействие с селектором.
 */
class ColorSelectorUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Проверяет, что при выборе кастомного цвета появляется дополнительный чип.
     */
    @Test
    fun colorSelector_whenCustomColorSelected_thenShowsCustomColorChip() {
        // Given
        @Suppress("MagicNumber")
        val customColor = Color(0xFFFF6600) // Оранжевый — не в preset
        val selectedColor = mutableStateOf<Color?>(customColor)

        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(selectedColor = selectedColor)
            }
        }

        // Then - должен быть 7 чипов (6 preset + 1 custom)
        composeTestRule
            .onAllNodesWithContentDescription("Цвет")
            .assertCountEquals(7)
    }

    /**
     * Проверяет, что при выборе предустановленного цвета кастомный чип не отображается.
     */
    @Test
    fun colorSelector_whenPresetColorSelected_thenHidesCustomColorChip() {
        // Given
        val presetColor = PresetColors.Red
        val selectedColor = mutableStateOf<Color?>(presetColor)

        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(selectedColor = selectedColor)
            }
        }

        // Then - должен быть только 6 preset чипов
        composeTestRule
            .onAllNodesWithContentDescription("Цвет")
            .assertCountEquals(6)
    }

    /**
     * Проверяет, что при null цвете кастомный чип не отображается.
     */
    @Test
    fun colorSelector_whenNoColorSelected_thenHidesCustomColorChip() {
        // Given
        val selectedColor = mutableStateOf<Color?>(null)

        // When
        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(selectedColor = selectedColor)
            }
        }

        // Then - должен быть только 6 preset чипов
        composeTestRule
            .onAllNodesWithContentDescription("Цвет")
            .assertCountEquals(6)
    }

    /**
     * Проверяет, что при клике на кастомный цвет он снимается.
     */
    @Test
    fun colorSelector_whenCustomColorClicked_thenDeselects() {
        // Given
        @Suppress("MagicNumber")
        val customColor = Color(0xFFFF6600) // Оранжевый
        val selectedColor = mutableStateOf<Color?>(customColor)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(selectedColor = selectedColor)
            }
        }

        // When - кликаем на первый чип (кастомный цвет в начале списка)
        composeTestRule
            .onAllNodesWithContentDescription("Цвет")[0]
            .performClick()

        // Then - цвет должен быть null
        assert(selectedColor.value == null)
    }
}
