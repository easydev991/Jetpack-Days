package com.dayscounter.data.formatter

/**
 * Абстракция над Context.getString() для обеспечения тестируемости.
 *
 * Позволяет мокировать предоставление строковых ресурсов в тестах
 * и разрывает зависимость от Android Context.
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
     * Возвращает строковый ресурс для множественного числа.
     *
     * @param resId Идентификатор ресурса множественного числа
     * @param quantity Количество для выбора правильной формы
     * @param formatArgs Аргументы для форматирования строки
     * @return Форматированная строка с правильной формой множественного числа
     */
    fun getQuantityString(
        resId: Int,
        quantity: Int,
        vararg formatArgs: Any,
    ): String
}
