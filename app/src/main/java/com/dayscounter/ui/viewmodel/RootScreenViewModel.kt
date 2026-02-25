package com.dayscounter.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.dayscounter.navigation.Screen

/**
 * ViewModel для управления состоянием корневого экрана с вкладками
 */
class RootScreenViewModel : ViewModel() {
    // Текущая выбранная вкладка
    private val _currentTab = mutableStateOf<Screen>(Screen.Events)
    val currentTab: State<Screen> = _currentTab

    // Список вкладок
    val tabs = listOf(Screen.Events, Screen.More)

    /**
     * Переключает на указанную вкладку
     * @param tab вкладка, на которую нужно переключиться
     */
    fun switchTab(tab: Screen) {
        if (tabs.contains(tab)) {
            _currentTab.value = tab
        }
    }

    /**
     * Возвращает true, если текущая вкладка - Events
     */
    fun isEventsTabSelected(): Boolean = _currentTab.value == Screen.Events

    /**
     * Возвращает true, если текущая вкладка - More
     */
    fun isMoreTabSelected(): Boolean = _currentTab.value == Screen.More
}
