package com.dayscounter.data.provider

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod

/**
 * Реализация [DaysFormatter].
 *
 * Форматирует количество дней в соответствии с локализацией
 * и выбранной опцией отображения.
 */
class DaysFormatterImpl : DaysFormatter {
    companion object {
        /** Константа для максимального количества компонентов (годы, месяцы, дни) */
        private const val MAX_COMPONENTS = 3

        /** Константа для формата с двумя компонентами */
        private const val TWO_COMPONENTS = 2

        /** Количество месяцев в году */
        private const val MONTHS_IN_YEAR = 12

        /**
         * Контейнер для компонентов времени.
         *
         * @param showYears Флаг: добавлять годы
         * @param yearsValue Значение лет
         * @param showMonths Флаг: добавлять месяцы
         * @param monthsValue Значение месяцев
         * @param showDays Флаг: добавлять дни
         * @param daysValue Значение дней
         */
        private data class TimeComponents(
            val showYears: Boolean = false,
            val yearsValue: Int = 0,
            val showMonths: Boolean = false,
            val monthsValue: Int = 0,
            val showDays: Boolean = false,
            val daysValue: Int = 0,
        )
    }

    override fun format(
        days: Int,
        resourceProvider: ResourceProvider,
    ): String =
        resourceProvider.getQuantityString(
            resId = ResourceIds.DAYS_COUNT,
            quantity = days,
        )

    override fun formatMonths(
        months: Int,
        resourceProvider: ResourceProvider,
    ): String =
        resourceProvider.getQuantityString(
            resId = ResourceIds.MONTHS_COUNT,
            quantity = months,
        )

    override fun formatYears(
        years: Int,
        resourceProvider: ResourceProvider,
    ): String =
        resourceProvider.getQuantityString(
            resId = ResourceIds.YEARS_COUNT,
            quantity = years,
        )

    override fun formatComposite(
        period: TimePeriod,
        displayOption: DisplayOption,
        resourceProvider: ResourceProvider,
        totalDays: Int,
        showMinus: Boolean,
    ): String =
        when (displayOption) {
            DisplayOption.DAY -> {
                // Для DisplayOption.DAY используем общее количество дней
                // period.days может быть меньше (остаток после вычета лет и месяцев)
                val daysToShow = if (showMinus) totalDays else kotlin.math.abs(totalDays)
                format(daysToShow, resourceProvider)
            }

            DisplayOption.MONTH_DAY -> {
                formatMonthDay(period, resourceProvider, showMinus, totalDays)
            }

            DisplayOption.YEAR_MONTH_DAY -> {
                formatYearMonthDay(period, resourceProvider, showMinus, totalDays)
            }
        }

    /**
     * Форматирует месяцы и дни.
     *
     * Для MONTH_DAY конвертирует годы в месяцы, так же как в iOS-приложении
     * (DateComponentsFormatter с allowedUnits = [.month, .day]).
     *
     * @param period Период времени (годы, месяцы, дни)
     * @param resourceProvider Провайдер строковых ресурсов
     * @param showMinus Показывать ли минус для будущих дат
     * @param totalDays Общее количество дней (используется для определения будущего)
     */
    private fun formatMonthDay(
        period: TimePeriod,
        resourceProvider: ResourceProvider,
        showMinus: Boolean,
        totalDays: Int,
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

        val timeComponents =
            TimeComponents(
                showMonths = monthsValue != 0,
                monthsValue = monthsValue,
                showDays = daysValue != 0,
                daysValue = daysValue,
            )

        val components = buildComponentsList(timeComponents, resourceProvider)

        return formatComponents(components)
    }

    /**
     * Форматирует годы, месяцы и дни.
     *
     * @param period Период времени (годы, месяцы, дни)
     * @param resourceProvider Провайдер строковых ресурсов
     * @param showMinus Показывать ли минус для будущих дат
     * @param totalDays Общее количество дней (используется для определения будущего)
     */
    private fun formatYearMonthDay(
        period: TimePeriod,
        resourceProvider: ResourceProvider,
        showMinus: Boolean,
        totalDays: Int,
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

        val timeComponents =
            TimeComponents(
                showYears = yearsValue != 0,
                yearsValue = yearsValue,
                showMonths = monthsValue != 0,
                monthsValue = monthsValue,
                showDays = daysValue != 0,
                daysValue = daysValue,
            )

        val components = buildComponentsList(timeComponents, resourceProvider)

        return formatComponents(components)
    }

    /**
     * Создает список компонентов для форматирования.
     *
     * @param components Контейнер с флагами и значениями компонентов
     * @param resourceProvider Провайдер ресурсов
     * @return Список строк компонентов
     */
    private fun buildComponentsList(
        components: TimeComponents,
        resourceProvider: ResourceProvider,
    ): List<String> {
        val result = mutableListOf<String>()

        if (components.showYears) {
            result.add(formatYears(components.yearsValue, resourceProvider))
        }

        if (components.showMonths) {
            result.add(formatMonths(components.monthsValue, resourceProvider))
        }

        if (components.showDays) {
            result.add(format(components.daysValue, resourceProvider))
        }

        return result
    }

    /**
     * Форматирует список компонентов в строку.
     */
    private fun formatComponents(components: List<String>): String =
        when (components.size) {
            MAX_COMPONENTS ->
                components.joinToString(" ")

            TWO_COMPONENTS ->
                components.joinToString(" ")

            1 -> components.firstOrNull() ?: ""
            else -> {
                // Возвращаем пустую строку для остальных случаев
                ""
            }
        }
}
