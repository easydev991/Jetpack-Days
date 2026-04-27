package com.dayscounter.data.database

import com.dayscounter.data.database.entity.ReminderEntity
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ReminderMapperTest {
    @Test
    fun toDomain_whenEntityWithAllFields_thenConvertsCorrectly() {
        // Given
        val entity =
            ReminderEntity(
                itemId = 7L,
                mode = ReminderMode.AT_DATE.name,
                targetEpochMillis = 1_777_000_000_000L,
                intervalAmount = 5,
                intervalUnit = ReminderIntervalUnit.WEEK.name,
                selectedDateEpochMillis = 1_776_960_000_000L,
                selectedHour = 10,
                selectedMinute = 5,
                status = ReminderStatus.ACTIVE.name,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_100_000L
            )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals(7L, domain.itemId)
        assertEquals(ReminderMode.AT_DATE, domain.mode)
        assertEquals(1_777_000_000_000L, domain.targetEpochMillis)
        assertEquals(5, domain.intervalAmount)
        assertEquals(ReminderIntervalUnit.WEEK, domain.intervalUnit)
        assertEquals(ReminderStatus.ACTIVE, domain.status)
    }

    @Test
    fun toEntity_whenDomainWithAllFields_thenConvertsCorrectly() {
        // Given
        val domain =
            Reminder(
                itemId = 9L,
                mode = ReminderMode.AFTER_INTERVAL,
                targetEpochMillis = 1_778_000_000_000L,
                intervalAmount = 1,
                intervalUnit = ReminderIntervalUnit.YEAR,
                selectedDateEpochMillis = null,
                selectedHour = null,
                selectedMinute = null,
                status = ReminderStatus.CANCELLED,
                createdAt = 1_700_000_000_000L,
                updatedAt = 1_700_000_200_000L
            )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals(9L, entity.itemId)
        assertEquals(ReminderMode.AFTER_INTERVAL.name, entity.mode)
        assertEquals(ReminderIntervalUnit.YEAR.name, entity.intervalUnit)
        assertEquals(ReminderStatus.CANCELLED.name, entity.status)
        assertEquals(1_778_000_000_000L, entity.targetEpochMillis)
    }
}
