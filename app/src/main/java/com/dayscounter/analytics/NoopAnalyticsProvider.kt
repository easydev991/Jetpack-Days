package com.dayscounter.analytics

/**
 * Noop провайдер аналитики для debug и test окружений.
 *
 * Не выполняет никаких действий при логировании событий.
 */
class NoopAnalyticsProvider : AnalyticsProvider {
    override fun log(event: AnalyticsEvent) {
        Unit
    }
}
