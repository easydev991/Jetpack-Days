package com.dayscounter.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.dayscounter.domain.model.ReminderStatus

/**
 * Room Entity для одноразового напоминания, привязанного к записи.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["itemId"])]
)
data class ReminderEntity(
    @PrimaryKey
    val itemId: Long,
    val mode: String,
    val targetEpochMillis: Long,
    val intervalAmount: Int? = null,
    val intervalUnit: String? = null,
    val selectedDateEpochMillis: Long? = null,
    val selectedHour: Int? = null,
    val selectedMinute: Int? = null,
    val status: String = ReminderStatus.ACTIVE.name,
    val createdAt: Long,
    val updatedAt: Long
)
