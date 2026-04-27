package com.dayscounter.ui.screens.createedit

import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CreateEditReminderStateTest {
    @Test
    fun `toReminderRequest when disabled then returns null`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = false

        val request = state.toReminderRequest(itemId = 42L)

        assertNull(request)
    }

    @Test
    fun `toReminderRequest when at date mode then builds request with date and time`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AT_DATE
        state.selectedDate.value = LocalDate.of(2026, 5, 1)
        state.hour.value = 14
        state.minute.value = 20

        val request = state.toReminderRequest(itemId = 11L)

        checkNotNull(request)
        assertEquals(11L, request.itemId)
        assertEquals(ReminderMode.AT_DATE, request.mode)
        assertEquals(LocalDate.of(2026, 5, 1), request.atDate)
        assertEquals(LocalTime.of(14, 20), request.atTime)
    }

    @Test
    fun `toReminderRequest when interval mode then builds request with amount and unit`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AFTER_INTERVAL
        state.intervalValue.value = "7"
        state.intervalUnit.value = ReminderIntervalUnit.WEEK

        val request = state.toReminderRequest(itemId = 99L)

        checkNotNull(request)
        assertEquals(99L, request.itemId)
        assertEquals(ReminderMode.AFTER_INTERVAL, request.mode)
        assertEquals(7, request.afterAmount)
        assertEquals(ReminderIntervalUnit.WEEK, request.afterUnit)
    }

    @Test
    fun `isInputValid when at date in future then true`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AT_DATE
        state.selectedDate.value = LocalDate.of(2026, 5, 10)
        state.hour.value = 10
        state.minute.value = 30

        val isValid =
            state.isInputValid(
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertTrue(isValid)
    }

    @Test
    fun `isInputValid when at date in past then false`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AT_DATE
        state.selectedDate.value = LocalDate.of(2026, 5, 10)
        state.hour.value = 8
        state.minute.value = 59

        val isValid =
            state.isInputValid(
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertFalse(isValid)
    }

    @Test
    fun `isInputValid when interval is positive integer then true`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AFTER_INTERVAL
        state.intervalValue.value = "3"

        assertTrue(state.isInputValid(currentDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)))
    }

    @Test
    fun `isInputValid when interval contains letters then false`() {
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AFTER_INTERVAL
        state.intervalValue.value = "5a"

        assertFalse(state.isInputValid(currentDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)))
    }

    @Test
    fun `toChangeFingerprint for ui state and domain reminder should be aligned`() {
        val selectedDateMillis = 1_800_000_000_000L
        val reminder =
            Reminder(
                itemId = 5L,
                mode = ReminderMode.AT_DATE,
                targetEpochMillis = selectedDateMillis,
                selectedDateEpochMillis = selectedDateMillis,
                selectedHour = 16,
                selectedMinute = 45,
                status = ReminderStatus.ACTIVE,
                createdAt = 1_799_999_999_000L,
                updatedAt = 1_799_999_999_500L
            )
        val state = ReminderFormUiState()
        state.isEnabled.value = true
        state.mode.value = ReminderMode.AT_DATE
        state.selectedDate.value =
            java.time.Instant
                .ofEpochMilli(selectedDateMillis)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        state.hour.value = 16
        state.minute.value = 45

        val uiFingerprint = state.toChangeFingerprint()
        val domainFingerprint = reminder.toChangeFingerprint()

        assertEquals(domainFingerprint, uiFingerprint)
    }
}
