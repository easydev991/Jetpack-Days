package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

/**
 * Unit-тесты для ImportBackupUseCase.
 * Проверяет корректность импорта различных форматов резервных копий.
 */
class ImportBackupUseCaseTest {
    private val repository: ItemRepository = mockk()
    private val context: Context = mockk()
    private val logger: Logger = mockk(relaxed = true)
    private val contentResolver: android.content.ContentResolver = mockk()

    private lateinit var useCase: ImportBackupUseCase

    @BeforeEach
    fun setup() {
        every { context.contentResolver } returns contentResolver
        useCase = ImportBackupUseCase(repository, context, logger)
    }

    // MARK: - Импорт Android-бекапа с полем format: "android"

    @Test
    fun `invoke whenAndroidFormatWithWrapper_thenImportsCorrectly`() =
        runBlocking {
            // Given
            val json =
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
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow())
            coVerify { repository.insertItem(any()) }
        }

    // MARK: - Импорт iOS-бекапа с полем format: "ios"
    // Примечание: timestamp в iOS формате - секунды с 2001-01-01 (Double),
    // должен конвертироваться в миллисекунды с 1970-01-01

    @Test
    fun `invoke whenIosFormatWithWrapper_thenConvertsTimestampAndImports`() =
        runBlocking {
            // Given
            // iOS timestamp: -1756176000 сек с 2001-01-01 (как Double)
            // Ожидаемый Android timestamp: (-1756176000 + 978307200) * 1000 = -777870400000
            val json =
                """
                {
                    "format": "ios",
                    "items": [
                        {
                            "title": "День победы",
                            "details": "9 мая 1945 года",
                            "timestamp": -1756176000.0,
                            "colorTag": null,
                            "displayOption": "yearMonthDay"
                        }
                    ]
                }
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow())
            coVerify { repository.insertItem(any()) }
        }

    @Test
    fun `invoke whenIosFormatWithIntegerTimestamp_thenConvertsAndImports`() =
        runBlocking {
            // Given
            // iOS timestamp: 631152000 сек с 2001-01-01 (как целое число)
            // Это 2020-01-01 в iOS формате
            val json =
                """
                {
                    "format": "ios",
                    "items": [
                        {
                            "title": "Без цвета",
                            "details": "Событие без цветового тега",
                            "timestamp": 631152000,
                            "colorTag": null,
                            "displayOption": "day"
                        }
                    ]
                }
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow())
            coVerify { repository.insertItem(any()) }
        }

    // MARK: - Импорт старого Android-бекапа без поля format (массив напрямую)

    @Test
    fun `invoke whenOldAndroidFormatArray_thenFallsBackAndImports`() =
        runBlocking {
            // Given
            val json =
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
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow())
            coVerify(exactly = 2) { repository.insertItem(any()) }
        }

    // MARK: - Импорт бекапа без формата в wrapper (format == null)

    @Test
    fun `invoke whenWrapperWithNullFormat_thenImportsAsAndroid`() =
        runBlocking {
            // Given
            val json =
                """
                {
                    "items": [
                        {
                            "title": "No Format Event",
                            "details": "No format specified",
                            "timestamp": 999999999000,
                            "colorTag": "#00FF00",
                            "displayOption": "monthDay"
                        }
                    ]
                }
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrThrow())
            coVerify { repository.insertItem(any()) }
        }

    // MARK: - Дубликаты

    @Test
    fun `invoke whenDuplicateItems_thenSkipsDuplicates`() =
        runBlocking {
            // Given
            val json =
                """
                {
                    "format": "android",
                    "items": [
                        {
                            "title": "Existing Event",
                            "details": "Existing Details",
                            "timestamp": 1234567890000,
                            "colorTag": "#FF0000",
                            "displayOption": "day"
                        }
                    ]
                }
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            val existingItem =
                com.dayscounter.domain.model.Item(
                    id = 1L,
                    title = "Existing Event",
                    details = "Existing Details",
                    timestamp = 1234567890000L,
                    colorTag = 0xFFFF0000.toInt(),
                    displayOption = com.dayscounter.domain.model.DisplayOption.DAY
                )

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(listOf(existingItem))
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrThrow()) // 0 потому что дубликат
            coVerify(exactly = 0) { repository.insertItem(any()) }
        }

    // MARK: - Несколько элементов

    @Test
    fun `invoke whenMultipleItems_thenImportsAllNonDuplicates`() =
        runBlocking {
            // Given
            val json =
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
                            "colorTag": "#00FF00",
                            "displayOption": "monthDay"
                        },
                        {
                            "title": "Event 3",
                            "details": "Details 3",
                            "timestamp": 3000000000000,
                            "colorTag": null,
                            "displayOption": "yearMonthDay"
                        }
                    ]
                }
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrThrow())
            coVerify(exactly = 3) { repository.insertItem(any()) }
        }

    // MARK: - Элементы с null values

    @Test
    fun `invoke whenItemsWithNullValues_thenImportsCorrectly`() =
        runBlocking {
            // Given
            val json =
                """
                {
                    "format": "android",
                    "items": [
                        {
                            "title": "No Color",
                            "details": "Has details",
                            "timestamp": 1000000000000,
                            "colorTag": null,
                            "displayOption": "day"
                        },
                        {
                            "title": "No Details",
                            "details": null,
                            "timestamp": 2000000000000,
                            "colorTag": "#FF0000",
                            "displayOption": "day"
                        }
                    ]
                }
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow())
            coVerify(exactly = 2) { repository.insertItem(any()) }
        }
}
