package com.dayscounter.ui.state

import com.dayscounter.navigation.Screen
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class RootScreenStateTest {
    @Test
    fun `whenStateCreatedWithDefaults_thenEventsTabIsSelected`() {
        // Given
        val state = RootScreenState()

        // When & Then
        assertEquals(Screen.Events, state.currentTab)
        assertTrue(state.tabs.contains(Screen.Events))
        assertTrue(state.tabs.contains(Screen.More))
        assertEquals(2, state.tabs.size)
    }

    @Test
    fun `whenStateCreatedWithCustomTab_thenCustomTabIsSelected`() {
        // Given
        val customTab = Screen.More

        // When
        val state = RootScreenState(currentTab = customTab)

        // Then
        assertEquals(customTab, state.currentTab)
    }

    @Test
    fun `whenStateCreatedWithCustomTabs_thenCustomTabsAreAvailable`() {
        // Given
        val customTabs = listOf(Screen.Events, Screen.More)

        // When
        val state = RootScreenState(tabs = customTabs)

        // Then
        assertEquals(customTabs, state.tabs)
    }
}
