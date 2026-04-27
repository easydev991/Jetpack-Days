package com.dayscounter.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.di.AppModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Перепланирует активные напоминания после перезагрузки устройства.
 */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }

        val pendingResult = goAsync()
        val appContext = context.applicationContext

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val database = DaysDatabase.getDatabase(appContext)
                val reminderManager = AppModule.createReminderManager(appContext, database)
                reminderManager.rescheduleFutureReminders()
            }
            pendingResult.finish()
        }
    }
}
