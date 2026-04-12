package com.dayscounter.analytics

/**
 * Интерфейс провайдера аналитики.
 *
 * Позволяет подключать различные реализации (Firebase, Amplitude, Mixpanel и т.д.)
 * без изменения кода, использующего аналитику.
 */
interface AnalyticsProvider {
    /**
     * Логирует событие аналитики.
     *
     * @param event Событие для логирования
     */
    fun log(event: AnalyticsEvent)
}
