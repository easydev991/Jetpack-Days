package com.dayscounter.domain.model

/**
 * Перечисление доступных иконок приложения:
 * [AppIcon.DEFAULT] — основная (по умолчанию),
 * [AppIcon.ICON_2] — второй вариант, [AppIcon.ICON_3] — третий,
 * [AppIcon.ICON_4] — четвёртый, [AppIcon.ICON_5] — пятый, [AppIcon.ICON_6] — шестой.
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
