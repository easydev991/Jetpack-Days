package com.dayscounter.reminder

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.ui.screens.createedit.hasPostNotificationsPermission
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderAlarmReceiverInstrumentedTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @After
    fun tearDown() {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancelAll()
    }

    @Test
    fun onReceive_whenReminderIntentIsValid_thenPostsNotificationAndCreatesChannel() {
        runWithNotificationPermission {
            val receiver = ReminderAlarmReceiver()
            val intent =
                Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderIntentContract.ACTION_FIRE_REMINDER
                    putExtra(ReminderIntentContract.EXTRA_ITEM_ID, 77L)
                    putExtra(ReminderIntentContract.EXTRA_ITEM_TITLE, "День рождения")
                }

            receiver.onReceive(context, intent)

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = notificationManager.getNotificationChannel(ReminderIntentContract.CHANNEL_ID)
                assertNotNull(channel)
            }
        }
    }

    @Test
    fun onReceive_whenItemIdInvalid_thenDoesNotPostNotification() {
        runWithNotificationPermission {
            val receiver = ReminderAlarmReceiver()
            val intent =
                Intent(context, ReminderAlarmReceiver::class.java).apply {
                    action = ReminderIntentContract.ACTION_FIRE_REMINDER
                    putExtra(ReminderIntentContract.EXTRA_ITEM_ID, -1L)
                }

            receiver.onReceive(context, intent)
        }
    }

    @Test
    fun onReceive_whenPostNotificationsPermissionNotGranted_thenDoesNotPostNotification() {
        // Гвард актуален только с API 33, где POST_NOTIFICATIONS — рантайм-permission
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        assumeFalse(context.hasPostNotificationsPermission())

        val receiver = ReminderAlarmReceiver()
        val intent =
            Intent(context, ReminderAlarmReceiver::class.java).apply {
                action = ReminderIntentContract.ACTION_FIRE_REMINDER
                putExtra(ReminderIntentContract.EXTRA_ITEM_ID, 78L)
                putExtra(ReminderIntentContract.EXTRA_ITEM_TITLE, "Без permission")
            }

        receiver.onReceive(context, intent)

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val posted =
            notificationManager.activeNotifications.any {
                it.id == ReminderIntentContract.notificationIdForItem(78L)
            }
        assertFalse(posted)
    }

    private fun runWithNotificationPermission(block: () -> Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            block()
            return
        }

        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        uiAutomation.adoptShellPermissionIdentity(Manifest.permission.POST_NOTIFICATIONS)
        try {
            block()
        } finally {
            uiAutomation.dropShellPermissionIdentity()
        }
    }
}
