package com.dayscounter.data.formatter

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
    fun `formatDays when singular then returns singular form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 1,
            )
        } returns "1 день"

        // When
        val result = formatter.format(1, resourceProvider)

        // Then
        assertEquals("1 день", result, "Ожидалась форма единственного числа")
    }

    @Test
    fun `formatDays when plural few then returns few form for ru`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 2,
            )
        } returns "2 дня"

        // When
        val result = formatter.format(2, resourceProvider)

        // Then
        assertEquals("2 дня", result, "Ожидалась форма few (2 дня)")
    }

    @Test
    fun `formatDays when plural many then returns many form for ru`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дней"

        // When
        val result = formatter.format(5, resourceProvider)

        // Then
        assertEquals("5 дней", result, "Ожидалась форма many (5 дней)")
    }

    @Test
    fun `formatMonths when singular then returns singular form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 1,
            )
        } returns "1 месяц"

        // When
        val result = formatter.formatMonths(1, resourceProvider)

        // Then
        assertEquals("1 месяц", result, "Ожидалась форма единственного числа")
    }

    @Test
    fun `formatYears when singular then returns singular form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1,
            )
        } returns "1 год"

        // When
        val result = formatter.formatYears(1, resourceProvider)

        // Then
        assertEquals("1 год", result, "Ожидалась форма единственного числа")
    }

    @Test
    fun `formatYears when quantity 2 then returns few form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 2,
            )
        } returns "2 года"

        // When
        val result = formatter.formatYears(2, resourceProvider)

        // Then
        assertEquals("2 года", result, "Ожидалась форма few (2 года)")
    }

    @Test
    fun `formatYears when quantity 3 then returns few form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 3,
            )
        } returns "3 года"

        // When
        val result = formatter.formatYears(3, resourceProvider)

        // Then
        assertEquals("3 года", result, "Ожидалась форма few (3 года)")
    }

    @Test
    fun `formatYears when quantity 4 then returns few form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 4,
            )
        } returns "4 года"

        // When
        val result = formatter.formatYears(4, resourceProvider)

        // Then
        assertEquals("4 года", result, "Ожидалась форма few (4 года)")
    }

    @Test
    fun `formatYears when quantity 5 then returns many form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 5,
            )
        } returns "5 лет"

        // When
        val result = formatter.formatYears(5, resourceProvider)

        // Then
        assertEquals("5 лет", result, "Ожидалась форма many (5 лет)")
    }

    @Test
    fun `formatYears when quantity 11 then returns many form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 11,
            )
        } returns "11 лет"

        // When
        val result = formatter.formatYears(11, resourceProvider)

        // Then
        assertEquals("11 лет", result, "Ожидалась форма many (11 лет - исключение для few)")
    }

    @Test
    fun `formatYears when quantity 21 then returns one form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 21,
            )
        } returns "21 год"

        // When
        val result = formatter.formatYears(21, resourceProvider)

        // Then
        assertEquals("21 год", result, "Ожидалась форма one (21 год)")
    }

    @Test
    fun `formatYears when quantity 22 then returns few form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 22,
            )
        } returns "22 года"

        // When
        val result = formatter.formatYears(22, resourceProvider)

        // Then
        assertEquals("22 года", result, "Ожидалась форма few (22 года)")
    }

    @Test
    fun `formatYears when quantity 25 then returns many form`() {
        // Given
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 25,
            )
        } returns "25 лет"

        // When
        val result = formatter.formatYears(25, resourceProvider)

        // Then
        assertEquals("25 лет", result, "Ожидалась форма many (25 лет)")
    }

    @Test
    fun `formatComposite when DAY option then returns days only`() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 3)
        val totalDays = 365 // 1 год + 2 месяца + 3 дня ≈ 365 дней
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 365,
            )
        } returns "365 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                resourceProvider,
                totalDays,
                showMinus = false,
            )

        // Then
        assertEquals("365 дней", result, "Ожидались только дни для опции DAY")
    }

    @Test
    fun `formatComposite when MONTH_DAY option and both values then returns abbreviated format`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false,
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат")
    }

    @Test
    fun `formatComposite when MONTH_DAY option with years then converts years to months`() {
        // Given - 4 года конвертируются в 48 месяцев
        val period = TimePeriod(years = 4, months = 0, days = 10)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 48,
            )
        } returns "48 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 10,
            )
        } returns "10 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false,
            )

        // Then
        assertEquals("48 мес. 10 дн.", result, "Ожидалась конвертация лет в месяцы для MONTH_DAY (сокращённый формат)")
    }

    @Test
    fun `formatComposite when MONTH_DAY option with years and months then sums all months`() {
        // Given - 1 год + 2 месяца = 14 месяцев
        val period = TimePeriod(years = 1, months = 2, days = 3)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 14,
            )
        } returns "14 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 3,
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
                showMinus = false,
            )

        // Then
        assertEquals("14 мес. 3 дн.", result, "Ожидалась сумма лет и месяцев для MONTH_DAY")
    }

    @Test
    fun `formatComposite when MONTH_DAY option and only months then returns full format`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 0)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 месяца"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false,
            )

        // Then
        assertEquals("2 месяца", result, "Ожидался полный формат")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option and all values then returns abbreviated format`() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 5)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1,
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false,
            )

        // Then
        assertEquals("1 г. 2 мес. 5 дн.", result, "Ожидался сокращённый формат")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option and two values then returns short format`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false,
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался краткий формат (сокращённый)")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option and one value then returns full format`() {
        // Given
        val period = TimePeriod(years = 1, months = 0, days = 0)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1,
            )
        } returns "1 г."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = false,
            )

        // Then
        assertEquals("1 г.", result, "Ожидался полный формат")
    }

    // Тесты с showMinus = true

    @Test
    fun `formatComposite when DAY option and showMinus true with positive days then returns positive days`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 10)
        val totalDays = 10
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 10,
            )
        } returns "10 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                resourceProvider,
                totalDays,
                showMinus = true,
            )

        // Then
        assertEquals("10 дней", result, "Ожидались 10 дней для showMinus = true")
    }

    @Test
    fun `formatComposite when DAY option and showMinus true with negative days then returns negative days`() {
        // Given
        val period = TimePeriod(years = 0, months = 0, days = 7)
        val totalDays = -7
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = -7,
            )
        } returns "-7 дней"

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.DAY,
                resourceProvider,
                totalDays,
                showMinus = true,
            )

        // Then
        assertEquals("-7 дней", result, "Ожидались -7 дней для showMinus = true")
    }

    @Test
    fun `formatComposite when MONTH_DAY option and showMinus true with positive values`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = true,
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат для showMinus = true")
    }

    @Test
    fun `formatComposite when MONTH_DAY option and showMinus true with negative values`() {
        // Given
        val period = TimePeriod(years = 0, months = 2, days = 5)
        val totalDays = -65
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays,
                showMinus = true,
            )

        // Then
        assertEquals("2 мес. 5 дн.", result, "Ожидался сокращённый формат для showMinus = true")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option and showMinus true with positive values`() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 5)
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1,
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays = 0,
                showMinus = true,
            )

        // Then
        assertEquals("1 г. 2 мес. 5 дн.", result, "Ожидался сокращённый формат для showMinus = true")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option and showMinus true with negative values`() {
        // Given
        val period = TimePeriod(years = 1, months = 2, days = 5)
        val totalDays = -800
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1,
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays,
                showMinus = true,
            )

        // Then
        assertEquals("1 г. 2 мес. 5 дн.", result, "Ожидался сокращённый формат для showMinus = true")
    }

    // Тесты для будущих дат с showMinus = false (исправление бага)

    @Test
    fun `formatComposite when MONTH_DAY option with future date and showMinus false then shows no minus`() {
        // Given - будущая дата (через 1 месяц 2 дня)
        val period = TimePeriod(years = 0, months = -1, days = -2)
        val totalDays = -30
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 1,
            )
        } returns "1 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 2,
            )
        } returns "2 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays,
                showMinus = false,
            )

        // Then - не должно быть минуса, используется абсолютные значения
        assertEquals("1 мес. 2 дн.", result, "Не должно быть минуса для будущей даты с showMinus = false")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option with future date and showMinus false then shows no minus`() {
        // Given - будущая дата (через 1 год 2 месяца 5 дней)
        val period = TimePeriod(years = -1, months = -2, days = -5)
        val totalDays = -400
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = 1,
            )
        } returns "1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = 2,
            )
        } returns "2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = 5,
            )
        } returns "5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays,
                showMinus = false,
            )

        // Then - не должно быть минуса, используется абсолютные значения
        assertEquals("1 г. 2 мес. 5 дн.", result, "Не должно быть минуса для будущей даты с showMinus = false")
    }

    @Test
    fun `formatComposite when MONTH_DAY option with future date and showMinus true then shows minus`() {
        // Given - будущая дата (через 1 месяц 2 дня)
        val period = TimePeriod(years = 0, months = -1, days = -2)
        val totalDays = -30
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = -1,
            )
        } returns "-1 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = -2,
            )
        } returns "-2 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.MONTH_DAY,
                resourceProvider,
                totalDays,
                showMinus = true,
            )

        // Then - должен быть минус для будущей даты с showMinus = true
        assertEquals("-1 мес. -2 дн.", result, "Должен быть минус для будущей даты с showMinus = true")
    }

    @Test
    fun `formatComposite when YEAR_MONTH_DAY option with future date and showMinus true then shows minus`() {
        // Given - будущая дата (через 1 год 2 месяца 5 дней)
        val period = TimePeriod(years = -1, months = -2, days = -5)
        val totalDays = -400
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.years_count,
                quantity = -1,
            )
        } returns "-1 г."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.months_count,
                quantity = -2,
            )
        } returns "-2 мес."
        every {
            resourceProvider.getQuantityString(
                resId = R.plurals.days_count,
                quantity = -5,
            )
        } returns "-5 дн."

        // When
        val result =
            formatter.formatComposite(
                period,
                DisplayOption.YEAR_MONTH_DAY,
                resourceProvider,
                totalDays,
                showMinus = true,
            )

        // Then - должен быть минус для будущей даты с showMinus = true
        assertEquals("-1 г. -2 мес. -5 дн.", result, "Должен быть минус для будущей даты с showMinus = true")
    }
}
