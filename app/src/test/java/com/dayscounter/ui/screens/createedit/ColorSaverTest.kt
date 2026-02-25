package com.dayscounter.ui.screens.createedit

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Тесты для логики сохранения/восстановления Color.
 */
class ColorSaverTest {
    private fun saveColor(color: Color?): Int = color?.toArgb() ?: -1

    private fun restoreColor(argb: Int): Color? =
        if (argb == -1) {
            null
        } else {
            Color(argb)
        }

    @Test
    fun saveAndRestore_whenValidColor_thenRestoresCorrectly() {
        // Given
        val originalColor = Color.Red

        // When
        val saved = saveColor(originalColor)
        val restored = restoreColor(saved)

        // Then
        assertEquals(originalColor, restored)
    }

    @Test
    fun saveAndRestore_whenNullColor_thenRestoresNull() {
        // Given
        val originalColor: Color? = null

        // When
        val saved = saveColor(originalColor)
        val restored = restoreColor(saved)

        // Then
        assertNull(restored)
    }

    @Test
    fun save_whenDifferentColors_thenSavesDifferentArgb() {
        // Given
        val color1 = Color.Red
        val color2 = Color.Blue

        // When
        val saved1 = saveColor(color1)
        val saved2 = saveColor(color2)

        // Then
        assert(saved1 != saved2)
    }

    @Test
    fun saveAndRestore_whenCustomColor_thenRestoresCorrectly() {
        // Given
        val customColor = Color(0.5f, 0.3f, 0.8f, 1.0f)

        // When
        val saved = saveColor(customColor)
        val restored = restoreColor(saved)

        // Then
        assertEquals(customColor, restored)
    }

    @Test
    fun saveAndRestore_whenTransparentColor_thenRestoresCorrectly() {
        // Given
        val transparentColor = Color(0.5f, 0.3f, 0.8f, 0.5f)

        // When
        val saved = saveColor(transparentColor)
        val restored = restoreColor(saved)

        // Then
        assertEquals(transparentColor, restored)
    }
}
