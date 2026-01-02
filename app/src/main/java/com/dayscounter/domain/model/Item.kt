package com.dayscounter.domain.model

/**
 * Доменная модель события для отслеживания дней.
 *
 * @property id Уникальный идентификатор события
 * @property title Название события (обязательное поле)
 * @property details Описание события (необязательное, по умолчанию пустая строка)
 * @property timestamp Дата события в миллисекундах с 1970-01-01
 * @property colorTag ARGB-цвет для цветовой метки (необязательное)
 * @property displayOption Опция отображения дней (по умолчанию DAY)
 */
data class Item(
    val id: Long = 0L,
    val title: String,
    val details: String = "",
    val timestamp: Long,
    val colorTag: Int? = null,
    val displayOption: DisplayOption = DisplayOption.DEFAULT,
) {
    /**
     * Вычисляет и форматирует количество дней с момента события до текущей даты.
     *
     * **Примечание**: Этот метод устарел. Используйте расширение [makeDaysCount]
     * из [ItemExtensions] с [GetFormattedDaysForItemUseCase].
     *
     * @param currentDate Текущая дата в миллисекундах (не используется)
     * @return Отформатированная строка с количеством дней (заглушка)
     * @deprecated Используйте Item.makeDaysCount(useCase, currentDate) вместо этого метода
     */
    @Deprecated(
        message = "Используйте Item.makeDaysCount(useCase, currentDate) вместо этого метода",
        replaceWith = ReplaceWith("makeDaysCount(useCase, currentDate)"),
    )
    @Suppress("FunctionOnlyReturningConstant", "SameReturnValue", "UnusedParameter")
    fun makeDaysCount(currentDate: Long): String {
        // Заглушка для обратной совместимости
        return "0 дней"
    }
}
