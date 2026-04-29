package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.dayscounter.R
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.usecase.ReminderRequest
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * UI-состояние блока напоминания на форме Create/Edit.
 */
data class ReminderFormUiState(
    val isEnabled: MutableState<Boolean> = mutableStateOf(false),
    val mode: MutableState<ReminderMode> = mutableStateOf(ReminderMode.AT_DATE),
    val selectedDate: MutableState<LocalDate?> = mutableStateOf(defaultReminderDate()),
    val showDatePicker: MutableState<Boolean> = mutableStateOf(false),
    val hour: MutableState<Int> = mutableStateOf(LocalTime.now().hour),
    val minute: MutableState<Int> = mutableStateOf(LocalTime.now().minute),
    val intervalValue: MutableState<String> = mutableStateOf(""),
    val intervalUnit: MutableState<ReminderIntervalUnit> = mutableStateOf(ReminderIntervalUnit.DAY),
    val isInitializedFromSource: MutableState<Boolean> = mutableStateOf(false)
)

internal fun defaultReminderDate(today: LocalDate = LocalDate.now()): LocalDate = today.plusDays(1)

internal fun isCreateEditFormValid(
    title: String,
    selectedDate: LocalDate?,
    reminderUiState: ReminderFormUiState,
    currentDateTime: LocalDateTime = LocalDateTime.now()
): Boolean = title.isNotEmpty() && selectedDate != null && reminderUiState.isInputValid(currentDateTime)

internal fun ReminderFormUiState.toReminderRequest(itemId: Long): ReminderRequest? {
    if (!isEnabled.value) {
        return null
    }

    return when (mode.value) {
        ReminderMode.AT_DATE ->
            ReminderRequest(
                itemId = itemId,
                mode = ReminderMode.AT_DATE,
                atDate = selectedDate.value,
                atTime = LocalTime.of(hour.value, minute.value)
            )

        ReminderMode.AFTER_INTERVAL ->
            ReminderRequest(
                itemId = itemId,
                mode = ReminderMode.AFTER_INTERVAL,
                afterAmount = intervalValue.value.toIntOrNull(),
                afterUnit = intervalUnit.value
            )
    }
}

internal fun ReminderFormUiState.isInputValid(currentDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    val isReminderEnabled = isEnabled.value
    val currentMode = mode.value

    if (!isReminderEnabled) return true

    return when (currentMode) {
        ReminderMode.AT_DATE ->
            selectedDate.value?.let { date ->
                LocalDateTime.of(date, LocalTime.of(hour.value, minute.value)).isAfter(currentDateTime)
            } ?: false

        ReminderMode.AFTER_INTERVAL ->
            intervalValue.value
                .toIntOrNull()
                ?.let { amount -> amount >= 1 } ?: false
    }
}

internal fun ReminderFormUiState.validationErrorResId(currentDateTime: LocalDateTime = LocalDateTime.now()): Int? {
    if (!isEnabled.value) {
        return null
    }

    return when (mode.value) {
        ReminderMode.AT_DATE -> {
            val isFuture =
                selectedDate.value?.let { date ->
                    LocalDateTime.of(date, LocalTime.of(hour.value, minute.value)).isAfter(currentDateTime)
                } == true
            if (isFuture) {
                null
            } else {
                R.string.reminder_error_past_datetime
            }
        }

        ReminderMode.AFTER_INTERVAL ->
            intervalValue.value
                .toIntOrNull()
                ?.takeIf { amount -> amount >= 1 }
                ?.let { null } ?: R.string.reminder_error_invalid_amount
    }
}

internal fun ReminderFormUiState.toChangeFingerprint(): String? {
    if (!isEnabled.value) {
        return null
    }

    return when (mode.value) {
        ReminderMode.AT_DATE -> {
            val datePart = selectedDate.value?.toString().orEmpty()
            "${mode.value.name}:$datePart:${hour.value}:${minute.value}"
        }

        ReminderMode.AFTER_INTERVAL ->
            "${mode.value.name}:${intervalValue.value}:${intervalUnit.value.name}"
    }
}

internal fun Reminder?.toChangeFingerprint(): String? {
    if (this == null) {
        return null
    }

    return when (mode) {
        ReminderMode.AT_DATE -> {
            val zoneId = ZoneId.systemDefault()
            val datePart =
                selectedDateEpochMillis?.let { millis ->
                    Instant
                        .ofEpochMilli(millis)
                        .atZone(zoneId)
                        .toLocalDate()
                        .toString()
                } ?: Instant
                    .ofEpochMilli(targetEpochMillis)
                    .atZone(zoneId)
                    .toLocalDate()
                    .toString()
            "${mode.name}:$datePart:${selectedHour ?: -1}:${selectedMinute ?: -1}"
        }

        ReminderMode.AFTER_INTERVAL ->
            "${mode.name}:${intervalAmount ?: -1}:${intervalUnit?.name.orEmpty()}"
    }
}

internal fun ReminderFormUiState.applyReminder(reminder: Reminder?) {
    isInitializedFromSource.value = true

    if (reminder == null) {
        isEnabled.value = false
        mode.value = ReminderMode.AT_DATE
        return
    }

    isEnabled.value = true
    mode.value = reminder.mode

    when (reminder.mode) {
        ReminderMode.AT_DATE -> {
            val zoneId = ZoneId.systemDefault()
            selectedDate.value =
                reminder.selectedDateEpochMillis?.let { millis ->
                    Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                } ?: Instant.ofEpochMilli(reminder.targetEpochMillis).atZone(zoneId).toLocalDate()
            hour.value = reminder.selectedHour ?: 0
            minute.value = reminder.selectedMinute ?: 0
        }

        ReminderMode.AFTER_INTERVAL -> {
            intervalValue.value = reminder.intervalAmount?.toString().orEmpty()
            intervalUnit.value = reminder.intervalUnit ?: ReminderIntervalUnit.DAY
        }
    }
}
