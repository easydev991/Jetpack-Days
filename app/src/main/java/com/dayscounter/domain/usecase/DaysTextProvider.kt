package com.dayscounter.domain.usecase

/**
 * Семантический провайдер строк для доменного форматирования дней.
 *
 * Домен не знает про `resId: Int` и `R` — методы сгруппированы по смыслу,
 * а не зеркалят константы ресурсов. Реализация поверх Android-ресурсов
 * живёт в data-слое (`DaysTextProviderImpl`); в тестах используется fake.
 */
interface DaysTextProvider {
    /** Строка «Сегодня» */
    fun today(): String

    /** Строка «осталось» */
    fun remaining(): String

    /** Строка «прошло» */
    fun elapsed(): String

    /** Строка «Ошибка форматирования» */
    fun formattingError(): String

    /** Локализованное множественное число дней (например, «5 дней») */
    fun daysCount(quantity: Int): String

    /** Локализованное множественное число месяцев (например, «2 месяца») */
    fun monthsCount(quantity: Int): String

    /** Локализованное множественное число лет (например, «1 год») */
    fun yearsCount(quantity: Int): String
}
