package com.dayscounter.domain.usecase

import com.dayscounter.data.provider.DaysFormatter
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.data.provider.StubResourceProvider
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для FormatDaysTextUseCase.
 *
 * Тестирует корректное форматирование текста дней с учетом параметра showMinus.
 */
class FormatDaysTextUseCaseTest {
    private val daysFormatter: DaysFormatter = mockk()
    private val resourceProvider: ResourceProvider = StubResourceProvider()

    private val useCase = FormatDaysTextUseCase(daysFormatter = daysFormatter)

    @Test
    fun invoke_when_today_then_returns_today_string() {
        // Given
        val difference = DaysDifference.Today(timestamp = 1234567890000L)

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true
            )

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun invoke_when_today_and_showminus_false_then_returns_today_string() {
        // Given
        val difference = DaysDifference.Today(timestamp = 1234567890000L)

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false
            )

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun invoke_when_calculated_positive_days_and_showminus_true_then_shows_number_with_sign() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = 10,
                timestamp = 1234567890000L
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = 10,
                showMinus = true
            )
        } returns "10 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true
            )

        // Then
        assertEquals("10 дней", result)
    }

    @Test
    fun invoke_when_calculated_positive_days_and_showminus_false_then_shows_number_without_sign() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = 10,
                timestamp = 1234567890000L
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = 10,
                showMinus = false
            )
        } returns "10 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false
            )

        // Then
        assertEquals("10 дней", result)
    }

    @Test
    fun invoke_when_calculated_negative_days_and_showminus_true_then_shows_negative_number() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 7)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -7,
                timestamp = 1234567890000L
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = -7,
                showMinus = true
            )
        } returns "-7 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true
            )

        // Then
        assertEquals("-7 дней", result)
        assertTrue(result.contains("-"), "Минус должен присутствовать в результате")
    }

    @Test
    fun invoke_when_calculated_negative_days_and_showminus_false_then_shows_absolute_value() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 7)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -7,
                timestamp = 1234567890000L
            )
        // После исправления бага FormatDaysTextUseCase передает оригинальное totalDays,
        // а DaysFormatterImpl сам решает, показывать минус или нет
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = -7,
                showMinus = false
            )
        } returns "7 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false
            )

        // Then
        assertEquals("7 дней", result)
    }

    @Test
    fun invoke_when_month_day_option_and_showminus_true_then_passes_correct_parameter() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -40,
                timestamp = 1234567890000L
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -40,
                showMinus = true
            )
        } returns "-40 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = true
            )

        // Then
        assertEquals("-40 дней", result)
    }

    @Test
    fun invoke_when_month_day_option_and_showminus_false_then_passes_correct_parameter() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -40,
                timestamp = 1234567890000L
            )
        // После исправления бага FormatDaysTextUseCase передает оригинальное totalDays,
        // а DaysFormatterImpl сам решает, показывать минус или нет
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -40,
                showMinus = false
            )
        } returns "40 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = false
            )

        // Then
        assertEquals("40 дней", result)
    }

    @Test
    fun invoke_when_year_month_day_option_and_showminus_true_then_passes_correct_parameter() {
        // Given
        val period = TimePeriod(years = 2, months = 2, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -800,
                timestamp = 1234567890000L
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -800,
                showMinus = true
            )
        } returns "-800 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = true
            )

        // Then
        assertEquals("-800 дней", result)
    }

    @Test
    fun invoke_when_year_month_day_option_and_showminus_false_then_passes_correct_parameter() {
        // Given
        val period = TimePeriod(years = 2, months = 2, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -800,
                timestamp = 1234567890000L
            )
        // После исправления бага FormatDaysTextUseCase передает оригинальное totalDays,
        // а DaysFormatterImpl сам решает, показывать минус или нет
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -800,
                showMinus = false
            )
        } returns "800 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = false
            )

        // Then
        assertEquals("800 дней", result)
    }
}
