package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Наблюдает за состоянием напоминания при возврате на экран.
 * Принимает plain-значения и callback'и вместо params.
 */
@Composable
internal fun ObserveReminderStateOnResume(
    isReminderEnabled: Boolean,
    onReminderDisabled: () -> Unit,
    onReminderNotificationsUnavailable: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, isReminderEnabled) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) {
                    return@LifecycleEventObserver
                }

                val syncDecision =
                    decideReminderResumeSync(
                        isReminderEnabled = isReminderEnabled,
                        areReminderNotificationsEnabled = context.areReminderNotificationsEnabled()
                    )

                if (!syncDecision.shouldKeepReminderEnabled) {
                    onReminderDisabled()
                }

                if (syncDecision.shouldShowNotificationsUnavailableFeedback) {
                    onReminderNotificationsUnavailable()
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
