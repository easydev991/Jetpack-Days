package com.dayscounter.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream

/**
 * Unit-тесты для ImportBackupUseCase.
 *
 * Тестирует импорт данных из JSON файла с поддержкой:
 * - Hex colorTag (Android формат)
 * - Base64 NSKeyedArchiver colorTag (iOS формат)
 * - Фильтрацию дубликатов
 */
@Suppress("TooManyFunctions")
class ImportBackupUseCaseTest {
    private val repository: ItemRepository = mockk()
    private val context: Context = mockk()
    private val logger: Logger = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk()

    private lateinit var useCase: ImportBackupUseCase

    @BeforeEach
    fun setup() {
        every { context.contentResolver } returns contentResolver
        useCase = ImportBackupUseCase(repository, context, logger)
    }

    // MARK: - Successful Import Tests

    @Test
    fun `invoke when validJsonWithHexColorTag then importsSuccessfully`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify { repository.insertItem(any()) }
        }

    @Test
    fun `invoke when validJsonWithIosColorTag then importsSuccessfully`() =
        runBlocking {
            // Given - реальный iOS Base64 colorTag для зеленого цвета
            val iosGreenBase64 =
                "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
                    "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
                    "Q29tcG9uZW50Q291bnRWVUlHcmVublZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
                    "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
                    "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
                    "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
                    "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"$iosGreenBase64","displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify { repository.insertItem(any()) }
        }

    @Test
    fun `invoke when multipleItems then importsAllSuccessfully`() =
        runBlocking {
            // Given
            val json =
                """
                [
                  {"title":"Событие 1","details":"Описание 1","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"day"},
                  {"title":"Событие 2","details":"Описание 2","timestamp":709417600000,"colorTag":"#00FF00","displayOption":"monthDay"},
                  {"title":"Событие 3","details":"Описание 3","timestamp":719417600000,"colorTag":"#0000FF","displayOption":"yearMonthDay"}
                ]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(3, result.getOrNull())
            coVerify(exactly = 3) { repository.insertItem(any()) }
        }

    @Test
    fun `invoke when nullColorTag then importsSuccessfully`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":null,"displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify { repository.insertItem(any()) }
        }

    // MARK: - Duplicate Detection Tests

    @Test
    fun `invoke when duplicateItemExists then skipsDuplicate`() =
        runBlocking {
            // Given
            val existingItem =
                Item(
                    id = 1L,
                    title = "Существующее",
                    details = "Описание",
                    timestamp = 699417600000,
                    colorTag = null,
                    displayOption = DisplayOption.DAY,
                )
            val json =
                """
                [{"title":"Существующее","details":"Описание","timestamp":699417600000,"colorTag":null,"displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(listOf(existingItem))

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
            coVerify(exactly = 0) { repository.insertItem(any()) }
        }

    @Test
    fun `invoke when mixedNewAndDuplicateItems then importsOnlyNew`() =
        runBlocking {
            // Given
            val existingItem =
                Item(
                    id = 1L,
                    title = "Существующее",
                    details = "Описание",
                    timestamp = 699417600000,
                    colorTag = null,
                    displayOption = DisplayOption.DAY,
                )
            val json =
                """
                [
                  {"title":"Существующее","details":"Описание","timestamp":699417600000,"colorTag":null,"displayOption":"day"},
                  {"title":"Новое","details":"Новое описание","timestamp":709417600000,"colorTag":"#FF0000","displayOption":"day"}
                ]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(listOf(existingItem))
            coEvery { repository.insertItem(any()) } returns 2L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
            coVerify(exactly = 1) { repository.insertItem(any()) }
        }

    // MARK: - Error Handling Tests

    @Test
    fun `invoke when fileNotFound then throwsBackupException`() =
        runBlocking {
            // Given
            val uri: Uri = mockk()

            every { contentResolver.openInputStream(uri) } returns null
            coEvery { repository.getAllItems() } returns flowOf(emptyList())

            // When & Then
            var exceptionThrown = false
            try {
                useCase.invoke(uri)
            } catch (e: BackupException) {
                exceptionThrown = true
                assertTrue(e.message?.contains("Не удалось открыть InputStream") == true)
            }
            assertTrue(exceptionThrown, "Ожидалось выбрасывание BackupException")
        }

    @Test
    fun `invoke when invalidJson then returnsFailure`() =
        runBlocking {
            // Given
            val invalidJson = "not a valid json"
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(invalidJson.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is BackupException)
        }

    @Test
    fun `invoke when invalidDisplayOption then skipsItem`() =
        runBlocking {
            // Given
            val json =
                """
                [
                  {"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"invalidOption"}
                ]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
        }

    // MARK: - Display Option Tests

    @Test
    fun `invoke when displayOptionDay then importsCorrectly`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
        }

    @Test
    fun `invoke when displayOptionMonthDay then importsCorrectly`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"monthDay"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
        }

    @Test
    fun `invoke when displayOptionYearMonthDay then importsCorrectly`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"yearMonthDay"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
        }

    // MARK: - Empty Data Tests

    @Test
    fun `invoke when emptyJsonArray then returnsZero`() =
        runBlocking {
            // Given
            val json = "[]"
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(0, result.getOrNull())
        }

    @Test
    fun `invoke when emptyDatabase then importsAllItems`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            val result = useCase.invoke(uri)

            // Then
            assertTrue(result.isSuccess)
            assertEquals(1, result.getOrNull())
        }

    // MARK: - Logging Tests

    @Test
    fun `invoke when successfulImport then logsSuccess`() =
        runBlocking {
            // Given
            val json =
                """
                [{"title":"Тест","details":"","timestamp":699417600000,"colorTag":"#FF0000","displayOption":"day"}]
                """.trimIndent()
            val uri: Uri = mockk()
            val inputStream = ByteArrayInputStream(json.toByteArray())

            every { contentResolver.openInputStream(uri) } returns inputStream
            coEvery { repository.getAllItems() } returns flowOf(emptyList())
            coEvery { repository.insertItem(any()) } returns 1L

            // When
            useCase.invoke(uri)

            // Then
            verify { logger.d(any(), any<String>()) }
        }
}
