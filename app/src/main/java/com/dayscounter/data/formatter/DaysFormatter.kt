package com.dayscounter.data.formatter

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod

/**
 * Интерфейс для форматирования разницы дней в строку.
 *
 * Предоставляет методы для форматирования количества дней в различных форматах
 * с учётом локализации и множественного числа.
 */
interface DaysFormatter {
    /**
     * Форматирует количество дней в строку.
     *
     * @param days Количество дней
     * @param resourceProvider Провайдер строковых ресурсов
     * @return Отформатированная строка (например, "5 дней")
     */
    fun format(
        days: Int,
        resourceProvider: ResourceProvider,
    ): String

    /**
     * Форматирует количество месяцев в строку.
     *
     * @param months Количество месяцев
     * @param resourceProvider Провайдер строковых ресурсов
     * @return Отформатированная строка (например, "2 месяца")
     */
    fun formatMonths(
        months: Int,
        resourceProvider: ResourceProvider,
    ): String

    /**
     * Форматирует количество лет в строку.
     *
     * @param years Количество лет
     * @param resourceProvider Провайдер строковых ресурсов
     * @return Отформатированная строка (например, "1 год")
     */
    fun formatYears(
        years: Int,
        resourceProvider: ResourceProvider,
    ): String

    /**
     * Форматирует период времени в строку согласно опции отображения.
     *
     * @param period Период времени (годы, месяцы, дни)
     * @param displayOption Опция отображения
     * @param resourceProvider Провайдер строковых ресурсов
     * @param totalDays Общее количество дней (используется для DisplayOption.DAY)
     * @param showMinus Показывать ли минус для отрицательных чисел
     * @return Отформатированная строка согласно опции отображения
     */
    fun formatComposite(
        period: TimePeriod,
        displayOption: DisplayOption,
        resourceProvider: ResourceProvider,
        totalDays: Int,
        showMinus: Boolean,
    ): String
}
