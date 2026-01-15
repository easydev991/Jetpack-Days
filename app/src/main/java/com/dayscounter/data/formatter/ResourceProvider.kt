package com.dayscounter.data.formatter

/**
 * Абстракция над Context.getString() для обеспечения тестируемости.
 *
 * Позволяет мокировать предоставление строковых ресурсов в тестах
 * и разрывает зависимость от Android Context в domain/data слоях.
 */
interface ResourceProvider {
    /**
     * Возвращает строковый ресурс с форматированием.
     *
     * @param resId Идентификатор строкового ресурса
     * @param formatArgs Аргументы для форматирования строки
     * @return Форматированная строка
     */
    fun getString(
        resId: Int,
        vararg formatArgs: Any,
    ): String

    /**
     * Возвращает строковый ресурс для множественного числа дней.
     *
     * @param resId Идентификатор ресурса множественного числа (R.plurals.days_count)
     * @param quantity Количество для выбора правильной формы
     * @param formatArgs Аргументы для форматирования строки
     * @return Форматированная строка с правильной формой множественного числа
     */
    fun getQuantityString(
        resId: Int,
        quantity: Int,
        vararg formatArgs: Any,
    ): String

    /**
     * Возвращает строковый ресурс для множественного числа лет.
     *
     * @param quantity Количество лет
     * @return Форматированная строка (год/года/лет)
     */
    fun getYearsString(quantity: Int): String

    /**
     * Возвращает строковый ресурс для множественного числа месяцев.
     *
     * @param quantity Количество месяцев
     * @return Форматированная строка (месяц/месяца/месяцев)
     */
    fun getMonthsString(quantity: Int): String
}
