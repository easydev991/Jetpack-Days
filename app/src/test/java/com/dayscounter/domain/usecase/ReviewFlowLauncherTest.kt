package com.dayscounter.domain.usecase

import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.ReviewFlowOutcome
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit-тесты контракта [ReviewFlowLauncher] на fake-реализациях (фейки — внутри файла).
 *
 * Проверяет семантику контракта: успешный исход (форма показана), исход с каждой
 * доменной ошибкой [ReviewException] и различимость вариантов классификации.
 * Маппинг ошибок SDK в доменные варианты — ответственность flavor-реализации
 * лаунчера и в этих тестах не участвует.
 */
class ReviewFlowLauncherTest {
    /**
     * Fake лаунчера: реализует контракт явно, отдаёт настраиваемый исход
     * и считает вызовы — позволяет проверить, что каждый запуск завершается
     * своим исходом и контракт не скрывает повторные вызовы.
     */
    private class FakeReviewFlowLauncher : ReviewFlowLauncher {
        var nextOutcome: ReviewFlowOutcome = ReviewFlowOutcome.Shown
        var invocationCount: Int = 0
            private set

        override suspend fun launch(): ReviewFlowOutcome {
            invocationCount++
            return nextOutcome
        }
    }

    @Test
    fun launch_on_success_then_returns_shown_outcome() =
        runTest {
            // Given: лаунчер через SAM-конструктор — флоу завершился показом формы
            val launcher = ReviewFlowLauncher { ReviewFlowOutcome.Shown }

            // When
            val outcome = launcher.launch()

            // Then
            assertTrue(outcome is ReviewFlowOutcome.Shown, "Успешный флоу должен давать исход Shown")
        }

    @Test
    fun launch_on_not_installed_error_then_returns_error_with_not_installed() =
        runTest {
            // Given
            val launcher =
                FakeReviewFlowLauncher().apply {
                    nextOutcome = ReviewFlowOutcome.Error(ReviewException.NotInstalled("RuStore не установлен"))
                }

            // When
            val outcome = launcher.launch()

            // Then
            val errorOutcome = checkNotNull(outcome as? ReviewFlowOutcome.Error) { "Должен быть исход Error" }
            assertTrue(
                errorOutcome.error is ReviewException.NotInstalled,
                "Тип ошибки должен быть NotInstalled"
            )
            assertEquals("RuStore не установлен", errorOutcome.error.message, "Сообщение должно сохраняться")
        }

    @Test
    fun launch_on_outdated_error_then_returns_error_with_outdated() =
        runTest {
            // Given
            val launcher =
                FakeReviewFlowLauncher().apply {
                    nextOutcome = ReviewFlowOutcome.Error(ReviewException.Outdated("RuStore устарел"))
                }

            // When
            val outcome = launcher.launch()

            // Then
            val errorOutcome = checkNotNull(outcome as? ReviewFlowOutcome.Error) { "Должен быть исход Error" }
            assertTrue(errorOutcome.error is ReviewException.Outdated, "Тип ошибки должен быть Outdated")
            assertEquals("RuStore устарел", errorOutcome.error.message, "Сообщение должно сохраняться")
        }

    @Test
    fun launch_on_not_authorized_error_then_returns_error_with_not_authorized() =
        runTest {
            // Given
            val launcher =
                FakeReviewFlowLauncher().apply {
                    nextOutcome = ReviewFlowOutcome.Error(ReviewException.NotAuthorized("Пользователь не авторизован"))
                }

            // When
            val outcome = launcher.launch()

            // Then
            val errorOutcome = checkNotNull(outcome as? ReviewFlowOutcome.Error) { "Должен быть исход Error" }
            assertTrue(errorOutcome.error is ReviewException.NotAuthorized, "Тип ошибки должен быть NotAuthorized")
            assertEquals(
                "Пользователь не авторизован",
                errorOutcome.error.message,
                "Сообщение должно сохраняться"
            )
        }

    @Test
    fun launch_on_request_limit_reached_error_then_returns_error_with_request_limit_reached() =
        runTest {
            // Given
            val launcher =
                FakeReviewFlowLauncher().apply {
                    nextOutcome = ReviewFlowOutcome.Error(ReviewException.RequestLimitReached("Лимит исчерпан"))
                }

            // When
            val outcome = launcher.launch()

            // Then
            val errorOutcome = checkNotNull(outcome as? ReviewFlowOutcome.Error) { "Должен быть исход Error" }
            assertTrue(
                errorOutcome.error is ReviewException.RequestLimitReached,
                "Тип ошибки должен быть RequestLimitReached"
            )
            assertEquals("Лимит исчерпан", errorOutcome.error.message, "Сообщение должно сохраняться")
        }

    @Test
    fun launch_on_review_already_exists_error_then_returns_error_with_review_already_exists() =
        runTest {
            // Given
            val launcher =
                FakeReviewFlowLauncher().apply {
                    nextOutcome = ReviewFlowOutcome.Error(ReviewException.ReviewAlreadyExists("Отзыв уже есть"))
                }

            // When
            val outcome = launcher.launch()

            // Then
            val errorOutcome = checkNotNull(outcome as? ReviewFlowOutcome.Error) { "Должен быть исход Error" }
            assertTrue(
                errorOutcome.error is ReviewException.ReviewAlreadyExists,
                "Тип ошибки должен быть ReviewAlreadyExists"
            )
            assertEquals("Отзыв уже есть", errorOutcome.error.message, "Сообщение должно сохраняться")
        }

    @Test
    fun launch_on_unknown_error_then_returns_error_with_unknown() =
        runTest {
            // Given
            val launcher =
                FakeReviewFlowLauncher().apply {
                    nextOutcome = ReviewFlowOutcome.Error(ReviewException.Unknown("Прочая ошибка SDK"))
                }

            // When
            val outcome = launcher.launch()

            // Then
            val errorOutcome = checkNotNull(outcome as? ReviewFlowOutcome.Error) { "Должен быть исход Error" }
            assertTrue(errorOutcome.error is ReviewException.Unknown, "Тип ошибки должен быть Unknown")
            assertEquals("Прочая ошибка SDK", errorOutcome.error.message, "Сообщение должно сохраняться")
        }

    @Test
    fun launch_when_invoked_twice_then_each_call_returns_its_own_outcome() =
        runTest {
            // Given: fake настроен сначала на показ, затем на ошибку лимита
            val launcher = FakeReviewFlowLauncher()
            launcher.nextOutcome = ReviewFlowOutcome.Shown

            // When
            val first = launcher.launch()
            launcher.nextOutcome = ReviewFlowOutcome.Error(ReviewException.RequestLimitReached("Лимит исчерпан"))
            val second = launcher.launch()

            // Then
            assertTrue(first is ReviewFlowOutcome.Shown, "Первый запуск должен вернуть Shown")
            assertTrue(second is ReviewFlowOutcome.Error, "Второй запуск должен вернуть свой исход")
            assertEquals(2, launcher.invocationCount, "Каждый запуск должен учитываться отдельно")
        }

    @Test
    fun review_exception_when_variants_share_message_then_remain_distinguishable() {
        // Given: все шесть вариантов доменной классификации с одинаковым сообщением
        val message = "ошибка"
        val variants: List<ReviewException> =
            listOf(
                ReviewException.NotInstalled(message),
                ReviewException.Outdated(message),
                ReviewException.NotAuthorized(message),
                ReviewException.RequestLimitReached(message),
                ReviewException.ReviewAlreadyExists(message),
                ReviewException.Unknown(message)
            )

        // When: собираем классы вариантов
        val distinctClasses = variants.map { it::class }.toSet()

        // Then: классы уникальны, каждый хранит сообщение, варианты попарно не равны
        assertEquals(variants.size, distinctClasses.size, "Классы вариантов должны быть различны")
        assertTrue(variants.all { it.message == message }, "Каждый вариант должен хранить сообщение")
        variants.forEachIndexed { i, first ->
            variants.forEachIndexed { j, second ->
                if (i != j) {
                    assertNotEquals(first, second, "Разные варианты классификации не должны быть равны")
                }
            }
        }
    }

    @Test
    fun review_exception_unknown_when_cause_given_then_preserves_it() {
        // Given
        val cause = IllegalStateException("Исходный сбой")

        // When
        val exception = ReviewException.Unknown("Прочая ошибка", cause)

        // Then
        assertEquals(cause, exception.cause, "Причина должна сохраняться в слоте cause")
    }
}
