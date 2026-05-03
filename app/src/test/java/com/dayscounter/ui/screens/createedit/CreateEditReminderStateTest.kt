package com.dayscounter.ui.screens.createedit

import com.dayscounter.R
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
    fun defaultReminderDate_whenCalled_thenReturnsNextDay() {
        val result = defaultReminderDate(today = LocalDate.of(2026, 4, 29))

        assertEquals(LocalDate.of(2026, 4, 30), result)
    }

    @Test
    fun reminderFormUiState_whenCreated_thenDefaultSelectedDateIsNextDay() {
        val expected = LocalDate.now().plusDays(1)
        val state = ReminderFormUiState()

        assertEquals(expected, state.selectedDate)
    }

    @Test
    fun toreminderrequest_when_disabled_then_returns_null() {
        val state = ReminderFormUiState(isEnabled = false)

        val request = state.toReminderRequest(itemId = 42L)

        assertNull(request)
    }

    @Test
    fun toreminderrequest_when_at_date_mode_then_builds_request_with_date_and_time() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate = LocalDate.of(2026, 5, 1),
                hour = 14,
                minute = 20
            )

        val request = state.toReminderRequest(itemId = 11L)

        checkNotNull(request)
        assertEquals(11L, request.itemId)
        assertEquals(ReminderMode.AT_DATE, request.mode)
        assertEquals(LocalDate.of(2026, 5, 1), request.atDate)
        assertEquals(LocalTime.of(14, 20), request.atTime)
    }

    @Test
    fun toreminderrequest_when_interval_mode_then_builds_request_with_amount_and_unit() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = "7",
                intervalUnit = ReminderIntervalUnit.WEEK
            )

        val request = state.toReminderRequest(itemId = 99L)

        checkNotNull(request)
        assertEquals(99L, request.itemId)
        assertEquals(ReminderMode.AFTER_INTERVAL, request.mode)
        assertEquals(7, request.afterAmount)
        assertEquals(ReminderIntervalUnit.WEEK, request.afterUnit)
    }

    @Test
    fun isinputvalid_when_at_date_in_future_then_true() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate = LocalDate.of(2026, 5, 10),
                hour = 10,
                minute = 30
            )

        val isValid =
            state.isInputValid(
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertTrue(isValid)
    }

    @Test
    fun isinputvalid_when_at_date_in_past_then_false() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate = LocalDate.of(2026, 5, 10),
                hour = 8,
                minute = 59
            )

        val isValid =
            state.isInputValid(
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertFalse(isValid)
    }

    @Test
    fun isinputvalid_when_interval_is_positive_integer_then_true() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = "3"
            )

        assertTrue(state.isInputValid(currentDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)))
    }

    @Test
    fun isinputvalid_when_interval_contains_letters_then_false() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = "5a"
            )

        assertFalse(state.isInputValid(currentDateTime = LocalDateTime.of(2026, 1, 1, 0, 0)))
    }

    @Test
    fun tochangefingerprint_for_ui_state_and_domain_reminder_should_be_aligned() {
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
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate =
                    java.time.Instant
                        .ofEpochMilli(selectedDateMillis)
                        .atZone(java.time.ZoneId.systemDefault())
                        .toLocalDate(),
                hour = 16,
                minute = 45
            )

        val uiFingerprint = state.toChangeFingerprint()
        val domainFingerprint = reminder.toChangeFingerprint()

        assertEquals(domainFingerprint, uiFingerprint)
    }

    @Test
    fun iscreateeditformvalid_when_title_and_date_valid_and_reminder_disabled_then_true() {
        val state = ReminderFormUiState(isEnabled = false)

        val isValid =
            isCreateEditFormValid(
                title = "Запись",
                selectedDate = LocalDate.of(2026, 5, 10),
                reminderUiState = state,
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertTrue(isValid)
    }

    @Test
    fun iscreateeditformvalid_when_reminder_enabled_and_at_date_is_not_configured_then_false() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate = null
            )

        val isValid =
            isCreateEditFormValid(
                title = "Запись",
                selectedDate = LocalDate.of(2026, 5, 10),
                reminderUiState = state,
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertFalse(isValid)
    }

    @Test
    fun iscreateeditformvalid_when_reminder_enabled_and_interval_is_empty_then_false() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = ""
            )

        val isValid =
            isCreateEditFormValid(
                title = "Запись",
                selectedDate = LocalDate.of(2026, 5, 10),
                reminderUiState = state,
                currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0)
            )

        assertFalse(isValid)
    }

    @Test
    fun validationerrorresid_when_interval_invalid_then_returns_amount_error() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = ""
            )

        val errorResId = state.validationErrorResId(currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0))

        assertEquals(R.string.reminder_error_invalid_amount, errorResId)
    }

    @Test
    fun validationerrorresid_when_at_date_valid_then_returns_null() {
        val state =
            ReminderFormUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE,
                selectedDate = LocalDate.of(2026, 5, 12),
                hour = 10,
                minute = 0
            )

        val errorResId = state.validationErrorResId(currentDateTime = LocalDateTime.of(2026, 5, 10, 9, 0))

        assertNull(errorResId)
    }
}
