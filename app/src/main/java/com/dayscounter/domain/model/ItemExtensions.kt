package com.dayscounter.domain.model

import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import java.time.LocalDate

/**
 * Методы расширения для [Item].
 *
 * Вычисляет и форматирует количество дней с момента события до текущей даты.
 *
 * Использует [GetFormattedDaysForItemUseCase] для получения форматированного текста.
 *
 * @param useCase Use case для форматирования дней
 * @param currentDate Текущая дата (опционально, для тестирования)
 * @param defaultDisplayOption Опция отображения по умолчанию
 * @return Форматированная строка с количеством дней (например, "5 дней", "1 год 2 месяца")
 */
fun Item.makeDaysCount(
    useCase: GetFormattedDaysForItemUseCase,
    currentDate: LocalDate? = null,
    defaultDisplayOption: DisplayOption = DisplayOption.DAY,
): String =
    useCase(
        item = this,
        currentDate = currentDate,
        defaultDisplayOption = defaultDisplayOption,
    )
