package com.dayscounter.ui.screens.events

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.domain.model.SortOrder

/**
 * Data class for parameters of main screen top bar.
 */
internal data class MainScreenTopBarState(
    val itemsCount: Int,
    val sortOrder: SortOrder,
    val searchQuery: String,
    val onSearchQueryChange: (String) -> Unit,
    val onSortClick: () -> Unit,
    val onSortOrderChange: (SortOrder) -> Unit,
    val availableColorTags: List<Int>,
    val selectedColorTag: Int?,
    val onFilterClick: () -> Unit
)

/**
 * Top bar for main screen with sort functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenTopBar(state: MainScreenTopBarState) {
    TopAppBar(
        title = { Text(stringResource(R.string.events)) },
        navigationIcon = {
            if (state.itemsCount > 1) {
                SortMenu(
                    sortOrder = state.sortOrder,
                    onSortClick = state.onSortClick,
                    onSortOrderChange = state.onSortOrderChange
                )
            }
        },
        actions = {
            if (
                (state.itemsCount >= 2 || state.selectedColorTag != null) &&
                state.availableColorTags.isNotEmpty()
            ) {
                IconButton(onClick = state.onFilterClick) {
                    Icon(
                        imageVector =
                            if (state.selectedColorTag != null) {
                                Icons.Filled.Palette
                            } else {
                                Icons.Outlined.Palette
                            },
                        contentDescription = stringResource(R.string.open_filter)
                    )
                }
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface
            )
    )
}
