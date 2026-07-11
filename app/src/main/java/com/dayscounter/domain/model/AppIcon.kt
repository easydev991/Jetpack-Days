package com.dayscounter.domain.model

/**
 * Перечисление доступных иконок приложения.
 *
 * @property DEFAULT Основная иконка (по умолчанию)
 * @property ICON_2 Второй вариант иконки
 * @property ICON_3 Третий вариант иконки
 * @property ICON_4 Четвёртый вариант иконки
 * @property ICON_5 Пятый вариант иконки
 * @property ICON_6 Шестой вариант иконки
 */
enum class AppIcon {
    DEFAULT,
    ICON_2,
    ICON_3,
    ICON_4,
    ICON_5,
    ICON_6
    ;

    /**
     * Возвращает имя компонента Activity Alias для текущей иконки.
     *
     * @return Имя класса Activity Alias
     */
    fun getComponentName(): String =
        when (this) {
            DEFAULT -> "com.dayscounter.MainActivityAliasIcon1"
            ICON_2 -> "com.dayscounter.MainActivityIcon2"
            ICON_3 -> "com.dayscounter.MainActivityIcon3"
            ICON_4 -> "com.dayscounter.MainActivityIcon4"
            ICON_5 -> "com.dayscounter.MainActivityIcon5"
            ICON_6 -> "com.dayscounter.MainActivityIcon6"
        }
}
