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
    val days: Int = 0
)
