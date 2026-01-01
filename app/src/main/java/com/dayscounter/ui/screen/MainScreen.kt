package com.dayscounter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.ui.component.listItemView
import com.dayscounter.ui.util.NumberFormattingUtils
import com.dayscounter.viewmodel.MainScreenState
import com.dayscounter.viewmodel.MainScreenViewModel
import kotlinx.coroutines.launch

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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(
    modifier: Modifier = Modifier,
    onItemClick: (Long) -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    onCreateClick: () -> Unit = {},
) {
    val viewModel: MainScreenViewModel =
        viewModel(
            factory =
                MainScreenViewModel.factory(
                    com.dayscounter.di.AppModule.createItemRepository(
                        com.dayscounter.data.database.DaysDatabase.getDatabase(
                            androidx.compose.ui.platform.LocalContext.current.applicationContext,
                        ),
                    ),
                ),
        )
    mainScreenContent(
        modifier = modifier,
        viewModel = viewModel,
        onItemClick = onItemClick,
        onEditClick = onEditClick,
        onCreateClick = onCreateClick,
    )
}

/**
 * Основной контент экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainScreenContent(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    onCreateClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val itemsCount by viewModel.itemsCount.collectAsState()
    val listState = rememberLazyListState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            mainScreenTopBar(
                state =
                    MainScreenTopBarState(
                        isSearchActive = isSearchActive,
                        searchQuery = searchQuery,
                        itemsCount = itemsCount,
                        sortOrder = sortOrder,
                        viewModel = viewModel,
                        onSearchActiveChange = { isSearchActive = it },
                        onSortOrderChange = { viewModel.updateSortOrder(it) },
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate to create item screen
                    onCreateClick()
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_item),
                )
            }
        },
    ) { paddingValues ->
        mainScreenContentByState(
            state =
                MainScreenContentState(
                    uiState = uiState,
                    searchQuery = searchQuery,
                    listState = listState,
                    onItemClick = onItemClick,
                    onEditClick = onEditClick,
                    viewModel = viewModel,
                ),
            paddingValues = paddingValues,
        )
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            title = { Text(stringResource(R.string.delete_item_title)) },
            text = {
                Text(stringResource(R.string.delete_item_message, showDeleteDialog!!.title))
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.confirmDelete() },
                ) {
                    Text(stringResource(R.string.delete_item_confirm))
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.cancelDelete() },
                ) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

/**
 * Меню сортировки.
 */
@Composable
private fun sortMenu(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.sort_24px),
                contentDescription = stringResource(R.string.sort),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.old_first)) },
                onClick = {
                    onSortOrderChange(SortOrder.ASCENDING)
                    expanded = false
                },
                trailingIcon = {
                    if (sortOrder == SortOrder.ASCENDING) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    }
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.new_first)) },
                onClick = {
                    onSortOrderChange(SortOrder.DESCENDING)
                    expanded = false
                },
                trailingIcon = {
                    if (sortOrder == SortOrder.DESCENDING) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                        )
                    }
                },
            )
        }
    }
}

/**
 * Контент с пустым списком.
 */
@Composable
private fun emptyContent(paddingValues: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.spacing_huge)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.what_should_we_remember),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        Text(
            text = stringResource(R.string.create_your_first_item),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Контент с пустым результатом поиска.
 */
@Composable
private fun emptySearchContent(paddingValues: androidx.compose.foundation.layout.PaddingValues = androidx.compose.foundation.layout.PaddingValues()) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.spacing_huge)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.no_results_found),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        Text(
            text = stringResource(R.string.try_different_search_terms),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Контент при загрузке.
 */
@Composable
private fun loadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Контент со списком записей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun itemsListContent(
    items: List<com.dayscounter.domain.model.Item>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = paddingValues,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            val dismissBoxState =
                rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        when (value) {
                            SwipeToDismissBoxValue.StartToEnd -> {
                                // Swipe right to edit
                                onEditClick(item.id)
                            }

                            SwipeToDismissBoxValue.EndToStart -> {
                                // Swipe left to delete
                                coroutineScope.launch {
                                    viewModel.requestDelete(item)
                                }
                            }

                            else -> {}
                        }
                        true
                    },
                )

            SwipeToDismissBox(
                state = dismissBoxState,
                backgroundContent = {
                    val direction = dismissBoxState.dismissDirection
                    val color =
                        when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                            SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                            else -> Color.Transparent
                        }
                    val alignment =
                        when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    val icon =
                        when (direction) {
                            SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Edit
                            SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                            else -> Icons.Filled.Delete
                        }

                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                        contentAlignment = alignment,
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                },
                content = {
                    val eventDate =
                        java.time.LocalDateTime
                            .ofInstant(
                                java.time.Instant.ofEpochMilli(item.timestamp),
                                java.time.ZoneId.systemDefault(),
                            ).toLocalDate()

                    val currentDate = java.time.LocalDate.now()
                    val daysSince =
                        java.time.temporal.ChronoUnit.DAYS
                            .between(eventDate, currentDate)
                            .toInt()

                    listItemView(
                        item = item,
                        formattedDaysText = NumberFormattingUtils.formatDaysCount(daysSince),
                        onClick = { onItemClick(it.id) },
                    )
                },
                enableDismissFromStartToEnd = true,
                enableDismissFromEndToStart = true,
            )
        }
    }
}

/**
 * Swipe to dismiss wrapper for a single item.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun itemSwipeToDismiss(
    item: com.dayscounter.domain.model.Item,
    dismissBoxState: androidx.compose.material3.SwipeToDismissBoxState,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
) {
    SwipeToDismissBox(
        state = dismissBoxState,
        backgroundContent = {
            val direction = dismissBoxState.dismissDirection
            val color =
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                    else -> Color.Transparent
                }
            val alignment =
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.Center
                }
            val icon =
                when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Edit
                    SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                    else -> Icons.Filled.Delete
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(color)
                        .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                contentAlignment = alignment,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                )
            }
        },
        content = {
            val eventDate =
                java.time.LocalDateTime
                    .ofInstant(
                        java.time.Instant.ofEpochMilli(item.timestamp),
                        java.time.ZoneId.systemDefault(),
                    ).toLocalDate()

            val currentDate = java.time.LocalDate.now()
            val daysSince =
                java.time.temporal.ChronoUnit.DAYS
                    .between(eventDate, currentDate)
                    .toInt()

            listItemView(
                item = item,
                formattedDaysText = NumberFormattingUtils.formatDaysCount(daysSince),
                onClick = { onItemClick(it.id) },
            )
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
    )
}

/**
 * Контент при ошибке.
 */
@Composable
private fun errorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacing_huge)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Swipe to dismiss wrapper for a single item.
 */
@OptIn(ExperimentalMaterial3Api::class)

/**
 * Data class for parameters of the main screen content by state.
 */
private data class MainScreenContentState(
    val uiState: MainScreenState,
    val searchQuery: String,
    val listState: androidx.compose.foundation.lazy.LazyListState,
    val onItemClick: (Long) -> Unit,
    val onEditClick: (Long) -> Unit,
    val viewModel: MainScreenViewModel,
)

/**
 * Displays content based on the UI state.
 */
@Composable
private fun mainScreenContentByState(
    state: MainScreenContentState,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
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
                    items = uiState.items,
                    listState = state.listState,
                    onItemClick = state.onItemClick,
                    onEditClick = state.onEditClick,
                    viewModel = state.viewModel,
                    paddingValues = paddingValues,
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

/**
 * Data class for parameters of the main screen top bar.
 */
private data class MainScreenTopBarState(
    val isSearchActive: Boolean,
    val searchQuery: String,
    val itemsCount: Int,
    val sortOrder: SortOrder,
    val viewModel: MainScreenViewModel,
    val onSearchActiveChange: (Boolean) -> Unit,
    val onSortOrderChange: (SortOrder) -> Unit,
)

/**
 * Top bar for the main screen with search and sort functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainScreenTopBar(state: MainScreenTopBarState) {
    if (state.isSearchActive) {
        SearchBar(
            query = state.searchQuery,
            onQueryChange = { state.viewModel.updateSearchQuery(it) },
            onSearch = { },
            active = state.isSearchActive,
            onActiveChange = { state.onSearchActiveChange(it) },
            placeholder = { Text(stringResource(R.string.search)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                IconButton(onClick = {
                    state.onSearchActiveChange(false)
                    state.viewModel.updateSearchQuery("")
                }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            content = { },
        )
    } else {
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
            actions = {
                IconButton(onClick = { state.onSearchActiveChange(true) }) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = stringResource(R.string.search),
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
}
