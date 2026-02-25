@file:Suppress("ktlint:standard:property-naming")

package com.dayscounter.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.entity.ItemEntity

/**
 * Room Database для хранения данных приложения.
 *
 * @property version Версия базы данных (1)
 * @property entities Список Entity классов
 * @property exportSchema Экспорт схемы отключен для упрощения
 */
@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(DisplayOptionConverter::class)
abstract class DaysDatabase : RoomDatabase() {
    /**
     * Получает DAO для работы с записями событий.
     *
     * @return ItemDao
     */
    abstract fun itemDao(): ItemDao

    companion object {
        @Volatile
        @Suppress("VariableNaming")
        private var INSTANCE: DaysDatabase? = null

        /**
         * Получает экземпляр базы данных (Singleton паттерн).
         *
         * @param context Контекст приложения
         * @return Экземпляр DaysDatabase
         */
        fun getDatabase(context: Context): DaysDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            DaysDatabase::class.java,
                            "days_database",
                        ).build()
                INSTANCE = instance
                instance
            }
    }
}
