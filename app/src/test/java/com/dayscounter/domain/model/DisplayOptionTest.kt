package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class DisplayOptionTest {

    @Test
    fun `fromString_whenDay_thenReturnsDay`() {
        // When
        val result = DisplayOption.fromString("day")

        // Then
        assertEquals(DisplayOption.DAY, result)
    }

    @Test
    fun `fromString_whenMonthDay_thenReturnsMonthDay`() {
        // When
        val result = DisplayOption.fromString("monthDay")

        // Then
        assertEquals(DisplayOption.MONTH_DAY, result)
    }

    @Test
    fun `fromString_whenYearMonthDay_thenReturnsYearMonthDay`() {
        // When
        val result = DisplayOption.fromString("yearMonthDay")

        // Then
        assertEquals(DisplayOption.YEAR_MONTH_DAY, result)
    }

    @Test
    fun `fromString_whenUnknownValue_thenReturnsDefault`() {
        // When
        val result = DisplayOption.fromString("unknown")

        // Then
        assertEquals(DisplayOption.DEFAULT, result)
    }

    @Test
    fun `fromString_whenCaseInsensitive_thenReturnsCorrectValue`() {
        // When
        val result1 = DisplayOption.fromString("DAY")
        val result2 = DisplayOption.fromString("MonthDay")
        val result3 = DisplayOption.fromString("YEARMONTHDAY")

        // Then
        assertEquals(DisplayOption.DAY, result1)
        assertEquals(DisplayOption.MONTH_DAY, result2)
        assertEquals(DisplayOption.YEAR_MONTH_DAY, result3)
    }

    @Test
    fun `toJsonString_whenDay_thenReturnsDay`() {
        // When
        val result = DisplayOption.DAY.toJsonString()

        // Then
        assertEquals("day", result)
    }

    @Test
    fun `toJsonString_whenMonthDay_thenReturnsMonthDay`() {
        // When
        val result = DisplayOption.MONTH_DAY.toJsonString()

        // Then
        assertEquals("monthDay", result)
    }

    @Test
    fun `toJsonString_whenYearMonthDay_thenReturnsYearMonthDay`() {
        // When
        val result = DisplayOption.YEAR_MONTH_DAY.toJsonString()

        // Then
        assertEquals("yearMonthDay", result)
    }

    @Test
    fun `defaultValue_isDay`() {
        // Then
        assertEquals(DisplayOption.DAY, DisplayOption.DEFAULT)
    }
}

