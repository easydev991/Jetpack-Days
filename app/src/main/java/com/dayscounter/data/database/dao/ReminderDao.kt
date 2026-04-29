package com.dayscounter.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dayscounter.data.database.entity.ReminderEntity

/**
 * DAO для работы с напоминаниями.
 */
@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders WHERE itemId = :itemId LIMIT 1")
    suspend fun getReminderByItemId(itemId: Long): ReminderEntity?

    @Query(
        """
        SELECT * FROM reminders
        WHERE status = 'ACTIVE' AND targetEpochMillis > :nowEpochMillis
        ORDER BY targetEpochMillis ASC
        """
    )
    suspend fun getFutureActiveReminders(nowEpochMillis: Long): List<ReminderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminder(reminder: ReminderEntity)

    @Query("UPDATE reminders SET status = :status, updatedAt = :updatedAt WHERE itemId = :itemId")
    suspend fun updateStatus(
        itemId: Long,
        status: String,
        updatedAt: Long
    )

    @Query("DELETE FROM reminders WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: Long)
}
