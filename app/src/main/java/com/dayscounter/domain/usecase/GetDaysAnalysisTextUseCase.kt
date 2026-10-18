package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.Item
import java.time.LocalDate

/**
 * UseCase для получения полного текста анализа дней с префиксом.
 *
 * Используется на детальном экране для отображения текста с префиксом
 * "осталось"/"прошло" и количеством дней без знака минус.
 *
 * ВАЖНО: Для форматирования использует существующий GetFormattedDaysForItemUseCase,
 * который правильно учитывает displayOption и форматирует текст в соответствии
 * с выбранной опцией отображения (дни, месяцы+дни, годы+месяцы+дни).
 *
 * @property calculateDaysDifferenceUseCase Use case для вычисления разницы дат
 * @property getFormattedDaysForItemUseCase Use case для получения форматированного текста
 * @property textProvider Провайдер семантических строк для локализации
 */
class GetDaysAnalysisTextUseCase(
    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
    private val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    private val textProvider: DaysTextProvider
) {
    /**
     * Получает полный текст анализа дней с префиксом.
     *
     * @param item Элемент для анализа
     * @param currentDate Текущая дата (опционально, для тестирования)
     * @return Полный текст анализа (например, "осталось 7 дней", "прошло 1 год 2 месяца", "Сегодня")
     */
    operator fun invoke(
        item: Item,
        currentDate: LocalDate? = null
    ): String {
        val difference =
            calculateDaysDifferenceUseCase(
                eventTimestamp = item.timestamp,
                currentDate = currentDate ?: LocalDate.now()
            )

        return when (difference) {
            is DaysDifference.Today -> {
                textProvider.today()
            }

            is DaysDifference.Calculated -> {
                if (difference.totalDays == 0) {
                    textProvider.today()
                } else {
                    // Определяем префикс на основе знака totalDays
                    val prefix =
                        when {
                            difference.totalDays < 0 -> textProvider.remaining()
                            else -> textProvider.elapsed()
                        }

                    // Используем существующий use case для форматирования
                    // с учетом displayOption из item
                    val formattedDays =
                        getFormattedDaysForItemUseCase(
                            item = item,
                            currentDate = currentDate,
                            showMinus = false
                        )

                    "$prefix $formattedDays"
                }
            }
        }
    }
}
