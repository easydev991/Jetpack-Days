package com.dayscounter.ui.screens.events

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.UserActionType
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.ui.viewmodel.MainScreenState

/**
 * Состояние Scaffold главного экрана.
 */
internal data class MainScreenScaffoldState(
    val uiState: MainScreenState,
    val searchQuery: String,
    val sortOrder: SortOrder,
    val itemsCount: Int,
    val listState: LazyListState,
    val availableColorTags: List<Int>,
    val selectedColorTag: Int?
)

/**
 * Scaffold экрана со списком, шапкой и FAB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreenScaffold(
    state: MainScreenScaffoldState,
    params: MainScreenParams,
    modifier: Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets =
            WindowInsets
                .safeDrawing
                .only(WindowInsetsSides.Horizontal),
        topBar = {
            ScreenHeader(
                state =
                    MainScreenTopBarState(
                        itemsCount = state.itemsCount,
                        sortOrder = state.sortOrder,
                        onSortClick = {
                            params.analyticsService.log(AnalyticsEvent.UserAction(UserActionType.SORT))
                        },
                        onSortOrderChange = { newSortOrder ->
                            params.viewModel.updateSortOrder(newSortOrder)
                        },
                        availableColorTags = state.availableColorTags,
                        selectedColorTag = state.selectedColorTag,
                        onFilterClick = {
                            params.analyticsService.log(AnalyticsEvent.UserAction(UserActionType.OPEN_FILTER))
                            params.viewModel.toggleFilterDialog()
                        }
                    )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = params.onCreateClick
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_item)
                )
            }
        }
    ) { paddingValues ->
        ScreenBody(
            searchQuery = state.searchQuery,
            itemsCount = state.itemsCount,
            paddingValues = paddingValues,
            state =
                MainScreenContentState(
                    uiState = state.uiState,
                    searchQuery = state.searchQuery,
                    listState = state.listState,
                    getFormattedDaysForItemUseCase = params.getFormattedDaysForItemUseCase,
                    onItemClick = params.onItemClick,
                    onEditClick = params.onEditClick,
                    viewModel = params.viewModel
                ),
            onSearchQueryChange = { params.viewModel.updateSearchQuery(it) }
        )
    }
}

/**
 * Состояние диалогов главного экрана.
 */
internal data class MainScreenDialogsState(
    val showDeleteDialog: Item?,
    val showFilterDialog: Boolean,
    val availableColorTags: List<Int>,
    val selectedColorTag: Int?,
    val onDeleteConfirm: () -> Unit,
    val onDeleteCancel: () -> Unit,
    val onFilterApply: (Int?) -> Unit,
    val onFilterDismiss: () -> Unit
)

/**
 * Диалоги главного экрана (удаление и фильтр по цвету).
 */
@Composable
internal fun MainScreenDialogs(state: MainScreenDialogsState) {
    state.showDeleteDialog?.let { item ->
        DeleteDialog(
            item = item,
            onConfirm = state.onDeleteConfirm,
            onCancel = state.onDeleteCancel
        )
    }

    if (state.showFilterDialog) {
        ColorTagFilterDialog(
            availableColors = state.availableColorTags,
            currentFilter = state.selectedColorTag,
            onApply = state.onFilterApply,
            onDismiss = state.onFilterDismiss
        )
    }
}
