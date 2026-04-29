@file:Suppress("ktlint:standard:property-naming")

package com.dayscounter.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.dao.ReminderDao
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.data.database.entity.ReminderEntity

/**
 * Room Database для хранения данных приложения.
 */
@Database(
    entities = [ItemEntity::class, ReminderEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(DisplayOptionConverter::class)
abstract class DaysDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao

    abstract fun reminderDao(): ReminderDao

    companion object {
        val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS reminders (
                            itemId INTEGER NOT NULL,
                            mode TEXT NOT NULL,
                            targetEpochMillis INTEGER NOT NULL,
                            intervalAmount INTEGER,
                            intervalUnit TEXT,
                            selectedDateEpochMillis INTEGER,
                            selectedHour INTEGER,
                            selectedMinute INTEGER,
                            status TEXT NOT NULL,
                            createdAt INTEGER NOT NULL,
                            updatedAt INTEGER NOT NULL,
                            PRIMARY KEY(itemId),
                            FOREIGN KEY(itemId) REFERENCES items(id) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent()
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_reminders_itemId ON reminders(itemId)"
                    )
                }
            }

        @Volatile
        @Suppress("VariableNaming")
        private var INSTANCE: DaysDatabase? = null

        fun getDatabase(context: Context): DaysDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    Room
                        .databaseBuilder(
                            context.applicationContext,
                            DaysDatabase::class.java,
                            "days_database"
                        ).addMigrations(MIGRATION_1_2)
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
