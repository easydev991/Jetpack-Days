package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

// ponytail: 4× дублирование atTime→atZone→toInstant→toEpochMilli вынесено в extension.
// time-of-day = now() — допустимо, т.к. LocalDate не несёт значимого времени;
// реальный источник истины — Item.timestamp в БД, загружаемый через loadItemData после restore.
private fun LocalDate.toBundleMillis(): Long =
    this
        .atTime(LocalTime.now())
        .atZone(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()

/**
 * Saver для ReminderFormUiState.
 * Сохраняет все поля как список примитивов.
 */
@Suppress("SwallowedException", "MagicNumber")
val ReminderFormUiStateSaver: Saver<ReminderFormUiState, List<Any?>> =
    Saver(
        save = { state ->
            listOf(
                state.isEnabled,
                state.mode.name,
                state.selectedDate?.toBundleMillis(),
                state.showDatePicker,
                state.hour,
                state.minute,
                state.intervalValue,
                state.intervalUnit.name,
                state.isInitializedFromSource
            )
        },
        restore = { saved ->
            @Suppress("UNCHECKED_CAST")
            ReminderFormUiState(
                isEnabled = saved[0] as Boolean,
                mode =
                    try {
                        ReminderMode.valueOf(saved[1] as String)
                    } catch (e: IllegalArgumentException) {
                        ReminderMode.AT_DATE
                    },
                selectedDate =
                    (saved[2] as Long?)?.let { millis ->
                        if (millis == -1L) {
                            null
                        } else {
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                    },
                showDatePicker = saved[3] as Boolean,
                hour = saved[4] as Int,
                minute = saved[5] as Int,
                intervalValue = saved[6] as String,
                intervalUnit =
                    try {
                        ReminderIntervalUnit.valueOf(saved[7] as String)
                    } catch (e: IllegalArgumentException) {
                        ReminderIntervalUnit.DAY
                    },
                isInitializedFromSource = saved[8] as Boolean
            )
        }
    )

/**
 * Saver для CreateEditUiState.
 * Сохраняет все поля как список значений.
 */
@Suppress("SwallowedException", "MagicNumber")
val CreateEditUiStateSaver: Saver<CreateEditUiState, List<Any?>> =
    Saver(
        save = { state ->
            listOf(
                state.title,
                state.details,
                state.selectedDate?.toBundleMillis(),
                state.selectedColor?.toArgb(),
                state.selectedDisplayOption.name,
                state.showDatePicker,
                with(ReminderFormUiStateSaver) { save(state.reminder) }
            )
        },
        restore = { saved ->
            @Suppress("UNCHECKED_CAST")
            CreateEditUiState(
                title = saved[0] as String,
                details = saved[1] as String,
                selectedDate =
                    (saved[2] as Long?)?.let { millis ->
                        if (millis == -1L) {
                            null
                        } else {
                            Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                        }
                    },
                selectedColor =
                    (saved[3] as Int?)?.let { argb ->
                        if (argb == -1) {
                            null
                        } else {
                            Color(argb)
                        }
                    },
                selectedDisplayOption =
                    try {
                        DisplayOption.valueOf(saved[4] as String)
                    } catch (e: IllegalArgumentException) {
                        DisplayOption.DAY
                    },
                showDatePicker = saved[5] as Boolean,
                reminder = ReminderFormUiStateSaver.restore(saved[6] as List<Any?>) ?: ReminderFormUiState()
            )
        }
    )
