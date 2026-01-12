package com.dayscounter.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Определяет экраны приложения для навигации
 */
sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Events : Screen("events", "События", Icons.Filled.List)

    object More : Screen("more", "Ещё", Icons.Filled.MoreVert)
}
