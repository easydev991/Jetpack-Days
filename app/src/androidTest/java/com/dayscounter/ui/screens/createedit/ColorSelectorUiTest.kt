package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
 * ColorSelector принимает plain-значение и callback.
 */
class ColorSelectorUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Проверяет, что при выборе кастомного цвета появляется дополнительный чип.
     */
    @Test
    fun colorSelector_whenCustomColorSelected_thenShowsCustomColorChip() {
        @Suppress("MagicNumber")
        val customColor = Color(0xFF123456)
        var selectedColor by mutableStateOf<Color?>(customColor)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Цвет")
            .assertCountEquals(7)
    }

    /**
     * Проверяет, что при выборе предустановленного цвета кастомный чип не отображается.
     */
    @Test
    fun colorSelector_whenPresetColorSelected_thenHidesCustomColorChip() {
        var selectedColor by mutableStateOf<Color?>(PresetColors.Red)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Цвет")
            .assertCountEquals(6)
    }

    /**
     * Проверяет, что при null цвете кастомный чип не отображается.
     */
    @Test
    fun colorSelector_whenNoColorSelected_thenHidesCustomColorChip() {
        var selectedColor by mutableStateOf<Color?>(null)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Цвет")
            .assertCountEquals(6)
    }

    /**
     * Проверяет, что при клике на кастомный цвет он снимается.
     */
    @Test
    fun colorSelector_whenCustomColorClicked_thenDeselects() {
        @Suppress("MagicNumber")
        val customColor = Color(0xFF123456)
        var selectedColor by mutableStateOf<Color?>(customColor)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ColorSelector(
                    selectedColor = selectedColor,
                    onColorSelected = { selectedColor = it }
                )
            }
        }

        composeTestRule
            .onAllNodesWithContentDescription("Цвет")[0]
            .performClick()

        assert(selectedColor == null)
    }
}
