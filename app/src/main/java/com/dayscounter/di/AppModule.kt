@file:Suppress("MaxLineLength")

package com.dayscounter.di

import android.content.Context
import com.dayscounter.BuildConfig
import com.dayscounter.DaysCounterApplication
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.FirebaseAnalyticsProvider
import com.dayscounter.analytics.NoopAnalyticsProvider
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.data.repository.ReminderRepositoryImpl
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.repository.ReminderRepository
import com.dayscounter.reminder.AlarmReminderScheduler
import com.dayscounter.reminder.DefaultReminderManager
import com.dayscounter.reminder.ReminderManager

/**
 * Модуль внедрения зависимостей для приложения.
 */
object AppModule {
    @Volatile
    private var analyticsService: AnalyticsService? = null

    fun createAnalyticsService(context: Context): AnalyticsService {
        val cachedService = analyticsService
        if (cachedService != null) return cachedService

        return synchronized(this) {
            val synchronizedCachedService = analyticsService
            if (synchronizedCachedService != null) {
                synchronizedCachedService
            } else {
                val providers =
                    if (BuildConfig.DEBUG) {
                        listOf(NoopAnalyticsProvider())
                    } else {
                        listOf(FirebaseAnalyticsProvider(context.applicationContext))
                    }
                AnalyticsService(providers).also { analyticsService = it }
            }
        }
    }

    val resourceProvider: ResourceProvider by lazy {
        FormatterModule.createResourceProvider(DaysCounterApplication.instance)
    }

    fun createItemRepository(database: DaysDatabase): ItemRepository = ItemRepositoryImpl(database.itemDao())

    fun createReminderRepository(database: DaysDatabase): ReminderRepository = ReminderRepositoryImpl(database.reminderDao())

    fun createReminderManager(
        context: Context,
        database: DaysDatabase
    ): ReminderManager =
        DefaultReminderManager(
            reminderRepository = createReminderRepository(database),
            itemRepository = createItemRepository(database),
            reminderScheduler = AlarmReminderScheduler(context.applicationContext)
        )

    fun createAppSettingsDataStore(context: Context): AppSettingsDataStore = AppSettingsDataStore(context)
}
