package com.dayscounter.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import com.dayscounter.R

/**
 * Определяет экраны приложения для навигации
 */
sealed class Screen(
    val route: String,
    val icon: ImageVector? = null,
    val titleResId: Int? = null,
) {
    object Events : Screen(
        route = "events",
        icon = Icons.AutoMirrored.Filled.List,
        titleResId = R.string.events,
    )

    object More : Screen(
        route = "more",
        icon = Icons.Filled.MoreVert,
        titleResId = R.string.more,
    )

    object ItemDetail : Screen(
        route = "item_detail/{itemId}",
    ) {
        /** Создает маршрут для экрана деталей с указанным itemId */
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }

    object CreateItem : Screen(
        route = "create_item",
    )

    object EditItem : Screen(
        route = "edit_item/{itemId}",
    ) {
        /** Создает маршрут для экрана редактирования с указанным itemId */
        fun createRoute(itemId: Long) = "edit_item/$itemId"
    }
}
