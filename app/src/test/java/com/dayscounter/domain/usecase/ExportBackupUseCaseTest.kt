package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

/**
 * Unit-тесты для ExportBackupUseCase.
 * Проверяет корректность экспорта данных в резервную копию.
 */
class ExportBackupUseCaseTest {
    private val repository: ItemRepository = mockk()
    private val context: Context = mockk()
    private val logger: Logger = mockk(relaxed = true)
    private val contentResolver: android.content.ContentResolver = mockk()

    private lateinit var useCase: ExportBackupUseCase
    private val outputStream = ByteArrayOutputStream()
    private val jsonParser =
        Json {
            ignoreUnknownKeys = true
        }

    @BeforeEach
    fun setup() {
        every { context.contentResolver } returns contentResolver
        useCase = ExportBackupUseCase(repository, context, logger)
        outputStream.reset()
    }

    // MARK: - Экспорт содержит format: "android"

    @Test
    fun `invoke whenExporting_thenContainsFormatAndroid`() =
        runBlocking {
            // Given
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Test Event",
                        details = "Test Details",
                        timestamp = 1234567890000L,
                        colorTag = 0xFFFF5722.toInt(),
                        displayOption = DisplayOption.DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)
            assertEquals(BackupFormat.ANDROID, wrapper.format)
        }

    // MARK: - Структура JSON соответствует BackupWrapper

    @Test
    fun `invoke whenExporting_thenStructureMatchesBackupWrapper`() =
        runBlocking {
            // Given
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Event 1",
                        details = "Details 1",
                        timestamp = 1000000000000L,
                        colorTag = 0xFFFF0000.toInt(),
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Event 2",
                        details = "Details 2",
                        timestamp = 2000000000000L,
                        colorTag = 0xFF00FF00.toInt(),
                        displayOption = DisplayOption.MONTH_DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(2, result.getOrThrow())

            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)

            assertEquals(BackupFormat.ANDROID, wrapper.format)
            assertEquals(2, wrapper.items.size)
        }

    // MARK: - Timestamp экспортируется в миллисекундах с 1970-01-01

    @Test
    fun `invoke whenExporting_thenTimestampInMillisecondsSince1970`() =
        runBlocking {
            // Given
            val testTimestamp = 1609459200000L // 2021-01-01 00:00:00 UTC
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "New Year 2021",
                        details = "",
                        timestamp = testTimestamp,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)
            assertEquals(testTimestamp, wrapper.items[0].timestamp)
        }

    // MARK: - ColorTag экспортируется в hex формате (#RRGGBB)

    @Test
    fun `invoke whenExporting_thenColorTagInHexFormat`() =
        runBlocking {
            // Given
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Red Event",
                        details = "Red color",
                        timestamp = 1000000000000L,
                        colorTag = 0xFFFF0000.toInt(), // ARGB красный
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "Green Event",
                        details = "Green color",
                        timestamp = 2000000000000L,
                        colorTag = 0xFF4CAF50.toInt(), // ARGB зелёный (Material Design)
                        displayOption = DisplayOption.MONTH_DAY
                    ),
                    Item(
                        id = 3L,
                        title = "Blue Event",
                        details = "Blue color",
                        timestamp = 3000000000000L,
                        colorTag = 0xFF2196F3.toInt(), // ARGB синий (Material Design)
                        displayOption = DisplayOption.YEAR_MONTH_DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)

            // Проверяем hex формат цветов
            assertEquals("#FF0000", wrapper.items.find { it.title == "Red Event" }?.colorTag)
            assertEquals("#4CAF50", wrapper.items.find { it.title == "Green Event" }?.colorTag)
            assertEquals("#2196F3", wrapper.items.find { it.title == "Blue Event" }?.colorTag)
        }

    // MARK: - Null colorTag обрабатывается корректно

    @Test
    fun `invoke whenNullColorTag_thenExportsNullInJson`() =
        runBlocking {
            // Given
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "No Color Event",
                        details = "No color",
                        timestamp = 1000000000000L,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)
            assertNull(wrapper.items[0].colorTag)
        }

    // MARK: - Пустой список

    @Test
    fun `invoke whenEmptyList_thenExportsEmptyItemsArray`() =
        runBlocking {
            // Given
            val items = emptyList<Item>()
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrThrow())

            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)

            assertEquals(BackupFormat.ANDROID, wrapper.format)
            assertTrue(wrapper.items.isEmpty())
        }

    // MARK: - displayOption конвертируется корректно

    @Test
    fun `invoke whenDifferentDisplayOptions_thenExportsCorrectStrings`() =
        runBlocking {
            // Given
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Day Event",
                        details = "",
                        timestamp = 1000000000000L,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    ),
                    Item(
                        id = 2L,
                        title = "MonthDay Event",
                        details = "",
                        timestamp = 2000000000000L,
                        colorTag = null,
                        displayOption = DisplayOption.MONTH_DAY
                    ),
                    Item(
                        id = 3L,
                        title = "YearMonthDay Event",
                        details = "",
                        timestamp = 3000000000000L,
                        colorTag = null,
                        displayOption = DisplayOption.YEAR_MONTH_DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)

            assertEquals("day", wrapper.items.find { it.title == "Day Event" }?.displayOption)
            assertEquals(
                "monthDay",
                wrapper.items.find { it.title == "MonthDay Event" }?.displayOption
            )
            assertEquals(
                "yearMonthDay",
                wrapper.items.find { it.title == "YearMonthDay Event" }?.displayOption
            )
        }

    // MARK: - details null и empty обрабатываются корректно

    @Test
    fun `invoke whenEmptyDetails_thenExportsEmptyStringInJson`() =
        runBlocking {
            // Given
            val items =
                listOf(
                    Item(
                        id = 1L,
                        title = "Empty Details Event",
                        details = "", // empty string
                        timestamp = 1000000000000L,
                        colorTag = null,
                        displayOption = DisplayOption.DAY
                    )
                )
            val uri: Uri = mockk()

            coEvery { repository.getAllItems() } returns flowOf(items)
            every { contentResolver.openOutputStream(uri) } returns outputStream

            // When
            val result = useCase(uri)

            // Then
            assertTrue(result.isSuccess)
            val jsonOutput = outputStream.toString("UTF-8")
            val wrapper = jsonParser.decodeFromString<BackupWrapper>(jsonOutput)

            // Item.details всегда String (non-null), экспортируется как ""
            assertNotNull(wrapper.items[0].details)
        }
}
