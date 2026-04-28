package com.dayscounter.ui.screens.createedit

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

internal const val NOTIFICATION_PERMISSION_RUNTIME_SDK = 33

internal enum class ReminderToggleDecision {
    ENABLE,
    DISABLE,
    REQUEST_PERMISSION
}

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
