package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dayscounter.navigation.Screen
import com.dayscounter.ui.screen.components.navHostContent
import com.dayscounter.ui.screen.components.navigationBarContent
import com.dayscounter.ui.screen.components.updateTabBasedOnRoute
import com.dayscounter.viewmodel.RootScreenViewModel

/**
 * Экран с навигацией между главным экраном событий и экраном настроек.
 *
 * @param modifier Modifier для экрана
 * @param viewModel ViewModel для управления состоянием
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rootScreen(
    modifier: Modifier = Modifier,
    viewModel: RootScreenViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel(),
) {
    rootScreenContent(
        modifier = modifier,
        viewModel = viewModel,
    )
}

/**
 * Основной контент экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rootScreenContent(
    modifier: Modifier = Modifier,
    viewModel: RootScreenViewModel,
) {
    val navController = rememberNavController()
    val items =
        listOf(
            Screen.Events,
            Screen.More,
        )

    // Получаем текущую вкладку из ViewModel
    val currentTab by viewModel.currentTab

    // Обновляем вкладку при изменении маршрута
    updateTabBasedOnRoute(navController, viewModel)

    Column(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        // Основной контент с навигацией
        navHostContent(
            navController = navController,
        )

        // Навигационная панель
        navigationBarContent(
            items = items,
            viewModel = viewModel,
            navController = navController,
        )
    }
}
