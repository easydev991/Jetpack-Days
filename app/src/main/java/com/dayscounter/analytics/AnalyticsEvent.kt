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
 * Исходы авто-запроса review-флоу.
 */
enum class ReviewAutoRequestOutcome(
    val value: String
) {
    SHOWN("shown"),
    ERROR("error"),
    CANCELLED("cancelled")
}

/**
 * Типы ошибок авто-запроса review-флоу.
 *
 * Семантика вариантов повторяет доменный [com.dayscounter.domain.exception.ReviewException],
 * но доменные классы в аналитику не тянутся.
 */
enum class ReviewErrorType(
    val value: String
) {
    NOT_INSTALLED("not_installed"),
    OUTDATED("outdated"),
    NOT_AUTHORIZED("not_authorized"),
    REQUEST_LIMIT_REACHED("request_limit_reached"),
    REVIEW_ALREADY_EXISTS("review_already_exists"),
    UNKNOWN("unknown")
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

    /**
     * Событие авто-запроса review-флоу (milestone-триггер).
     *
     * @param outcome Исход: форма показана или ошибка
     * @param errorType Тип ошибки (только для [ReviewAutoRequestOutcome.ERROR])
     */
    data class ReviewAutoRequest(
        val outcome: ReviewAutoRequestOutcome,
        val errorType: ReviewErrorType? = null
    ) : AnalyticsEvent
}
