package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Saver для LocalDate (non-null).
 */
val LocalDateSaver: Saver<LocalDate, Long> =
    Saver(
        save = { localDate ->
            localDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        },
        restore = { epochMilli ->
            Instant
                .ofEpochMilli(epochMilli)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
    )

/**
 * Saver для Color (nullable).
 * Использует -1 как sentinel значение для null.
 */
val NullableColorSaver: Saver<Color?, Int> =
    Saver(
        save = { color -> color?.toArgb() ?: -1 },
        restore = { argb ->
            if (argb == -1) {
                null
            } else {
                Color(argb)
            }
        }
    )

/**
 * Saver для DisplayOption (non-null).
 */
@Suppress("SwallowedException")
val DisplayOptionSaver: Saver<DisplayOption, String> =
    Saver(
        save = { option -> option.name },
        restore = { name ->
            try {
                DisplayOption.valueOf(name)
            } catch (e: IllegalArgumentException) {
                DisplayOption.DAY
            }
        }
    )

/**
 * Saver для LocalDate? (nullable).
 * Использует -1L как sentinel значение для null.
 */
val NullableLocalDateSaver: Saver<LocalDate?, Long> =
    Saver(
        save = { localDate ->
            localDate?.let {
                it
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            } ?: -1L
        },
        restore = { epochMilli ->
            if (epochMilli == -1L) {
                null
            } else {
                Instant
                    .ofEpochMilli(epochMilli)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
            }
        }
    )

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
                state.selectedDate?.let { date ->
                    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                },
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
                state.selectedDate?.let { date ->
                    date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                },
                state.selectedColor?.toArgb(),
                state.selectedDisplayOption.name,
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
                reminder = ReminderFormUiStateSaver.restore(saved[5] as List<Any?>) ?: ReminderFormUiState()
            )
        }
    )
