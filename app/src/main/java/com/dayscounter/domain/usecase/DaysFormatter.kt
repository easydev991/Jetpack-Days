package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.TimePeriod

/**
 * Интерфейс для форматирования разницы дней в строку.
 *
 * Форматирует количество дней в строку согласно опции отображения
 * с учётом локализации и множественного числа.
 */
interface DaysFormatter {
    /**
     * Форматирует период времени в строку согласно опции отображения.
     *
     * @param period Период времени (годы, месяцы, дни)
     * @param displayOption Опция отображения
     * @param textProvider Провайдер строк для форматирования
     * @param totalDays Общее количество дней (используется для DisplayOption.DAY)
     * @param showMinus Показывать ли минус для отрицательных чисел
     * @return Отформатированная строка согласно опции отображения
     */
    fun formatComposite(
        period: TimePeriod,
        displayOption: DisplayOption,
        textProvider: DaysTextProvider,
        totalDays: Int,
        showMinus: Boolean
    ): String
}
