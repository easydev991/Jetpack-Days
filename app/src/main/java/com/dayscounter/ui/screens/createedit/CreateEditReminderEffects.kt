package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dayscounter.ui.viewmodel.CreateEditChangeInput
import java.time.ZoneId

@Composable
internal fun rememberOnCreateEditValueChange(params: CreateEditFormParams): () -> Unit =
    {
        if (params.itemId != null) {
            val timestamp =
                params.uiStates.selectedDate.value
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: 0L

            params.viewModel.checkHasChanges(
                CreateEditChangeInput(
                    title = params.uiStates.title.value,
                    details = params.uiStates.details.value,
                    timestamp = timestamp,
                    colorTag =
                        params.uiStates.selectedColor.value
                            ?.toArgb(),
                    displayOption = params.uiStates.selectedDisplayOption.value,
                    reminderFingerprint = params.uiStates.reminder.toChangeFingerprint()
                )
            )
        }
    }

@Composable
internal fun ObserveReminderStateOnResume(
    params: CreateEditFormParams,
    onValueChange: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, params.uiStates.reminder.isEnabled.value) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event != Lifecycle.Event.ON_RESUME) {
                    return@LifecycleEventObserver
                }

                val syncDecision =
                    decideReminderResumeSync(
                        isReminderEnabled = params.uiStates.reminder.isEnabled.value,
                        areReminderNotificationsEnabled = context.areReminderNotificationsEnabled()
                    )

                if (!syncDecision.shouldKeepReminderEnabled) {
                    params.uiStates.reminder.isEnabled.value = false
                    onValueChange()
                }

                if (syncDecision.shouldShowNotificationsUnavailableFeedback) {
                    params.onReminderNotificationsUnavailable()
                }
            }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}
