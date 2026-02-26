package com.dayscounter.domain.usecase

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Интеграционный тест полного цикла импорта iOS-бекапа.
 *
 * Тестирует:
 * - Чтение JSON из ресурсов
 * - Парсинг IosBackupItem через kotlinx.serialization
 * - Конвертацию timestamp: секунды → миллисекунды
 * - Парсинг colorTag: Base64 NSKeyedArchiver → hex
 * - Конвертацию в Item для сохранения в БД
 */
@Suppress("TooManyFunctions")
class IosBackupIntegrationTest {
    private lateinit var json: Json

    @BeforeEach
    fun setup() {
        json =
            Json {
                ignoreUnknownKeys = true
            }
    }

    // MARK: - Full Cycle Tests

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `full cycle - parse ios-backup-sample json and convert to items`() {
        // Given - читаем JSON из ресурсов
        val inputStream =
            javaClass.classLoader
                ?.getResourceAsStream("ios-backup-sample.json")
                ?: throw AssertionError("Не удалось найти ios-backup-sample.json в ресурсах")

        // When - парсим JSON
        val iosBackupItems: List<IosBackupItem> = json.decodeFromStream(inputStream)

        // Then - проверяем что все 4 записи прочитаны
        assertEquals(4, iosBackupItems.size)

        // Проверяем первую запись (День рождения)
        val birthday = iosBackupItems[0]
        assertEquals("День рождения", birthday.title)
        assertEquals("Мой день рождения", birthday.details)
        assertEquals(699417600.0, birthday.timestamp)
        assertNotNull(birthday.colorTag)
        assertEquals("day", birthday.displayOption)

        // Проверяем вторую запись (Свадьба)
        val wedding = iosBackupItems[1]
        assertEquals("Свадьба", wedding.title)
        assertEquals("День свадьбы", wedding.details)
        assertEquals(1151808000.0, wedding.timestamp)
        assertNotNull(wedding.colorTag)
        assertEquals("monthDay", wedding.displayOption)

        // Проверяем третью запись (Работа)
        val work = iosBackupItems[2]
        assertEquals("Первый день на работе", work.title)
        assertEquals("Начало карьеры", work.details)
        assertEquals(1262736000.0, work.timestamp)
        assertNotNull(work.colorTag)
        assertEquals("yearMonthDay", work.displayOption)

        // Проверяем четвертую запись (Выпускной - без цвета)
        val graduation = iosBackupItems[3]
        assertEquals("Выпускной", graduation.title)
        assertEquals("Окончание университета", graduation.details)
        assertEquals(1020489600.0, graduation.timestamp)
        assertNull(graduation.colorTag)
        assertEquals("day", graduation.displayOption)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `full cycle - convert all ios items to backup items`() {
        // Given
        val inputStream =
            javaClass.classLoader
                ?.getResourceAsStream("ios-backup-sample.json")
                ?: throw AssertionError("Не удалось найти ios-backup-sample.json в ресурсах")
        val iosBackupItems: List<IosBackupItem> = json.decodeFromStream(inputStream)

        // When - конвертируем все записи
        val backupItems = iosBackupItems.map { it.toBackupItem() }

        // Then - все 4 записи успешно конвертированы
        assertEquals(4, backupItems.size)
        backupItems.forEach { assertNotNull(it) }

        // Проверяем конвертацию timestamp (секунды → миллисекунды)
        val birthday = backupItems[0]!!
        assertEquals(699417600000L, birthday.timestamp) // 699417600.0 * 1000

        val wedding = backupItems[1]!!
        assertEquals(1151808000000L, wedding.timestamp) // 1151808000.0 * 1000

        val work = backupItems[2]!!
        assertEquals(1262736000000L, work.timestamp) // 1262736000.0 * 1000

        val graduation = backupItems[3]!!
        assertEquals(1020489600000L, graduation.timestamp) // 1020489600.0 * 1000
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `full cycle - parse colorTag from base64 to hex`() {
        // Given
        val inputStream =
            javaClass.classLoader
                ?.getResourceAsStream("ios-backup-sample.json")
                ?: throw AssertionError("Не удалось найти ios-backup-sample.json в ресурсах")
        val iosBackupItems: List<IosBackupItem> = json.decodeFromStream(inputStream)

        // When - конвертируем и проверяем colorTag
        val backupItems = iosBackupItems.map { it.toBackupItem() }

        // Then - проверяем что colorTag успешно конвертирован из Base64 в hex
        val birthday = backupItems[0]!!
        assertNotNull(birthday.colorTag)
        assertTrue(
            birthday.colorTag!!.startsWith("#"),
            "colorTag должен быть в hex формате (#RRGGBB)"
        )

        val wedding = backupItems[1]!!
        assertNotNull(wedding.colorTag)
        assertTrue(wedding.colorTag!!.startsWith("#"))

        val work = backupItems[2]!!
        assertNotNull(work.colorTag)
        assertTrue(work.colorTag!!.startsWith("#"))

        // Запись без цвета
        val graduation = backupItems[3]!!
        assertNull(graduation.colorTag)
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `full cycle - convert backup items to domain items`() {
        // Given
        val inputStream =
            javaClass.classLoader
                ?.getResourceAsStream("ios-backup-sample.json")
                ?: throw AssertionError("Не удалось найти ios-backup-sample.json в ресурсах")
        val iosBackupItems: List<IosBackupItem> = json.decodeFromStream(inputStream)
        val backupItems = iosBackupItems.mapNotNull { it.toBackupItem() }

        // When - конвертируем в Item
        val items = backupItems.mapNotNull { it.toItem() }

        // Then - все 4 записи успешно конвертированы
        assertEquals(4, items.size)

        // Проверяем первую запись
        val birthday = items[0]
        assertEquals("День рождения", birthday.title)
        assertEquals("Мой день рождения", birthday.details)
        assertEquals(699417600000L, birthday.timestamp)
        assertNotNull(birthday.colorTag)
        assertEquals(com.dayscounter.domain.model.DisplayOption.DAY, birthday.displayOption)

        // Проверяем displayOption для всех записей
        assertEquals(com.dayscounter.domain.model.DisplayOption.DAY, items[0].displayOption)
        assertEquals(com.dayscounter.domain.model.DisplayOption.MONTH_DAY, items[1].displayOption)
        assertEquals(
            com.dayscounter.domain.model.DisplayOption.YEAR_MONTH_DAY,
            items[2].displayOption
        )
        assertEquals(com.dayscounter.domain.model.DisplayOption.DAY, items[3].displayOption)
    }

    // MARK: - Timestamp Conversion Tests

    @Test
    fun `timestamp conversion - seconds to milliseconds`() {
        // Given
        val iosItem =
            IosBackupItem(
                title = "Тест",
                details = null,
                timestamp = 699417600.0, // Секунды
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals(699417600000L, backupItem!!.timestamp) // Миллисекунды
    }

    @Test
    fun `timestamp conversion - preserves date correctly`() {
        // Given - 1 января 2000 года 00:00:00 UTC в секундах
        val timestamp2000 = 946684800.0
        val iosItem =
            IosBackupItem(
                title = "2000",
                details = null,
                timestamp = timestamp2000,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertEquals(946684800000L, backupItem!!.timestamp)

        // Проверяем что дата корректна
        val date = java.util.Date(backupItem.timestamp)
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        calendar.time = date
        assertEquals(2000, calendar.get(java.util.Calendar.YEAR))
        assertEquals(java.util.Calendar.JANUARY, calendar.get(java.util.Calendar.MONTH))
        assertEquals(1, calendar.get(java.util.Calendar.DAY_OF_MONTH))
    }

    // MARK: - ColorTag Parsing Tests

    @Test
    fun `colorTag parsing - null colorTag remains null`() {
        // Given
        val iosItem =
            IosBackupItem(
                title = "Тест",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertNull(backupItem!!.colorTag)
    }

    @Test
    fun `colorTag parsing - valid base64 converts to hex`() {
        // Given - Base64 для зеленого цвета (из существующих тестов)
        val greenBase64 =
            "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
                "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
                "Q29tcG9uZW50Q291bnRWVUlHcmVublZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
                "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
                "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
                "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
                "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="

        val iosItem =
            IosBackupItem(
                title = "Тест",
                details = null,
                timestamp = 699417600.0,
                colorTag = greenBase64,
                displayOption = "day",
            )

        // When
        val backupItem = iosItem.toBackupItem()

        // Then
        assertNotNull(backupItem)
        assertNotNull(backupItem!!.colorTag)
        assertTrue(backupItem.colorTag!!.startsWith("#"))
    }

    // MARK: - DisplayOption Validation Tests

    @Test
    fun `displayOption validation - valid options convert correctly`() {
        // Given
        val validOptions = listOf("day", "monthDay", "yearMonthDay")

        validOptions.forEach { option ->
            val iosItem =
                IosBackupItem(
                    title = "Тест",
                    details = null,
                    timestamp = 699417600.0,
                    colorTag = null,
                    displayOption = option,
                )

            // When
            val backupItem = iosItem.toBackupItem()

            // Then
            assertNotNull(backupItem, "displayOption '$option' должен быть валидным")
            assertEquals(option, backupItem!!.displayOption)
        }
    }

    @Test
    fun `displayOption validation - invalid option returns null`() {
        // Given
        val iosItem =
            IosBackupItem(
                title = "Тест",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "invalidOption",
            )

        // When
        val backupItem = iosItem.toBackupItem()

        // Then
        assertNull(backupItem, "Невалидный displayOption должен возвращать null")
    }

    // MARK: - Round-trip Tests

    @OptIn(ExperimentalSerializationApi::class)
    @Test
    fun `round trip - item to iOS backup and back preserves data`() {
        // Given
        val inputStream =
            javaClass.classLoader
                ?.getResourceAsStream("ios-backup-sample.json")
                ?: throw AssertionError("Не удалось найти ios-backup-sample.json в ресурсах")
        val iosBackupItems: List<IosBackupItem> = json.decodeFromStream(inputStream)
        val originalIosItem = iosBackupItems[0] // День рождения

        // When - конвертируем iOS → BackupItem → Item
        val backupItem = originalIosItem.toBackupItem()
        assertNotNull(backupItem)
        val item = backupItem!!.toItem()
        assertNotNull(item)

        // Then - проверяем что данные сохранились
        val restoredItem = item!!
        assertEquals(originalIosItem.title, restoredItem.title)
        assertEquals(originalIosItem.details, restoredItem.details)
        assertEquals((originalIosItem.timestamp * 1000).toLong(), restoredItem.timestamp)

        // colorTag должен быть конвертирован из Base64 в Int
        assertNotNull(restoredItem.colorTag)
    }

    // MARK: - Edge Cases Tests

    @Test
    fun `edge case - empty details converts to empty string in item`() {
        // Given
        val iosItem =
            IosBackupItem(
                title = "Тест",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosItem.toBackupItem()
        val item = backupItem?.toItem()

        // Then
        assertNotNull(item)
        assertEquals("", item!!.details) // null конвертируется в пустую строку
    }

    @Test
    fun `edge case - empty title is preserved`() {
        // Given
        val iosItem =
            IosBackupItem(
                title = "",
                details = null,
                timestamp = 699417600.0,
                colorTag = null,
                displayOption = "day",
            )

        // When
        val backupItem = iosItem.toBackupItem()
        val item = backupItem?.toItem()

        // Then
        assertNotNull(item)
        assertEquals("", item!!.title)
    }

    // MARK: - NSKeyedArchiver Detection Tests

    @Test
    fun `nsKeyedArchiver detection - valid base64 is detected`() {
        // Given
        val greenBase64 =
            "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
                "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
                "Q29tcG9uZW50Q291bnRWVUlHcmVublZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
                "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
                "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
                "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
                "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="

        // When & Then
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(greenBase64))
    }

    @Test
    fun `nsKeyedArchiver detection - hex color is not detected as nsKeyedArchiver`() {
        // Given
        val hexColor = "#FF0000"

        // When & Then
        assertFalse(NsKeyedArchiverParser.isNsKeyedArchiver(hexColor))
    }

    @Test
    fun `nsKeyedArchiver detection - empty string is not detected`() {
        // Given
        val empty = ""

        // When & Then
        assertFalse(NsKeyedArchiverParser.isNsKeyedArchiver(empty))
    }
}
