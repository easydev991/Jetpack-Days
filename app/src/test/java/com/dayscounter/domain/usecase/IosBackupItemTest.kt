package com.dayscounter.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Тесты для IosBackupItem - модели для парсинга JSON из iOS.
 * Проверяет корректность конвертации iOS формата в Android формат.
 */
class IosBackupItemTest {
    // Пример Base64 NSKeyedArchiver для красного цвета (реальный пример из iOS)
    // ktlint-disable max-line-length
    @Suppress("MaxLineLength")
    private val redColorBase64 =
        "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
            "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
            "Q29tcG9uZW50Q291bnRWVUlHcmVlblZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
            "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
            "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
            "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
            "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="

    @Test
    fun `toBackupItem converts iOS timestamp (seconds) to Android timestamp (milliseconds)`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = "Test Details",
                timestamp = 699417600.0, // iOS: секунды с 1970-01-01
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals(699417600000L, backupItem!!.timestamp) // Android: миллисекунды
    }

    @Test
    fun `toBackupItem converts iOS timestamp with fractional seconds`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = "Test Details",
                timestamp = 769363669.529918, // iOS: секунды с дробной частью
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals(769363669529L, backupItem!!.timestamp) // Округляем до миллисекунд
    }

    @Test
    fun `toBackupItem preserves title and details`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Оффер",
                details = "Компания, 999 гросс",
                timestamp = 769363669.529918,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals("Оффер", backupItem!!.title)
        assertEquals("Компания, 999 гросс", backupItem.details)
    }

    @Test
    fun `toBackupItem preserves displayOption`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "monthDay",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals("monthDay", backupItem!!.displayOption)
    }

    @Test
    fun `toBackupItem preserves colorTag as null when not present`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertNull(backupItem!!.colorTag)
    }

    @Test
    fun `toBackupItem returns null for invalid displayOption`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "invalid_option",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNull(backupItem)
    }

    @Test
    fun `toBackupItem with iOS colorTag Base64 parses to hex format`() {
        // Given - реальный пример из iOS с Base64 NSKeyedArchiver
        val iosBackupItem =
            IosBackupItem(
                title = "Оффер",
                details = "Компания, 999 гросс",
                timestamp = 769363669.529918,
                colorTag = redColorBase64,
                displayOption = "day",
            )

        // When - colorTag парсится из Base64 NSKeyedArchiver в hex
        val backupItem = iosBackupItem.toBackupItem()

        // Then - colorTag должен быть в hex формате "#RRGGBB"
        assertNotNull(backupItem)
        assertEquals("#FF3A30", backupItem!!.colorTag)
    }

    @Test
    fun `toBackupItem handles null details`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertNull(backupItem!!.details)
    }

    @Test
    fun `toBackupItem handles yearMonthDay displayOption`() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "yearMonthDay",
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals("yearMonthDay", backupItem!!.displayOption)
    }
}
