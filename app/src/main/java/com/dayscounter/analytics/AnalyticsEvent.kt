package com.dayscounter.analytics

/**
 * Имена экранов для логирования screen_view событий.
 */
enum class AppScreen(
    val screenName: String
) {
    EVENTS("EventsScreen"),
    DETAIL("DetailScreen"),
    CREATE_EDIT("CreateEditScreen"),
    MORE("MoreScreen"),
    THEME_ICON("ThemeIconScreen"),
    APP_DATA("AppDataScreen")
}

/**
 * Типы пользовательских действий для логирования.
 */
enum class UserActionType(
    val value: String
) {
    CREATE("create"),
    EDIT("edit"),
    DELETE("delete"),
    SORT("sort"),
    OPEN_FILTER("open_filter"),
    ITEM_SAVED("item_saved"),
    ICON_SELECTED("icon_selected"),
    CREATE_BACKUP("create_backup"),
    RESTORE_BACKUP("restore_backup"),
    DELETE_ALL_DATA("delete_all_data")
}

/**
 * Типы операций для логирования ошибок.
 */
enum class AppErrorOperation(
    val value: String
) {
    SET_ICON("set_icon"),
    CREATE_BACKUP("create_backup"),
    RESTORE_BACKUP("restore_backup"),
    DELETE_ALL_DATA("delete_all_data"),
    CREATE_ITEM("create_item"),
    UPDATE_ITEM("update_item")
}

/**
 * События аналитики.
 */
sealed interface AnalyticsEvent {
    /**
     * Событие просмотра экрана.
     *
     * @param screen Экран, который просматривается
     * @param screenClass Полное имя класса экрана (опционально)
     */
    data class ScreenView(
        val screen: AppScreen,
        val screenClass: String? = null
    ) : AnalyticsEvent

    /**
     * Событие пользовательского действия.
     *
     * @param action Тип действия
     * @param iconName Название иконки (только для ICON_SELECTED)
     */
    data class UserAction(
        val action: UserActionType,
        val iconName: String? = null
    ) : AnalyticsEvent

    /**
     * Событие ошибки приложения.
     *
     * @param operation Операция, в которой произошла ошибка
     * @param throwable Исключение
     */
    data class AppError(
        val operation: AppErrorOperation,
        val throwable: Throwable
    ) : AnalyticsEvent
}
