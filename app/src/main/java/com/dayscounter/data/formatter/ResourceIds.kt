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

    /** Идентификатор строкового ресурса "Событие не найдено" */
    val EVENT_NOT_FOUND = R.string.event_not_found

    /** Идентификатор строкового ресурса "Ошибка загрузки события: %1$s" */
    val ERROR_LOADING_EVENT = R.string.error_loading_event

    /** Идентификатор строкового ресурса "Ошибка создания события: %1$s" */
    val ERROR_CREATING_EVENT = R.string.error_creating_event

    /** Идентификатор строкового ресурса "Ошибка обновления события: %1$s" */
    val ERROR_UPDATING_EVENT = R.string.error_updating_event

    /** Идентификатор строкового ресурса "Ошибка форматирования" */
    val ERROR_FORMATTING = R.string.error_formatting

    /** Идентификатор строкового ресурса "Ошибка при вычислении: %1$s" */
    val ERROR_CALCULATING = R.string.error_calculating

    /** Идентификатор строкового ресурса "Ошибка форматирования: %1$s" */
    val ERROR_FORMATTING_DETAILS = R.string.error_formatting_details
}
