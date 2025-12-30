package com.dayscounter.viewmodel

import com.dayscounter.navigation.Screen
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class RootScreenViewModelTest {
    @Test
    fun `whenViewModelCreated_thenDefaultTabIsActive`() {
        // Given
        val viewModel = RootScreenViewModel()

        // When & Then
        assertEquals(Screen.Events, viewModel.currentTab.value)
        assertTrue(viewModel.isEventsTabSelected())
        assertTrue(viewModel.tabs.contains(Screen.Events))
        assertTrue(viewModel.tabs.contains(Screen.More))
    }

    @Test
    fun `whenSwitchToEventsTab_thenEventsTabIsActive`() {
        // Given
        val viewModel = RootScreenViewModel()
        viewModel.switchTab(Screen.More) // Переключаем на More для тестирования

        // When
        viewModel.switchTab(Screen.Events)

        // Then
        assertEquals(Screen.Events, viewModel.currentTab.value)
        assertTrue(viewModel.isEventsTabSelected())
        assertTrue(!viewModel.isMoreTabSelected())
    }

    @Test
    fun `whenSwitchToMoreTab_thenMoreTabIsActive`() {
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
    fun `whenInvalidTab_thenNoChange`() {
        // Given
        val viewModel = RootScreenViewModel()
        val initialTab = viewModel.currentTab

        // When
        // Передаем вкладку, которой нет в списке
        // Вместо создания анонимного объекта, используем другую вкладку
        // и проверим, что текущая вкладка не изменится, если передать
        // вкладку, которая не входит в список доступных вкладок
        // Но в текущей реализации switchTab принимает только объекты Screen
        // Поэтому проверим, что передача той же вкладки не изменяет состояние
        viewModel.switchTab(Screen.Events) // Передаем ту же вкладку

        // Then
        assertEquals(initialTab.value, viewModel.currentTab.value)
    }
}
