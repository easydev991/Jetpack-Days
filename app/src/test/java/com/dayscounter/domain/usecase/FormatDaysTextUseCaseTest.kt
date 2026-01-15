package com.dayscounter.domain.usecase

import com.dayscounter.data.formatter.DaysFormatter
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.formatter.StubResourceProvider
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
    fun `invoke when Today then returns today string`() {
        // Given
        val difference = DaysDifference.Today(timestamp = 1234567890000L)

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun `invoke when Today and showMinus false then returns today string`() {
        // Given
        val difference = DaysDifference.Today(timestamp = 1234567890000L)

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun `invoke when Calculated positive days and showMinus true then shows number with sign`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = 10,
                timestamp = 1234567890000L,
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = 10,
                showMinus = true,
            )
        } returns "10 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )

        // Then
        assertEquals("10 дней", result)
    }

    @Test
    fun `invoke when Calculated positive days and showMinus false then shows number without sign`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = 10,
                timestamp = 1234567890000L,
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = 10,
                showMinus = false,
            )
        } returns "10 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )

        // Then
        assertEquals("10 дней", result)
    }

    @Test
    fun `invoke when Calculated negative days and showMinus true then shows negative number`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 7)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -7,
                timestamp = 1234567890000L,
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = -7,
                showMinus = true,
            )
        } returns "-7 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )

        // Then
        assertEquals("-7 дней", result)
        assertTrue(result.contains("-"), "Минус должен присутствовать в результате")
    }

    @Test
    fun `invoke when Calculated negative days and showMinus false then shows absolute value`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 7)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -7,
                timestamp = 1234567890000L,
            )
        // После исправления бага FormatDaysTextUseCase передает оригинальное totalDays,
        // а DaysFormatterImpl сам решает, показывать минус или нет
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                totalDays = -7,
                showMinus = false,
            )
        } returns "7 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )

        // Then
        assertEquals("7 дней", result)
    }

    @Test
    fun `invoke when MONTH_DAY option and showMinus true then passes correct parameter`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -40,
                timestamp = 1234567890000L,
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -40,
                showMinus = true,
            )
        } returns "-40 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )

        // Then
        assertEquals("-40 дней", result)
    }

    @Test
    fun `invoke when MONTH_DAY option and showMinus false then passes correct parameter`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -40,
                timestamp = 1234567890000L,
            )
        // После исправления бага FormatDaysTextUseCase передает оригинальное totalDays,
        // а DaysFormatterImpl сам решает, показывать минус или нет
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -40,
                showMinus = false,
            )
        } returns "40 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )

        // Then
        assertEquals("40 дней", result)
    }

    @Test
    fun `invoke when YEAR_MONTH_DAY option and showMinus true then passes correct parameter`() {
        // Given
        val period = TimePeriod(years = 2, months = 2, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -800,
                timestamp = 1234567890000L,
            )
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -800,
                showMinus = true,
            )
        } returns "-800 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )

        // Then
        assertEquals("-800 дней", result)
    }

    @Test
    fun `invoke when YEAR_MONTH_DAY option and showMinus false then passes correct parameter`() {
        // Given
        val period = TimePeriod(years = 2, months = 2, days = 10)
        val difference =
            DaysDifference.Calculated(
                period = period,
                totalDays = -800,
                timestamp = 1234567890000L,
            )
        // После исправления бага FormatDaysTextUseCase передает оригинальное totalDays,
        // а DaysFormatterImpl сам решает, показывать минус или нет
        every {
            daysFormatter.formatComposite(
                period = period,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                totalDays = -800,
                showMinus = false,
            )
        } returns "800 дней"

        // When
        val result =
            useCase.invoke(
                difference = difference,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )

        // Then
        assertEquals("800 дней", result)
    }
}
