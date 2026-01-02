package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.data.database.DaysDatabase.Companion.getDatabase
import com.dayscounter.di.AppModule.createItemRepository
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.component.listItemView
import com.dayscounter.viewmodel.MainScreenState
import com.dayscounter.viewmodel.MainScreenViewModel

// Минимальное количество записей для отображения поля поиска
private const val MIN_ITEMS_FOR_SEARCH = 5

/**
 * Главный экран со списком событий.
 *
 * Отображает все события из базы данных с количеством прошедших дней.
 * Использует [MainScreenViewModel] для управления состоянием.
 *
 * @param modifier Modifier для экрана
 * @param onItemClick Обработчик клика по событию
 * @param onEditClick Обработчик клика на редактирование
 * @param onCreateClick Обработчик клика на создание новой записи
 */
@Composable
fun mainScreen(
    modifier: Modifier = Modifier,
    onItemClick: (Long) -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    onCreateClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel =
        viewModel(
            factory =
                MainScreenViewModel.factory(
                    createItemRepository(
                        getDatabase(
                            context.applicationContext,
                        ),
                    ),
                ),
        )
    // Создаем use cases для форматирования
    val resourceProvider =
        com.dayscounter.di.FormatterModule
            .createResourceProvider(context)
    val daysFormatter =
        com.dayscounter.di.FormatterModule
            .createDaysFormatter()
    val formatDaysTextUseCase = FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        GetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
            formatDaysTextUseCase = formatDaysTextUseCase,
            resourceProvider = resourceProvider,
        )

    mainScreenContent(
        params =
            MainScreenParams(
                viewModel = viewModel,
                getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
                onItemClick = onItemClick,
                onEditClick = onEditClick,
                onCreateClick = onCreateClick,
            ),
        modifier = modifier,
    )
}

/**
 * Заголовок экрана (TopBar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun screenHeader(
    itemsCount: Int,
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    mainScreenTopBar(
        state =
            MainScreenTopBarState(
                itemsCount = itemsCount,
                sortOrder = sortOrder,
                onSortOrderChange = onSortOrderChange,
            ),
    )
}

/**
 * Тело экрана со списком и полем поиска.
 */
@Composable
private fun screenBody(
    searchQuery: String,
    itemsCount: Int,
    paddingValues: PaddingValues,
    state: MainScreenContentState,
    onSearchQueryChange: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        val showSearchField = searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH
        if (showSearchField) {
            searchField(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        mainScreenContentByState(
            state = state,
            paddingValues =
                PaddingValues(
                    top = if (showSearchField) 0.dp else paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = paddingValues.calculateBottomPadding(),
                ),
        )
    }
}

/**
 * Диалог подтверждения удаления.
 */
@Composable
private fun deleteDialog(
    item: com.dayscounter.domain.model.Item,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_item_title)) },
        text = {
            Text(stringResource(R.string.delete_item_message, item.title))
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = onConfirm,
            ) {
                Text(stringResource(R.string.delete_item_confirm))
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(
                onClick = onCancel,
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

/**
 * Основной контент экрана.
 */
@Composable
private fun mainScreenContent(
    params: MainScreenParams,
    modifier: Modifier = Modifier,
) {
    val uiState by params.viewModel.uiState.collectAsState()
    val searchQuery by params.viewModel.searchQuery.collectAsState()
    val sortOrder by params.viewModel.sortOrder.collectAsState()
    val itemsCount by params.viewModel.itemsCount.collectAsState()
    val listState = rememberLazyListState()
    val showDeleteDialog by params.viewModel.showDeleteDialog.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            screenHeader(
                itemsCount = itemsCount,
                sortOrder = sortOrder,
                onSortOrderChange = { params.viewModel.updateSortOrder(it) },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = params.onCreateClick,
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_item),
                )
            }
        },
    ) { paddingValues ->
        screenBody(
            searchQuery = searchQuery,
            itemsCount = itemsCount,
            paddingValues = paddingValues,
            state =
                MainScreenContentState(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    listState = listState,
                    getFormattedDaysForItemUseCase = params.getFormattedDaysForItemUseCase,
                    onItemClick = params.onItemClick,
                    onEditClick = params.onEditClick,
                    viewModel = params.viewModel,
                ),
            onSearchQueryChange = { params.viewModel.updateSearchQuery(it) },
        )
    }

    showDeleteDialog?.let { item ->
        deleteDialog(
            item = item,
            onConfirm = { params.viewModel.confirmDelete() },
            onCancel = { params.viewModel.cancelDelete() },
        )
    }
}

/**
 * Обертка для элемента списка с позиционированием.
 */
@Composable
private fun listItemWrapper(
    item: com.dayscounter.domain.model.Item,
    formattedDaysText: String,
    onItemClick: (Long) -> Unit,
    onLongClick: (Offset, Offset) -> Unit,
    isSelected: Boolean,
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier =
            Modifier
                .onGloballyPositioned { coordinates ->
                    itemPosition = coordinates.positionInRoot()
                },
    ) {
        listItemView(
            params =
                com.dayscounter.ui.component.ListItemParams(
                    item = item,
                    formattedDaysText = formattedDaysText,
                    onClick = { onItemClick(item.id) },
                    onLongClick = { localOffset -> onLongClick(localOffset, itemPosition) },
                    isSelected = isSelected,
                ),
        )
    }
}

/**
 * Контекстное меню для элемента списка.
 */
@Composable
private fun contextMenu(params: ContextMenuParams) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = params.onDismiss,
        offset = params.menuOffset,
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.context_menu_view)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                )
            },
            onClick = {
                params.onDismiss()
                params.onItemClick(params.item.id)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.context_menu_edit)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                )
            },
            onClick = {
                params.onDismiss()
                params.onEditClick(params.item.id)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.context_menu_delete)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null,
                )
            },
            onClick = {
                params.onDismiss()
                params.onDeleteClick(params.item)
            },
        )
    }
}

/**
 * Контент со списком записей.
 */
@Composable
private fun itemsListContent(params: ItemsListParams) {
    var contextMenuItem by remember { mutableStateOf<com.dayscounter.domain.model.Item?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = params.listState,
        contentPadding = params.paddingValues,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        items(
            items = params.items,
            key = { it.id },
        ) { item ->
            val formattedDaysText = params.getFormattedDaysForItemUseCase(item = item)
            listItemWrapper(
                item = item,
                formattedDaysText = formattedDaysText,
                onItemClick = params.onItemClick,
                onLongClick = { localOffset, itemPosition ->
                    contextMenuItem = item
                    menuOffset =
                        with(density) {
                            DpOffset(
                                (itemPosition.x + localOffset.x).toDp(),
                                (itemPosition.y + localOffset.y).toDp(),
                            )
                        }
                },
                isSelected = contextMenuItem?.id == item.id,
            )
        }
    }

    contextMenuItem?.let { item ->
        contextMenu(
            params =
                ContextMenuParams(
                    item = item,
                    menuOffset = menuOffset,
                    onDismiss = { contextMenuItem = null },
                    onItemClick = params.onItemClick,
                    onEditClick = params.onEditClick,
                    onDeleteClick = { params.viewModel.requestDelete(it) },
                ),
        )
    }
}

/**
 * Data class for parameters of main screen top bar.
 */
private data class MainScreenTopBarState(
    val itemsCount: Int,
    val sortOrder: SortOrder,
    val onSortOrderChange: (SortOrder) -> Unit,
)

/**
 * Top bar for main screen with sort functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainScreenTopBar(state: MainScreenTopBarState) {
    TopAppBar(
        title = { Text(stringResource(R.string.events)) },
        navigationIcon = {
            if (state.itemsCount > 1) {
                sortMenu(
                    sortOrder = state.sortOrder,
                    onSortOrderChange = state.onSortOrderChange,
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
    )
}

/**
 * Data class for parameters of main screen content by state.
 */
private data class MainScreenContentState(
    val uiState: MainScreenState,
    val searchQuery: String,
    val listState: androidx.compose.foundation.lazy.LazyListState,
    val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    val onItemClick: (Long) -> Unit,
    val onEditClick: (Long) -> Unit,
    val viewModel: MainScreenViewModel,
)

/**
 * Displays content based on UI state.
 */
@Composable
private fun mainScreenContentByState(
    state: MainScreenContentState,
    paddingValues: PaddingValues,
) {
    when (val uiState = state.uiState) {
        is MainScreenState.Loading -> {
            loadingContent(modifier = Modifier.padding(paddingValues))
        }

        is MainScreenState.Success -> {
            if (uiState.items.isEmpty()) {
                if (state.searchQuery.isNotEmpty()) {
                    emptySearchContent(paddingValues)
                } else {
                    emptyContent(paddingValues)
                }
            } else {
                itemsListContent(
                    params =
                        ItemsListParams(
                            items = uiState.items,
                            listState = state.listState,
                            getFormattedDaysForItemUseCase = state.getFormattedDaysForItemUseCase,
                            onItemClick = state.onItemClick,
                            onEditClick = state.onEditClick,
                            viewModel = state.viewModel,
                            paddingValues = paddingValues,
                        ),
                )
            }
        }

        is MainScreenState.Error -> {
            errorContent(
                message = uiState.message,
                modifier = Modifier.padding(paddingValues),
            )
        }
    }
}
