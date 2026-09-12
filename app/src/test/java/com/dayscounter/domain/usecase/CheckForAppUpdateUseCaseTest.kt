package com.dayscounter.domain.usecase

import com.dayscounter.domain.usecase.http.HttpRequestExecutor
import com.dayscounter.domain.usecase.http.HttpResponse
import com.dayscounter.util.NoOpLogger
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для [CheckForAppUpdateUseCase].
 * Проверяет маппинг ответа GitHub Releases API и семантическое сравнение версий.
 */
class CheckForAppUpdateUseCaseTest {
    private val executor: HttpRequestExecutor = mockk()
    private lateinit var useCase: CheckForAppUpdateUseCase

    @BeforeEach
    fun setup() {
        // currentVersion задаётся явно, чтобы тест не зависел от BuildConfig
        useCase =
            CheckForAppUpdateUseCase(
                executor = executor,
                currentVersion = "1.2.0",
                logger = NoOpLogger()
            )
    }

    @Test
    fun invoke_on_success_then_returns_update_info() =
        runTest {
            // Given
            val body =
                """
                {
                    "tag_name": "v1.3.0",
                    "html_url": "https://github.com/easydev991/Jetpack-Days/releases/tag/v1.3.0",
                    "body": "Release notes"
                }
                """.trimIndent()
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            val info = result.getOrNull()
            assertTrue(result.isSuccess, "Ожидается успешный результат")
            assertEquals("1.3.0", info?.version, "Версия должна быть без префикса v")
            assertEquals(
                "https://github.com/easydev991/Jetpack-Days/releases/tag/v1.3.0",
                info?.releaseUrl,
                "URL релиза должен браться из html_url"
            )
            assertEquals("Release notes", info?.notes, "Notes должен браться из body")
        }

    @Test
    fun invoke_on_network_error_then_returns_failure_with_app_update_exception() =
        runTest {
            // Given
            every { executor.execute(any()) } throws java.io.IOException("Нет сети")

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isFailure, "Сетевая ошибка должна дать Result.failure")
            assertTrue(
                result.exceptionOrNull() is AppUpdateException,
                "Исключение должно быть AppUpdateException"
            )
        }

    @Test
    fun invoke_on_malformed_json_then_returns_failure_with_app_update_exception() =
        runTest {
            // Given
            every { executor.execute(any()) } returns HttpResponse(200, "не JSON")

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isFailure, "Malformed JSON должен дать Result.failure")
            assertTrue(
                result.exceptionOrNull() is AppUpdateException,
                "Исключение должно быть AppUpdateException"
            )
        }

    @Test
    fun invoke_when_tag_with_v_prefix_and_current_older_then_returns_update_info() =
        runTest {
            // Given: tag v1.2.3 новее текущей 1.2.0
            val body = """{"tag_name": "v1.2.3", "html_url": "https://example.com/r"}"""
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            assertEquals("1.2.3", result.getOrNull()?.version, "Префикс v должен быть удалён")
        }

    @Test
    fun invoke_when_tag_without_v_prefix_then_returns_update_info() =
        runTest {
            // Given: tag без префикса v, новее текущей 1.2.0
            val body = """{"tag_name": "1.2.3", "html_url": "https://example.com/r"}"""
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            assertEquals("1.2.3", result.getOrNull()?.version, "Tag без v должен парситься")
        }

    @Test
    fun invoke_when_versions_equal_then_returns_null() =
        runTest {
            // Given: tag v1.2.3 равен текущей 1.2.3
            useCase = CheckForAppUpdateUseCase(executor, currentVersion = "1.2.3", logger = NoOpLogger())
            val body = """{"tag_name": "v1.2.3", "html_url": "https://example.com/r"}"""
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isSuccess, "Равные версии — это успех")
            assertNull(result.getOrNull(), "Обновление не требуется — UpdateInfo должен быть null")
        }

    @Test
    fun invoke_when_current_version_newer_then_returns_null() =
        runTest {
            // Given: текущая 2.0.0 новее тега v1.5.0
            useCase = CheckForAppUpdateUseCase(executor, currentVersion = "2.0.0", logger = NoOpLogger())
            val body = """{"tag_name": "v1.5.0", "html_url": "https://example.com/r"}"""
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isSuccess, "Текущая новее — это успех")
            assertNull(result.getOrNull(), "Обновление не требуется — UpdateInfo должен быть null")
        }

    @Test
    fun invoke_on_non_200_status_then_returns_failure_with_app_update_exception() =
        runTest {
            // Given
            every { executor.execute(any()) } returns HttpResponse(403, "")

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isFailure, "HTTP 403 должен дать Result.failure")
            assertTrue(
                result.exceptionOrNull() is AppUpdateException,
                "Исключение должно быть AppUpdateException"
            )
        }

    @Test
    fun invoke_when_tag_name_missing_then_returns_failure_with_app_update_exception() =
        runTest {
            // Given
            val body = """{"html_url": "https://example.com/r"}"""
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isFailure, "Отсутствие tag_name должно дать Result.failure")
            assertTrue(
                result.exceptionOrNull() is AppUpdateException,
                "Исключение должно быть AppUpdateException"
            )
        }

    @Test
    fun invoke_when_html_url_missing_then_returns_failure_with_app_update_exception() =
        runTest {
            // Given
            val body = """{"tag_name": "v9.9.9"}"""
            every { executor.execute(any()) } returns HttpResponse(200, body)

            // When
            val result = useCase.invoke()

            // Then
            assertTrue(result.isFailure, "Отсутствие html_url должно дать Result.failure")
            assertTrue(
                result.exceptionOrNull() is AppUpdateException,
                "Исключение должно быть AppUpdateException"
            )
        }
}
