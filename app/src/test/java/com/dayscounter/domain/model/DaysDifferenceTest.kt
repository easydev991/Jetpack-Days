package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты для [DaysDifference].
 */
class DaysDifferenceTest {
    @Test
    fun today_корректно_хранит_timestamp() {
        // Given
        val timestamp = System.currentTimeMillis()

        // When
        val difference = DaysDifference.Today(timestamp)

        // Then
        assertEquals(timestamp, difference.timestamp, "Timestamp должен совпадать")
    }

    @Test
    fun calculated_корректно_хранит_период() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 3)
        val totalDays = 400
        val timestamp = System.currentTimeMillis()

        // When
        val difference = DaysDifference.Calculated(period, totalDays, timestamp)

        // Then
        assertEquals(period, difference.period, "Период должен совпадать")
        assertEquals(totalDays, difference.totalDays, "Общее количество дней должно совпадать")
        assertEquals(timestamp, difference.timestamp, "Timestamp должен совпадать")
    }

    @Test
    fun daysdifference_поддерживает_when_выражение() {
        // Given
        val differences: List<DaysDifference> =
            listOf(
                DaysDifference.Today(System.currentTimeMillis()),
                DaysDifference.Calculated(TimePeriod(), 100, System.currentTimeMillis())
            )

        // When
        val results =
            differences.map { diff ->
                when (diff) {
                    is DaysDifference.Today -> "today"
                    is DaysDifference.Calculated -> "calculated"
                }
            }

        // Then
        assertEquals("today", results[0], "Today должен правильно определяться в when")
        assertEquals("calculated", results[1], "Calculated должен правильно определяться в when")
    }

    @Test
    fun calculated_с_нулевым_периодом_сохраняется_корректно() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 0)
        val totalDays = 0
        val timestamp = System.currentTimeMillis()

        // When
        val difference = DaysDifference.Calculated(period, totalDays, timestamp)

        // Then
        assertTrue(difference.period.isEmpty(), "Период должен быть пустым")
        assertEquals(0, difference.totalDays, "Общее количество дней должно быть 0")
    }

    @Test
    fun calculated_с_периодом_из_одних_лет_сохраняется_корректно() {
        // Given
        val period = TimePeriod(years = 5, months = 0, days = 0)

        // When
        val difference = DaysDifference.Calculated(period, 1826, System.currentTimeMillis())

        // Then
        assertEquals(5, difference.period.years, "Количество лет должно быть 5")
        assertEquals(0, difference.period.months, "Количество месяцев должно быть 0")
        assertEquals(0, difference.period.days, "Количество дней должно быть 0")
    }
}
