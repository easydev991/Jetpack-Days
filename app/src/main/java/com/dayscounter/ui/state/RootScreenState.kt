package com.dayscounter.ui.state

import com.dayscounter.navigation.Screen

/**
 * Состояние UI для корневого экрана
 */
data class RootScreenState(
    val currentTab: Screen = Screen.Events,
    val tabs: List<Screen> = listOf(Screen.Events, Screen.More),
)
