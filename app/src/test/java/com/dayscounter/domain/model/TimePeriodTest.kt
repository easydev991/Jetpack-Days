package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты для [TimePeriod].
 */
class TimePeriodTest {
    @Test
    fun `isEmpty возвращает true при всех нулях`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 0)

        // When
        val result = period.isEmpty()

        // Then
        assertTrue(result, "Период должен быть пустым при всех нулях")
    }

    @Test
    fun `isEmpty возвращает false при ненулевом period`() {
        // Given - ненулевые годы
        val period1 = TimePeriod(years = 1, months = 0, days = 0)
        val period2 = TimePeriod(years = 0, months = 1, days = 0)
        val period3 = TimePeriod(years = 0, months = 0, days = 1)
        val period4 = TimePeriod(years = 1, months = 2, days = 3)

        // When
        val result1 = period1.isEmpty()
        val result2 = period2.isEmpty()
        val result3 = period3.isEmpty()
        val result4 = period4.isEmpty()

        // Then
        assertFalse(result1, "Период не должен быть пустым при ненулевых годах")
        assertFalse(result2, "Период не должен быть пустым при ненулевых месяцах")
        assertFalse(result3, "Период не должен быть пустым при ненулевых днях")
        assertFalse(result4, "Период не должен быть пустым при ненулевых значениях")
    }

    @Test
    fun `isNotEmpty возвращает true при ненулевом period`() {
        // Given
        val period = TimePeriod(years = 1, months = 0, days = 0)

        // When
        val result = period.isNotEmpty()

        // Then
        assertTrue(result, "Период должен быть не пустым при ненулевых годах")
    }

    @Test
    fun `isNotEmpty возвращает false при всех нулях`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 0)

        // When
        val result = period.isNotEmpty()

        // Then
        assertFalse(result, "Период не должен быть не пустым при всех нулях")
    }

    @Test
    fun `isNotEmpty обратный метод для isEmpty`() {
        // Given
        val periodEmpty = TimePeriod(years = 0, months = 0, days = 0)
        val periodNotEmpty = TimePeriod(years = 1, months = 0, days = 0)

        // When
        val emptyResult = periodEmpty.isNotEmpty()
        val notEmptyResult = periodNotEmpty.isNotEmpty()

        // Then
        assertEquals(periodEmpty.isEmpty(), !emptyResult, "isNotEmpty должен быть обратным isEmpty")
        assertEquals(periodNotEmpty.isEmpty(), !notEmptyResult, "isNotEmpty должен быть обратным isEmpty")
    }

    @Test
    fun `TimePeriod корректно хранит значения`() {
        // Given
        val years = 5
        val months = 3
        val days = 12

        // When
        val period = TimePeriod(years = years, months = months, days = days)

        // Then
        assertEquals(years, period.years, "Значение years должно совпадать")
        assertEquals(months, period.months, "Значение months должно совпадать")
        assertEquals(days, period.days, "Значение days должно совпадать")
    }
}
