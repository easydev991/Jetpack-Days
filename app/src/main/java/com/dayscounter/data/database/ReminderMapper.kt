package com.dayscounter.data.database

import com.dayscounter.data.database.entity.ReminderEntity
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.domain.model.ReminderStatus

/**
 * Мапперы между ReminderEntity и доменной моделью Reminder.
 */
fun ReminderEntity.toDomain(): Reminder =
    Reminder(
        itemId = itemId,
        mode = ReminderMode.valueOf(mode),
        targetEpochMillis = targetEpochMillis,
        intervalAmount = intervalAmount,
        intervalUnit = intervalUnit?.let { ReminderIntervalUnit.valueOf(it) },
        selectedDateEpochMillis = selectedDateEpochMillis,
        selectedHour = selectedHour,
        selectedMinute = selectedMinute,
        status = ReminderStatus.valueOf(status),
        createdAt = createdAt,
        updatedAt = updatedAt
    )

fun Reminder.toEntity(): ReminderEntity =
    ReminderEntity(
        itemId = itemId,
        mode = mode.name,
        targetEpochMillis = targetEpochMillis,
        intervalAmount = intervalAmount,
        intervalUnit = intervalUnit?.name,
        selectedDateEpochMillis = selectedDateEpochMillis,
        selectedHour = selectedHour,
        selectedMinute = selectedMinute,
        status = status.name,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
