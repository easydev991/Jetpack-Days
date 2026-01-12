package com.dayscounter.navigation

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test

class ScreenTest {
    @Test
    fun `whenEventsScreenCreated_thenPropertiesAreCorrect`() {
        // Given
        val screen = Screen.Events

        // When & Then
        assertEquals("events", screen.route)
        assertEquals("События", screen.title)
        assertNotNull(screen.icon)
    }

    @Test
    fun `whenMoreScreenCreated_thenPropertiesAreCorrect`() {
        // Given
        val screen = Screen.More

        // When & Then
        assertEquals("more", screen.route)
        assertEquals("Ещё", screen.title)
        assertNotNull(screen.icon)
    }
}
