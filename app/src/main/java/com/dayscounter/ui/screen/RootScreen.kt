package com.dayscounter.ui.screen

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dayscounter.R
import com.dayscounter.navigation.Screen
import com.dayscounter.viewmodel.RootScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rootScreen(
    modifier: Modifier = Modifier,
    viewModel: RootScreenViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
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
        NavHost(
            navController = navController,
            startDestination = Screen.Events.route,
            modifier =
                Modifier
                    .weight(1f),
        ) {
            composable(Screen.Events.route) {
                eventsScreenContent()
            }
            composable(Screen.More.route) {
                moreScreenContent()
            }
        }

        // Навигационная панель
        NavigationBar {
            items.forEach { screen ->
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = screen.icon,
                            contentDescription = stringResource(id = screen.titleResId),
                        )
                    },
                    label = {
                        Text(text = stringResource(id = screen.titleResId))
                    },
                    selected = currentTab == screen,
                    onClick = {
                        // Переключаем вкладку в ViewModel
                        viewModel.switchTab(screen)

                        // Навигируем к соответствующему экрану
                        navController.navigate(screen.route) {
                            // Убираем дубликаты из стека навигации
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Сохраняем состояние при переключении
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun updateTabBasedOnRoute(
    navController: NavHostController,
    viewModel: RootScreenViewModel,
) {
    // Определяем текущий маршрут для обновления состояния вкладки
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Обновляем вкладку при изменении маршрута
    when (currentRoute) {
        Screen.Events.route -> {
            if (viewModel.currentTab != Screen.Events) {
                viewModel.switchTab(Screen.Events)
            }
        }
        Screen.More.route -> {
            if (viewModel.currentTab != Screen.More) {
                viewModel.switchTab(Screen.More)
            }
        }
    }
}

@Composable
private fun eventsScreenContent() {
    // Заглушка для экрана событий
    Log.d("RootScreen", "Экран событий не реализован")
    Surface(
        modifier =
            Modifier
                .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Экран событий не реализован",
            )
        }
    }
}

@Composable
private fun moreScreenContent() {
    // Заглушка для экрана "Ещё"
    Log.d("RootScreen", "Экран 'Ещё' не реализован")
    Surface(
        modifier =
            Modifier
                .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Экран 'Ещё' не реализован",
            )
        }
    }
}
