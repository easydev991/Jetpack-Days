package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.dayscounter.domain.model.DisplayOption
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
        },
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
        },
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
        },
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
        },
    )
