package com.dayscounter.ui.screens.createedit

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
 *
 * Plain data class без MutableState — иммутабельный контракт.
 * Мутация через copy().
 */
data class ReminderFormUiState(
    val isEnabled: Boolean = false,
    val mode: ReminderMode = ReminderMode.AT_DATE,
    val selectedDate: LocalDate? = defaultReminderDate(),
    val showDatePicker: Boolean = false,
    val hour: Int = LocalTime.now().hour,
    val minute: Int = LocalTime.now().minute,
    val intervalValue: String = "",
    val intervalUnit: ReminderIntervalUnit = ReminderIntervalUnit.DAY,
    val isInitializedFromSource: Boolean = false
)

internal fun defaultReminderDate(today: LocalDate = LocalDate.now()): LocalDate = today.plusDays(1)

internal fun isCreateEditFormValid(
    title: String,
    selectedDate: LocalDate?,
    reminderUiState: ReminderFormUiState,
    currentDateTime: LocalDateTime = LocalDateTime.now()
): Boolean = title.isNotEmpty() && selectedDate != null && reminderUiState.isInputValid(currentDateTime)

internal fun ReminderFormUiState.toReminderRequest(itemId: Long): ReminderRequest? {
    if (!isEnabled) {
        return null
    }

    return when (mode) {
        ReminderMode.AT_DATE ->
            ReminderRequest(
                itemId = itemId,
                mode = ReminderMode.AT_DATE,
                atDate = selectedDate,
                atTime = LocalTime.of(hour, minute)
            )

        ReminderMode.AFTER_INTERVAL ->
            ReminderRequest(
                itemId = itemId,
                mode = ReminderMode.AFTER_INTERVAL,
                afterAmount = intervalValue.toIntOrNull(),
                afterUnit = intervalUnit
            )
    }
}

internal fun ReminderFormUiState.isInputValid(currentDateTime: LocalDateTime = LocalDateTime.now()): Boolean {
    if (!isEnabled) return true

    return when (mode) {
        ReminderMode.AT_DATE ->
            selectedDate?.let { date ->
                LocalDateTime.of(date, LocalTime.of(hour, minute)).isAfter(currentDateTime)
            } ?: false

        ReminderMode.AFTER_INTERVAL ->
            intervalValue
                .toIntOrNull()
                ?.let { amount -> amount >= 1 } ?: false
    }
}

internal fun ReminderFormUiState.validationErrorResId(currentDateTime: LocalDateTime = LocalDateTime.now()): Int? {
    if (!isEnabled) {
        return null
    }

    return when (mode) {
        ReminderMode.AT_DATE -> {
            val isFuture =
                selectedDate?.let { date ->
                    LocalDateTime.of(date, LocalTime.of(hour, minute)).isAfter(currentDateTime)
                } == true
            if (isFuture) {
                null
            } else {
                R.string.reminder_error_past_datetime
            }
        }

        ReminderMode.AFTER_INTERVAL ->
            intervalValue
                .toIntOrNull()
                ?.takeIf { amount -> amount >= 1 }
                ?.let { null } ?: R.string.reminder_error_invalid_amount
    }
}

internal fun ReminderFormUiState.toChangeFingerprint(): String? {
    if (!isEnabled) {
        return null
    }

    return when (mode) {
        ReminderMode.AT_DATE -> {
            val datePart = selectedDate?.toString().orEmpty()
            "${mode.name}:$datePart:$hour:$minute"
        }

        ReminderMode.AFTER_INTERVAL ->
            "${mode.name}:$intervalValue:${intervalUnit.name}"
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

/**
 * Применяет данные напоминания к UI-состоянию.
 * Возвращает новое состояние через copy().
 */
internal fun ReminderFormUiState.applyReminder(reminder: Reminder?): ReminderFormUiState {
    if (reminder == null) {
        return copy(isInitializedFromSource = true, isEnabled = false, mode = ReminderMode.AT_DATE)
    }

    return when (reminder.mode) {
        ReminderMode.AT_DATE -> {
            val zoneId = ZoneId.systemDefault()
            val date =
                reminder.selectedDateEpochMillis?.let { millis ->
                    Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
                } ?: Instant.ofEpochMilli(reminder.targetEpochMillis).atZone(zoneId).toLocalDate()

            copy(
                isInitializedFromSource = true,
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate = date,
                hour = reminder.selectedHour ?: 0,
                minute = reminder.selectedMinute ?: 0
            )
        }

        ReminderMode.AFTER_INTERVAL ->
            copy(
                isInitializedFromSource = true,
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = reminder.intervalAmount?.toString().orEmpty(),
                intervalUnit = reminder.intervalUnit ?: ReminderIntervalUnit.DAY
            )
    }
}
