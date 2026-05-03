package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для кастомного цвета в CreateEditScreen.
 *
 * Проверяет поведение кастомного цвета при взаимодействии с формой.
 * Статические тесты (начальное состояние) находятся в ColorSelectorUiTest.
 */
@RunWith(AndroidJUnit4::class)
class CreateEditScreenCustomColorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Проверяет, что при выборе preset-цвета он становится выбранным.
     */
    @Test
    fun createEditForm_whenPresetColorSelected_thenSelectsPresetColor() {
        @Suppress("MagicNumber")
        val customColor = Color(0xFF123456)

        var latestSelectedColor: Color? = customColor

        composeTestRule.setContent {
            JetpackDaysTheme {
                var selectedColor by remember { mutableStateOf<Color?>(customColor) }
                latestSelectedColor = selectedColor

                val uiState =
                    CreateEditUiState(
                        title = "Test Title",
                        details = "",
                        selectedDate = java.time.LocalDate.now(),
                        selectedColor = selectedColor,
                        selectedDisplayOption = DisplayOption.DAY
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiState,
                        onShowDatePickerChange = {},
                        onTitleChange = { title -> selectedColor = null },
                        onDetailsChange = {},
                        onColorChange = { newColor ->
                            selectedColor = newColor
                            latestSelectedColor = newColor
                        },
                        onDisplayOptionChange = {},
                        onReminderChange = {},
                        onValueChange = {},
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                        onReminderNotificationsUnavailable = {}
                    )

                CreateEditFormContent(params)
            }
        }

        // When - кликаем на Red (индекс 1 в списке: custom, Red, Teal, Blue, Green, Yellow, Purple)
        val colorDescription = context.getString(R.string.color)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[1] // Red
            .performClick()

        // Then - выбранный цвет должен быть Red
        assert(latestSelectedColor == PresetColors.Red) {
            "Expected Red but got $latestSelectedColor"
        }
    }

    /**
     * Проверяет, что при выборе preset-цвета кастомный чип исчезает.
     */
    @Test
    fun createEditForm_whenPresetColorSelected_thenHidesCustomColorChip() {
        @Suppress("MagicNumber")
        val customColor = Color(0xFF123456)

        composeTestRule.setContent {
            JetpackDaysTheme {
                var selectedColor by remember { mutableStateOf<Color?>(customColor) }

                val uiState =
                    CreateEditUiState(
                        title = "Test Title",
                        details = "",
                        selectedDate = java.time.LocalDate.now(),
                        selectedColor = selectedColor,
                        selectedDisplayOption = DisplayOption.DAY
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiState,
                        onShowDatePickerChange = {},
                        onTitleChange = { selectedColor = null },
                        onDetailsChange = {},
                        onColorChange = { newColor -> selectedColor = newColor },
                        onDisplayOptionChange = {},
                        onReminderChange = {},
                        onValueChange = {},
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                        onReminderNotificationsUnavailable = {}
                    )

                CreateEditFormContent(params)
            }
        }

        // Сначала проверяем, что есть 7 чипов (6 preset + 1 custom)
        val colorDescription = context.getString(R.string.color)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(7)

        // When - кликаем на Red (индекс 1 в списке: custom, Red, Teal, Blue, Green, Yellow, Purple)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[1]
            .performClick()

        // Then - должен остаться только 6 preset чипов (кастомный исчез)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(6)
    }
}
