package com.dayscounter.navigation

import com.dayscounter.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class ScreenTest {
    @Test
    fun `whenEventsScreenCreated_thenPropertiesAreCorrect`() {
        // Given
        val screen = Screen.Events

        // When & Then
        assertEquals("events", screen.route)
        assertEquals(R.string.events, screen.titleResId)
        assertNotNull(screen.icon)
    }

    @Test
    fun `whenMoreScreenCreated_thenPropertiesAreCorrect`() {
        // Given
        val screen = Screen.More

        // When & Then
        assertEquals("more", screen.route)
        assertEquals(R.string.more, screen.titleResId)
        assertNotNull(screen.icon)
    }
}
