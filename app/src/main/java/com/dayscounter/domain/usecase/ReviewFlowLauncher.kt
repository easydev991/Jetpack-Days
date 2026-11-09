package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.ReviewFlowOutcome

/**
 * Контракт запуска review-флоу — нативной формы оценки магазина приложений.
 *
 * Чистый доменный контракт без android-типов и классов SDK: конкретные реализации
 * живут во flavor source-sets (rustore — на RuStore Review SDK, github — no-op)
 * и сами маппят свои ошибки в доменный исход. Двухфазная схема «подготовка →
 * показ» — деталь flavor-реализации, домену не видна.
 */
fun interface ReviewFlowLauncher {
    /**
     * Запускает флоу и дожидается его завершения.
     *
     * Ошибки возвращаются значением [ReviewFlowOutcome.Error] с доменной
     * классификацией, а не выбросом: вызывающий обрабатывает исход единообразно.
     * Отмена корутины прерывает запуск стандартно (исключением отмены).
     *
     * @return [ReviewFlowOutcome.Shown], если форма показана пользователю;
     * [ReviewFlowOutcome.Error] — при любой ошибке запуска.
     */
    suspend fun launch(): ReviewFlowOutcome
}
