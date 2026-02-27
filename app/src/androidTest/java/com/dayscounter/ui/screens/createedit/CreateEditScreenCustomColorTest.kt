package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.PaddingValues
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
 * Проверяет отображение кастомного цвета в форме редактирования
 * и его поведение при выборе preset-цвета.
 */
@RunWith(AndroidJUnit4::class)
class CreateEditScreenCustomColorTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Проверяет, что при открытии формы с кастомным цветом
     * отображается 7 чипов (6 preset + 1 custom).
     */
    @Test
    fun createEditForm_whenCustomColorInitiallySet_thenShowsCustomColorChip() {
        // Given - кастомный цвет (оранжевый, не в preset)
        @Suppress("MagicNumber")
        val customColor = Color(0xFFFF6600)

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
                        selectedDisplayOption = selectedDisplayOption,
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiStates,
                        showDatePicker = showDatePicker,
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                    )

                CreateEditFormContent(params)
            }
        }

        // Then - должен быть 7 чипов (6 preset + 1 custom)
        val colorDescription = context.getString(R.string.color)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(7)
    }

    /**
     * Проверяет, что при выборе preset-цвета кастомный чип исчезает.
     */
    @Test
    fun createEditForm_whenPresetColorSelected_thenHidesCustomColorChip() {
        // Given - сначала выбран кастомный цвет
        @Suppress("MagicNumber")
        val customColor = Color(0xFFFF6600)

        composeTestRule.setContent {
            JetpackDaysTheme {
                val title = rememberSaveable { mutableStateOf("Test Title") }
                val details = rememberSaveable { mutableStateOf("") }
                val selectedDate = rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
                val selectedColorState = remember { mutableStateOf<Color?>(customColor) }
                val selectedDisplayOption =
                    rememberSaveable { mutableStateOf(DisplayOption.DAY) }
                val showDatePicker = remember { mutableStateOf(false) }

                val uiStates =
                    CreateEditUiState(
                        title = title,
                        details = details,
                        selectedDate = selectedDate,
                        selectedColor = selectedColorState,
                        selectedDisplayOption = selectedDisplayOption,
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiStates,
                        showDatePicker = showDatePicker,
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                    )

                CreateEditFormContent(params)
            }
        }

        // When - кликаем на последний чип (preset цвет)
        val colorDescription = context.getString(R.string.color)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)[6] // Последний preset
            .performClick()

        // Then - должен быть только 6 preset чипов (кастомный исчез)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(6)
    }

    /**
     * Проверяет, что при открытии формы с preset-цветом
     * отображается только 6 чипов.
     */
    @Test
    fun createEditForm_whenPresetColorInitiallySet_thenShowsOnlyPresetChips() {
        // Given - preset цвет (красный)
        val presetColor = PresetColors.Red

        composeTestRule.setContent {
            JetpackDaysTheme {
                val title = rememberSaveable { mutableStateOf("Test Title") }
                val details = rememberSaveable { mutableStateOf("") }
                val selectedDate = rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
                val selectedColor = remember { mutableStateOf<Color?>(presetColor) }
                val selectedDisplayOption =
                    rememberSaveable { mutableStateOf(DisplayOption.DAY) }
                val showDatePicker = remember { mutableStateOf(false) }

                val uiStates =
                    CreateEditUiState(
                        title = title,
                        details = details,
                        selectedDate = selectedDate,
                        selectedColor = selectedColor,
                        selectedDisplayOption = selectedDisplayOption,
                    )

                val params =
                    CreateEditFormParams(
                        itemId = 1L,
                        paddingValues = PaddingValues(),
                        uiStates = uiStates,
                        showDatePicker = showDatePicker,
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                    )

                CreateEditFormContent(params)
            }
        }

        // Then - должен быть только 6 preset чипов
        val colorDescription = context.getString(R.string.color)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(6)
    }

    /**
     * Проверяет, что при открытии формы без цвета
     * отображается только 6 preset чипов.
     */
    @Test
    fun createEditForm_whenNoColorInitiallySet_thenShowsOnlyPresetChips() {
        // Given - цвет не выбран

        composeTestRule.setContent {
            JetpackDaysTheme {
                val title = rememberSaveable { mutableStateOf("Test Title") }
                val details = rememberSaveable { mutableStateOf("") }
                val selectedDate = rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
                val selectedColor = remember { mutableStateOf<Color?>(null) }
                val selectedDisplayOption =
                    rememberSaveable { mutableStateOf(DisplayOption.DAY) }
                val showDatePicker = remember { mutableStateOf(false) }

                val uiStates =
                    CreateEditUiState(
                        title = title,
                        details = details,
                        selectedDate = selectedDate,
                        selectedColor = selectedColor,
                        selectedDisplayOption = selectedDisplayOption,
                    )

                val params =
                    CreateEditFormParams(
                        itemId = null,
                        paddingValues = PaddingValues(),
                        uiStates = uiStates,
                        showDatePicker = showDatePicker,
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                    )

                CreateEditFormContent(params)
            }
        }

        // Then - должен быть только 6 preset чипов
        val colorDescription = context.getString(R.string.color)
        composeTestRule
            .onAllNodesWithContentDescription(colorDescription)
            .assertCountEquals(6)
    }
}
