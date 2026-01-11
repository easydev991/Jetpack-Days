package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для перечисления AppTheme.
 */
@DisplayName("Тесты для AppTheme")
class AppThemeTest {
    @Test
    @DisplayName("AppTheme содержит все ожидаемые значения")
    fun appTheme_shouldContainAllExpectedValues() {
        // Given
        val expectedValues =
            listOf(
                AppTheme.LIGHT,
                AppTheme.DARK,
                AppTheme.SYSTEM,
            )

        // When
        val actualValues = AppTheme.entries

        // Then
        assertEquals(expectedValues.size, actualValues.size, "Количество тем должно быть 3")
        assertEquals(expectedValues, actualValues, "Список тем должен соответствовать ожидаемому")
    }

    @Test
    @DisplayName("Светлая тема должна существовать")
    fun appTheme_shouldHaveLightValue() {
        // Then
        assertEquals("LIGHT", AppTheme.LIGHT.name)
    }

    @Test
    @DisplayName("Тёмная тема должна существовать")
    fun appTheme_shouldHaveDarkValue() {
        // Then
        assertEquals("DARK", AppTheme.DARK.name)
    }

    @Test
    @DisplayName("Системная тема должна существовать")
    fun appTheme_shouldHaveSystemValue() {
        // Then
        assertEquals("SYSTEM", AppTheme.SYSTEM.name)
    }

    @Test
    @DisplayName("Количество тем должно быть 3")
    fun appTheme_shouldHaveThreeEntries() {
        // When
        val entriesCount = AppTheme.entries.size

        // Then
        assertEquals(3, entriesCount, "В перечислении должно быть 3 значения")
    }
}
