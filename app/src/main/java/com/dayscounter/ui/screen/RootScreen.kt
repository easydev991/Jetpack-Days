package com.dayscounter.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
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

    // Получаем текущий маршрут для управления видимостью навигации
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Определяем, должна ли быть видна навигационная панель
    val shouldShowNavigationBar = currentRoute in listOf(Screen.Events.route, Screen.More.route)

    // Обновляем вкладку при изменении маршрута
    updateTabBasedOnRoute(navController, viewModel)

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // Навигационная панель (только на главных экранах)
            if (shouldShowNavigationBar) {
                navigationBarContent(
                    items = items,
                    viewModel = viewModel,
                    navController = navController,
                )
            }
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
    ) { paddingValues ->
        // Основной контент с навигацией
        navHostContent(
            navController = navController,
            paddingValues = paddingValues,
        )
    }
}
