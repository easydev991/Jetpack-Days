package com.dayscounter.analytics

import android.content.Context
import android.util.Log
import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

/**
 * Расширения для NavController с автоматическим логированием screen_view событий
 * в Firebase Analytics.
 *
 * Эти события автоматически становятся breadcrumb logs в Crashlytics, показывая
 * последовательность экранов перед крашем.
 *
 * @param context Контекст приложения
 * @param route Маршрут для навигации
 * @param builder Конфигурация NavOptions (опционально)
 * @param screenName Название экрана для логирования (опционально,
 * используется по умолчанию route)
 */
fun NavController.navigateWithLogging(
    context: Context,
    route: String,
    builder: (NavOptionsBuilder.() -> Unit)? = null,
    screenName: String? = null,
) {
    // Логируем screen_view перед навигацией
    val screenToLog = screenName ?: route
    FirebaseAnalyticsHelper.logScreenView(
        context = context,
        screenName = screenToLog,
        screenClass = route,
    )

    // Выполняем навигацию
    if (builder != null) {
        navigate(route, builder)
    } else {
        navigate(route)
    }

    Log.d("NavigationAnalytics", "Навигация к: $route (screenName: $screenToLog)")
}

/**
 * Имена экранов для логирования в Analytics.
 */
object ScreenNames {
    const val EVENTS = "EventsScreen"
    const val DETAIL = "DetailScreen"
    const val CREATE = "CreateEditScreen"
    const val MORE = "MoreScreen"
    const val THEME_ICON = "ThemeIconScreen"
    const val APP_DATA = "AppDataScreen"
}
