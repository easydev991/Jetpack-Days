package com.dayscounter.review

import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.ReviewFlowOutcome
import com.dayscounter.domain.usecase.ReviewFlowLauncher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.rustore.sdk.core.exception.RuStoreException
import ru.rustore.sdk.core.exception.RuStoreNotInstalledException
import ru.rustore.sdk.core.exception.RuStoreOutdatedException
import ru.rustore.sdk.core.exception.RuStoreUserUnauthorizedException
import ru.rustore.sdk.review.RuStoreReviewManager
import ru.rustore.sdk.review.errors.RuStoreRequestLimitReached
import ru.rustore.sdk.review.errors.RuStoreReviewExists

/**
 * SDK-лаунчер review-флоу для flavor `rustore` — реализация доменного контракта
 * [ReviewFlowLauncher] поверх RuStore Review SDK.
 *
 * Флоу двухфазный: подготовка [RuStoreReviewManager.requestReviewFlow] (получение
 * [ru.rustore.sdk.review.model.ReviewInfo]) и показ
 * [RuStoreReviewManager.launchReviewFlow]. Любая ошибка иерархии [RuStoreException]
 * маппится в доменный [ReviewException] и возвращается значением
 * [ReviewFlowOutcome.Error], а не выбросом; runtime-сбои SDK вне этой иерархии
 * (например, [IllegalStateException] при отвале binding) — тоже значение исхода
 * ([ReviewException.Unknown]): фича «тихая», краш ей противоречит. Только
 * [CancellationException] пробрасывается дальше — кооперативная отмена корутины.
 */
class RuStoreReviewFlowLauncher(
    private val manager: RuStoreReviewManager
) : ReviewFlowLauncher {
    /**
     * Запускает двухфазный флоу и дожидается его завершения.
     *
     * Ошибка на любой фазе прерывает флоу (вторая фаза не выполняется) и
     * возвращается как [ReviewFlowOutcome.Error] — включая runtime-сбои SDK
     * вне иерархии [RuStoreException]; только [CancellationException]
     * пробрасывается дальше (кооперативная отмена корутины).
     *
     * `Task.await()` SDK блокирует поток (CountDownLatch), а доставку результата
     * SDK выполняет через main-looper — поэтому await обязан работать вне
     * главного потока, иначе взаимная блокировка и ANR (найдено на эмуляторе).
     */
    override suspend fun launch(): ReviewFlowOutcome =
        withContext(Dispatchers.IO) {
            try {
                val reviewInfo = manager.requestReviewFlow().await()
                manager.launchReviewFlow(reviewInfo).await()
                ReviewFlowOutcome.Shown
            } catch (e: RuStoreException) {
                ReviewFlowOutcome.Error(e.toDomainError())
            } catch (e: CancellationException) {
                throw e
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception
            ) {
                // Не-RuStoreException (runtime-сбои SDK вне иерархии, напр.
                // IllegalStateException при отвале binding) — тоже значение
                // исхода, а не выброс: фича «тихая», краш ей противоречит.
                ReviewFlowOutcome.Error(
                    ReviewException.Unknown(
                        message = e.message ?: "Неожиданный сбой review-флоу в RuStore",
                        cause = e
                    )
                )
            }
        }

    /**
     * Маппит ошибку SDK в доменную классификацию [ReviewException].
     */
    private fun RuStoreException.toDomainError(): ReviewException =
        when (this) {
            is RuStoreNotInstalledException ->
                ReviewException.NotInstalled(
                    message = "RuStore не установлен"
                )
            is RuStoreOutdatedException ->
                ReviewException.Outdated(
                    message = "RuStore устарел и не поддерживает review-флоу"
                )
            is RuStoreUserUnauthorizedException ->
                ReviewException.NotAuthorized(
                    message = "Пользователь не авторизован в RuStore"
                )
            is RuStoreRequestLimitReached ->
                ReviewException.RequestLimitReached(
                    message = "Лимит запросов показа формы оценки исчерпан"
                )
            is RuStoreReviewExists ->
                ReviewException.ReviewAlreadyExists(
                    message = "Отзыв на приложение уже оставлен"
                )
            else ->
                ReviewException.Unknown(
                    message = "Ошибка запуска review-флоу в RuStore",
                    cause = this
                )
        }
}
