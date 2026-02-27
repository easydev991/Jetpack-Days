package com.dayscounter.ui.screens.createedit

import androidx.compose.ui.graphics.Color
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты для логики определения кастомного цвета в ColorSelector.
 */
class ColorSelectorUtilsTest {
    private val presetColors = PresetColors.all

    @Test
    fun isCustomColor_whenColorMatchesPreset_thenReturnsFalse() {
        // Given
        val selectedColor = PresetColors.Red

        // When
        val result = isCustomColor(selectedColor, presetColors)

        // Then
        assertFalse(result)
    }

    @Test
    fun isCustomColor_whenColorDoesNotMatchPreset_thenReturnsTrue() {
        // Given
        @Suppress("MagicNumber")
        val customColor = Color(0xFFFF6600) // Оранжевый — не в preset

        // When
        val result = isCustomColor(customColor, presetColors)

        // Then
        assertTrue(result)
    }

    @Test
    fun isCustomColor_whenColorIsNull_thenReturnsFalse() {
        // Given
        val selectedColor: Color? = null

        // When
        val result = isCustomColor(selectedColor, presetColors)

        // Then
        assertFalse(result)
    }

    @Test
    fun isCustomColor_whenColorMatchesAnotherPreset_thenReturnsFalse() {
        // Given
        val selectedColor = PresetColors.Purple

        // When
        val result = isCustomColor(selectedColor, presetColors)

        // Then
        assertFalse(result)
    }

    @Test
    fun isCustomColor_whenSimilarButNotExactColor_thenReturnsTrue() {
        // Given
        @Suppress("MagicNumber")
        val similarColor = Color(0xFFE53936) // Почти Red, но последний байт отличается

        // When
        val result = isCustomColor(similarColor, presetColors)

        // Then
        assertTrue(result)
    }

    @Test
    fun isCustomColor_whenEmptyPresetList_thenReturnTrue() {
        // Given
        val emptyPreset = emptyList<Color>()
        val anyColor = Color.Red

        // When
        val result = isCustomColor(anyColor, emptyPreset)

        // Then
        assertTrue(result)
    }
}
