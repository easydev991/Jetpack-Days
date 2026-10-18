package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption

/**
 * Use case для форматирования разницы дней в текст.
 *
 * Использует [DaysFormatter] для форматирования результата вычисления
 * разницы дат в соответствии с выбранной опцией отображения.
 */
class FormatDaysTextUseCase(
    private val daysFormatter: DaysFormatter
) {
    /**
     * Форматирует разницу дней в текст.
     *
     * @param difference Результат вычисления разницы дат
     * @param displayOption Опция отображения
     * @param textProvider Провайдер семантических строк
     * @param showMinus Показывать ли минус для отрицательных чисел (по умолчанию true)
     * @return Отформатированная строка
     */
    operator fun invoke(
        difference: DaysDifference,
        displayOption: DisplayOption,
        textProvider: DaysTextProvider,
        showMinus: Boolean
    ): String =
        when (difference) {
            is DaysDifference.Today -> {
                // Возвращаем локализованную строку "Сегодня"
                textProvider.today()
            }

            is DaysDifference.Calculated -> {
                // Форматируем период согласно опции отображения
                // Передаем оригинальное totalDays, чтобы форматировщик мог
                // правильно определить будущее (totalDays < 0)
                daysFormatter.formatComposite(
                    period = difference.period,
                    displayOption = displayOption,
                    textProvider = textProvider,
                    totalDays = difference.totalDays,
                    showMinus = showMinus
                )
            }
        }
}
