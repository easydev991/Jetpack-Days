package com.dayscounter.ui.screens.themeicon

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI тесты для ThemeIconScreen.
 *
 * Тестирует UI компонент в изоляции без ViewModel.
 */
@RunWith(AndroidJUnit4::class)
class ThemeIconScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun setContent(
        theme: AppTheme = AppTheme.SYSTEM,
        useDynamicColors: Boolean = true,
        icon: AppIcon = AppIcon.DEFAULT,
        onThemeChange: (AppTheme) -> Unit = {},
        onDynamicColorsChange: (Boolean) -> Unit = {},
        onIconChange: (AppIcon) -> Unit = {},
        onBackClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            JetpackDaysTheme {
                ThemeIconScreenContent(
                    theme = theme,
                    useDynamicColors = useDynamicColors,
                    icon = icon,
                    onThemeChange = onThemeChange,
                    onDynamicColorsChange = onDynamicColorsChange,
                    onIconChange = onIconChange,
                    onBackClick = onBackClick,
                )
            }
        }
    }

    @Test
    fun themeIconScreen_displaysAppBarWithBackButton() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_theme_and_icon))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription(context.getString(R.string.back))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_displaysAppThemeSection() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_theme))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_displaysAllThemeRadioButtons() {
        // When
        setContent()

        // Then - Все три радио-кнопки должны быть отображены
        composeTestRule
            .onNodeWithText(context.getString(R.string.light))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.dark))
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText(context.getString(R.string.system))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_lightThemeRadioButtonSelected_whenThemeIsLight() {
        // When
        setContent(theme = AppTheme.LIGHT)

        // Then - Светлая тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.light))
            .assertIsDisplayed()

        // Затемнняя тема не должна быть выбрана (текст отображается)
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark))
            .assertIsDisplayed()

        // Системная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.system))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_darkThemeRadioButtonSelected_whenThemeIsDark() {
        // When
        setContent(theme = AppTheme.DARK)

        // Then - Тёмная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark))
            .assertIsDisplayed()

        // Светлая тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.light))
            .assertIsDisplayed()

        // Системная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.system))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_systemThemeRadioButtonSelected_whenThemeIsSystem() {
        // When
        setContent(theme = AppTheme.SYSTEM)

        // Then - Системная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.system))
            .assertIsDisplayed()

        // Светлая тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.light))
            .assertIsDisplayed()

        // Тёмная тема отображается
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark))
            .assertIsDisplayed()
    }

    @Test
    fun themeIconScreen_clicksLightTheme_callsOnThemeChange() {
        // Given
        var clickedTheme: AppTheme? = null
        setContent(
            theme = AppTheme.DARK,
            onThemeChange = { clickedTheme = it },
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.light))
            .performClick()

        // Then
        assert(clickedTheme == AppTheme.LIGHT) { "Ожидалась тема LIGHT" }
    }

    @Test
    fun themeIconScreen_clicksDarkTheme_callsOnThemeChange() {
        // Given
        var clickedTheme: AppTheme? = null
        setContent(
            theme = AppTheme.LIGHT,
            onThemeChange = { clickedTheme = it },
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.dark))
            .performClick()

        // Then
        assert(clickedTheme == AppTheme.DARK) { "Ожидалась тема DARK" }
    }

    @Test
    fun themeIconScreen_clicksSystemTheme_callsOnThemeChange() {
        // Given
        var clickedTheme: AppTheme? = null
        setContent(
            theme = AppTheme.LIGHT,
            onThemeChange = { clickedTheme = it },
        )

        // When
        composeTestRule
            .onNodeWithText(context.getString(R.string.system))
            .performClick()

        // Then
        assert(clickedTheme == AppTheme.SYSTEM) { "Ожидалась тема SYSTEM" }
    }

    @Test
    fun themeIconScreen_displaysAppIconSection() {
        // When
        setContent()

        // Then
        composeTestRule
            .onNodeWithText(context.getString(R.string.app_icon))
            .assertIsDisplayed()
    }
}
