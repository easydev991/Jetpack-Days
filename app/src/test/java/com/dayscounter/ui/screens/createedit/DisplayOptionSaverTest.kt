package com.dayscounter.ui.screens.createedit

import com.dayscounter.domain.model.DisplayOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Тесты для логики сохранения/восстановления DisplayOption.
 */
class DisplayOptionSaverTest {
    private fun saveDisplayOption(option: DisplayOption): String = option.name

    @Suppress("SwallowedException")
    private fun restoreDisplayOption(name: String): DisplayOption =
        try {
            DisplayOption.valueOf(name)
        } catch (e: IllegalArgumentException) {
            DisplayOption.DAY
        }

    @Test
    fun saveAndRestore_whenValidDisplayOption_thenRestoresCorrectly() {
        // Given
        val originalOption = DisplayOption.DAY

        // When
        val saved = saveDisplayOption(originalOption)
        val restored = restoreDisplayOption(saved)

        // Then
        assertEquals(originalOption, restored)
    }

    @Test
    fun save_whenDifferentOptions_thenSavesDifferentNames() {
        // Given
        val option1 = DisplayOption.DAY
        val option2 = DisplayOption.MONTH_DAY

        // When
        val saved1 = saveDisplayOption(option1)
        val saved2 = saveDisplayOption(option2)

        // Then
        assertNotEquals(saved1, saved2)
    }

    @Test
    fun saveAndRestore_whenMonthDay_thenRestoresCorrectly() {
        // Given
        val originalOption = DisplayOption.MONTH_DAY

        // When
        val saved = saveDisplayOption(originalOption)
        val restored = restoreDisplayOption(saved)

        // Then
        assertEquals(originalOption, restored)
    }

    @Test
    fun saveAndRestore_whenYearMonthDay_thenRestoresCorrectly() {
        // Given
        val originalOption = DisplayOption.YEAR_MONTH_DAY

        // When
        val saved = saveDisplayOption(originalOption)
        val restored = restoreDisplayOption(saved)

        // Then
        assertEquals(originalOption, restored)
    }

    @Test
    fun restore_whenInvalidName_thenReturnsDefault() {
        // Given
        val invalidName = "INVALID_OPTION"

        // When
        val restored = restoreDisplayOption(invalidName)

        // Then
        assertEquals(DisplayOption.DAY, restored)
    }
}
