package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod

/**
 * Реализация [DaysFormatter].
 *
 * Форматирует количество дней в соответствии с локализацией
 * и выбранной опцией отображения.
 */
class DefaultDaysFormatter : DaysFormatter {
    companion object {
        /** Количество месяцев в году */
        private const val MONTHS_IN_YEAR = 12
    }

    override fun formatComposite(
        period: TimePeriod,
        displayOption: DisplayOption,
        textProvider: DaysTextProvider,
        totalDays: Int,
        showMinus: Boolean
    ): String =
        when (displayOption) {
            DisplayOption.DAY -> {
                // Для DisplayOption.DAY используем общее количество дней
                // period.days может быть меньше (остаток после вычета лет и месяцев)
                val daysToShow = if (showMinus) totalDays else kotlin.math.abs(totalDays)
                textProvider.daysCount(daysToShow)
            }

            DisplayOption.MONTH_DAY -> {
                formatMonthDay(period, textProvider, showMinus, totalDays)
            }

            DisplayOption.YEAR_MONTH_DAY -> {
                formatYearMonthDay(period, textProvider, showMinus, totalDays)
            }
        }

    /**
     * Форматирует месяцы и дни.
     *
     * Для MONTH_DAY конвертирует годы в месяцы, так же как в iOS-приложении
     * (DateComponentsFormatter с allowedUnits = [.month, .day]).
     *
     * @param period Период времени (годы, месяцы, дни)
     * @param textProvider Провайдер строковых ресурсов
     * @param showMinus Показывать ли минус для будущих дат
     * @param totalDays Общее количество дней (используется для определения будущего)
     */
    private fun formatMonthDay(
        period: TimePeriod,
        textProvider: DaysTextProvider,
        showMinus: Boolean,
        totalDays: Int
    ): String {
        // Конвертируем годы в месяцы для MONTH_DAY (как в iOS)
        val totalMonths = period.years * MONTHS_IN_YEAR + period.months

        // Для будущих дат с showMinus = false используем абсолютные значения
        // Если дата в будущем (totalDays < 0) и showMinus = false, формируем текст без минуса
        val (monthsValue, daysValue) =
            if (totalDays < 0 && !showMinus) {
                // Будущее без минуса - используем абсолютные значения
                Pair(kotlin.math.abs(totalMonths), kotlin.math.abs(period.days))
            } else {
                // Прошлое или разрешен минус - используем вычисленные значения
                Pair(totalMonths, period.days)
            }

        return joinComponents(textProvider, years = 0, months = monthsValue, days = daysValue)
    }

    /**
     * Форматирует годы, месяцы и дни.
     *
     * @param period Период времени (годы, месяцы, дни)
     * @param textProvider Провайдер строковых ресурсов
     * @param showMinus Показывать ли минус для будущих дат
     * @param totalDays Общее количество дней (используется для определения будущего)
     */
    private fun formatYearMonthDay(
        period: TimePeriod,
        textProvider: DaysTextProvider,
        showMinus: Boolean,
        totalDays: Int
    ): String {
        // Для будущих дат с showMinus = false используем абсолютные значения
        // Если дата в будущем (totalDays < 0) и showMinus = false, формируем текст без минуса
        val (yearsValue, monthsValue, daysValue) =
            if (totalDays < 0 && !showMinus) {
                // Будущее без минуса - используем абсолютные значения
                Triple(
                    kotlin.math.abs(period.years),
                    kotlin.math.abs(period.months),
                    kotlin.math.abs(period.days)
                )
            } else {
                // Прошлое или разрешен минус - используем вычисленные значения
                Triple(period.years, period.months, period.days)
            }

        return joinComponents(textProvider, years = yearsValue, months = monthsValue, days = daysValue)
    }

    /**
     * Склеивает ненулевые компоненты периода в одну строку
     * (пустой период даёт пустую строку).
     */
    private fun joinComponents(
        textProvider: DaysTextProvider,
        years: Int,
        months: Int,
        days: Int
    ): String =
        buildList {
            if (years != 0) add(textProvider.yearsCount(years))
            if (months != 0) add(textProvider.monthsCount(months))
            if (days != 0) add(textProvider.daysCount(days))
        }.joinToString(" ")
}
