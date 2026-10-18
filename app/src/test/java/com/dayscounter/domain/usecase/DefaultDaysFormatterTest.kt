package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Тесты для [DefaultDaysFormatter].
 */
class DefaultDaysFormatterTest {
    private val textProvider = mockk<DaysTextProvider>()
    private val formatter = DefaultDaysFormatter()

    @Test
    fun formatcomposite_when_day_option_then_returns_days_only() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 3)
        val totalDays = 365 // 1 год + 2 месяца + 3 дня ≈ 365 дней
        every { textProvider.daysCount(365) } returns "365 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                textProvider,
                totalDays,
                showMinus = false
            )

        // Then
        assertEquals("365 дней", result, "Ожидались только дни для опции DAY")
    }

    @Test
    fun formatcomposite_when_month_day_option_and_both_values_then_returns_abbreviated_format() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат")
    }

    @Test
    fun formatcomposite_when_period_is_empty_then_returns_empty_string() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 0)

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("", result, "Ожидалась пустая строка для нулевого периода")
    }

    @Test
    fun formatcomposite_when_month_day_option_with_years_then_converts_years_to_months() {
        // Given - 4 года конвертируются в 48 месяцев
        val period = TimePeriod(years = 4, months = 0, days = 10)
        every { textProvider.monthsCount(48) } returns "48 мес."
        every { textProvider.daysCount(10) } returns "10 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals(
            "48 мес. 10 дн.",
            result,
            "Ожидалась конвертация лет в месяцы для MONTH_DAY (сокращённый формат)"
        )
    }

    @Test
    fun formatcomposite_when_month_day_option_with_years_and_months_then_sums_all_months() {
        // Given - 1 год + 2 месяца = 14 месяцев
        val period = TimePeriod(years = 1, months = 2, days = 3)
        every { textProvider.monthsCount(14) } returns "14 мес."
        every { textProvider.daysCount(3) } returns "3 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("14 мес. 3 дн.", result, "Ожидалась сумма лет и месяцев для MONTH_DAY")
    }

    @Test
    fun formatcomposite_when_month_day_option_and_only_months_then_returns_full_format() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 0)
        every { textProvider.monthsCount(2) } returns "2 месяца"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("2 месяца", result, "Ожидался полный формат")
    }

    @Test
    fun formatcomposite_when_year_month_day_option_and_all_values_then_returns_abbreviated_format() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 5)
        every { textProvider.yearsCount(1) } returns "1 г."
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("1 г. 2 мес. 5 дн.", result, "Ожидался сокращённый формат")
    }

    @Test
    fun formatcomposite_when_year_month_day_option_and_two_values_then_returns_short_format() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался краткий формат (сокращённый)")
    }

    @Test
    fun formatcomposite_when_year_month_day_option_and_one_value_then_returns_full_format() {
        // Given
        val period = TimePeriod(years = 1, months = 0, days = 0)
        every { textProvider.yearsCount(1) } returns "1 г."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("1 г.", result, "Ожидался полный формат")
    }

    // Тесты с showMinus = true

    @Test
    fun formatcomposite_when_day_option_and_showminus_true_with_positive_days_then_returns_positive_days() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val totalDays = 10
        every { textProvider.daysCount(10) } returns "10 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                textProvider,
                totalDays,
                showMinus = true
            )

        // Then
        assertEquals("10 дней", result, "Ожидались 10 дней для showMinus = true")
    }

    @Test
    fun formatcomposite_when_day_option_and_showminus_true_with_negative_days_then_returns_negative_days() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 7)
        val totalDays = -7
        every { textProvider.daysCount(-7) } returns "-7 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                textProvider,
                totalDays,
                showMinus = true
            )

        // Then
        assertEquals("-7 дней", result, "Ожидались -7 дней для showMinus = true")
    }

    @Test
    fun formatcomposite_when_month_day_option_and_showminus_true_with_positive_values() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = true
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат для showMinus = true")
    }

    @Test
    fun formatcomposite_when_month_day_option_and_showminus_true_with_negative_values() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        val totalDays = -65
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays,
                showMinus = true
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат для showMinus = true")
    }

    @Test
    fun formatcomposite_when_year_month_day_option_and_showminus_true_with_positive_values() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 5)
        every { textProvider.yearsCount(1) } returns "1 г."
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays = 0,
                showMinus = true
            )

        // Then
        assertEquals(
            "1 г. 2 мес. 5 дн.",
            result,
            "Ожидался сокращённый формат для showMinus = true"
        )
    }

    @Test
    fun formatcomposite_when_year_month_day_option_and_showminus_true_with_negative_values() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 5)
        val totalDays = -800
        every { textProvider.yearsCount(1) } returns "1 г."
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays,
                showMinus = true
            )

        // Then
        assertEquals(
            "1 г. 2 мес. 5 дн.",
            result,
            "Ожидался сокращённый формат для showMinus = true"
        )
    }

    // Тесты для будущих дат с showMinus = false (исправление бага)

    @Test
    fun formatcomposite_when_month_day_option_with_future_date_and_showminus_false_then_shows_no_minus() {
        // Given - будущая дата (через 1 месяц 2 дня)
        val period = TimePeriod(years = 0, months = -1, days = -2)
        val totalDays = -30
        every { textProvider.monthsCount(1) } returns "1 мес."
        every { textProvider.daysCount(2) } returns "2 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays,
                showMinus = false
            )

        // Then - не должно быть минуса, используется абсолютные значения
        assertEquals(
            "1 мес. 2 дн.",
            result,
            "Не должно быть минуса для будущей даты с showMinus = false"
        )
    }

    @Test
    fun formatcomposite_when_year_month_day_option_with_future_date_and_showminus_false_then_shows_no_minus() {
        // Given - будущая дата (через 1 год 2 месяца 5 дней)
        val period = TimePeriod(years = -1, months = -2, days = -5)
        val totalDays = -400
        every { textProvider.yearsCount(1) } returns "1 г."
        every { textProvider.monthsCount(2) } returns "2 мес."
        every { textProvider.daysCount(5) } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays,
                showMinus = false
            )

        // Then - не должно быть минуса, используется абсолютные значения
        assertEquals(
            "1 г. 2 мес. 5 дн.",
            result,
            "Не должно быть минуса для будущей даты с showMinus = false"
        )
    }

    @Test
    fun formatcomposite_when_month_day_option_with_future_date_and_showminus_true_then_shows_minus() {
        // Given - будущая дата (через 1 месяц 2 дня)
        val period = TimePeriod(years = 0, months = -1, days = -2)
        val totalDays = -30
        every { textProvider.monthsCount(-1) } returns "-1 мес."
        every { textProvider.daysCount(-2) } returns "-2 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                textProvider,
                totalDays,
                showMinus = true
            )

        // Then - должен быть минус для будущей даты с showMinus = true
        assertEquals(
            "-1 мес. -2 дн.",
            result,
            "Должен быть минус для будущей даты с showMinus = true"
        )
    }

    @Test
    fun formatcomposite_when_year_month_day_option_with_future_date_and_showminus_true_then_shows_minus() {
        // Given - будущая дата (через 1 год 2 месяца 5 дней)
        val period = TimePeriod(years = -1, months = -2, days = -5)
        val totalDays = -400
        every { textProvider.yearsCount(-1) } returns "-1 г."
        every { textProvider.monthsCount(-2) } returns "-2 мес."
        every { textProvider.daysCount(-5) } returns "-5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                textProvider,
                totalDays,
                showMinus = true
            )

        // Then - должен быть минус для будущей даты с showMinus = true
        assertEquals(
            "-1 г. -2 мес. -5 дн.",
            result,
            "Должен быть минус для будущей даты с showMinus = true"
        )
    }
}
