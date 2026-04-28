package com.dayscounter.ui.screens.createedit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReminderNotificationPermissionPolicyTest {
    @Test
    fun decideremindertoggle_when_toggled_off_then_returns_disable() {
        val decision =
            decideReminderToggle(
                isChecked = false,
                sdkInt = 35,
                hasPostNotificationsPermission = false
            )

        assertEquals(ReminderToggleDecision.DISABLE, decision)
    }

    @Test
    fun decideremindertoggle_when_sdk_below_33_then_enables_without_permission_request() {
        val decision =
            decideReminderToggle(
                isChecked = true,
                sdkInt = 32,
                hasPostNotificationsPermission = false
            )

        assertEquals(ReminderToggleDecision.ENABLE, decision)
    }

    @Test
    fun decideremindertoggle_when_sdk_33_and_permission_granted_then_enables() {
        val decision =
            decideReminderToggle(
                isChecked = true,
                sdkInt = 33,
                hasPostNotificationsPermission = true
            )

        assertEquals(ReminderToggleDecision.ENABLE, decision)
    }

    @Test
    fun decideremindertoggle_when_sdk_33_and_permission_not_granted_then_requests_permission() {
        val decision =
            decideReminderToggle(
                isChecked = true,
                sdkInt = 33,
                hasPostNotificationsPermission = false
            )

        assertEquals(ReminderToggleDecision.REQUEST_PERMISSION, decision)
    }
}
