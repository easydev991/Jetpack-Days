package com.dayscounter.ui.screen.components.createedit

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Тесты для логики сохранения/восстановления LocalDate.
 */
class LocalDateSaverTest {
    private fun saveLocalDate(localDate: LocalDate?): Long? =
        localDate?.let {
            it
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } ?: -1L

    private fun restoreLocalDate(epochMilli: Long): LocalDate? =
        if (epochMilli == -1L) {
            null
        } else {
            Instant
                .ofEpochMilli(epochMilli)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }

    @Test
    fun saveAndRestore_whenValidDate_thenRestoresCorrectly() {
        // Given
        val originalDate = LocalDate.of(2024, 1, 15)

        // When
        val saved = saveLocalDate(originalDate)
        val restored = restoreLocalDate(saved ?: -1L)

        // Then
        assertEquals(originalDate, restored)
    }

    @Test
    fun saveAndRestore_whenNullDate_thenRestoresNull() {
        // Given
        val originalDate: LocalDate? = null

        // When
        val saved = saveLocalDate(originalDate)
        val restored = if (saved != null) restoreLocalDate(saved) else null

        // Then
        assertNull(restored)
    }

    @Test
    fun save_whenDifferentDates_thenSavesDifferentEpochMillis() {
        // Given
        val date1 = LocalDate.of(2024, 1, 15)
        val date2 = LocalDate.of(2024, 12, 31)

        // When
        val saved1 = saveLocalDate(date1)
        val saved2 = saveLocalDate(date2)

        // Then
        assertNotEquals(saved1, saved2)
    }

    @Test
    fun saveAndRestore_whenLeapYearDate_thenRestoresCorrectly() {
        // Given
        val leapYearDate = LocalDate.of(2024, 2, 29)

        // When
        val saved = saveLocalDate(leapYearDate)
        val restored = restoreLocalDate(saved ?: -1L)

        // Then
        assertEquals(leapYearDate, restored)
    }

    @Test
    fun saveAndRestore_whenPastDate_thenRestoresCorrectly() {
        // Given
        val pastDate = LocalDate.of(2020, 3, 10)

        // When
        val saved = saveLocalDate(pastDate)
        val restored = restoreLocalDate(saved ?: -1L)

        // Then
        assertEquals(pastDate, restored)
    }

    @Test
    fun saveAndRestore_whenFutureDate_thenRestoresCorrectly() {
        // Given
        val futureDate = LocalDate.of(2030, 12, 25)

        // When
        val saved = saveLocalDate(futureDate)
        val restored = restoreLocalDate(saved ?: -1L)

        // Then
        assertEquals(futureDate, restored)
    }
}
