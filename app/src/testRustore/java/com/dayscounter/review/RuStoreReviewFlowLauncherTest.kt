package com.dayscounter.review

import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.ReviewFlowOutcome
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
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
 * фаз (requestReviewFlow → launchReviewFlow(ReviewInfo)), успешный исход и
 * маппинг каждой ошибки иерархии `RuStoreException` в доменный
 * [ReviewException].
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
    fun `успешный флоу возвращает Shown и проходит обе фазы по порядку`() =
        runTest {
            requestReviewFlowSucceeds()
            launchReviewFlowSucceeds()

            val outcome = launcher().launch()

            assertEquals(ReviewFlowOutcome.Shown, outcome)
            verifyOrder {
                manager.requestReviewFlow()
                manager.launchReviewFlow(reviewInfo)
            }
        }

    @Test
    fun `ошибка первой фазы не запускает вторую и маппится в Error`() =
        runTest {
            val task: Task<ReviewInfo> = mockk()
            every { task.await() } throws RuStoreRequestLimitReached()
            every { manager.requestReviewFlow() } returns task

            val outcome = launcher().launch()

            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            assertInstanceOf(ReviewException.RequestLimitReached::class.java, error)
            coVerify(exactly = 0) { manager.launchReviewFlow(any()) }
        }

    @Test
    fun `ошибка второй фазы маппится в Error`() =
        runTest {
            requestReviewFlowSucceeds()
            val task: Task<Unit> = mockk()
            every { task.await() } throws RuStoreReviewExists()
            every { manager.launchReviewFlow(reviewInfo) } returns task

            val outcome = launcher().launch()

            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            assertInstanceOf(ReviewException.ReviewAlreadyExists::class.java, error)
        }

    @Test
    fun `RuStoreNotInstalledException маппится в NotInstalled`() =
        assertSdkMapped(RuStoreNotInstalledException(), ReviewException.NotInstalled::class)

    @Test
    fun `RuStoreOutdatedException маппится в Outdated`() = assertSdkMapped(RuStoreOutdatedException(), ReviewException.Outdated::class)

    @Test
    fun `RuStoreUserUnauthorizedException маппится в NotAuthorized`() =
        assertSdkMapped(RuStoreUserUnauthorizedException(), ReviewException.NotAuthorized::class)

    @Test
    fun `RuStoreRequestLimitReached маппится в RequestLimitReached`() =
        assertSdkMapped(RuStoreRequestLimitReached(), ReviewException.RequestLimitReached::class)

    @Test
    fun `RuStoreReviewExists маппится в ReviewAlreadyExists`() =
        assertSdkMapped(RuStoreReviewExists(), ReviewException.ReviewAlreadyExists::class)

    @Test
    fun `базовый RuStoreException маппится в Unknown с cause`() =
        runTest {
            val cause = RuStoreException("сбой SDK")
            val task: Task<ReviewInfo> = mockk()
            every { task.await() } throws cause
            every { manager.requestReviewFlow() } returns task

            val outcome = launcher().launch()

            val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
            val unknown = assertInstanceOf(ReviewException.Unknown::class.java, error)
            assertSame(cause, unknown.cause)
            assertTrue(unknown.message.isNotEmpty())
        }

    private fun assertSdkMapped(
        sdkError: RuStoreException,
        expected: kotlin.reflect.KClass<out ReviewException>
    ) = runTest {
        val task: Task<ReviewInfo> = mockk()
        every { task.await() } throws sdkError
        every { manager.requestReviewFlow() } returns task

        val outcome = launcher().launch()

        val error = assertInstanceOf(ReviewFlowOutcome.Error::class.java, outcome).error
        assertEquals(expected, error::class)
    }
}
