package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DaysDifference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Тесты для [CalculateDaysDifferenceUseCase].
 */
class CalculateDaysDifferenceUseCaseTest {
    private val useCase = CalculateDaysDifferenceUseCase()

    @Test
    fun calculate_when_same_day_then_returns_today() {
        // Given
        val today = LocalDate.now()
        val timestamp = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp)

        // Then
        assertTrue(result is DaysDifference.Today, "Результат должен быть Today")
    }

    @Test
    fun calculate_when_1_day_difference_then_returns_1_day() {
        // Given
        val currentDate = LocalDate.now()
        val eventDate = currentDate.minusDays(1)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.days, "Должен быть 1 день")
        assertEquals(0, calculated.period.months, "Месяцев быть не должно")
        assertEquals(0, calculated.period.years, "Лет быть не должно")
    }

    @Test
    fun calculate_when_5_days_difference_then_returns_5_days() {
        // Given
        val currentDate = LocalDate.now()
        val eventDate = currentDate.minusDays(5)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(5, calculated.period.days, "Должно быть 5 дней")
        assertEquals(0, calculated.period.months, "Месяцев быть не должно")
        assertEquals(0, calculated.period.years, "Лет быть не должно")
    }

    @Test
    fun calculate_when_30_days_difference_then_returns_30_days() {
        // Given - используем даты в пределах одного месяца для точного подсчёта 30 дней
        val currentDate = LocalDate.of(2024, 3, 31)
        val eventDate = LocalDate.of(2024, 3, 1)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(30, calculated.period.days, "Должно быть 30 дней")
        assertEquals(0, calculated.period.months, "Месяцев быть не должно")
        assertEquals(0, calculated.period.years, "Лет быть не должно")
    }

    @Test
    fun calculate_when_365_days_difference_then_returns_1_year() {
        // Given
        val currentDate = LocalDate.now()
        val eventDate = currentDate.minusDays(365)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertTrue(calculated.period.years > 0, "Должен быть хотя бы 1 год")
    }

    @Test
    fun calculate_when_1_month_then_returns_1_month_0_days() {
        // Given
        val currentDate = LocalDate.of(2024, 1, 15)
        val eventDate = LocalDate.of(2023, 12, 15)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.months, "Должен быть 1 месяц")
        assertEquals(0, calculated.period.days, "Дней быть не должно")
        assertEquals(0, calculated.period.years, "Лет быть не должно")
    }

    @Test
    fun calculate_when_2_months_5_days_then_returns_2_months_5_days() {
        // Given
        val currentDate = LocalDate.of(2024, 3, 20)
        val eventDate = LocalDate.of(2024, 1, 15)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(2, calculated.period.months, "Должно быть 2 месяца")
        assertEquals(5, calculated.period.days, "Должно быть 5 дней")
        assertEquals(0, calculated.period.years, "Лет быть не должно")
    }

    @Test
    fun calculate_when_11_months_30_days_then_returns_11_months_30_days() {
        // Given
        val currentDate = LocalDate.of(2024, 12, 31)
        val eventDate = LocalDate.of(2024, 1, 1)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(11, calculated.period.months, "Должно быть 11 месяцев")
        assertEquals(30, calculated.period.days, "Должно быть 30 дней")
        assertEquals(0, calculated.period.years, "Лет быть не должно")
    }

    @Test
    fun calculate_when_1_year_then_returns_1_year_0_months_0_days() {
        // Given
        val currentDate = LocalDate.of(2025, 1, 15)
        val eventDate = LocalDate.of(2024, 1, 15)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.years, "Должен быть 1 год")
        assertEquals(0, calculated.period.months, "Месяцев быть не должно")
        assertEquals(0, calculated.period.days, "Дней быть не должно")
    }

    @Test
    fun calculate_when_1_year_2_months_then_returns_1_year_2_months_0_days() {
        // Given
        val currentDate = LocalDate.of(2025, 3, 15)
        val eventDate = LocalDate.of(2024, 1, 15)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.years, "Должен быть 1 год")
        assertEquals(2, calculated.period.months, "Должно быть 2 месяца")
        assertEquals(0, calculated.period.days, "Дней быть не должно")
    }

    @Test
    fun calculate_when_1_year_2_months_5_days_then_returns_correct_period() {
        // Given
        val currentDate = LocalDate.of(2025, 3, 20)
        val eventDate = LocalDate.of(2024, 1, 15)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.years, "Должен быть 1 год")
        assertEquals(2, calculated.period.months, "Должно быть 2 месяца")
        assertEquals(5, calculated.period.days, "Должно быть 5 дней")
    }

    @Test
    fun calculate_when_leap_year_february_29_then_calculates_correctly() {
        // Given - 29 февраля 2024 (високосный год)
        val currentDate = LocalDate.of(2024, 3, 1)
        val eventDate = LocalDate.of(2024, 2, 29)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(1, calculated.period.days, "Должен быть 1 день")
    }

    @Test
    fun calculate_when_current_date_is_provided_then_uses_it() {
        // Given
        val providedDate = LocalDate.of(2024, 6, 15)
        val eventDate = LocalDate.of(2024, 6, 10)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = providedDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(5, calculated.period.days, "Должно быть 5 дней")
    }

    @Test
    fun totaldays_в_calculated_содержит_общее_количество_дней() {
        // Given
        val currentDate = LocalDate.of(2024, 1, 16)
        val eventDate = LocalDate.of(2024, 1, 10)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        // When
        val result = useCase(eventTimestamp = timestamp, currentDate = currentDate)

        // Then
        assertTrue(result is DaysDifference.Calculated, "Результат должен быть Calculated")
        val calculated = result as DaysDifference.Calculated
        assertEquals(6, calculated.totalDays, "Общее количество дней должно быть 6")
    }
}
