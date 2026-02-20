package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для перечисления AppIcon.
 */
@DisplayName("Тесты для AppIcon")
class AppIconTest {
    @Test
    @DisplayName("AppIcon содержит все ожидаемые значения")
    fun appIcon_shouldContainAllExpectedValues() {
        // Given
        val expectedValues =
            listOf(
                AppIcon.DEFAULT,
                AppIcon.ICON_2,
                AppIcon.ICON_3,
                AppIcon.ICON_4,
                AppIcon.ICON_5,
                AppIcon.ICON_6,
            )

        // When
        val actualValues = AppIcon.entries

        // Then
        assertEquals(expectedValues.size, actualValues.size, "Количество иконок должно быть 6")
        assertEquals(
            expectedValues,
            actualValues,
            "Список иконок должен соответствовать ожидаемому"
        )
    }

    @Test
    @DisplayName("Основная иконка должна существовать")
    fun appIcon_shouldHaveDefaultValue() {
        // Then
        assertEquals("DEFAULT", AppIcon.DEFAULT.name)
    }

    @Test
    @DisplayName("Вторая иконка должна существовать")
    fun appIcon_shouldHaveIcon2Value() {
        // Then
        assertEquals("ICON_2", AppIcon.ICON_2.name)
    }

    @Test
    @DisplayName("Третья иконка должна существовать")
    fun appIcon_shouldHaveIcon3Value() {
        // Then
        assertEquals("ICON_3", AppIcon.ICON_3.name)
    }

    @Test
    @DisplayName("Четвёртая иконка должна существовать")
    fun appIcon_shouldHaveIcon4Value() {
        // Then
        assertEquals("ICON_4", AppIcon.ICON_4.name)
    }

    @Test
    @DisplayName("Пятая иконка должна существовать")
    fun appIcon_shouldHaveIcon5Value() {
        // Then
        assertEquals("ICON_5", AppIcon.ICON_5.name)
    }

    @Test
    @DisplayName("Шестая иконка должна существовать")
    fun appIcon_shouldHaveIcon6Value() {
        // Then
        assertEquals("ICON_6", AppIcon.ICON_6.name)
    }

    @Test
    @DisplayName("Количество иконок должно быть 6")
    fun appIcon_shouldHaveSixEntries() {
        // When
        val entriesCount = AppIcon.entries.size

        // Then
        assertEquals(6, entriesCount, "В перечислении должно быть 6 значений")
    }
}
