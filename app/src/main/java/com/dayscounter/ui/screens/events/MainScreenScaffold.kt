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
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
 * Nested-scroll connection, растящий collapse поисковой строки при свайпе вверх
 * и возвращающий его при свайпе вниз. Пока активен поиск (`searchQuery` не пуст),
 * возвращает [Offset.Zero] — поле не должно уезжать во время набора текста.
 *
 * [searchFieldHeightPx] нужен для клампа: collapse не превышает реальную высоту поля.
 */
@Composable
private fun rememberSearchBarCollapseConnection(
    collapseState: MutableFloatState,
    searchFieldHeightPx: Int,
    searchQuery: String
): NestedScrollConnection =
    remember(searchFieldHeightPx, searchQuery) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (searchQuery.isNotEmpty()) return Offset.Zero
                // swipe up → available.y<0; вычитаем, чтобы растить collapse.
                val newOffset =
                    (collapseState.floatValue - available.y)
                        .coerceIn(0f, searchFieldHeightPx.toFloat())
                val consumedY = newOffset - collapseState.floatValue
                collapseState.floatValue = newOffset
                return Offset(0f, consumedY)
            }
        }
    }

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
    var searchFieldHeightPx by remember { mutableIntStateOf(0) }
    val searchBarCollapsePx = rememberSaveable { mutableFloatStateOf(0f) }
    val nestedScrollConnection =
        rememberSearchBarCollapseConnection(
            collapseState = searchBarCollapsePx,
            searchFieldHeightPx = searchFieldHeightPx,
            searchQuery = state.searchQuery
        )
    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(nestedScrollConnection),
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
                        searchQuery = state.searchQuery,
                        onSearchQueryChange = { params.viewModel.updateSearchQuery(it) },
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
        floatingActionButton = { MainScreenFab(onCreateClick = params.onCreateClick) }
    ) { paddingValues ->
        ScreenBody(
            paddingValues = paddingValues,
            state =
                MainScreenContentState(
                    uiState = state.uiState,
                    searchQuery = state.searchQuery,
                    itemsCount = state.itemsCount,
                    onSearchQueryChange = { params.viewModel.updateSearchQuery(it) },
                    onSearchFieldHeightPxChange = { searchFieldHeightPx = it },
                    searchBarCollapsePx = searchBarCollapsePx.floatValue,
                    listState = state.listState,
                    getFormattedDaysForItemUseCase = params.getFormattedDaysForItemUseCase,
                    onItemClick = params.onItemClick,
                    onEditClick = params.onEditClick,
                    viewModel = params.viewModel
                )
        )
    }
}

/**
 * FAB создания новой записи.
 */
@Composable
private fun MainScreenFab(onCreateClick: () -> Unit) {
    FloatingActionButton(onClick = onCreateClick) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.add_item)
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
