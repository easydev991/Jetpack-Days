package com.dayscounter.review

import android.content.Context
import com.dayscounter.domain.usecase.ReviewFlowLauncher

/**
 * Фабрика лаунчера review-флоу для flavor `github`.
 *
 * Публичный FQN [ReviewFlowFactory] и сигнатура [create] совпадают с
 * rustore-фабрикой: wiring в DI (задача 6.1 change `add-rustore-in-app-review`)
 * выбирает реализацию flavor-ом без условной логики.
 *
 * В github-сборке review-функциональность отсутствует, поэтому фабрика
 * выдаёт [GithubNoOpReviewLauncher] — заглушку доменного контракта
 * [ReviewFlowLauncher]. SDK-зависимостей у flavor нет в принципе; контекст
 * принимается только для совпадения сигнатуры с rustore-вариантом.
 */
class ReviewFlowFactory {
    /**
     * Создаёт лаунчер review-флоу для github-сборки.
     */
    fun create(context: Context): ReviewFlowLauncher = GithubNoOpReviewLauncher()
}
