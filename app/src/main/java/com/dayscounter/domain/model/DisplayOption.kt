package com.dayscounter.domain.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dayscounter.R

/**
 * Опция отображения дней для события.
 * Используется для форматирования количества дней в различных форматах.
 *
 * @property DAY Отображать только количество дней
 * @property MONTH_DAY Отображать месяц и день
 * @property YEAR_MONTH_DAY Отображать год, месяц и день
 */
enum class DisplayOption {
    /**
     * Отображать только количество дней (например, "5 дней")
     */
    DAY,

    /**
     * Отображать месяц и день (например, "5 дней (15 марта)")
     */
    MONTH_DAY,

    /**
     * Отображать год, месяц и день (например, "5 дней (15 марта 2024)")
     */
    YEAR_MONTH_DAY,

    ;

    companion object {
        /**
         * Значение по умолчанию: DAY
         */
        val DEFAULT = DAY

        /**
         * Преобразует строковое значение в DisplayOption.
         * Поддерживает camelCase формат для совместимости с iOS.
         *
         * @param value Строковое значение ("day", "monthDay", "yearMonthDay")
         * @return DisplayOption или DEFAULT, если значение неизвестно
         */
        fun fromString(value: String): DisplayOption =
            when (value.lowercase()) {
                "day" -> DAY
                "monthday", "month_day" -> MONTH_DAY
                "yearmonthday", "year_month_day" -> YEAR_MONTH_DAY
                else -> DEFAULT
            }
    }

    /**
     * Преобразует DisplayOption в строковое представление для JSON.
     * Возвращает camelCase формат для совместимости с iOS.
     *
     * @return Строковое представление в формате camelCase
     */
    fun toJsonString(): String =
        when (this) {
            DAY -> "day"
            MONTH_DAY -> "monthDay"
            YEAR_MONTH_DAY -> "yearMonthDay"
            DEFAULT -> "day"
        }

    /**
     * Возвращает локализованное название опции отображения.
     *
     * @return Локализованная строка с названием опции
     */
    @Composable
    fun getLocalizedTitle(): String =
        when (this) {
            DAY -> stringResource(R.string.days_only)
            MONTH_DAY -> stringResource(R.string.months_and_days)
            YEAR_MONTH_DAY -> stringResource(R.string.years_months_and_days)
            DEFAULT -> stringResource(R.string.days_only)
        }
}
