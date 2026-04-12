package com.dayscounter.analytics

import com.dayscounter.util.Logger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class AnalyticsServiceTest {
    private val logger: Logger = mockk(relaxed = true)

    @Test
    fun log_delegates_event_to_all_providers() {
        val provider1 = mockk<AnalyticsProvider>(relaxed = true)
        val provider2 = mockk<AnalyticsProvider>(relaxed = true)
        val service = AnalyticsService(listOf(provider1, provider2), logger)
        val event = AnalyticsEvent.ScreenView(AppScreen.EVENTS)

        service.log(event)

        verify { provider1.log(event) }
        verify { provider2.log(event) }
    }

    @Test
    fun log_with_empty_providers_list_does_not_throw() {
        val service = AnalyticsService(emptyList(), logger)
        val event = AnalyticsEvent.ScreenView(AppScreen.EVENTS)

        service.log(event)
    }

    @Test
    fun log_when_one_provider_throws_does_not_affect_others() {
        val failingProvider =
            mockk<AnalyticsProvider>(relaxed = true) {
                every { log(any()) } throws RuntimeException("Test error")
            }
        val workingProvider = mockk<AnalyticsProvider>(relaxed = true)
        val service = AnalyticsService(listOf(failingProvider, workingProvider), logger)
        val event = AnalyticsEvent.ScreenView(AppScreen.EVENTS)

        service.log(event)

        verify { workingProvider.log(event) }
    }
}
