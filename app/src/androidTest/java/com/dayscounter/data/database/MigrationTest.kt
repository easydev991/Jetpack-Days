package com.dayscounter.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционный тест миграции БД 1→2 ([DaysDatabase.MIGRATION_1_2]).
 *
 * Сценарий: создаётся БД версии 1 по реконструированной схеме `1.json`,
 * в неё вносится запись в `items` (raw SQL — DAO текущего кода знает только
 * схему v2), прогоняется миграция с валидацией структуры против `2.json`,
 * проверяется сохранность данных и работоспособность новой таблицы `reminders`.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            DaysDatabase::class.java
        )

    @Test
    fun migration_1_2_whenItemsExist_thenDataSurvivesAndRemindersWritable() {
        // Given: БД версии 1 с записью в items
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                """
                INSERT INTO items (id, title, details, timestamp, colorTag, displayOption)
                VALUES ($ITEM_ID, '$ITEM_TITLE', '$ITEM_DETAILS', $ITEM_TIMESTAMP, NULL, 'DAY')
                """.trimIndent()
            )
            close()
        }

        // When: миграция 1→2 с валидацией структуры против схемы v2
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, DaysDatabase.MIGRATION_1_2)

        // Then: запись items пережила миграцию без потерь
        db
            .query(
                "SELECT id, title, details, timestamp, displayOption FROM items WHERE id = $ITEM_ID"
            ).use { cursor ->
                assertTrue("Запись в items должна сохраниться после миграции", cursor.moveToFirst())
                assertEquals("title должен сохраниться после миграции", ITEM_TITLE, cursor.getString(1))
                assertEquals("details должен сохраниться после миграции", ITEM_DETAILS, cursor.getString(2))
                assertEquals(
                    "timestamp должен сохраниться после миграции",
                    ITEM_TIMESTAMP,
                    cursor.getLong(3)
                )
                assertEquals(
                    "displayOption должен сохраниться после миграции",
                    "DAY",
                    cursor.getString(4)
                )
            }

        // And: таблица reminders работоспособна — запись через FK проходит
        db.execSQL(
            """
            INSERT INTO reminders (
                itemId, mode, targetEpochMillis, intervalAmount, intervalUnit,
                selectedDateEpochMillis, selectedHour, selectedMinute, status, createdAt, updatedAt
            )
            VALUES ($ITEM_ID, 'AT_DATE', $ITEM_TIMESTAMP, NULL, NULL, NULL, NULL, NULL, 'ACTIVE', 1, 1)
            """.trimIndent()
        )
        db.query("SELECT COUNT(*) FROM reminders").use { cursor ->
            assertTrue("Запрос count из reminders должен вернуть строку", cursor.moveToFirst())
            assertEquals(
                "Запись в reminders должна быть добавлена после миграции",
                1,
                cursor.getInt(0)
            )
        }
        db.close()
    }

    companion object {
        private const val TEST_DB = "migration-test"

        private const val ITEM_ID = 1L
        private const val ITEM_TITLE = "Тестовое событие"
        private const val ITEM_DETAILS = "Описание события"
        private const val ITEM_TIMESTAMP = 1_700_000_000_000L
    }
}
