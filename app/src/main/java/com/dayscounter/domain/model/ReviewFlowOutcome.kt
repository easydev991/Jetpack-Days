package com.dayscounter.domain.model

import com.dayscounter.domain.exception.ReviewException

/**
 * Исход запуска review-флоу.
 *
 * Форма либо показана пользователю, либо запуск завершился ошибкой с доменной
 * классификацией [ReviewException]; маппинг ошибок SDK в неё — ответственность
 * flavor-реализации лаунчера.
 */
sealed class ReviewFlowOutcome {
    /**
     * Нативная форма оценки показана пользователю.
     */
    data object Shown : ReviewFlowOutcome()

    /**
     * Флоу завершился ошибкой до или во время показа формы.
     */
    data class Error(
        val error: ReviewException
    ) : ReviewFlowOutcome()
}
