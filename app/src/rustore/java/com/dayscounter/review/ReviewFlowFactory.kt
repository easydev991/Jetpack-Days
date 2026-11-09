package com.dayscounter.review

import android.content.Context
import ru.rustore.sdk.review.RuStoreReviewManagerFactory

/**
 * Фабрика лаунчера review-флоу для flavor `rustore` (задача 4.1 change
 * `add-rustore-in-app-review`).
 *
 * Создает лаунчер на RuStore Review SDK: менеджер строится
 * [RuStoreReviewManagerFactory.create], обёртка [RuStoreReviewFlowLauncher]
 * реализует доменный контракт [com.dayscounter.domain.usecase.ReviewFlowLauncher].
 * Публичный FQN `com.dayscounter.review.ReviewFlowFactory` зафиксирован и
 * совпадает с фабрикой flavor `github`; wiring — задача 6.1.
 */
class ReviewFlowFactory {
    /**
     * Создает лаунчер review-флоу для переданного контекста.
     */
    fun create(context: Context): RuStoreReviewFlowLauncher = RuStoreReviewFlowLauncher(RuStoreReviewManagerFactory.create(context))
}
