package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Тесты для [TimePeriod].
 */
class TimePeriodTest {
    @Test
    fun timeperiod_корректно_хранит_значения() {
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
