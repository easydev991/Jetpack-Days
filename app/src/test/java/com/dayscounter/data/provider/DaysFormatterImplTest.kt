package com.dayscounter.data.provider

import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Тесты для [DaysFormatterImpl].
 */
class DaysFormatterImplTest {
    private val resourceProvider = mockk<ResourceProvider>()
    private val formatter = DaysFormatterImpl()

    @Test
    fun formatdays_when_singular_then_returns_singular_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 1
            )
        } returns "1 день"

        // When
        val result = formatter.format(1, resourceProvider)

        // Then
        assertEquals("1 день", result, "Ожидалась форма единственного числа")
    }

    @Test
    fun formatdays_when_plural_few_then_returns_few_form_for_ru() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 2
            )
        } returns "2 дня"

        // When
        val result = formatter.format(2, resourceProvider)

        // Then
        assertEquals("2 дня", result, "Ожидалась форма few (2 дня)")
    }

    @Test
    fun formatdays_when_plural_many_then_returns_many_form_for_ru() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дней"

        // When
        val result = formatter.format(5, resourceProvider)

        // Then
        assertEquals("5 дней", result, "Ожидалась форма many (5 дней)")
    }

    @Test
    fun formatmonths_when_singular_then_returns_singular_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 1
            )
        } returns "1 месяц"

        // When
        val result = formatter.formatMonths(1, resourceProvider)

        // Then
        assertEquals("1 месяц", result, "Ожидалась форма единственного числа")
    }

    @Test
    fun formatyears_when_singular_then_returns_singular_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1
            )
        } returns "1 год"

        // When
        val result = formatter.formatYears(1, resourceProvider)

        // Then
        assertEquals("1 год", result, "Ожидалась форма единственного числа")
    }

    @Test
    fun formatyears_when_quantity_2_then_returns_few_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 2
            )
        } returns "2 года"

        // When
        val result = formatter.formatYears(2, resourceProvider)

        // Then
        assertEquals("2 года", result, "Ожидалась форма few (2 года)")
    }

    @Test
    fun formatyears_when_quantity_3_then_returns_few_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 3
            )
        } returns "3 года"

        // When
        val result = formatter.formatYears(3, resourceProvider)

        // Then
        assertEquals("3 года", result, "Ожидалась форма few (3 года)")
    }

    @Test
    fun formatyears_when_quantity_4_then_returns_few_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 4
            )
        } returns "4 года"

        // When
        val result = formatter.formatYears(4, resourceProvider)

        // Then
        assertEquals("4 года", result, "Ожидалась форма few (4 года)")
    }

    @Test
    fun formatyears_when_quantity_5_then_returns_many_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 5
            )
        } returns "5 лет"

        // When
        val result = formatter.formatYears(5, resourceProvider)

        // Then
        assertEquals("5 лет", result, "Ожидалась форма many (5 лет)")
    }

    @Test
    fun formatyears_when_quantity_11_then_returns_many_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 11
            )
        } returns "11 лет"

        // When
        val result = formatter.formatYears(11, resourceProvider)

        // Then
        assertEquals("11 лет", result, "Ожидалась форма many (11 лет - исключение для few)")
    }

    @Test
    fun formatyears_when_quantity_21_then_returns_one_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 21
            )
        } returns "21 год"

        // When
        val result = formatter.formatYears(21, resourceProvider)

        // Then
        assertEquals("21 год", result, "Ожидалась форма one (21 год)")
    }

    @Test
    fun formatyears_when_quantity_22_then_returns_few_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 22
            )
        } returns "22 года"

        // When
        val result = formatter.formatYears(22, resourceProvider)

        // Then
        assertEquals("22 года", result, "Ожидалась форма few (22 года)")
    }

    @Test
    fun formatyears_when_quantity_25_then_returns_many_form() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 25
            )
        } returns "25 лет"

        // When
        val result = formatter.formatYears(25, resourceProvider)

        // Then
        assertEquals("25 лет", result, "Ожидалась форма many (25 лет)")
    }

    @Test
    fun formatcomposite_when_day_option_then_returns_days_only() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 3)
        val totalDays = 365 // 1 год + 2 месяца + 3 дня ≈ 365 дней
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 365
            )
        } returns "365 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат")
    }

    @Test
    fun formatcomposite_when_month_day_option_with_years_then_converts_years_to_months() {
        // Given - 4 года конвертируются в 48 месяцев
        val period = TimePeriod(years = 4, months = 0, days = 10)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 48
            )
        } returns "48 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 10
            )
        } returns "10 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 14
            )
        } returns "14 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 3
            )
        } returns "3 дн."
        // Mock для сокращений (новая логика)
        every { resourceProvider.getString(ResourceIds.MONTHS_ABBREVIATED) } returns "мес."
        every { resourceProvider.getString(ResourceIds.DAYS_ABBREVIATED) } returns "дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 месяца"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1
            )
        } returns "1 г."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 10
            )
        } returns "10 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = -7
            )
        } returns "-7 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 1
            )
        } returns "1 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 2
            )
        } returns "2 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = -1
            )
        } returns "-1 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = -2
            )
        } returns "-2 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
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
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = -1
            )
        } returns "-1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = -2
            )
        } returns "-2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = -5
            )
        } returns "-5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
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
