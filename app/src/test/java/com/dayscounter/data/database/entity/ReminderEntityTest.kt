package com.dayscounter.data.database.entity

import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReminderEntityTest {
    @Test
    fun createReminderEntity_withRequiredFields_thenCreatesSuccessfully() {
        // Given
        val targetEpochMillis = 1_777_000_000_000L

        // When
        val entity =
            ReminderEntity(
                itemId = 10L,
                mode = ReminderMode.AFTER_INTERVAL.name,
                targetEpochMillis = targetEpochMillis,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_000_000L
            )

        // Then
        assertEquals(10L, entity.itemId)
        assertEquals(ReminderMode.AFTER_INTERVAL.name, entity.mode)
        assertEquals(targetEpochMillis, entity.targetEpochMillis)
        assertEquals(ReminderStatus.ACTIVE.name, entity.status)
    }

    @Test
    fun createReminderEntity_withAllFields_thenStoresAllValues() {
        // Given
        val createdAt = 1_700_000_000_000L
        val updatedAt = 1_700_000_100_000L

        // When
        val entity =
            ReminderEntity(
                itemId = 3L,
                mode = ReminderMode.AT_DATE.name,
                targetEpochMillis = 1_777_000_000_000L,
                intervalAmount = 2,
                intervalUnit = ReminderIntervalUnit.MONTH.name,
                selectedDateEpochMillis = 1_776_960_000_000L,
                selectedHour = 9,
                selectedMinute = 15,
                status = ReminderStatus.CONSUMED.name,
                createdAt = createdAt,
                updatedAt = updatedAt
            )

        // Then
        assertEquals(2, entity.intervalAmount)
        assertEquals(ReminderIntervalUnit.MONTH.name, entity.intervalUnit)
        assertEquals(1_776_960_000_000L, entity.selectedDateEpochMillis)
        assertEquals(9, entity.selectedHour)
        assertEquals(15, entity.selectedMinute)
        assertEquals(ReminderStatus.CONSUMED.name, entity.status)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(updatedAt, entity.updatedAt)
    }
}
