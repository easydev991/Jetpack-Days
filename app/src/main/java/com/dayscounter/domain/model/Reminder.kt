package com.dayscounter.domain.model

/**
 * Доменная модель одноразового напоминания для записи.
 */
data class Reminder(
    val itemId: Long,
    val mode: ReminderMode,
    val targetEpochMillis: Long,
    val intervalAmount: Int? = null,
    val intervalUnit: ReminderIntervalUnit? = null,
    val selectedDateEpochMillis: Long? = null,
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val status: ReminderStatus = ReminderStatus.ACTIVE,
    val createdAt: Long,
    val updatedAt: Long
)

enum class ReminderMode {
    AT_DATE,
    AFTER_INTERVAL
}

enum class ReminderIntervalUnit {
    DAY,
    WEEK,
    MONTH,
    YEAR
}

enum class ReminderStatus {
    ACTIVE,
    CONSUMED,
    CANCELLED
}
