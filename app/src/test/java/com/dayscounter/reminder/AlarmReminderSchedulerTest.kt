package com.dayscounter.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для [AlarmReminderScheduler] без Robolectric.
 *
 * Подход: мокаем `Context`, `AlarmManager` и статический [PendingIntent.getBroadcast]
 * (как в [ClipboardHelperTest] — на JVM без Robolectric реальная реализация падает).
 */
class AlarmReminderSchedulerTest {
    private val context: Context = mockk()
    private val alarmManager: AlarmManager = mockk(relaxed = true)
    private val pendingIntent: PendingIntent = mockk(relaxed = true)
    private val logger: Logger = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        mockkStatic(PendingIntent::class)
        every { PendingIntent.getBroadcast(any(), any(), any<Intent>(), any<Int>()) } returns pendingIntent
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun schedule_when_set_exact_succeeds_then_no_fallback() {
        // Given
        every { alarmManager.canScheduleExactAlarms() } returns true
        val scheduler = AlarmReminderScheduler(context, logger)
        val reminder = sampleReminder(itemId = 1L)

        // When
        scheduler.schedule(reminder, itemTitle = "Title")

        // Then: setExact вызван, fallback не вызван, логов нет
        verify(exactly = 1) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetEpochMillis,
                pendingIntent
            )
        }
        verify(exactly = 0) { alarmManager.setAndAllowWhileIdle(any(), any(), any()) }
        verify(exactly = 0) { logger.w(any(), any(), any()) }
    }

    @Test
    fun schedule_when_security_exception_then_fallback_invoked_and_logged() {
        // Given
        every { alarmManager.setExactAndAllowWhileIdle(any(), any(), any()) } throws
            SecurityException("missing SCHEDULE_EXACT_ALARM")
        val scheduler = AlarmReminderScheduler(context, logger)
        val reminder = sampleReminder(itemId = 2L)

        // When
        scheduler.schedule(reminder, itemTitle = "Title")

        // Then
        verify(exactly = 1) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                reminder.targetEpochMillis,
                pendingIntent
            )
        }
        verify(exactly = 1) {
            logger.w(
                "AlarmReminderScheduler",
                match { it.contains("SCHEDULE_EXACT_ALARM") && it.contains("setAndAllowWhileIdle") },
                ofType(SecurityException::class)
            )
        }
    }

    private fun sampleReminder(itemId: Long): Reminder =
        Reminder(
            itemId = itemId,
            mode = ReminderMode.AT_DATE,
            targetEpochMillis = 1_778_000_000_000L,
            intervalAmount = 1,
            intervalUnit = ReminderIntervalUnit.DAY,
            createdAt = 0L,
            updatedAt = 0L
        )
}
