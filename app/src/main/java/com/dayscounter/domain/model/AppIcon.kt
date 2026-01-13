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
    ICON_6,
    ;

    /**
     * Возвращает имя компонента Activity Alias для текущей иконки и темы.
     *
     * @param isDarkTheme Признак темной темы
     * @return Имя класса Activity Alias
     */
    fun getComponentName(isDarkTheme: Boolean): String {
        val suffix = if (isDarkTheme) "Dark" else ""
        return when (this) {
            // DEFAULT иконка учитывает тему (светлая/тёмная версия)
            DEFAULT -> "com.dayscounter.MainActivityAliasIcon1$suffix"
            ICON_2 -> "com.dayscounter.MainActivityIcon2$suffix"
            ICON_3 -> "com.dayscounter.MainActivityIcon3$suffix"
            ICON_4 -> "com.dayscounter.MainActivityIcon4$suffix"
            ICON_5 -> "com.dayscounter.MainActivityIcon5$suffix"
            ICON_6 -> "com.dayscounter.MainActivityIcon6$suffix"
        }
    }
}
