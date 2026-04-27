package com.dayscounter.data.database

import androidx.sqlite.db.SupportSQLiteDatabase
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class DaysDatabaseMigrationTest {
    @Test
    fun migration_1_2_whenExecuted_thenCreatesRemindersTableAndIndex() {
        // Given
        val database = mockk<SupportSQLiteDatabase>(relaxed = true)

        // When
        DaysDatabase.MIGRATION_1_2.migrate(database)

        // Then
        verify {
            database.execSQL(
                match { sql ->
                    sql.contains("CREATE TABLE IF NOT EXISTS reminders") &&
                        sql.contains("FOREIGN KEY(itemId) REFERENCES items(id)")
                }
            )
            database.execSQL(match { sql -> sql.contains("CREATE INDEX IF NOT EXISTS index_reminders_itemId") })
        }
    }
}
