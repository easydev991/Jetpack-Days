package com.dayscounter.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.graphics.vector.ImageVector
import com.dayscounter.R

/**
 * Определяет экраны приложения для навигации
 */
sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val titleResId: Int,
) {
    object Events : Screen(
        route = "events",
        icon = Icons.Filled.List,
        titleResId = R.string.events,
    )

    object More : Screen(
        route = "more",
        icon = Icons.Filled.MoreVert,
        titleResId = R.string.more,
    )
}
