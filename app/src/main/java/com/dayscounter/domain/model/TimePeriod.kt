package com.dayscounter.domain.model

/**
 * DTO для хранения вычисленного периода времени между двумя датами.
 *
 * @property years Полные годы в периоде
 * @property months Полные месяцы в периоде (после вычета лет)
 * @property days Оставшиеся дни в периоде (после вычета месяцев)
 */
data class TimePeriod(
    val years: Int = 0,
    val months: Int = 0,
    val days: Int = 0,
) {
    /**
     * Проверяет, является ли период пустым (все значения равны нулю).
     *
     * @return true, если годы, месяцы и дни равны 0, иначе false
     */
    fun isEmpty(): Boolean = years == 0 && months == 0 && days == 0

    /**
     * Проверяет, является ли период не пустым (хотя бы одно значение не равно нулю).
     *
     * @return true, если хотя бы одно из значений не равно 0, иначе false
     */
    fun isNotEmpty(): Boolean = !isEmpty()
}
