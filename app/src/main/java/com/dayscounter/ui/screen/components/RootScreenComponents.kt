package com.dayscounter.ui.screen.components

import android.util.Log
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dayscounter.R
import com.dayscounter.navigation.Screen
import com.dayscounter.viewmodel.RootScreenViewModel

/**
 * Навигационная панель.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun navigationBarContent(
    items: List<Screen>,
    viewModel: RootScreenViewModel,
    navController: NavHostController,
) {
    val currentTab by viewModel.currentTab

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon!!,
                        contentDescription = stringResource(id = screen.titleResId!!),
                    )
                },
                label = {
                    Text(text = stringResource(id = screen.titleResId!!))
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

/**
 * NavHost с маршрутами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun navHostContent(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Events.route,
    ) {
        composable(Screen.Events.route) {
            eventsScreenContent(navController)
        }
        composable(Screen.More.route) {
            moreScreenContent()
        }
        composable(
            route = Screen.ItemDetail.route,
            arguments =
                listOf(
                    navArgument("itemId") {
                        type = NavType.LongType
                    },
                ),
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
            com.dayscounter.ui.screen.detailScreen(
                itemId = itemId,
                onBackClick = { navController.popBackStack() },
                onEditClick = { id ->
                    navController.navigate(Screen.EditItem.createRoute(id))
                },
            )
        }
        composable(Screen.CreateItem.route) {
            com.dayscounter.ui.screen.createEditScreen(
                itemId = null,
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(
            route = Screen.EditItem.route,
            arguments =
                listOf(
                    navArgument("itemId") {
                        type = NavType.LongType
                    },
                ),
        ) { backStackEntry ->
            val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
            com.dayscounter.ui.screen.createEditScreen(
                itemId = itemId,
                onBackClick = { navController.popBackStack() },
            )
        }
    }
}

/**
 * Контент для экрана событий.
 */
@Composable
internal fun eventsScreenContent(navController: NavHostController) {
    Log.d("RootScreen", "Отображение экрана событий")
    com.dayscounter.ui.screen.mainScreen(
        onItemClick = { itemId ->
            Log.d("RootScreen", "Навигация к экрану деталей: $itemId")
            navController.navigate(Screen.ItemDetail.createRoute(itemId))
        },
    )
}

/**
 * Контент для экрана настроек.
 */
@Composable
internal fun moreScreenContent() {
    // Заглушка для экрана настроек
    Log.d("RootScreen", "Экран настроек не реализован")
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
                    .padding(dimensionResource(R.dimen.spacing_extra_large)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Экран настроек не реализован",
            )
        }
    }
}

/**
 * Обновление вкладки на основе маршрута.
 */
@Composable
internal fun updateTabBasedOnRoute(
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
