package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BuildReminderUseCaseTest {
    private val fixedInstant = Instant.parse("2026-04-27T10:15:30Z")
    private val zoneId = ZoneId.of("Europe/Moscow")
    private val clock: Clock = Clock.fixed(fixedInstant, zoneId)

    @Test
    fun invoke_whenAfterIntervalDays_thenKeepsCurrentTimeOfDayAndAddsDays() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val request =
            ReminderRequest(
                itemId = 42L,
                mode = ReminderMode.AFTER_INTERVAL,
                afterAmount = 3,
                afterUnit = ReminderIntervalUnit.DAY
            )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess, "Результат должен быть успешным")
        val reminder = result.getOrThrow()
        val expected = ZonedDateTime.ofInstant(fixedInstant, zoneId).plusDays(3)
        assertEquals(expected.toInstant().toEpochMilli(), reminder.targetEpochMillis)
        assertEquals(3, reminder.intervalAmount)
        assertEquals(ReminderIntervalUnit.DAY, reminder.intervalUnit)
    }

    @Test
    fun invoke_whenAfterIntervalMonths_thenAddsMonthsFromCurrentMoment() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val request =
            ReminderRequest(
                itemId = 42L,
                mode = ReminderMode.AFTER_INTERVAL,
                afterAmount = 2,
                afterUnit = ReminderIntervalUnit.MONTH
            )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess, "Результат должен быть успешным")
        val reminder = result.getOrThrow()
        val expected = ZonedDateTime.ofInstant(fixedInstant, zoneId).plusMonths(2)
        assertEquals(expected.toInstant().toEpochMilli(), reminder.targetEpochMillis)
    }

    @Test
    fun invoke_whenAtDateWithTime_thenBuildsTargetInSystemZone() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val selectedDate = LocalDate.of(2026, 5, 10)
        val selectedTime = LocalTime.of(9, 45)
        val request =
            ReminderRequest(
                itemId = 11L,
                mode = ReminderMode.AT_DATE,
                atDate = selectedDate,
                atTime = selectedTime
            )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isSuccess, "Результат должен быть успешным")
        val reminder = result.getOrThrow()
        val expectedTargetMillis =
            selectedDate
                .atTime(selectedTime)
                .atZone(zoneId)
                .toInstant()
                .toEpochMilli()
        assertEquals(expectedTargetMillis, reminder.targetEpochMillis)
    }

    @Test
    fun invoke_whenAfterIntervalAmountIsZero_thenReturnsFailure() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val request =
            ReminderRequest(
                itemId = 42L,
                mode = ReminderMode.AFTER_INTERVAL,
                afterAmount = 0,
                afterUnit = ReminderIntervalUnit.WEEK
            )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure, "Должна быть ошибка валидации")
    }

    @Test
    fun invoke_whenAtDateInPast_thenReturnsFailure() {
        // Given
        val useCase = BuildReminderUseCase(clock = clock)
        val request =
            ReminderRequest(
                itemId = 42L,
                mode = ReminderMode.AT_DATE,
                atDate = LocalDate.of(2026, 4, 20),
                atTime = LocalTime.of(10, 0)
            )

        // When
        val result = useCase(request)

        // Then
        assertTrue(result.isFailure, "Для прошедшей даты/времени должна быть ошибка")
    }

    @Test
    fun invoke_whenZoneHasOffset_thenUsesClockZoneForAtDateCalculation() {
        // Given
        val utcClock = Clock.fixed(fixedInstant, ZoneOffset.UTC)
        val useCase = BuildReminderUseCase(clock = utcClock)
        val request =
            ReminderRequest(
                itemId = 99L,
                mode = ReminderMode.AT_DATE,
                atDate = LocalDate.of(2026, 5, 1),
                atTime = LocalTime.of(12, 0)
            )

        // When
        val result = useCase(request)

        // Then
        val reminder = result.getOrThrow()
        val expectedMillis =
            LocalDate
                .of(2026, 5, 1)
                .atTime(12, 0)
                .atZone(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        assertEquals(expectedMillis, reminder.targetEpochMillis)
    }
}
