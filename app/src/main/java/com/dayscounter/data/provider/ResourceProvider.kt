package com.dayscounter.data.provider

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
        vararg formatArgs: Any
    ): String
}
