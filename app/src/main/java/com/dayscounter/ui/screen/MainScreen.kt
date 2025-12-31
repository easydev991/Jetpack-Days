package com.dayscounter.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(
    modifier: Modifier = Modifier,
    onItemClick: (Long) -> Unit = {},
    onEditClick: (Long) -> Unit = {},
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
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val itemsCount by viewModel.itemsCount.collectAsState()
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.events)) },
                navigationIcon = {
                    if (itemsCount > 1) {
                        SortMenu(
                            sortOrder = sortOrder,
                            onSortOrderChange = { viewModel.updateSortOrder(it) },
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.add_item),
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.updateSearchQuery("") },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_item),
                )
            }
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is MainScreenState.Loading -> {
                loadingContent(modifier = Modifier.padding(paddingValues))
            }
            is MainScreenState.Success -> {
                if (state.items.isEmpty()) {
                    if (searchQuery.isNotEmpty()) {
                        emptySearchContent()
                    } else {
                        emptyContent()
                    }
                } else {
                    itemsListContent(
                        items = state.items,
                        listState = listState,
                        onItemClick = onItemClick,
                        onEditClick = onEditClick,
                        viewModel = viewModel,
                    )
                }
            }
            is MainScreenState.Error -> {
                errorContent(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

/**
 * Меню сортировки.
 */
@Composable
private fun SortMenu(
    sortOrder: SortOrder,
    onSortOrderChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = Icons.Filled.Sort,
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
private fun emptyContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
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
private fun emptySearchContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
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
