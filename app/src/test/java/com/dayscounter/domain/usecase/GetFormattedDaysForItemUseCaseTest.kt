package com.dayscounter.domain.usecase

import com.dayscounter.data.formatter.StubResourceProvider
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.TimePeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit-тесты для GetFormattedDaysForItemUseCase.
 *
 * Тестирует корректное получение форматированного текста дней для Item
 * с учетом параметра showMinus.
 */
class GetFormattedDaysForItemUseCaseTest {
    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase = mockk()
    private val formatDaysTextUseCase: FormatDaysTextUseCase = mockk()
    private val resourceProvider = StubResourceProvider()

    private val useCase =
        GetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
            formatDaysTextUseCase = formatDaysTextUseCase,
            resourceProvider = resourceProvider,
        )

    @Test
    fun `invoke when event in future and showMinus true then shows minus sign`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(7)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Новый год",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 7),
                totalDays = -7,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "-7 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = true)

        // Then
        assertEquals("-7 дней", result)
        assertTrue(
            result.contains("-"),
            "Минус должен присутствовать в результате для showMinus = true"
        )
    }

    @Test
    fun `invoke when event in future and showMinus false then shows absolute value`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(7)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Новый год",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 7),
                totalDays = -7,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )
        } returns "7 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = false)

        // Then
        assertEquals("7 дней", result)
    }

    @Test
    fun `invoke when event in past and showMinus true then shows positive number`() {
        // Given
        val now = LocalDate.now()
        val pastDate = now.minusDays(10)
        val timestamp = pastDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "День рождения",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 10),
                totalDays = 10,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "10 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = true)

        // Then
        assertEquals("10 дней", result)
    }

    @Test
    fun `invoke when event in past and showMinus false then shows positive number`() {
        // Given
        val now = LocalDate.now()
        val pastDate = now.minusDays(10)
        val timestamp = pastDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "День рождения",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 10),
                totalDays = 10,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )
        } returns "10 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = false)

        // Then
        assertEquals("10 дней", result)
    }

    @Test
    fun `invoke when event today and showMinus true then returns today string`() {
        // Given
        val now = LocalDate.now()
        val timestamp = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Сегодняшнее событие",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Today(timestamp = timestamp)
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "Сегодня"

        // When
        val result = useCase.invoke(item = item, showMinus = true)

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun `invoke when event today and showMinus false then returns today string`() {
        // Given
        val now = LocalDate.now()
        val timestamp = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Сегодняшнее событие",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Today(timestamp = timestamp)
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )
        } returns "Сегодня"

        // When
        val result = useCase.invoke(item = item, showMinus = false)

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun `invoke when MONTH_DAY option and showMinus true then passes correct parameter`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(40)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "День рождения",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.MONTH_DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 1, days = 5),
                totalDays = -40,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "-40 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = true)

        // Then
        assertEquals("-40 дней", result)
    }

    @Test
    fun `invoke when MONTH_DAY option and showMinus false then passes correct parameter`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(40)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "День рождения",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.MONTH_DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 1, days = 5),
                totalDays = -40,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )
        } returns "40 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = false)

        // Then
        assertEquals("40 дней", result)
    }

    @Test
    fun `invoke when YEAR_MONTH_DAY option and showMinus true then passes correct parameter`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(800)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Отпуск",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 2, months = 2, days = 10),
                totalDays = -800,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "-800 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = true)

        // Then
        assertEquals("-800 дней", result)
    }

    @Test
    fun `invoke when YEAR_MONTH_DAY option and showMinus false then passes correct parameter`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(800)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Отпуск",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 2, months = 2, days = 10),
                totalDays = -800,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.YEAR_MONTH_DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )
        } returns "800 дней"

        // When
        val result = useCase.invoke(item = item, showMinus = false)

        // Then
        assertEquals("800 дней", result)
    }

    @Test
    fun `invoke when custom date provided and showMinus true then uses custom date`() {
        // Given
        val customDate = LocalDate.of(2025, 1, 1)
        val eventDate = LocalDate.of(2025, 1, 8)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Новое событие",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), customDate) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 7),
                totalDays = -7,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "-7 дней"

        // When
        val result = useCase.invoke(item = item, currentDate = customDate, showMinus = true)

        // Then
        assertEquals("-7 дней", result)
    }

    @Test
    fun `invoke when custom date provided and showMinus false then uses custom date`() {
        // Given
        val customDate = LocalDate.of(2025, 1, 1)
        val eventDate = LocalDate.of(2025, 1, 8)
        val timestamp = eventDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Новое событие",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), customDate) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 7),
                totalDays = -7,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = false,
            )
        } returns "7 дней"

        // When
        val result = useCase.invoke(item = item, currentDate = customDate, showMinus = false)

        // Then
        assertEquals("7 дней", result)
    }

    @Test
    fun `invoke when defaultDisplayOption DAY and showMinus true then uses default`() {
        // Given
        val now = LocalDate.now()
        val futureDate = now.plusDays(7)
        val timestamp = futureDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Событие",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.DEFAULT,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 7),
                totalDays = -7,
                timestamp = timestamp,
            )
        every {
            formatDaysTextUseCase(
                difference = any(),
                displayOption = DisplayOption.DAY,
                resourceProvider = resourceProvider,
                showMinus = true,
            )
        } returns "-7 дней"

        // When
        val result =
            useCase.invoke(
                item = item,
                defaultDisplayOption = DisplayOption.DAY,
                showMinus = true,
            )

        // Then
        assertEquals("-7 дней", result)
    }
}
