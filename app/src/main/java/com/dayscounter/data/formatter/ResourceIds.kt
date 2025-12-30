package com.dayscounter.data.formatter

import com.dayscounter.R

/**
 * Константы для идентификаторов ресурсов форматирования дней.
 *
 * Используется для передачи resId в DaysFormatter без прямого использования R
 * в domain/data слоях.
 */
object ResourceIds {
    /** Идентификатор ресурса для множественного числа дней */
    val DAYS_COUNT = R.plurals.days_count

    /** Идентификатор ресурса для множественного числа месяцев */
    val MONTHS_COUNT = R.plurals.months_count

    /** Идентификатор ресурса для множественного числа лет */
    val YEARS_COUNT = R.plurals.years_count

    /** Идентификатор строкового ресурса "Сегодня" */
    val TODAY = R.string.today
}
