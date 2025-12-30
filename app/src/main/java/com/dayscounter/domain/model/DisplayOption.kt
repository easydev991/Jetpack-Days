package com.dayscounter.domain.model

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
        fun fromString(value: String): DisplayOption {
            return when (value.lowercase()) {
                "day" -> DAY
                "monthday", "month_day" -> MONTH_DAY
                "yearmonthday", "year_month_day" -> YEAR_MONTH_DAY
                else -> DEFAULT
            }
        }
    }

    /**
     * Преобразует DisplayOption в строковое представление для JSON.
     * Возвращает camelCase формат для совместимости с iOS.
     *
     * @return Строковое представление в формате camelCase
     */
    fun toJsonString(): String {
        return when (this) {
            DAY -> "day"
            MONTH_DAY -> "monthDay"
            YEAR_MONTH_DAY -> "yearMonthDay"
        }
    }
}
