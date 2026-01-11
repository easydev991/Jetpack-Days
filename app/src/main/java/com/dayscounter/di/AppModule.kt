package com.dayscounter.di

import android.content.Context
import com.dayscounter.DaysCounterApplication
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.repository.ItemRepository

/**
 * Модуль внедрения зависимостей для приложения.
 *
 * Использует ручной подход к DI через factory методы.
 *
 * Создает экземпляры репозиториев и настроек.
 */
object AppModule {
    /**
     * Создает ResourceProvider для работы со строковыми ресурсами.
     *
     * @return Экземпляр ResourceProvider
     */
    val resourceProvider: ResourceProvider by lazy {
        FormatterModule.createResourceProvider(DaysCounterApplication.instance)
    }

    /**
     * Создает репозиторий для работы с событиями.
     *
     * @param database База данных Room
     * @return Экземпляр ItemRepository
     */
    fun createItemRepository(database: DaysDatabase): ItemRepository = ItemRepositoryImpl(database.itemDao())

    /**
     * Создает DataStore для хранения настроек приложения.
     *
     * @param context Контекст приложения
     * @return Экземпляр AppSettingsDataStore
     */
    fun createAppSettingsDataStore(context: Context): AppSettingsDataStore = AppSettingsDataStore(context)
}
