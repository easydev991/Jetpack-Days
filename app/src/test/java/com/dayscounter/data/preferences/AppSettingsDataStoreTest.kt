package com.dayscounter.data.preferences

import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для enum моделей настроек.
 */
@DisplayName("Тесты для моделей настроек")
class AppSettingsDataStoreTest {
    @Test
    @DisplayName("AppTheme enum должен содержать все необходимые значения")
    fun appTheme_shouldContainAllValues() {
        // Given & When & Then
        assertEquals(3, AppTheme.entries.size, "Должны быть 3 значения темы")
        assert(AppTheme.entries.contains(AppTheme.LIGHT)) { "Должно содержать LIGHT" }
        assert(AppTheme.entries.contains(AppTheme.DARK)) { "Должно содержать DARK" }
        assert(AppTheme.entries.contains(AppTheme.SYSTEM)) { "Должно содержать SYSTEM" }
    }

    @Test
    @DisplayName("AppIcon enum должен содержать все необходимые значения")
    fun appIcon_shouldContainAllValues() {
        // Given & When & Then
        assertEquals(6, AppIcon.entries.size, "Должны быть 6 значений иконок")
        assert(AppIcon.entries.contains(AppIcon.DEFAULT)) { "Должно содержать DEFAULT" }
        assert(AppIcon.entries.contains(AppIcon.ICON_2)) { "Должно содержать ICON_2" }
        assert(AppIcon.entries.contains(AppIcon.ICON_3)) { "Должно содержать ICON_3" }
        assert(AppIcon.entries.contains(AppIcon.ICON_4)) { "Должно содержать ICON_4" }
        assert(AppIcon.entries.contains(AppIcon.ICON_5)) { "Должно содержать ICON_5" }
        assert(AppIcon.entries.contains(AppIcon.ICON_6)) { "Должно содержать ICON_6" }
    }

    @Test
    @DisplayName("AppTheme.valueOf должен корректно парсить значения")
    fun appTheme_valueOf_shouldParseCorrectly() {
        // Given & When & Then
        assertEquals(AppTheme.LIGHT, AppTheme.valueOf("LIGHT"))
        assertEquals(AppTheme.DARK, AppTheme.valueOf("DARK"))
        assertEquals(AppTheme.SYSTEM, AppTheme.valueOf("SYSTEM"))
    }

    @Test
    @DisplayName("AppIcon.valueOf должен корректно парсить значения")
    fun appIcon_valueOf_shouldParseCorrectly() {
        // Given & When & Then
        assertEquals(AppIcon.DEFAULT, AppIcon.valueOf("DEFAULT"))
        assertEquals(AppIcon.ICON_2, AppIcon.valueOf("ICON_2"))
        assertEquals(AppIcon.ICON_3, AppIcon.valueOf("ICON_3"))
        assertEquals(AppIcon.ICON_4, AppIcon.valueOf("ICON_4"))
        assertEquals(AppIcon.ICON_5, AppIcon.valueOf("ICON_5"))
        assertEquals(AppIcon.ICON_6, AppIcon.valueOf("ICON_6"))
    }

    @Test
    @DisplayName("AppTheme.valueOf должен выбрасывать исключение при некорректном значении")
    fun appTheme_valueOf_shouldThrowExceptionForInvalidValue() {
        // Given & When & Then
        try {
            AppTheme.valueOf("INVALID_THEME")
            assert(false) { "Должно выбросить IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // Expected
            assert(true)
        }
    }

    @Test
    @DisplayName("AppIcon.valueOf должен выбрасывать исключение при некорректном значении")
    fun appIcon_valueOf_shouldThrowExceptionForInvalidValue() {
        // Given & When & Then
        try {
            AppIcon.valueOf("INVALID_ICON")
            assert(false) { "Должно выбросить IllegalArgumentException" }
        } catch (e: IllegalArgumentException) {
            // Expected
            assert(true)
        }
    }

    @Test
    @DisplayName("AppTheme.name должен возвращать корректное строковое представление")
    fun appTheme_name_shouldReturnCorrectStringRepresentation() {
        // Given & When & Then
        assertEquals("LIGHT", AppTheme.LIGHT.name)
        assertEquals("DARK", AppTheme.DARK.name)
        assertEquals("SYSTEM", AppTheme.SYSTEM.name)
    }

    @Test
    @DisplayName("AppIcon.name должен возвращать корректное строковое представление")
    fun appIcon_name_shouldReturnCorrectStringRepresentation() {
        // Given & When & Then
        assertEquals("DEFAULT", AppIcon.DEFAULT.name)
        assertEquals("ICON_2", AppIcon.ICON_2.name)
        assertEquals("ICON_3", AppIcon.ICON_3.name)
        assertEquals("ICON_4", AppIcon.ICON_4.name)
        assertEquals("ICON_5", AppIcon.ICON_5.name)
        assertEquals("ICON_6", AppIcon.ICON_6.name)
    }
}
