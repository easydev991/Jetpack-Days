package com.dayscounter.domain.usecase

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Тесты для BackupWrapper.
 * Проверяет корректность сериализации/десериализации обёртки резервной копии.
 */
class BackupWrapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    // MARK: - Десериализация BackupWrapper

    @Test
    fun `decode BackupWrapper with format android`() {
        // Given
        val jsonString =
            """
            {
                "format": "android",
                "items": [
                    {
                        "title": "Test Event",
                        "details": "Test Details",
                        "timestamp": 1234567890000,
                        "colorTag": "#FF0000",
                        "displayOption": "day"
                    }
                ]
            }
            """.trimIndent()

        // When
        val wrapper = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertEquals(BackupFormat.ANDROID, wrapper.format)
        assertEquals(1, wrapper.items.size)
        assertEquals("Test Event", wrapper.items[0].title)
        assertEquals(1234567890000L, wrapper.items[0].timestamp)
    }

    @Test
    fun `decode BackupWrapper with format ios`() {
        // Given
        val jsonString =
            """
            {
                "format": "ios",
                "items": [
                    {
                        "title": "iOS Event",
                        "details": "From iOS",
                        "timestamp": -1756176000,
                        "colorTag": null,
                        "displayOption": "day"
                    }
                ]
            }
            """.trimIndent()

        // When
        val wrapper = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertEquals(BackupFormat.IOS, wrapper.format)
        assertEquals(1, wrapper.items.size)
        assertEquals("iOS Event", wrapper.items[0].title)
        assertEquals(-1756176000L, wrapper.items[0].timestamp)
    }

    @Test
    fun `decode BackupWrapper with format null`() {
        // Given
        val jsonString =
            """
            {
                "items": [
                    {
                        "title": "No Format Event",
                        "details": null,
                        "timestamp": 999999999000,
                        "colorTag": "#00FF00",
                        "displayOption": "monthDay"
                    }
                ]
            }
            """.trimIndent()

        // When
        val wrapper = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertNull(wrapper.format)
        assertEquals(1, wrapper.items.size)
        assertEquals("No Format Event", wrapper.items[0].title)
    }

    @Test
    fun `decode BackupWrapper with multiple items`() {
        // Given
        val jsonString =
            """
            {
                "format": "android",
                "items": [
                    {
                        "title": "Event 1",
                        "details": "Details 1",
                        "timestamp": 1000000000000,
                        "colorTag": "#FF0000",
                        "displayOption": "day"
                    },
                    {
                        "title": "Event 2",
                        "details": "Details 2",
                        "timestamp": 2000000000000,
                        "colorTag": null,
                        "displayOption": "monthDay"
                    },
                    {
                        "title": "Event 3",
                        "details": null,
                        "timestamp": 3000000000000,
                        "colorTag": "#0000FF",
                        "displayOption": "yearMonthDay"
                    }
                ]
            }
            """.trimIndent()

        // When
        val wrapper = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertEquals(BackupFormat.ANDROID, wrapper.format)
        assertEquals(3, wrapper.items.size)
        assertEquals("Event 1", wrapper.items[0].title)
        assertEquals("Event 2", wrapper.items[1].title)
        assertEquals("Event 3", wrapper.items[2].title)
    }

    // MARK: - Сериализация BackupWrapper

    @Test
    fun `encode BackupWrapper with format android`() {
        // Given
        val wrapper =
            BackupWrapper(
                format = BackupFormat.ANDROID,
                items =
                    listOf(
                        BackupItem(
                            title = "Test Event",
                            details = "Test Details",
                            timestamp = 1234567890000L,
                            colorTag = "#FF0000",
                            displayOption = "day"
                        )
                    )
            )

        // When
        val jsonString = Json.encodeToString(wrapper)

        // Then
        assert(jsonString.contains("\"format\":\"android\""))
        assert(jsonString.contains("\"title\":\"Test Event\""))
        assert(jsonString.contains("\"timestamp\":1234567890000"))
    }

    @Test
    fun `encode BackupWrapper with format null`() {
        // Given
        val wrapper =
            BackupWrapper(
                format = null,
                items =
                    listOf(
                        BackupItem(
                            title = "No Format",
                            details = null,
                            timestamp = 999999999000L,
                            colorTag = null,
                            displayOption = "day"
                        )
                    )
            )

        // When
        val jsonString = Json.encodeToString(wrapper)

        // Then - null поля не включаются в JSON по умолчанию
        assert(!jsonString.contains("\"format\""))
        assert(jsonString.contains("\"title\":\"No Format\""))
    }

    // MARK: - Round-trip тесты

    @Test
    fun `round-trip BackupWrapper with android format`() {
        // Given
        val original =
            BackupWrapper(
                format = BackupFormat.ANDROID,
                items =
                    listOf(
                        BackupItem(
                            title = "Round Trip",
                            details = "Test",
                            timestamp = 1111111111111L,
                            colorTag = "#ABCDEF",
                            displayOption = "monthDay"
                        )
                    )
            )

        // When
        val jsonString = Json.encodeToString(original)
        val decoded = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertEquals(original.format, decoded.format)
        assertEquals(original.items.size, decoded.items.size)
        assertEquals(original.items[0].title, decoded.items[0].title)
        assertEquals(original.items[0].timestamp, decoded.items[0].timestamp)
        assertEquals(original.items[0].colorTag, decoded.items[0].colorTag)
    }

    @Test
    fun `round-trip BackupWrapper with ios format`() {
        // Given
        val original =
            BackupWrapper(
                format = BackupFormat.IOS,
                items =
                    listOf(
                        BackupItem(
                            title = "iOS Backup",
                            details = "From iPhone",
                            timestamp = -1000000000L,
                            colorTag = null,
                            displayOption = "day"
                        )
                    )
            )

        // When
        val jsonString = Json.encodeToString(original)
        val decoded = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertEquals(BackupFormat.IOS, decoded.format)
        assertEquals(original.items[0].title, decoded.items[0].title)
    }

    // MARK: - Fallback на List<BackupItem> (старый формат)

    @Test
    fun `decode old format as List of BackupItem`() {
        // Given - старый формат без обёртки (массив напрямую)
        val jsonString =
            """
            [
                {
                    "title": "Old Event 1",
                    "details": "Old Details 1",
                    "timestamp": 1000000000000,
                    "colorTag": "#FF0000",
                    "displayOption": "day"
                },
                {
                    "title": "Old Event 2",
                    "details": null,
                    "timestamp": 2000000000000,
                    "colorTag": null,
                    "displayOption": "monthDay"
                }
            ]
            """.trimIndent()

        // When
        val items = json.decodeFromString<List<BackupItem>>(jsonString)

        // Then
        assertEquals(2, items.size)
        assertEquals("Old Event 1", items[0].title)
        assertEquals("Old Event 2", items[1].title)
    }
}
