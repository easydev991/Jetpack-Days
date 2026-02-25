package com.dayscounter.ui.screens.common

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dayscounter.analytics.FirebaseAnalyticsHelper
import com.dayscounter.analytics.ScreenNames
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.di.AppModule
import com.dayscounter.navigation.Screen
import com.dayscounter.ui.screens.appdata.AppDataScreen
import com.dayscounter.ui.screens.createedit.CreateEditScreen
import com.dayscounter.ui.screens.detail.DetailScreen
import com.dayscounter.ui.screens.events.MainScreen
import com.dayscounter.ui.screens.more.MoreScreen
import com.dayscounter.ui.screens.themeicon.ThemeIconScreen
import com.dayscounter.ui.viewmodel.AppDataScreenViewModel
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel
import com.dayscounter.ui.viewmodel.DetailScreenViewModel
import com.dayscounter.ui.viewmodel.RootScreenViewModel
import com.dayscounter.ui.viewmodel.ThemeIconViewModel

/**
 * Навигационная панель.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavigationBarContent(
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
 * Навигационное назначение для главного экрана событий.
 */
private fun androidx.navigation.NavGraphBuilder.mainScreenDestination(navController: NavHostController) {
    composable(Screen.Events.route) {
        val context = LocalContext.current
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.EVENTS,
            screenClass = "MainScreen",
        )
        EventsScreenContent(navController)
    }
}

/**
 * Навигационное назначение для экрана деталей события.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun androidx.navigation.NavGraphBuilder.detailScreenDestination(
    repository: com.dayscounter.domain.repository.ItemRepository,
    navController: NavHostController,
) {
    composable(
        route = Screen.ItemDetail.route,
        arguments =
            listOf(
                navArgument("itemId") {
                    type = NavType.LongType
                },
            ),
    ) { backStackEntry ->
        val context = LocalContext.current
        val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.DETAIL,
            screenClass = "DetailScreen",
        )
        DetailScreen(
            itemId = itemId,
            viewModel =
                viewModel(
                    factory = DetailScreenViewModel.factory(repository),
                ),
            onBackClick = { navController.popBackStack() },
            onEditClick = { id ->
                navController.navigate(Screen.EditItem.createRoute(id))
            },
        )
    }
}

/**
 * Навигационное назначение для экрана создания/редактирования события.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun androidx.navigation.NavGraphBuilder.createEditScreenDestination(
    repository: com.dayscounter.domain.repository.ItemRepository,
    resourceProvider: com.dayscounter.data.provider.ResourceProvider,
    navController: NavHostController,
) {
    composable(Screen.CreateItem.route) {
        val context = LocalContext.current
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.CREATE,
            screenClass = "CreateEditScreen",
        )
        CreateEditScreen(
            itemId = null,
            viewModel =
                viewModel(
                    factory = CreateEditScreenViewModel.factory(repository, resourceProvider),
                ),
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
        val context = LocalContext.current
        val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.CREATE,
            screenClass = "CreateEditScreen",
        )
        CreateEditScreen(
            itemId = itemId,
            viewModel =
                viewModel(
                    factory = CreateEditScreenViewModel.factory(repository, resourceProvider),
                ),
            onBackClick = { navController.popBackStack() },
        )
    }
}

/**
 * Навигационное назначение для экрана настроек.
 */
private fun androidx.navigation.NavGraphBuilder.moreScreenDestination(navController: NavHostController) {
    composable(Screen.More.route) {
        val context = LocalContext.current
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.MORE,
            screenClass = "MoreScreen",
        )
        MoreScreen(navController)
    }
}

/**
 * Навигационное назначение для экрана темы и иконки.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun androidx.navigation.NavGraphBuilder.themeIconScreenDestination(
    navController: NavHostController,
    dataStore: AppSettingsDataStore,
    application: android.app.Application,
) {
    composable(Screen.ThemeIcon.route) {
        val context = LocalContext.current
        val viewModel: ThemeIconViewModel =
            viewModel(factory = ThemeIconViewModel.factory(dataStore, application))
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.THEME_ICON,
            screenClass = "ThemeIconScreen",
        )
        ThemeIconScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
        )
    }
}

/**
 * Навигационное назначение для экрана данных приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun androidx.navigation.NavGraphBuilder.appDataScreenDestination(
    navController: NavHostController,
    repository: com.dayscounter.domain.repository.ItemRepository,
    application: android.app.Application,
) {
    composable(Screen.AppData.route) {
        val context = LocalContext.current
        val viewModel: AppDataScreenViewModel =
            viewModel(factory = AppDataScreenViewModel.factory(repository, application))
        // Логируем screen_view событие для Analytics и breadcrumb logs
        FirebaseAnalyticsHelper.logScreenView(
            context = context,
            screenName = ScreenNames.APP_DATA,
            screenClass = "AppDataScreen",
        )
        AppDataScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
        )
    }
}

/**
 * NavHost с маршрутами.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NavHostContent(
    navController: NavHostController,
    paddingValues: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout
            .PaddingValues(),
) {
    // Получаем зависимости для создания ViewModels
    val context = LocalContext.current.applicationContext
    val database = DaysDatabase.getDatabase(context)
    val repository = AppModule.createItemRepository(database)
    val resourceProvider = AppModule.resourceProvider
    val dataStore = AppModule.createAppSettingsDataStore(context)

    NavHost(
        navController = navController,
        startDestination = Screen.Events.route,
        modifier = Modifier.padding(paddingValues),
    ) {
        this.mainScreenDestination(navController)
        this.detailScreenDestination(repository, navController)
        this.createEditScreenDestination(repository, resourceProvider, navController)
        this.moreScreenDestination(navController)
        this.themeIconScreenDestination(
            navController,
            dataStore,
            context as android.app.Application,
        )
        this.appDataScreenDestination(
            navController,
            repository,
            context,
        )
    }
}

/**
 * Контент для экрана событий.
 */
@Composable
internal fun EventsScreenContent(navController: NavHostController) {
    Log.d("RootScreen", "Отображение экрана событий")
    MainScreen(
        onItemClick = { itemId ->
            Log.d("RootScreen", "Навигация к экрану деталей: $itemId")
            navController.navigate(Screen.ItemDetail.createRoute(itemId))
        },
        onEditClick = { itemId ->
            Log.d("RootScreen", "Навигация к экрану редактирования: $itemId")
            navController.navigate(Screen.EditItem.createRoute(itemId))
        },
        onCreateClick = {
            Log.d("RootScreen", "Навигация к экрану создания")
            navController.navigate(Screen.CreateItem.route)
        },
    )
}

/**
 * Обновление вкладки на основе маршрута.
 */
@Composable
internal fun UpdateTabBasedOnRoute(
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
