package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Тесты для конвертации BackupItem.
 * Проверяет корректность конвертации между Item и BackupItem.
 */
class BackupItemTest {
    @Test
    fun `toHexColor converts Int color to hex string`() {
        // Given
        val redColor = 0xFFFF0000.toInt() // Красный цвет в ARGB
        val greenColor = 0xFF00FF00.toInt() // Зеленый цвет в ARGB
        val blueColor = 0xFF0000FF.toInt() // Синий цвет в ARGB

        // When
        val redHex = redColor.toHexColor()
        val greenHex = greenColor.toHexColor()
        val blueHex = blueColor.toHexColor()

        // Then
        assertEquals("#FF0000", redHex)
        assertEquals("#00FF00", greenHex)
        assertEquals("#0000FF", blueHex)
    }

    @Test
    fun `fromHexColor converts hex string to Int color`() {
        // Given
        val redHex = "#FF0000"
        val greenHex = "#00FF00"
        val blueHex = "#0000FF"

        // When
        val redColor = redHex.fromHexColor()
        val greenColor = greenHex.fromHexColor()
        val blueColor = blueHex.fromHexColor()

        // Then
        assertNotNull(redColor)
        assertNotNull(greenColor)
        assertNotNull(blueColor)
        // fromHexColor возвращает полный ARGB цвет с альфа-каналом 0xFF
        assertEquals(0xFFFF0000.toInt(), redColor)
        assertEquals(0xFF00FF00.toInt(), greenColor)
        assertEquals(0xFF0000FF.toInt(), blueColor)
    }

    @Test
    fun `fromHexColor returns null for invalid hex string`() {
        // Given
        val invalidHex = "INVALID"

        // When
        val color = invalidHex.fromHexColor()

        // Then
        assertNull(color)
    }

    @Test
    fun `Item to BackupItem with colorTag`() {
        // Given
        val item =
            Item(
                id = 1,
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = 0xFFFF0000.toInt(),
                displayOption = DisplayOption.DAY,
            )

        // When
        val backupItem = item.toBackupItem()

        // Then
        assertEquals(item.title, backupItem.title)
        assertEquals(item.details, backupItem.details)
        assertEquals(item.timestamp, backupItem.timestamp)
        assertEquals("#FF0000", backupItem.colorTag)
        assertEquals("day", backupItem.displayOption)
    }

    @Test
    fun `Item to BackupItem without colorTag`() {
        // Given
        val item =
            Item(
                id = 1,
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )

        // When
        val backupItem = item.toBackupItem()

        // Then
        assertEquals(item.title, backupItem.title)
        assertEquals(item.details, backupItem.details)
        assertEquals(item.timestamp, backupItem.timestamp)
        assertNull(backupItem.colorTag)
        assertEquals("day", backupItem.displayOption)
    }

    @Test
    fun `Item to BackupItem with monthDay displayOption`() {
        // Given
        val item =
            Item(
                id = 1,
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = DisplayOption.MONTH_DAY,
            )

        // When
        val backupItem = item.toBackupItem()

        // Then
        assertEquals(item.title, backupItem.title)
        assertEquals(item.details, backupItem.details)
        assertEquals(item.timestamp, backupItem.timestamp)
        assertNull(backupItem.colorTag)
        assertEquals("monthDay", backupItem.displayOption)
    }

    @Test
    fun `Item to BackupItem with yearMonthDay displayOption`() {
        // Given
        val item =
            Item(
                id = 1,
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
            )

        // When
        val backupItem = item.toBackupItem()

        // Then
        assertEquals(item.title, backupItem.title)
        assertEquals(item.details, backupItem.details)
        assertEquals(item.timestamp, backupItem.timestamp)
        assertNull(backupItem.colorTag)
        assertEquals("yearMonthDay", backupItem.displayOption)
    }

    @Test
    fun `BackupItem to Item with colorTag`() {
        // Given
        val backupItem =
            BackupItem(
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = "#FF0000",
                displayOption = "day",
            )

        // When
        val item = backupItem.toItem()

        // Then
        assertNotNull(item)
        assertEquals(backupItem.title, item!!.title)
        assertEquals(backupItem.details, item.details)
        assertEquals(backupItem.timestamp, item.timestamp)
        assertEquals(0xFFFF0000.toInt(), item.colorTag)
        assertEquals(DisplayOption.DAY, item.displayOption)
    }

    @Test
    fun `BackupItem to Item without colorTag`() {
        // Given
        val backupItem =
            BackupItem(
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val item = backupItem.toItem()

        // Then
        assertNotNull(item)
        assertEquals(backupItem.title, item!!.title)
        assertEquals(backupItem.details, item.details)
        assertEquals(backupItem.timestamp, item.timestamp)
        assertNull(item.colorTag)
        assertEquals(DisplayOption.DAY, item.displayOption)
    }

    @Test
    fun `BackupItem to Item with monthDay displayOption`() {
        // Given
        val backupItem =
            BackupItem(
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = "monthDay",
            )

        // When
        val item = backupItem.toItem()

        // Then
        assertNotNull(item)
        assertEquals(backupItem.title, item!!.title)
        assertEquals(backupItem.details, item.details)
        assertEquals(backupItem.timestamp, item.timestamp)
        assertNull(item.colorTag)
        assertEquals(DisplayOption.MONTH_DAY, item.displayOption)
    }

    @Test
    fun `BackupItem to Item with yearMonthDay displayOption`() {
        // Given
        val backupItem =
            BackupItem(
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = "yearMonthDay",
            )

        // When
        val item = backupItem.toItem()

        // Then
        assertNotNull(item)
        assertEquals(backupItem.title, item!!.title)
        assertEquals(backupItem.details, item.details)
        assertEquals(backupItem.timestamp, item.timestamp)
        assertNull(item.colorTag)
        assertEquals(DisplayOption.YEAR_MONTH_DAY, item.displayOption)
    }

    @Test
    fun `BackupItem to Item with invalid displayOption returns null`() {
        // Given
        val backupItem =
            BackupItem(
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = null,
                displayOption = "invalid",
            )

        // When
        val item = backupItem.toItem()

        // Then
        assertNull(item)
    }

    @Test
    fun `round-trip conversion preserves item data`() {
        // Given
        val originalItem =
            Item(
                id = 1,
                title = "Test Title",
                details = "Test Details",
                timestamp = 1234567890000L,
                colorTag = 0xFF00FF00.toInt(),
                displayOption = DisplayOption.MONTH_DAY,
            )

        // When
        val backupItem = originalItem.toBackupItem()
        val restoredItem = backupItem.toItem()

        // Then
        assertNotNull(restoredItem)
        assertEquals(originalItem.title, restoredItem!!.title)
        assertEquals(originalItem.details, restoredItem.details)
        assertEquals(originalItem.timestamp, restoredItem.timestamp)
        // colorTag сохраняется полностью, без маски
        assertEquals(originalItem.colorTag, restoredItem.colorTag)
        assertEquals(originalItem.displayOption, restoredItem.displayOption)
    }

    // MARK: - parseColorTag Tests (hex and Base64 formats)

    @Test
    fun `parseColorTag with hex format returns correct color`() {
        // Given
        val hexColor = "#FF0000"

        // When
        val color = hexColor.parseColorTag()

        // Then
        assertNotNull(color)
        assertEquals(0xFFFF0000.toInt(), color)
    }

    @Test
    fun `parseColorTag with Base64 iOS format returns correct color`() {
        // Given - используем строку из NsKeyedArchiverParserTest напрямую
        @Suppress("MaxLineLength")
        val base64FromParserTest =
            "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
                "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
                "Q29tcG9uZW50Q291bnRWVUlHcmVublZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
                "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
                "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
                "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
                "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="

        // Debug: проверяем длину
        println("base64FromParserTest length: ${base64FromParserTest.length}")

        // When - сначала проверяем что парсер распознает формат
        val isNsKeyedArchiver = NsKeyedArchiverParser.isNsKeyedArchiver(base64FromParserTest)
        println("isNsKeyedArchiver: $isNsKeyedArchiver")
        val hexFromParser = NsKeyedArchiverParser.parseHexColor(base64FromParserTest)
        println("hexFromParser: $hexFromParser")
        val color = base64FromParserTest.parseColorTag()
        println("color: $color")

        // Then
        assertEquals(true, isNsKeyedArchiver, "Should be recognized as NSKeyedArchiver")
        assertEquals("#FF3A30", hexFromParser, "Parser should return #FF3A30")
        assertNotNull(color, "parseColorTag should return non-null")
        assertEquals(0xFFFF3A30.toInt(), color)
    }

    @Test
    fun `parseColorTag with null returns null`() {
        // Given
        val nullColor: String? = null

        // When
        val color = nullColor.parseColorTag()

        // Then
        assertNull(color)
    }

    @Test
    fun `parseColorTag with empty string returns null`() {
        // Given
        val emptyColor = ""

        // When
        val color = emptyColor.parseColorTag()

        // Then
        assertNull(color)
    }

    @Test
    fun `parseColorTag with invalid format returns null`() {
        // Given
        val invalidColor = "INVALID_COLOR"

        // When
        val color = invalidColor.parseColorTag()

        // Then
        assertNull(color)
    }

    @Test
    fun `BackupItem to Item with iOS Base64 colorTag`() {
        // Given - BackupItem с colorTag в формате iOS Base64
        @Suppress("MaxLineLength")
        val base64Color =
            "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
                "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
                "Q29tcG9uZW50Q291bnRWVUlHcmVublZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
                "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
                "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
                "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
                "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="

        val backupItem =
            BackupItem(
                title = "iOS Event",
                details = "Imported from iOS",
                timestamp = 769363669529L,
                colorTag = base64Color,
                displayOption = "day",
            )

        // When
        val item = backupItem.toItem()

        // Then
        assertNotNull(item)
        assertEquals(0xFFFF3A30.toInt(), item!!.colorTag)
    }
}
