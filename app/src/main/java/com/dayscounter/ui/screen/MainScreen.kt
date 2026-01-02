package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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
        modifier = modifier,
        viewModel = viewModel,
        getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
        onItemClick = onItemClick,
        onEditClick = onEditClick,
        onCreateClick = onCreateClick,
    )
}

/**
 * Основной контент экрана.
 */
@Composable
private fun mainScreenContent(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            mainScreenTopBar(
                state =
                    MainScreenTopBarState(
                        itemsCount = itemsCount,
                        sortOrder = sortOrder,
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
        Column(modifier = Modifier.fillMaxSize()) {
            // Поле поиска над списком (отображается, если есть текст в поиске ИЛИ есть 5+ записей)
            val showSearchField = searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH
            if (showSearchField) {
                searchField(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }

            // Список записей (адаптивный padding в зависимости от видимости поля поиска)
            mainScreenContentByState(
                state =
                    MainScreenContentState(
                        uiState = uiState,
                        searchQuery = searchQuery,
                        listState = listState,
                        getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
                        onItemClick = onItemClick,
                        onEditClick = onEditClick,
                        viewModel = viewModel,
                    ),
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
private fun emptyContent(paddingValues: PaddingValues = PaddingValues()) {
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
private fun emptySearchContent(paddingValues: PaddingValues = PaddingValues()) {
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
@Composable
private fun itemsListContent(
    items: List<com.dayscounter.domain.model.Item>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
    paddingValues: PaddingValues,
) {
    var contextMenuItem by remember { mutableStateOf<com.dayscounter.domain.model.Item?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var itemGlobalPosition by remember { mutableStateOf(Offset.Zero) }
    val density = LocalDensity.current

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
            var itemPosition by remember { mutableStateOf(Offset.Zero) }

            // Форматируем текст с учетом displayOption из Item
            val formattedDaysText = getFormattedDaysForItemUseCase(item = item)

            Box(
                modifier =
                    Modifier
                        .onGloballyPositioned { coordinates ->
                            itemPosition = coordinates.positionInRoot()
                        },
            ) {
                listItemView(
                    item = item,
                    formattedDaysText = formattedDaysText,
                    onClick = { onItemClick(it.id) },
                    onLongClick = { localOffset ->
                        contextMenuItem = item
                        itemGlobalPosition = itemPosition
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
    }

    // Контекстное меню
    contextMenuItem?.let { item ->
        DropdownMenu(
            expanded = true,
            onDismissRequest = { contextMenuItem = null },
            offset = menuOffset,
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
                    contextMenuItem = null
                    onItemClick(item.id)
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
                    contextMenuItem = null
                    onEditClick(item.id)
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
                    contextMenuItem = null
                    viewModel.requestDelete(item)
                },
            )
        }
    }
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
 * Поле поиска для фильтрации списка записей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun searchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(stringResource(R.string.search))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier.height(56.dp),
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
                    items = uiState.items,
                    listState = state.listState,
                    getFormattedDaysForItemUseCase = state.getFormattedDaysForItemUseCase,
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
