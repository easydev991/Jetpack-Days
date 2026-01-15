package com.dayscounter.domain.usecase

import com.dayscounter.data.formatter.DaysFormatter
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption

/**
 * Use case для форматирования разницы дней в текст.
 *
 * Использует [DaysFormatter] для форматирования результата вычисления
 * разницы дат в соответствии с выбранной опцией отображения.
 */
class FormatDaysTextUseCase(
    private val daysFormatter: DaysFormatter,
) {
    /**
     * Форматирует разницу дней в текст.
     *
     * @param difference Результат вычисления разницы дат
     * @param displayOption Опция отображения
     * @param resourceProvider Провайдер строковых ресурсов
     * @param showMinus Показывать ли минус для отрицательных чисел (по умолчанию true)
     * @return Отформатированная строка
     */
    operator fun invoke(
        difference: DaysDifference,
        displayOption: DisplayOption,
        resourceProvider: ResourceProvider,
        showMinus: Boolean,
    ): String =
        when (difference) {
            is DaysDifference.Today -> {
                // Возвращаем локализованную строку "Сегодня"
                resourceProvider.getString(com.dayscounter.data.formatter.ResourceIds.TODAY)
            }

            is DaysDifference.Calculated -> {
                // Определяем totalDays для форматирования
                val totalDaysToFormat =
                    if (showMinus) {
                        difference.totalDays
                    } else {
                        kotlin.math.abs(difference.totalDays)
                    }

                // Форматируем период согласно опции отображения
                daysFormatter.formatComposite(
                    period = difference.period,
                    displayOption = displayOption,
                    resourceProvider = resourceProvider,
                    totalDays = totalDaysToFormat,
                    showMinus = showMinus,
                )
            }
        }
}
