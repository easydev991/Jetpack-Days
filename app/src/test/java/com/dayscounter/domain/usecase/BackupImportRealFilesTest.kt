package com.dayscounter.domain.usecase

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream

/**
 * Интеграционные тесты для импорта реальных файлов резервных копий.
 *
 * Тестовые файлы в resources/:
 * - old-backup-sample.json — старый Android формат (массив без wrapper)
 * - old-ios-backup-sample.json — старый iOS формат (массив, plist colorTag)
 * - new-backup-sample.json — новый Android формат (с wrapper, format: "android")
 * - new-ios-backup.json — новый iOS формат (с wrapper, format: "ios")
 *
 * Примечание: iOS формат (new-ios-backup.json) использует timestamp в секундах с 2001-01-01,
 * поэтому парсится через IosBackupItem, а не напрямую через BackupWrapper.
 */
class BackupImportRealFilesTest {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    // MARK: - old-backup-sample.json — старый Android формат

    @Test
    fun parse_old_backup_sample_json_as_list_of_backupitem() {
        // Given
        val jsonString = loadResource("/old-backup-sample.json")

        // When
        val items = json.decodeFromString<List<BackupItem>>(jsonString)

        // Then
        assertEquals(8, items.size)

        // Проверяем первый элемент
        val firstItem = items[0]
        assertEquals("День рождения", firstItem.title)
        assertEquals("Мой день рождения", firstItem.details)
        assertEquals(699417600000L, firstItem.timestamp)
        assertEquals("#FF5722", firstItem.colorTag)
        assertEquals("day", firstItem.displayOption)

        // Проверяем элемент без описания
        val noDetailsItem = items.find { it.title == "Без описания" }
        assertNotNull(noDetailsItem)
        assertNull(noDetailsItem?.details)

        // Проверяем элемент без цвета
        val noColorItem = items.find { it.title == "Без цвета" }
        assertNotNull(noColorItem)
        assertNull(noColorItem?.colorTag)
    }

    @Test
    fun parse_old_backup_sample_and_convert_to_domain_items() {
        // Given
        val jsonString = loadResource("/old-backup-sample.json")
        val backupItems = json.decodeFromString<List<BackupItem>>(jsonString)

        // When
        val domainItems = backupItems.mapNotNull { it.toItem() }

        // Then
        assertEquals(8, domainItems.size)

        // Проверяем что hex цвета корректно парсятся
        val redItem = domainItems.find { it.title == "День рождения" }
        assertNotNull(redItem)
        assertEquals(0xFFFF5722.toInt(), redItem?.colorTag)
    }

    // MARK: - old-ios-backup-sample.json — старый iOS формат (массив)
    // Примечание: iOS файлы содержат timestamp как Double, поэтому парсятся через IosBackupItem

    @Test
    fun parse_old_ios_backup_sample_json_as_list_of_iosbackupitem() {
        // Given
        val jsonString = loadResource("/old-ios-backup-sample.json")

        // When - парсим как List<IosBackupItem> (timestamp = Double)
        val items = json.decodeFromString<List<IosBackupItem>>(jsonString)

        // Then
        assertEquals(4, items.size)

        // Проверяем что colorTag содержит Base64 данные (plist)
        val birthdayItem = items.find { it.title == "День рождения" }
        assertNotNull(birthdayItem)
        assertNotNull(birthdayItem?.colorTag)

        // Проверяем элемент без цвета (colorTag = null)
        val noColorItem = items.find { it.title == "Выпускной" }
        assertNotNull(noColorItem)
        assertNull(noColorItem?.colorTag)
    }

    @Test
    fun parse_old_ios_backup_sample_and_convert_colortag_successfully() {
        // Given
        val jsonString = loadResource("/old-ios-backup-sample.json")
        val iosItems = json.decodeFromString<List<IosBackupItem>>(jsonString)

        // When - toBackupItem() конвертирует Base64 plist colorTag в hex
        val androidItems = iosItems.mapNotNull { it.toBackupItem() }

        // Then
        assertEquals(4, androidItems.size)

        // Проверяем что iOS colorTag (Base64 plist) успешно конвертируется в hex
        val weddingItem = androidItems.find { it.title == "Свадьба" }
        assertNotNull(weddingItem)
        // colorTag должен быть сконвертирован из Base64 plist в hex (#RRGGBB)
        assertNotNull(weddingItem?.colorTag)
        assertTrue(weddingItem?.colorTag!!.startsWith("#"))
    }

    // MARK: - new-backup-sample.json — новый Android формат с wrapper

    @Test
    fun parse_new_backup_sample_json_as_backupwrapper() {
        // Given
        val jsonString = loadResource("/new-backup-sample.json")

        // When
        val wrapper = json.decodeFromString<BackupWrapper>(jsonString)

        // Then
        assertEquals(BackupFormat.ANDROID, wrapper.format)
        assertEquals(8, wrapper.items.size)

        // Проверяем структуру первого элемента
        val firstItem = wrapper.items[0]
        assertEquals("День рождения", firstItem.title)
        assertEquals("Мой день рождения", firstItem.details)
        assertEquals(699417600000L, firstItem.timestamp)
        assertEquals("#FF5722", firstItem.colorTag)
        assertEquals("day", firstItem.displayOption)
    }

    @Test
    fun parse_new_backup_sample_and_convert_to_domain_items() {
        // Given
        val jsonString = loadResource("/new-backup-sample.json")
        val wrapper = json.decodeFromString<BackupWrapper>(jsonString)

        // When
        val domainItems = wrapper.items.mapNotNull { it.toItem() }

        // Then
        assertEquals(8, domainItems.size)

        // Проверяем что hex цвета корректно парсятся
        val blueItem = domainItems.find { it.title == "Синий" }
        assertNotNull(blueItem)
        assertEquals(0xFF2196F3.toInt(), blueItem?.colorTag)
    }

    // MARK: - new-ios-backup.json — новый iOS формат
    // Примечание: iOS формат использует timestamp в секундах с 2001-01-01,
    // поэтому парсится через IosBackupWrapper (отдельный data class)

    /**
     * IosBackupWrapper для парсинга iOS формата с timestamp в секундах.
     */
    @kotlinx.serialization.Serializable
    private data class IosBackupWrapper(
        val format: String? = null,
        val items: List<IosBackupItem>
    )

    @Test
    fun parse_new_ios_backup_json_as_iosbackupwrapper() {
        // Given
        val jsonString = loadResource("/new-ios-backup.json")

        // When - парсим через IosBackupWrapper с IosBackupItem
        val wrapper = json.decodeFromString<IosBackupWrapper>(jsonString)

        // Then
        assertEquals("ios", wrapper.format)
        assertTrue(wrapper.items.isNotEmpty())

        // Проверяем элемент с известным timestamp
        val victoryDay = wrapper.items.find { it.title == "День победы" }
        assertNotNull(victoryDay)
        // iOS timestamp: -1756176000 сек с 2001-01-01
        assertEquals(-1756176000.0, victoryDay?.timestamp)
    }

    @Test
    fun parse_new_ios_backup_and_convert_timestamps_correctly() {
        // Given
        val jsonString = loadResource("/new-ios-backup.json")
        val wrapper = json.decodeFromString<IosBackupWrapper>(jsonString)

        // When - конвертируем iOS items в Android формат
        val androidItems = wrapper.items.mapNotNull { it.toBackupItem() }

        // Then
        assertTrue(androidItems.isNotEmpty())

        // Проверяем конвертацию timestamp для "День победы"
        val victoryDay = androidItems.find { it.title == "День победы" }
        assertNotNull(victoryDay)
        // iOS: -1756176000 сек с 2001-01-01
        // Android: ((-1756176000 + 978307200) * 1000) = -777868800000 мс с 1970-01-01
        val expectedAndroidTimestamp = ((-1756176000.0 + 978307200.0) * 1000.0).toLong()
        assertEquals(expectedAndroidTimestamp, victoryDay?.timestamp)
    }

    @Test
    fun parse_new_ios_backup_and_convert_colortags_successfully() {
        // Given
        val jsonString = loadResource("/new-ios-backup.json")
        val wrapper = json.decodeFromString<IosBackupWrapper>(jsonString)

        // When
        val androidItems = wrapper.items.mapNotNull { it.toBackupItem() }

        // Then
        assertTrue(androidItems.isNotEmpty())

        // Проверяем что colorTag успешно конвертируется
        val greenItem = androidItems.find { it.title == "Зелёный" }
        assertNotNull(greenItem)
        assertNotNull(greenItem?.colorTag)
        // Должен быть hex формат #RRGGBB
        assertTrue(greenItem?.colorTag!!.startsWith("#"))
    }

    // MARK: - Вспомогательные методы

    private fun loadResource(path: String): String {
        val stream: InputStream? = javaClass.getResourceAsStream(path)
        assertNotNull(stream, "Resource not found: $path")
        return stream!!.bufferedReader().use { it.readText() }
    }
}
