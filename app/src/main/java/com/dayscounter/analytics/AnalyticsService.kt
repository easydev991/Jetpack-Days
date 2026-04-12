package com.dayscounter.analytics

import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger

/**
 * Сервис аналитики, который делегирует события всем зарегистрированным провайдерам.
 *
 * @param providers Список провайдеров для отправки событий
 */
class AnalyticsService(
    private val providers: List<AnalyticsProvider>,
    private val logger: Logger = AndroidLogger()
) {
    private companion object {
        private const val TAG = "AnalyticsService"
    }

    /**
     * Логирует событие, отправляя его всем зарегистрированным провайдерам.
     *
     * Ошибки отдельных провайдеров не влияют на работу остальных.
     *
     * @param event Событие для логирования
     */
    @Suppress("TooGenericExceptionCaught")
    fun log(event: AnalyticsEvent) {
        providers.forEach { provider ->
            try {
                provider.log(event)
            } catch (e: Exception) {
                logger.e(
                    tag = TAG,
                    message = "Ошибка в провайдере ${provider::class.simpleName}: ${e.message}",
                    throwable = e
                )
            }
        }
    }
}
