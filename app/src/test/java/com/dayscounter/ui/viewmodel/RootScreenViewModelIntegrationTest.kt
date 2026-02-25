package com.dayscounter.ui.viewmodel

import com.dayscounter.navigation.Screen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RootScreenViewModelIntegrationTest {
    @Test
    fun whenViewModelCreated_thenEventsTabIsSelectedByDefault() {
        // Given
        val viewModel = RootScreenViewModel()

        // When & Then
        assertEquals(Screen.Events, viewModel.currentTab.value)
        assertTrue(viewModel.isEventsTabSelected())
        assertTrue(!viewModel.isMoreTabSelected())
    }

    @Test
    fun whenSwitchToMoreTab_thenMoreTabIsSelected() {
        // Given
        val viewModel = RootScreenViewModel()

        // When
        viewModel.switchTab(Screen.More)

        // Then
        assertEquals(Screen.More, viewModel.currentTab.value)
        assertTrue(viewModel.isMoreTabSelected())
        assertTrue(!viewModel.isEventsTabSelected())
    }

    @Test
    fun whenSwitchToEventsTab_thenEventsTabIsSelected() {
        // Given
        val viewModel = RootScreenViewModel()
        viewModel.switchTab(Screen.More) // Сначала переключаемся на More

        // When
        viewModel.switchTab(Screen.Events)

        // Then
        assertEquals(Screen.Events, viewModel.currentTab.value)
        assertTrue(viewModel.isEventsTabSelected())
        assertTrue(!viewModel.isMoreTabSelected())
    }

    @Test
    fun whenSwitchToSameTab_thenTabRemainsSelected() {
        // Given
        val viewModel = RootScreenViewModel()
        val initialTab = viewModel.currentTab

        // When
        viewModel.switchTab(initialTab.value)

        // Then
        assertEquals(initialTab.value, viewModel.currentTab.value)
        assertTrue(viewModel.isEventsTabSelected())
    }

    @Test
    fun whenViewModelCreated_thenBothTabsAreAvailable() {
        // Given
        val viewModel = RootScreenViewModel()

        // When & Then
        assertTrue(viewModel.tabs.contains(Screen.Events))
        assertTrue(viewModel.tabs.contains(Screen.More))
        assertEquals(2, viewModel.tabs.size)
    }
}
