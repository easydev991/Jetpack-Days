package com.dayscounter.domain.model

/**
 * Результат вычисления разницы между датами.
 *
 * Используется для представления результата вычисления разницы между датой события
 * и текущей датой. Может быть либо "Сегодня" (если разница равна 0), либо
 * вычисленным периодом времени.
 */
sealed class DaysDifference {
    /**
     * Состояние, когда разница между датами равна 0 (сегодня).
     *
     * @property timestamp Временная метка события в миллисекундах
     */
    data class Today(
        val timestamp: Long,
    ) : DaysDifference()

    /**
     * Состояние, когда разница между датами больше 0 дней.
     *
     * @property period Вычисленный период времени (годы, месяцы, дни)
     * @property totalDays Общее количество дней разницы (используется для опции DAY)
     * @property timestamp Временная метка события в миллисекундах
     */
    data class Calculated(
        val period: TimePeriod,
        val totalDays: Int,
        val timestamp: Long,
    ) : DaysDifference()
}
