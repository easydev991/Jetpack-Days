package com.dayscounter.domain.usecase

import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.formatter.StubResourceProvider
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.TimePeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit-тесты для GetDaysAnalysisTextUseCase.
 *
 * Тестирует корректное формирование текста анализа дней с префиксами
 * "осталось"/"прошло" и без знака минус для будущих событий.
 */
class GetDaysAnalysisTextUseCaseTest {
    private val resourceProvider: ResourceProvider = StubResourceProvider()

    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase = mockk()
    private val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase = mockk()

    private val useCase =
        GetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
            resourceProvider = resourceProvider,
        )

    @Test
    fun `invoke when event in future with DAY option then returns text without minus`() {
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
        every { getFormattedDaysForItemUseCase(any(), any(), showMinus = false) } returns "7 дней"

        // When
        val result = useCase.invoke(item)

        // Then
        assertTrue(result.startsWith("осталось ") || result.startsWith("remaining "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
        assertTrue(result.contains("7 дней"))
    }

    @Test
    fun `invoke when event in future with MONTH_DAY option then returns correct text`() {
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
            getFormattedDaysForItemUseCase(
                any(),
                any(),
                showMinus = false,
            )
        } returns "1 месяц 5 дней"

        every {
            getFormattedDaysForItemUseCase(
                any(),
                any(),
                showMinus = false
            )
        } returns "1 месяц 5 дней"

        // When
        val result = useCase.invoke(item)

        // Then
        assertTrue(result.startsWith("осталось ") || result.startsWith("remaining "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
        assertTrue(result.contains("1 месяц") || result.contains("1 month"))
    }

    @Test
    fun `invoke when event in future with YEAR_MONTH_DAY option then returns correct text`() {
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
        every { getFormattedDaysForItemUseCase(any(), any(), showMinus = false) } returns
            "2 года 2 месяца 10 дней"

        // When
        val result = useCase.invoke(item)

        // Then
        assertTrue(result.startsWith("осталось ") || result.startsWith("remaining "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
        assertTrue(result.contains("2 года") || result.contains("2 years"))
    }

    @Test
    fun `invoke when event in past with DAY option then returns correct text`() {
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
        every { getFormattedDaysForItemUseCase(any(), any(), showMinus = false) } returns "10 дней"

        // When
        val result = useCase.invoke(item)

        // Then
        assertTrue(result.startsWith("прошло ") || result.startsWith("elapsed "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
        assertTrue(result.contains("10 дней"))
    }

    @Test
    fun `invoke when event in past with MONTH_DAY option then returns correct text`() {
        // Given
        val now = LocalDate.now()
        val pastDate = now.minusDays(70)
        val timestamp = pastDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
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
                period = TimePeriod(years = 0, months = 2, days = 10),
                totalDays = 70,
                timestamp = timestamp,
            )
        every {
            getFormattedDaysForItemUseCase(
                any(),
                any(),
                showMinus = false,
            )
        } returns "2 месяца 10 дней"

        // When
        val result = useCase.invoke(item)

        // Then
        assertTrue(result.startsWith("прошло ") || result.startsWith("elapsed "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
        val resultLower = result.lowercase()
        assertTrue(resultLower.contains("месяц") || resultLower.contains("month"))
    }

    @Test
    fun `invoke when event in past with YEAR_MONTH_DAY option then returns correct text`() {
        // Given
        val now = LocalDate.now()
        val pastDate = now.minusDays(400)
        val timestamp = pastDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Свадьба",
                details = "",
                timestamp = timestamp,
                colorTag = null,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 1, months = 1, days = 5),
                totalDays = 400,
                timestamp = timestamp,
            )
        every {
            getFormattedDaysForItemUseCase(
                any(),
                any(),
                showMinus = false,
            )
        } returns "1 год 1 месяц 5 дней"

        // When
        val result = useCase.invoke(item)

        // Then
        assertTrue(result.startsWith("прошло ") || result.startsWith("elapsed "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
        assertTrue(result.contains("1 год") || result.contains("1 year"))
    }

    @Test
    fun `invoke when event is today then returns today string`() {
        // Given
        val now = Instant.now().toEpochMilli()
        val item =
            Item(
                id = 1L,
                title = "Сегодняшнее событие",
                details = "",
                timestamp = now,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Today(
                timestamp = now,
            )

        // When
        val result = useCase.invoke(item)

        // Then
        assertEquals("Сегодня", result)
    }

    @Test
    fun `invoke when event is today with zero totalDays then returns today string`() {
        // Given
        val now = LocalDate.now()
        val timestamp = now.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Item(
            id = 1L,
            title = "Сегодняшнее событие",
            details = "",
            timestamp = timestamp,
            colorTag = null,
            displayOption = DisplayOption.DAY,
        )

        every { calculateDaysDifferenceUseCase(any(), any()) } returns
            DaysDifference.Calculated(
                period = TimePeriod(years = 0, months = 0, days = 0),
                totalDays = 0,
                timestamp = timestamp,
            )
        every { getFormattedDaysForItemUseCase(any(), any(), showMinus = false) } returns "Сегодня"
    }

    @Test
    fun `invoke when custom date provided then uses custom date for calculation`() {
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
        every { getFormattedDaysForItemUseCase(any(), any(), showMinus = false) } returns "7 дней"

        // When
        val result = useCase.invoke(item, currentDate = customDate)

        // Then
        assertTrue(result.startsWith("осталось ") || result.startsWith("remaining "))
        assertFalse(result.contains("-"), "Минус не должен присутствовать в результате")
    }
}
