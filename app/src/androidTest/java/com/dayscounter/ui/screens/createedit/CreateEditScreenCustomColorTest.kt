package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
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
        // Given - сначала выбран кастомный цвет
        @Suppress("MagicNumber")
        val customColor = Color(0xFF123456)

        var selectedColorStateHolder: MutableState<Color?>? = null

        composeTestRule.setContent {
            JetpackDaysTheme {
                val title = rememberSaveable { mutableStateOf("Test Title") }
                val details = rememberSaveable { mutableStateOf("") }
                val selectedDate = rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
                val selectedColorState = remember { mutableStateOf<Color?>(customColor) }
                selectedColorStateHolder = selectedColorState
                val selectedDisplayOption =
                    rememberSaveable { mutableStateOf(DisplayOption.DAY) }
                val showDatePicker = remember { mutableStateOf(false) }

                val uiStates =
                    CreateEditUiState(
                        title = title,
                        details = details,
                        selectedDate = selectedDate,
                        selectedColor = selectedColorState,
                        selectedDisplayOption = selectedDisplayOption
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiStates,
                        showDatePicker = showDatePicker,
                        viewModel = createTestViewModel(),
                        onBackClick = {}
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
        assert(selectedColorStateHolder?.value == PresetColors.Red) {
            "Expected Red but got ${selectedColorStateHolder?.value}"
        }
    }

    /**
     * Проверяет, что при выборе preset-цвета кастомный чип исчезает.
     */
    @Test
    fun createEditForm_whenPresetColorSelected_thenHidesCustomColorChip() {
        // Given - сначала выбран кастомный цвет (7 чипов)
        @Suppress("MagicNumber")
        val customColor = Color(0xFF123456)

        composeTestRule.setContent {
            JetpackDaysTheme {
                val title = rememberSaveable { mutableStateOf("Test Title") }
                val details = rememberSaveable { mutableStateOf("") }
                val selectedDate = rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
                val selectedColor = remember { mutableStateOf<Color?>(customColor) }
                val selectedDisplayOption =
                    rememberSaveable { mutableStateOf(DisplayOption.DAY) }
                val showDatePicker = remember { mutableStateOf(false) }

                val uiStates =
                    CreateEditUiState(
                        title = title,
                        details = details,
                        selectedDate = selectedDate,
                        selectedColor = selectedColor,
                        selectedDisplayOption = selectedDisplayOption
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiStates,
                        showDatePicker = showDatePicker,
                        viewModel = createTestViewModel(),
                        onBackClick = {}
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
