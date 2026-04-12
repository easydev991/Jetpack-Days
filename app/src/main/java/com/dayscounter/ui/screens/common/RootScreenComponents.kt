package com.dayscounter.ui.screens.common

import android.util.Log
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.AppScreen
import com.dayscounter.analytics.UserActionType
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
    navController: NavHostController
) {
    val currentTab by viewModel.currentTab

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon!!,
                        contentDescription = stringResource(id = screen.titleResId!!)
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
                }
            )
        }
    }
}

/**
 * Навигационное назначение для главного экрана событий.
 */
private fun NavGraphBuilder.mainScreenDestination(
    analyticsService: AnalyticsService,
    navController: NavHostController
) {
    composable(Screen.Events.route) {
        LaunchedEffect(Unit) {
            analyticsService.log(AnalyticsEvent.ScreenView(AppScreen.EVENTS, "MainScreen"))
        }
        EventsScreenContent(navController, analyticsService)
    }
}

/**
 * Навигационное назначение для экрана деталей события.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.detailScreenDestination(
    repository: com.dayscounter.domain.repository.ItemRepository,
    analyticsService: AnalyticsService,
    navController: NavHostController
) {
    composable(
        route = Screen.ItemDetail.route,
        arguments =
            listOf(
                navArgument("itemId") {
                    type = NavType.LongType
                }
            )
    ) { backStackEntry ->
        val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
        LaunchedEffect(backStackEntry.id) {
            analyticsService.log(AnalyticsEvent.ScreenView(AppScreen.DETAIL, "DetailScreen"))
        }
        DetailScreen(
            itemId = itemId,
            viewModel =
                viewModel(
                    factory = DetailScreenViewModel.factory(repository)
                ),
            onBackClick = { navController.popBackStack() },
            onEditClick = { id ->
                analyticsService.log(AnalyticsEvent.UserAction(UserActionType.EDIT))
                navController.navigate(Screen.EditItem.createRoute(id))
            }
        )
    }
}

/**
 * Навигационное назначение для экрана создания/редактирования события.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.createEditScreenDestination(
    repository: com.dayscounter.domain.repository.ItemRepository,
    resourceProvider: com.dayscounter.data.provider.ResourceProvider,
    analyticsService: AnalyticsService,
    navController: NavHostController
) {
    composable(Screen.CreateItem.route) {
        LaunchedEffect(Unit) {
            analyticsService.log(
                AnalyticsEvent.ScreenView(
                    AppScreen.CREATE_EDIT,
                    "CreateEditScreen"
                )
            )
        }
        CreateEditScreen(
            itemId = null,
            viewModel =
                viewModel(
                    factory = CreateEditScreenViewModel.factory(repository, resourceProvider, analyticsService)
                ),
            onBackClick = { navController.popBackStack() },
            analyticsService = analyticsService
        )
    }
    composable(
        route = Screen.EditItem.route,
        arguments =
            listOf(
                navArgument("itemId") {
                    type = NavType.LongType
                }
            )
    ) { backStackEntry ->
        val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
        LaunchedEffect(backStackEntry.id) {
            analyticsService.log(
                AnalyticsEvent.ScreenView(
                    AppScreen.CREATE_EDIT,
                    "CreateEditScreen"
                )
            )
        }
        CreateEditScreen(
            itemId = itemId,
            viewModel =
                viewModel(
                    factory = CreateEditScreenViewModel.factory(repository, resourceProvider, analyticsService)
                ),
            onBackClick = { navController.popBackStack() },
            analyticsService = analyticsService
        )
    }
}

/**
 * Навигационное назначение для экрана настроек.
 */
private fun NavGraphBuilder.moreScreenDestination(
    analyticsService: AnalyticsService,
    navController: NavHostController
) {
    composable(Screen.More.route) {
        LaunchedEffect(Unit) {
            analyticsService.log(AnalyticsEvent.ScreenView(AppScreen.MORE, "MoreScreen"))
        }
        MoreScreen(navController)
    }
}

/**
 * Навигационное назначение для экрана темы и иконки.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.themeIconScreenDestination(
    analyticsService: AnalyticsService,
    navController: NavHostController,
    dataStore: AppSettingsDataStore,
    application: android.app.Application
) {
    composable(Screen.ThemeIcon.route) {
        val viewModel: ThemeIconViewModel =
            viewModel(factory = ThemeIconViewModel.factory(dataStore, application, analyticsService))
        LaunchedEffect(Unit) {
            analyticsService.log(AnalyticsEvent.ScreenView(AppScreen.THEME_ICON, "ThemeIconScreen"))
        }
        ThemeIconScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() }
        )
    }
}

/**
 * Навигационное назначение для экрана данных приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
private fun NavGraphBuilder.appDataScreenDestination(
    analyticsService: AnalyticsService,
    navController: NavHostController,
    repository: com.dayscounter.domain.repository.ItemRepository,
    application: android.app.Application
) {
    composable(Screen.AppData.route) {
        val viewModel: AppDataScreenViewModel =
            viewModel(factory = AppDataScreenViewModel.factory(repository, application, analyticsService))
        LaunchedEffect(Unit) {
            analyticsService.log(AnalyticsEvent.ScreenView(AppScreen.APP_DATA, "AppDataScreen"))
        }
        AppDataScreen(
            viewModel = viewModel,
            onBackClick = { navController.popBackStack() },
            analyticsService = analyticsService
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
    paddingValues: PaddingValues =
        PaddingValues(),
    analyticsService: AnalyticsService
) {
    // Получаем зависимости для создания ViewModels
    val context = LocalContext.current.applicationContext
    val application =
        checkNotNull(context as? android.app.Application) {
            "Application context is required"
        }
    val database = DaysDatabase.getDatabase(context)
    val repository = AppModule.createItemRepository(database)
    val resourceProvider = AppModule.resourceProvider
    val dataStore = AppModule.createAppSettingsDataStore(context)

    NavHost(
        navController = navController,
        startDestination = Screen.Events.route,
        modifier = Modifier.padding(paddingValues)
    ) {
        this.mainScreenDestination(analyticsService, navController)
        this.detailScreenDestination(repository, analyticsService, navController)
        this.createEditScreenDestination(
            repository,
            resourceProvider,
            analyticsService,
            navController
        )
        this.moreScreenDestination(analyticsService, navController)
        this.themeIconScreenDestination(
            analyticsService,
            navController,
            dataStore,
            application
        )
        this.appDataScreenDestination(
            analyticsService,
            navController,
            repository,
            application
        )
    }
}

/**
 * Контент для экрана событий.
 */
@Composable
internal fun EventsScreenContent(
    navController: NavHostController,
    analyticsService: AnalyticsService
) {
    Log.d("RootScreen", "Отображение экрана событий")
    MainScreen(
        onItemClick = { itemId ->
            Log.d("RootScreen", "Навигация к экрану деталей: $itemId")
            navController.navigate(Screen.ItemDetail.createRoute(itemId))
        },
        onEditClick = { itemId ->
            analyticsService.log(AnalyticsEvent.UserAction(UserActionType.EDIT))
            Log.d("RootScreen", "Навигация к экрану редактирования: $itemId")
            navController.navigate(Screen.EditItem.createRoute(itemId))
        },
        onCreateClick = {
            analyticsService.log(AnalyticsEvent.UserAction(UserActionType.CREATE))
            Log.d("RootScreen", "Навигация к экрану создания")
            navController.navigate(Screen.CreateItem.route)
        },
        analyticsService = analyticsService
    )
}

/**
 * Обновление вкладки на основе маршрута.
 */
@Composable
internal fun UpdateTabBasedOnRoute(
    navController: NavHostController,
    viewModel: RootScreenViewModel
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
