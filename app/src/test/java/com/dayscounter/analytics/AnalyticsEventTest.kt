package com.dayscounter.analytics

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AnalyticsEventTest {
    @Test
    fun app_screen_contains_all_target_screens() {
        assertEquals("EventsScreen", AppScreen.EVENTS.screenName)
        assertEquals("DetailScreen", AppScreen.DETAIL.screenName)
        assertEquals("CreateEditScreen", AppScreen.CREATE_EDIT.screenName)
        assertEquals("MoreScreen", AppScreen.MORE.screenName)
        assertEquals("ThemeIconScreen", AppScreen.THEME_ICON.screenName)
        assertEquals("AppDataScreen", AppScreen.APP_DATA.screenName)
    }

    @Test
    fun user_action_type_contains_all_expected_values() {
        assertEquals("create", UserActionType.CREATE.value)
        assertEquals("edit", UserActionType.EDIT.value)
        assertEquals("delete", UserActionType.DELETE.value)
        assertEquals("sort", UserActionType.SORT.value)
        assertEquals("item_saved", UserActionType.ITEM_SAVED.value)
        assertEquals("icon_selected", UserActionType.ICON_SELECTED.value)
        assertEquals("create_backup", UserActionType.CREATE_BACKUP.value)
        assertEquals("restore_backup", UserActionType.RESTORE_BACKUP.value)
        assertEquals("delete_all_data", UserActionType.DELETE_ALL_DATA.value)
    }

    @Test
    fun app_error_operation_contains_all_expected_values() {
        assertEquals("set_icon", AppErrorOperation.SET_ICON.value)
        assertEquals("create_backup", AppErrorOperation.CREATE_BACKUP.value)
        assertEquals("restore_backup", AppErrorOperation.RESTORE_BACKUP.value)
        assertEquals("delete_all_data", AppErrorOperation.DELETE_ALL_DATA.value)
    }

    @Test
    fun user_action_icon_selected_stores_icon_name_correctly() {
        val iconName = "icon_sunny"
        val event = AnalyticsEvent.UserAction(UserActionType.ICON_SELECTED, iconName)

        assertEquals(UserActionType.ICON_SELECTED, event.action)
        assertEquals(iconName, event.iconName)
    }

    @Test
    fun screen_view_stores_screen_and_screen_class() {
        val screen = AppScreen.EVENTS
        val screenClass = "EventsScreen"
        val event = AnalyticsEvent.ScreenView(screen, screenClass)

        assertEquals(screen, event.screen)
        assertEquals(screenClass, event.screenClass)
    }

    @Test
    fun screen_view_screen_class_is_nullable() {
        val event = AnalyticsEvent.ScreenView(AppScreen.EVENTS)
        assertEquals(null, event.screenClass)
    }

    @Test
    fun app_error_stores_operation_and_throwable() {
        val operation = AppErrorOperation.CREATE_BACKUP
        val throwable = RuntimeException("Test error")
        val event = AnalyticsEvent.AppError(operation, throwable)

        assertEquals(operation, event.operation)
        assertEquals(throwable, event.throwable)
    }
}
