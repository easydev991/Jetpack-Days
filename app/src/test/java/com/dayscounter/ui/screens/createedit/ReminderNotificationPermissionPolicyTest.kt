package com.dayscounter.ui.screens.createedit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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

    @Test
    fun decide_reminder_activation_when_permission_granted_and_notifications_enabled_then_enable() {
        val decision =
            decideReminderActivation(
                hasPostNotificationsPermission = true,
                areReminderNotificationsEnabled = true
            )

        assertEquals(ReminderActivationDecision.ENABLE, decision)
    }

    @Test
    fun decide_reminder_activation_when_permission_denied_then_show_notification_settings_feedback() {
        val decision =
            decideReminderActivation(
                hasPostNotificationsPermission = false,
                areReminderNotificationsEnabled = false
            )

        assertEquals(ReminderActivationDecision.SHOW_NOTIFICATION_SETTINGS_FEEDBACK, decision)
    }

    @Test
    fun decide_reminder_activation_when_notifications_disabled_then_show_notification_settings_feedback() {
        val decision =
            decideReminderActivation(
                hasPostNotificationsPermission = true,
                areReminderNotificationsEnabled = false
            )

        assertEquals(ReminderActivationDecision.SHOW_NOTIFICATION_SETTINGS_FEEDBACK, decision)
    }

    @Test
    fun is_reminder_notifications_enabled_when_app_notifications_disabled_then_returns_false() {
        val isEnabled =
            isReminderNotificationsEnabled(
                sdkInt = 35,
                areAppNotificationsEnabled = false,
                reminderChannelImportance = 4
            )

        assertFalse(isEnabled)
    }

    @Test
    fun is_reminder_notifications_enabled_when_channel_disabled_then_returns_false() {
        val isEnabled =
            isReminderNotificationsEnabled(
                sdkInt = 35,
                areAppNotificationsEnabled = true,
                reminderChannelImportance = 0
            )

        assertFalse(isEnabled)
    }

    @Test
    fun is_reminder_notifications_enabled_when_channel_missing_on_android_oreo_plus_then_returns_true() {
        val isEnabled =
            isReminderNotificationsEnabled(
                sdkInt = 35,
                areAppNotificationsEnabled = true,
                reminderChannelImportance = null
            )

        assertTrue(isEnabled)
    }

    @Test
    fun is_reminder_notifications_enabled_when_app_and_channel_enabled_then_returns_true() {
        val isEnabled =
            isReminderNotificationsEnabled(
                sdkInt = 35,
                areAppNotificationsEnabled = true,
                reminderChannelImportance = 4
            )

        assertTrue(isEnabled)
    }

    @Test
    fun is_reminder_notifications_enabled_when_sdk_below_oreo_and_app_notifications_enabled_then_returns_true() {
        val isEnabled =
            isReminderNotificationsEnabled(
                sdkInt = 25,
                areAppNotificationsEnabled = true,
                reminderChannelImportance = null
            )

        assertTrue(isEnabled)
    }

    @Test
    fun decide_reminder_resume_sync_when_toggle_enabled_and_notifications_disabled_then_disable_and_show_feedback() {
        val decision =
            decideReminderResumeSync(
                isReminderEnabled = true,
                areReminderNotificationsEnabled = false
            )

        assertFalse(decision.shouldKeepReminderEnabled)
        assertTrue(decision.shouldShowNotificationsUnavailableFeedback)
    }

    @Test
    fun decide_reminder_resume_sync_when_toggle_enabled_and_notifications_enabled_then_keep_without_feedback() {
        val decision =
            decideReminderResumeSync(
                isReminderEnabled = true,
                areReminderNotificationsEnabled = true
            )

        assertTrue(decision.shouldKeepReminderEnabled)
        assertFalse(decision.shouldShowNotificationsUnavailableFeedback)
    }

    @Test
    fun decide_reminder_resume_sync_when_toggle_disabled_then_keep_without_feedback() {
        val decision =
            decideReminderResumeSync(
                isReminderEnabled = false,
                areReminderNotificationsEnabled = false
            )

        assertTrue(decision.shouldKeepReminderEnabled)
        assertFalse(decision.shouldShowNotificationsUnavailableFeedback)
    }
}
