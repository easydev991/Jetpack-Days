package com.dayscounter.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlarmReminderSchedulerInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }

    @Test
    fun cancel_whenScheduled_thenPendingIntentIsRemoved() {
        val scheduler = AlarmReminderScheduler(context)
        val itemId = 777L
        val reminder =
            com.dayscounter.domain.model.Reminder(
                itemId = itemId,
                mode = com.dayscounter.domain.model.ReminderMode.AT_DATE,
                targetEpochMillis = System.currentTimeMillis() + 60_000L,
                status = com.dayscounter.domain.model.ReminderStatus.ACTIVE,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

        scheduler.schedule(reminder, "title")
        assertNotNull(findReminderPendingIntent(itemId))

        scheduler.cancel(itemId)

        assertNull(findReminderPendingIntent(itemId))
    }

    private fun findReminderPendingIntent(itemId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            ReminderIntentContract.requestCodeForItem(itemId),
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderIntentContract.ACTION_FIRE_REMINDER
            },
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
}
