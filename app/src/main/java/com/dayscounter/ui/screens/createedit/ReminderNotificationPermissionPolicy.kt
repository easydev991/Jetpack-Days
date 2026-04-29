package com.dayscounter.ui.screens.createedit

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dayscounter.reminder.ReminderIntentContract

internal const val NOTIFICATION_PERMISSION_RUNTIME_SDK = 33
private const val CHANNELS_AVAILABLE_SDK = 26

internal enum class ReminderToggleDecision {
    ENABLE,
    DISABLE,
    REQUEST_PERMISSION
}

internal enum class ReminderActivationDecision {
    ENABLE,
    SHOW_NOTIFICATION_SETTINGS_FEEDBACK
}

internal data class ReminderResumeSyncDecision(
    val shouldKeepReminderEnabled: Boolean,
    val shouldShowNotificationsUnavailableFeedback: Boolean
)

internal fun decideReminderToggle(
    isChecked: Boolean,
    sdkInt: Int,
    hasPostNotificationsPermission: Boolean
): ReminderToggleDecision {
    if (!isChecked) {
        return ReminderToggleDecision.DISABLE
    }

    val requiresRuntimePermission = sdkInt >= NOTIFICATION_PERMISSION_RUNTIME_SDK
    return if (requiresRuntimePermission && !hasPostNotificationsPermission) {
        ReminderToggleDecision.REQUEST_PERMISSION
    } else {
        ReminderToggleDecision.ENABLE
    }
}

internal fun Context.hasPostNotificationsPermission(sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
    if (sdkInt < NOTIFICATION_PERMISSION_RUNTIME_SDK) {
        return true
    }

    return ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED
}

internal fun decideReminderActivation(
    hasPostNotificationsPermission: Boolean,
    areReminderNotificationsEnabled: Boolean
): ReminderActivationDecision =
    if (hasPostNotificationsPermission && areReminderNotificationsEnabled) {
        ReminderActivationDecision.ENABLE
    } else {
        ReminderActivationDecision.SHOW_NOTIFICATION_SETTINGS_FEEDBACK
    }

internal fun decideReminderResumeSync(
    isReminderEnabled: Boolean,
    areReminderNotificationsEnabled: Boolean
): ReminderResumeSyncDecision {
    val shouldKeepReminderEnabled = !isReminderEnabled || areReminderNotificationsEnabled
    val shouldShowFeedback = isReminderEnabled && !areReminderNotificationsEnabled

    return ReminderResumeSyncDecision(
        shouldKeepReminderEnabled = shouldKeepReminderEnabled,
        shouldShowNotificationsUnavailableFeedback = shouldShowFeedback
    )
}

internal fun isReminderNotificationsEnabled(
    sdkInt: Int,
    areAppNotificationsEnabled: Boolean,
    reminderChannelImportance: Int?
): Boolean {
    val isChannelCheckNotRequired = sdkInt < CHANNELS_AVAILABLE_SDK
    val isChannelEnabled =
        reminderChannelImportance == null ||
            reminderChannelImportance != NotificationManager.IMPORTANCE_NONE

    return areAppNotificationsEnabled && (isChannelCheckNotRequired || isChannelEnabled)
}

internal fun Context.areReminderNotificationsEnabled(sdkInt: Int = Build.VERSION.SDK_INT): Boolean {
    val managerCompat = NotificationManagerCompat.from(this)
    val appNotificationsEnabled = managerCompat.areNotificationsEnabled()
    val channelImportance =
        if (sdkInt >= CHANNELS_AVAILABLE_SDK) {
            managerCompat.getNotificationChannel(ReminderIntentContract.CHANNEL_ID)?.importance
        } else {
            null
        }

    return isReminderNotificationsEnabled(
        sdkInt = sdkInt,
        areAppNotificationsEnabled = appNotificationsEnabled,
        reminderChannelImportance = channelImportance
    )
}

internal fun Context.openReminderNotificationSettings() {
    val intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    startActivity(intent)
}
