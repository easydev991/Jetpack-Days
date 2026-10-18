package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.util.CrashReporter
import com.dayscounter.util.Logger
import java.time.LocalDate

/**
 * UseCase для получения форматированного текста количества дней для Item.
 *
 * Использует CalculateDaysDifferenceUseCase и FormatDaysTextUseCase
 * для вычисления и форматирования разницы между датой события и текущим днем.
 *
 * @property calculateDaysDifferenceUseCase Use case для вычисления разницы дат
 * @property formatDaysTextUseCase Use case для форматирования текста
 * @property textProvider Провайдер семантических строк для локализации
 * @property crashReporter Канал репортинга сбоев (в release — Firebase Crashlytics)
 * @property logger Logger для логирования
 */
class GetFormattedDaysForItemUseCase(
    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
    private val formatDaysTextUseCase: FormatDaysTextUseCase,
    private val textProvider: DaysTextProvider,
    private val crashReporter: CrashReporter,
    private val logger: Logger
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
        showMinus: Boolean
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
                currentDate = currentDate ?: LocalDate.now()
            )

        // Форматируем результат в текст
        @Suppress("TooGenericExceptionCaught")
        return try {
            formatDaysTextUseCase(
                difference = difference,
                displayOption = displayOption,
                textProvider = textProvider,
                showMinus = showMinus
            )
        } catch (e: Exception) {
            // Обрабатываем исключения при форматировании
            val message = "Ошибка форматирования: ${e.message}"
            logger.e("GetFormattedDaysForItemUseCase", message, e)
            // Тот же текст, что и в logcat: канал одного сообщения
            crashReporter.logException(e, message)
            // Вычисляем общее количество дней как заглушку
            when (difference) {
                is DaysDifference.Today -> textProvider.today()
                is DaysDifference.Calculated ->
                    "${textProvider.formattingError()}: ${difference.totalDays}"
            }
        }
    }
}
