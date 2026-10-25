package com.dayscounter.ui.screens.root

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.UserActionType
import com.dayscounter.navigation.Screen
import com.dayscounter.ui.screens.common.NavHostContent
import com.dayscounter.ui.screens.common.NavigationBarContent
import com.dayscounter.ui.screens.common.UpdateTabBasedOnRoute
import com.dayscounter.ui.viewmodel.RootScreenViewModel

/**
 * Экран с навигацией между главным экраном событий и экраном настроек.
 *
 * Порог detekt LongParameterList (6) мал для compose-функций с парными колбэками —
 * подавление как в [com.dayscounter.ui.ds.DaysCountText].
 *
 * @param modifier Modifier для экрана
 * @param analyticsService Сервис аналитики
 * @param viewModel ViewModel для управления состоянием
 */
@Suppress("LongParameterList")
@Composable
fun RootScreen(
    modifier: Modifier = Modifier,
    analyticsService: AnalyticsService,
    pendingOpenDetailItemId: Long? = null,
    onPendingOpenHandled: () -> Unit = {},
    pendingCreateItem: Boolean = false,
    onPendingCreateHandled: () -> Unit = {},
    viewModel: RootScreenViewModel =
        androidx.lifecycle.viewmodel.compose
            .viewModel()
) {
    RootScreenContent(
        modifier = modifier,
        analyticsService = analyticsService,
        viewModel = viewModel,
        pendingOpenDetailItemId = pendingOpenDetailItemId,
        onPendingOpenHandled = onPendingOpenHandled,
        pendingCreateItem = pendingCreateItem,
        onPendingCreateHandled = onPendingCreateHandled
    )
}

/**
 * Основной контент экрана.
 */
@Suppress("LongParameterList")
@Composable
private fun RootScreenContent(
    modifier: Modifier = Modifier,
    analyticsService: AnalyticsService,
    viewModel: RootScreenViewModel,
    pendingOpenDetailItemId: Long?,
    onPendingOpenHandled: () -> Unit,
    pendingCreateItem: Boolean,
    onPendingCreateHandled: () -> Unit
) {
    val navController = rememberNavController()
    val items =
        listOf(
            Screen.Events,
            Screen.More
        )

    // Получаем текущую вкладку из ViewModel

    // Получаем текущий маршрут для управления видимостью навигации
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Определяем, должна ли быть видна навигационная панель
    val shouldShowNavigationBar = currentRoute in listOf(Screen.Events.route, Screen.More.route)

    // Обновляем вкладку при изменении маршрута
    UpdateTabBasedOnRoute(navController, viewModel)

    LaunchedEffect(pendingOpenDetailItemId) {
        val itemId = pendingOpenDetailItemId ?: return@LaunchedEffect
        navController.navigate(Screen.ItemDetail.createRoute(itemId))
        onPendingOpenHandled()
    }

    // Запрос шортката «Добавить запись»: экран создания не должен дублироваться —
    // если он уже есть в back stack (наверху или под другим экраном), навигация не выполняется.
    // currentBackStack помечен RestrictedApi — отсутствие в стеке определяется через
    // getBackStackEntry, который бросает IllegalArgumentException, если destination отсутствует.
    LaunchedEffect(pendingCreateItem) {
        if (!pendingCreateItem) return@LaunchedEffect
        val createAlreadyInStack =
            runCatching { navController.getBackStackEntry(Screen.CreateItem.route) }.isSuccess
        if (!createAlreadyInStack) {
            analyticsService.log(AnalyticsEvent.UserAction(UserActionType.CREATE))
            navController.navigate(Screen.CreateItem.route)
        }
        onPendingCreateHandled()
    }

    Scaffold(
        modifier = modifier,
        bottomBar = {
            // Навигационная панель (только на главных экранах)
            if (shouldShowNavigationBar) {
                NavigationBarContent(
                    items = items,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        },
        contentWindowInsets =
            androidx.compose.foundation.layout
                .WindowInsets(0, 0, 0, 0)
    ) { paddingValues ->
        // Основной контент с навигацией
        NavHostContent(
            navController = navController,
            paddingValues = paddingValues,
            analyticsService = analyticsService
        )
    }
}
