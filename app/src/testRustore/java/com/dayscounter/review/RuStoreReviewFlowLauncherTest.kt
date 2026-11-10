package com.dayscounter.review

import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.ReviewFlowOutcome
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.rustore.sdk.core.exception.RuStoreException
import ru.rustore.sdk.core.exception.RuStoreNotInstalledException
import ru.rustore.sdk.core.exception.RuStoreOutdatedException
import ru.rustore.sdk.core.exception.RuStoreUserUnauthorizedException
import ru.rustore.sdk.core.tasks.Task
import ru.rustore.sdk.review.RuStoreReviewManager
import ru.rustore.sdk.review.errors.RuStoreRequestLimitReached
import ru.rustore.sdk.review.errors.RuStoreReviewExists
import ru.rustore.sdk.review.model.ReviewInfo

/**
 * Тесты SDK-лаунчера review-флоу flavor `rustore` (задача 4.1).
 *
 * Менеджер RuStore Review SDK подменяется MockK-обёрткой; проверяются порядок
 * фаз (requestReviewFlow → launchReviewFlow(ReviewInfo)), успешный исход,
 * маппинг каждой ошибки иерархии `RuStoreException` в доменный
 * [ReviewException], а также runtime-сбои SDK вне этой иерархии
 * (`IllegalStateException` → [ReviewException.Unknown]) и проброс
 * [CancellationException] (кооперативная отмена корутины).
 */
class RuStoreReviewFlowLauncherTest {
    private val manager: RuStoreReviewManager = mockk()
    private val reviewInfo: ReviewInfo = mockk()

    private fun requestReviewFlowSucceeds() {
        val task: Task<ReviewInfo> = mockk()
        every { task.await() } returns reviewInfo
        every { manager.requestReviewFlow() } returns task
    }

    private fun launcher() = RuStoreReviewFlowLauncher(manager)

    private fun launchReviewFlowSucceeds() {
        val task: Task<Unit> = mockk()
        every { task.await() } returns Unit
        every { manager.launchReviewFlow(reviewInfo) } returns task
    }

    @Test
    fun launch_when_both_phases_succeed_then_returns_shown_in_order() =
        runTest {
            // Given
            requestReviewFlowSucceeds()
            launchReviewFlowSucceeds()

            // When
            val outcome = launcher().launch()

            // Then
            assertEquals(ReviewFlowOutcome.Shown, outcome, "Успешный флоу должен вернуть Shown")
            verifyOrder {
                manager.requestReviewFlow()
                manager.launchReviewFlow(reviewInfo)
            }
        }

    @Test
    fun launch_when_first_phase_fails_then_skips_second_phase_and_maps_to_error() =
        runTest {
            // Given
            val task: Task<ReviewInfo> = mockk()
            every { task.await() } throws RuStoreRequestLimitReached()
            every { manager.requestReviewFlow() } returns task

            // When
            val outcome = launcher().launch()

            // Then
            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            assertInstanceOf(
                ReviewException.RequestLimitReached::class.java,
                error,
                "Ошибка лимита должна маппиться в RequestLimitReached"
            )
            coVerify(exactly = 0) { manager.launchReviewFlow(any()) }
        }

    @Test
    fun launch_when_second_phase_fails_then_maps_to_error() =
        runTest {
            // Given
            requestReviewFlowSucceeds()
            val task: Task<Unit> = mockk()
            every { task.await() } throws RuStoreReviewExists()
            every { manager.launchReviewFlow(reviewInfo) } returns task

            // When
            val outcome = launcher().launch()

            // Then
            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            assertInstanceOf(
                ReviewException.ReviewAlreadyExists::class.java,
                error,
                "Ошибка второй фазы должна маппиться в ReviewAlreadyExists"
            )
        }

    @Test
    fun launch_when_not_installed_exception_then_maps_to_not_installed() =
        assertSdkMapped(RuStoreNotInstalledException(), ReviewException.NotInstalled::class)

    @Test
    fun launch_when_outdated_exception_then_maps_to_outdated() =
        assertSdkMapped(RuStoreOutdatedException(), ReviewException.Outdated::class)

    @Test
    fun launch_when_user_unauthorized_exception_then_maps_to_not_authorized() =
        assertSdkMapped(RuStoreUserUnauthorizedException(), ReviewException.NotAuthorized::class)

    @Test
    fun launch_when_request_limit_reached_then_maps_to_request_limit_reached() =
        assertSdkMapped(RuStoreRequestLimitReached(), ReviewException.RequestLimitReached::class)

    @Test
    fun launch_when_review_exists_then_maps_to_review_already_exists() =
        assertSdkMapped(RuStoreReviewExists(), ReviewException.ReviewAlreadyExists::class)

    @Test
    fun launch_when_base_ru_store_exception_then_maps_to_unknown_with_cause() =
        runTest {
            // Given
            val cause = RuStoreException("сбой SDK")
            val task: Task<ReviewInfo> = mockk()
            every { task.await() } throws cause
            every { manager.requestReviewFlow() } returns task

            // When
            val outcome = launcher().launch()

            // Then
            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            val unknown =
                assertInstanceOf(
                    ReviewException.Unknown::class.java,
                    error,
                    "Базовая ошибка SDK должна маппиться в Unknown"
                )
            assertSame(cause, unknown.cause, "Причина ошибки SDK должна сохраниться в cause")
            assertTrue(unknown.message.isNotEmpty(), "Сообщение Unknown не должно быть пустым")
        }

    @Test
    fun launch_when_runtime_exception_outside_hierarchy_then_maps_to_unknown_with_cause() =
        runTest {
            // Given
            val cause = IllegalStateException("binding отвалился")
            val task: Task<ReviewInfo> = mockk()
            every { task.await() } throws cause
            every { manager.requestReviewFlow() } returns task

            // When
            val outcome = launcher().launch()

            // Then
            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            val unknown =
                assertInstanceOf(
                    ReviewException.Unknown::class.java,
                    error,
                    "Runtime-сбой вне иерархии SDK должен попасть в Unknown"
                )
            assertSame(cause, unknown.cause, "Причина runtime-сбоя должна сохраниться в cause")
            assertTrue(unknown.message.isNotEmpty(), "Сообщение Unknown не должно быть пустым")
        }

    @Test
    fun launch_when_coroutine_cancelled_then_cancellation_exception_propagates() =
        runTest {
            // Given
            val task: Task<ReviewInfo> = mockk()
            every { task.await() } throws CancellationException()
            every { manager.requestReviewFlow() } returns task

            // When
            val thrown = runCatching { launcher().launch() }.exceptionOrNull()

            // Then
            assertInstanceOf(
                CancellationException::class.java,
                thrown,
                "Отмена корутины должна пробрасываться, а не маппиться в Error"
            )
        }

    private fun assertSdkMapped(
        sdkError: RuStoreException,
        expected: kotlin.reflect.KClass<out ReviewException>
    ) = runTest {
        // Given
        val task: Task<ReviewInfo> = mockk()
        every { task.await() } throws sdkError
        every { manager.requestReviewFlow() } returns task

        // When
        val outcome = launcher().launch()

        // Then
        val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
        assertEquals(expected, error::class)
    }
}
