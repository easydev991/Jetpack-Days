package com.dayscounter.domain.usecase

import com.dayscounter.data.provider.ResourceIds
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import java.time.LocalDate

/**
 * UseCase для получения форматированного текста количества дней для Item.
 *
 * Использует CalculateDaysDifferenceUseCase и FormatDaysTextUseCase
 * для вычисления и форматирования разницы между датой события и текущим днем.
 *
 * @property calculateDaysDifferenceUseCase Use case для вычисления разницы дат
 * @property formatDaysTextUseCase Use case для форматирования текста
 * @property resourceProvider Провайдер строковых ресурсов для локализации
 */
class GetFormattedDaysForItemUseCase(
    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
    private val formatDaysTextUseCase: FormatDaysTextUseCase,
    private val resourceProvider: ResourceProvider,
) {
    /**
     * Получает форматированный текст количества дней для указанного Item.
     *
     * Использует DisplayOption из самого Item, если он установлен,
     * иначе использует переданную опцию отображения.
     *
     * @param item Элемент для которого нужно получить форматированный текст
     * @param currentDate Текущая дата (опционально, для тестирования)
     * @param defaultDisplayOption Опция отображения по умолчанию (если item.displayOption == DEFAULT)
     * @param showMinus Показывать ли минус для отрицательных чисел (по умолчанию true)
     * @return Форматированная строка с количеством дней (например, "5 дней", "1 год 2 месяца")
     */
    operator fun invoke(
        item: Item,
        currentDate: LocalDate? = null,
        defaultDisplayOption: DisplayOption = DisplayOption.DAY,
        showMinus: Boolean,
    ): String {
        // Определяем опцию отображения
        val displayOption =
            if (item.displayOption == DisplayOption.DEFAULT) {
                defaultDisplayOption
            } else {
                item.displayOption
            }

        // Вычисляем разницу между датами
        val difference: DaysDifference =
            calculateDaysDifferenceUseCase(
                eventTimestamp = item.timestamp,
                currentDate = currentDate ?: LocalDate.now(),
            )

        // Форматируем результат в текст
        @Suppress("TooGenericExceptionCaught")
        return try {
            formatDaysTextUseCase(
                difference = difference,
                displayOption = displayOption,
                resourceProvider = resourceProvider,
                showMinus = showMinus,
            )
        } catch (e: Exception) {
            // Обрабатываем исключения при форматировании
            val message = "Ошибка форматирования: ${e.message}"
            android.util.Log.e("GetFormattedDaysForItemUseCase", message, e)
            // Вычисляем общее количество дней как заглушку
            when (difference) {
                is DaysDifference.Today -> resourceProvider.getString(ResourceIds.TODAY)
                is DaysDifference.Calculated ->
                    "${resourceProvider.getString(ResourceIds.ERROR_FORMATTING)}: ${difference.totalDays}"
            }
        }
    }
}
