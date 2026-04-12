package com.dayscounter.analytics

import org.junit.jupiter.api.Test

class NoopAnalyticsProviderTest {
    private val provider = NoopAnalyticsProvider()

    @Test
    fun log_screen_view_does_not_throw() {
        val event = AnalyticsEvent.ScreenView(AppScreen.EVENTS)
        provider.log(event)
    }

    @Test
    fun log_user_action_does_not_throw() {
        val event = AnalyticsEvent.UserAction(UserActionType.CREATE)
        provider.log(event)
    }

    @Test
    fun log_app_error_does_not_throw() {
        val event = AnalyticsEvent.AppError(AppErrorOperation.SET_ICON, RuntimeException())
        provider.log(event)
    }
}
