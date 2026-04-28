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
    fun tobackupitem_converts_ios_timestamp_seconds_to_android_timestamp_milliseconds() {
        // Given
        // iOS использует секунды с 2001-01-01 (timeIntervalSinceReferenceDate)
        // -278889600.0 сек с 2001 = 699417600000 мс с 1970
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = "Test Details",
                timestamp = -278889600.0, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "day"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals(699417600000L, backupItem!!.timestamp) // Android: миллисекунды с 1970-01-01
    }

    @Test
    fun tobackupitem_converts_ios_timestamp_with_fractional_seconds() {
        // Given
        // iOS использует секунды с 2001-01-01 (timeIntervalSinceReferenceDate)
        // -208943530.470082 сек с 2001 = 769363669529 мс с 1970
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = "Test Details",
                timestamp = -208943530.470082, // iOS: секунды с 2001-01-01 с дробной частью
                colorTag = null,
                displayOption = "day"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals(769363669529L, backupItem!!.timestamp) // Округляем до миллисекунд
    }

    @Test
    fun tobackupitem_preserves_title_and_details() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Оффер",
                details = "Компания, 999 гросс",
                timestamp = -208943530.470082, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "day"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals("Оффер", backupItem!!.title)
        assertEquals("Компания, 999 гросс", backupItem.details)
    }

    @Test
    fun tobackupitem_preserves_displayoption() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = -278889600.0, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "monthDay"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals("monthDay", backupItem!!.displayOption)
    }

    @Test
    fun tobackupitem_preserves_colortag_as_null_when_not_present() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = -278889600.0, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "day"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertNull(backupItem!!.colorTag)
    }

    @Test
    fun tobackupitem_returns_null_for_invalid_displayoption() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = -278889600.0, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "invalid_option"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNull(backupItem)
    }

    @Test
    fun tobackupitem_with_ios_colortag_base64_parses_to_hex_format() {
        // Given - реальный пример из iOS с Base64 NSKeyedArchiver
        val iosBackupItem =
            IosBackupItem(
                title = "Оффер",
                details = "Компания, 999 гросс",
                timestamp = -208943530.470082, // iOS: секунды с 2001-01-01
                colorTag = redColorBase64,
                displayOption = "day"
            )

        // When - colorTag парсится из Base64 NSKeyedArchiver в hex
        val backupItem = iosBackupItem.toBackupItem()

        // Then - colorTag должен быть в hex формате "#RRGGBB"
        assertNotNull(backupItem)
        assertEquals("#FF3A30", backupItem!!.colorTag)
    }

    @Test
    fun tobackupitem_handles_null_details() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = -278889600.0, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "day"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertNull(backupItem!!.details)
    }

    @Test
    fun tobackupitem_handles_yearmonthday_displayoption() {
        // Given
        val iosBackupItem =
            IosBackupItem(
                title = "Test Event",
                details = null,
                timestamp = -278889600.0, // iOS: секунды с 2001-01-01
                colorTag = null,
                displayOption = "yearMonthDay"
            )

        // When
        val backupItem = iosBackupItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals("yearMonthDay", backupItem!!.displayOption)
    }
}
