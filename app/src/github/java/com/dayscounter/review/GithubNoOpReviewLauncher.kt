package com.dayscounter.review

import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.ReviewFlowOutcome
import com.dayscounter.domain.usecase.ReviewFlowLauncher

/**
 * No-op реализация [ReviewFlowLauncher] для flavor `github`.
 *
 * В github-сборке review-функциональность отсутствует (нет магазина с
 * нативной формой оценки), поэтому лаунчер никогда не вызывается wiring-ом;
 * на всякий случай при вызове он возвращает доменный исход ошибки
 * [ReviewException.NotInstalled] вместо выброса исключения.
 *
 * Реализация — [ReviewFlowLauncher] из домена, без android-типов и без
 * SDK-зависимостей.
 */
class GithubNoOpReviewLauncher : ReviewFlowLauncher {
    override suspend fun launch(): ReviewFlowOutcome =
        ReviewFlowOutcome.Error(
            ReviewException.NotInstalled(
                message = "Review flow недоступен в github flavor"
            )
        )
}
