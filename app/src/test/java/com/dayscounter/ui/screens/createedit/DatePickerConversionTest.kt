package com.dayscounter.ui.screens.createedit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Тесты, демонстрирующие timezone-баг Material3 DatePicker.
 *
 * Material3 DatePicker внутри использует UTC для отображения дат:
 * `selectedDateMillis` интерпретируется как UTC-полночь выбранного дня.
 *
 * Если передать millis через `atStartOfDay(ZoneId.systemDefault())`,
 * для часового пояса с положительным смещением (например, MSK UTC+3)
 * дата смещается на предыдущий день:
 *   29 апреля 00:00 MSK = 28 апреля 21:00 UTC → DatePicker показывает 28 апреля
 *
 * Фикс: использовать `ZoneOffset.UTC` для конверсии в/из DatePicker.
 */
class DatePickerConversionTest {
    @Test
    fun oldConversion_whenNonUtcOffset_thenDateShiftsWhenReadAsUtc() {
        // MSK (UTC+3) — известный часовой пояс со смещением
        val mskZone = ZoneOffset.ofHours(3)
        val selectedDate = LocalDate.of(2026, 4, 29)

        // OLD BUGGY: atStartOfDay(MSK) → millis → DatePicker читает как UTC
        val oldMillis = selectedDate.atStartOfDay(mskZone).toInstant().toEpochMilli()
        val datePickerSees = Instant.ofEpochMilli(oldMillis).atZone(ZoneOffset.UTC).toLocalDate()

        // DatePicker показывает 28 апреля вместо 29 апреля!
        assertEquals(LocalDate.of(2026, 4, 28), datePickerSees)
        assertNotEquals(selectedDate, datePickerSees)
    }

    @Test
    fun fixedConversion_whenUtcOffset_thenPreservesDateThroughUtcLens() {
        val selectedDate = LocalDate.of(2026, 4, 29)

        // FIXED: atStartOfDay(UTC) → millis → DatePicker читает как UTC
        val newMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerSees = Instant.ofEpochMilli(newMillis).atZone(ZoneOffset.UTC).toLocalDate()

        // DatePicker показывает 29 апреля — правильно!
        assertEquals(selectedDate, datePickerSees)
    }

    @Test
    fun oldConversion_whenNegativeUtcOffset_thenDatePreservedButMillisNotMidnight() {
        // UTC-5 (например, EST)
        val estZone = ZoneOffset.ofHours(-5)
        val selectedDate = LocalDate.of(2026, 4, 29)

        val oldMillis = selectedDate.atStartOfDay(estZone).toInstant().toEpochMilli()
        val datePickerSees = Instant.ofEpochMilli(oldMillis).atZone(ZoneOffset.UTC).toLocalDate()

        // 29 апреля 00:00 EST = 29 апреля 05:00 UTC → DatePicker показывает 29 апреля
        // В данном случае смещение положительное по UTC, так что дата НЕ сдвигается назад,
        // но millis уже не соответствует полночи UTC → может быть другой баг при нормализации
        assertEquals(selectedDate, datePickerSees)
    }

    @Test
    fun roundtrip_with_utc_preserves_any_date() {
        val testDates =
            listOf(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 4, 29),
                LocalDate.of(2026, 12, 31),
                LocalDate.of(2024, 2, 29) // високосный
            )

        for (date in testDates) {
            val millis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            val restored = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            assertEquals(date, restored, "Roundtrip failed for $date")
        }
    }

    @Test
    fun confirmdatepickerselection_when_millis_null_then_dismisses_without_date() {
        var selectedDate: LocalDate? = null
        var dateSelected = false
        var dismissed = false

        confirmDatePickerSelection(
            selectedDateMillis = null,
            onDateSelected = {
                selectedDate = it
                dateSelected = true
            },
            onDismiss = { dismissed = true }
        )

        assertNull(selectedDate)
        assertFalse(dateSelected)
        assertTrue(dismissed)
    }
}
