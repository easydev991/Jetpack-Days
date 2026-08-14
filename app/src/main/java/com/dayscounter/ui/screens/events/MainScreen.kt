package com.dayscounter.ui.screens.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.UserActionType
import com.dayscounter.data.database.DaysDatabase.Companion.getDatabase
import com.dayscounter.data.preferences.createAppSettingsDataStore
import com.dayscounter.di.AppModule.createItemRepository
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.ds.ListItemParams
import com.dayscounter.ui.ds.ListItemView
import com.dayscounter.ui.viewmodel.MainScreenState
import com.dayscounter.ui.viewmodel.MainScreenViewModel

// Минимальное количество записей для отображения поля поиска
private const val MIN_ITEMS_FOR_SEARCH = 5

// Длительность анимации появления/скрытия поля поиска
private const val SEARCH_FIELD_ANIMATION_DURATION_MS = 200

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
fun MainScreen(
    modifier: Modifier = Modifier,
    onItemClick: (Long) -> Unit = {},
    onEditClick: (Long) -> Unit = {},
    onCreateClick: () -> Unit = {},
    analyticsService: AnalyticsService
) {
    val context = LocalContext.current
    val viewModel: MainScreenViewModel =
        viewModel(
            factory =
                MainScreenViewModel.factory(
                    createItemRepository(
                        getDatabase(
                            context.applicationContext
                        )
                    ),
                    createAppSettingsDataStore(context.applicationContext)
                )
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
            resourceProvider = resourceProvider
        )

    MainScreenContent(
        params =
            MainScreenParams(
                viewModel = viewModel,
                getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
                onItemClick = onItemClick,
                onEditClick = onEditClick,
                onCreateClick = onCreateClick,
                analyticsService = analyticsService
            ),
        modifier = modifier
    )
}

/**
 * Заголовок экрана (TopBar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScreenHeader(state: MainScreenTopBarState) {
    MainScreenTopBar(state = state)
}

/**
 * Тело экрана со списком и полем поиска.
 *
 * Верхний паддинг TopAppBar вынесен на сам [Column], чтобы высота поля поиска
 * изменялась плавно внутри [AnimatedVisibility] без перерасчёта паддингов у списка.
 */
@Composable
internal fun ScreenBody(
    searchQuery: String,
    itemsCount: Int,
    paddingValues: PaddingValues,
    state: MainScreenContentState,
    onSearchQueryChange: (String) -> Unit
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
                    end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
                    bottom = 0.dp
                )
    ) {
        val showSearchField = searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH
        AnimatedVisibility(
            visible = showSearchField,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(SEARCH_FIELD_ANIMATION_DURATION_MS)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(SEARCH_FIELD_ANIMATION_DURATION_MS))
        ) {
            SearchField(
                searchQuery = searchQuery,
                onSearchQueryChange = onSearchQueryChange,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
            )
        }
        MainScreenContentByState(
            state = state,
            paddingValues =
                PaddingValues(
                    top = 0.dp,
                    start = 0.dp,
                    end = 0.dp,
                    bottom = paddingValues.calculateBottomPadding()
                )
        )
    }
}

/**
 * Диалог подтверждения удаления.
 */
@Composable
internal fun DeleteDialog(
    item: com.dayscounter.domain.model.Item,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(R.string.delete_item_title)) },
        text = {
            Text(stringResource(R.string.delete_item_message, item.title))
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
            ) {
                Text(stringResource(R.string.delete_item_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

/**
 * Основной контент экрана.
 */
@Composable
private fun MainScreenContent(
    params: MainScreenParams,
    modifier: Modifier = Modifier
) {
    val uiState by params.viewModel.uiState.collectAsState()
    val searchQuery by params.viewModel.searchQuery.collectAsState()
    val sortOrder by params.viewModel.sortOrder.collectAsState()
    val itemsCount by params.viewModel.itemsCount.collectAsState()
    val listState = rememberLazyListState()
    val showDeleteDialog by params.viewModel.showDeleteDialog.collectAsState()
    val showFilterDialog by params.viewModel.showFilterDialog.collectAsState()
    val availableColorTags by params.viewModel.availableColorTags.collectAsState()
    val selectedColorTag by params.viewModel.selectedColorTag.collectAsState()

    MainScreenScaffold(
        state =
            MainScreenScaffoldState(
                uiState = uiState,
                searchQuery = searchQuery,
                sortOrder = sortOrder,
                itemsCount = itemsCount,
                listState = listState,
                availableColorTags = availableColorTags,
                selectedColorTag = selectedColorTag
            ),
        params = params,
        modifier = modifier
    )

    MainScreenDialogs(
        state =
            MainScreenDialogsState(
                showDeleteDialog = showDeleteDialog,
                showFilterDialog = showFilterDialog,
                availableColorTags = availableColorTags,
                selectedColorTag = selectedColorTag,
                onDeleteConfirm = {
                    params.analyticsService.log(AnalyticsEvent.UserAction(UserActionType.DELETE))
                    params.viewModel.confirmDelete()
                },
                onDeleteCancel = { params.viewModel.cancelDelete() },
                onFilterApply = { colorTag ->
                    params.viewModel.updateSelectedColorTag(colorTag)
                    params.viewModel.toggleFilterDialog()
                },
                onFilterDismiss = { params.viewModel.toggleFilterDialog() }
            )
    )
}

/**
 * Обертка для элемента списка с позиционированием.
 */
@Composable
private fun ListItemWrapper(
    item: com.dayscounter.domain.model.Item,
    formattedDaysText: String,
    onItemClick: (Long) -> Unit,
    onLongClick: (Offset, Offset) -> Unit,
    isSelected: Boolean
) {
    var itemPosition by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier =
            Modifier
                .onGloballyPositioned { coordinates ->
                    itemPosition = coordinates.positionInRoot()
                }
    ) {
        ListItemView(
            params =
                ListItemParams(
                    item = item,
                    formattedDaysText = formattedDaysText,
                    onClick = { onItemClick(item.id) },
                    onLongClick = { localOffset -> onLongClick(localOffset, itemPosition) },
                    isSelected = isSelected
                )
        )
    }
}

/**
 * Контекстное меню для элемента списка.
 */
@Composable
private fun ContextMenu(params: ContextMenuParams) {
    DropdownMenu(
        expanded = true,
        onDismissRequest = params.onDismiss,
        offset = params.menuOffset
    ) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.context_menu_view)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null
                )
            },
            onClick = {
                params.onDismiss()
                params.onItemClick(params.item.id)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.context_menu_edit)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )
            },
            onClick = {
                params.onDismiss()
                params.onEditClick(params.item.id)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.context_menu_delete)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = null
                )
            },
            onClick = {
                params.onDismiss()
                params.onDeleteClick(params.item)
            }
        )
    }
}

/**
 * Контент со списком записей.
 */
@Composable
private fun ItemsListContent(params: ItemsListParams) {
    var contextMenuItem by remember { mutableStateOf<com.dayscounter.domain.model.Item?>(null) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = params.listState,
        contentPadding = params.paddingValues,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        items(
            items = params.items,
            key = { it.id }
        ) { item ->
            val formattedDaysText =
                params.getFormattedDaysForItemUseCase(item = item, showMinus = true)
            ListItemWrapper(
                item = item,
                formattedDaysText = formattedDaysText,
                onItemClick = params.onItemClick,
                onLongClick = { localOffset, itemPosition ->
                    contextMenuItem = item
                    menuOffset =
                        with(density) {
                            DpOffset(
                                (itemPosition.x + localOffset.x).toDp(),
                                (itemPosition.y + localOffset.y).toDp()
                            )
                        }
                },
                isSelected = contextMenuItem?.id == item.id
            )
        }
    }

    contextMenuItem?.let { item ->
        ContextMenu(
            params =
                ContextMenuParams(
                    item = item,
                    menuOffset = menuOffset,
                    onDismiss = { contextMenuItem = null },
                    onItemClick = params.onItemClick,
                    onEditClick = params.onEditClick,
                    onDeleteClick = { params.viewModel.requestDelete(it) }
                )
        )
    }
}

/**
 * Data class for parameters of main screen top bar.
 */
internal data class MainScreenTopBarState(
    val itemsCount: Int,
    val sortOrder: SortOrder,
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
private fun MainScreenTopBar(state: MainScreenTopBarState) {
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
            // Кнопка фильтра отображается только если есть достаточное количество записей и доступные цвета.
            // При активном фильтре оставляем кнопку видимой, чтобы пользователь мог сбросить фильтр.
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

/**
 * Data class for parameters of main screen content by state.
 */
internal data class MainScreenContentState(
    val uiState: MainScreenState,
    val searchQuery: String,
    val listState: androidx.compose.foundation.lazy.LazyListState,
    val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    val onItemClick: (Long) -> Unit,
    val onEditClick: (Long) -> Unit,
    val viewModel: MainScreenViewModel
)

/**
 * Displays content based on UI state.
 */
@Composable
private fun MainScreenContentByState(
    state: MainScreenContentState,
    paddingValues: PaddingValues
) {
    when (val uiState = state.uiState) {
        is MainScreenState.Loading -> {
            LoadingContent(modifier = Modifier.padding(paddingValues))
        }

        is MainScreenState.Success -> {
            if (uiState.items.isEmpty()) {
                if (state.searchQuery.isNotEmpty()) {
                    EmptySearchContent(paddingValues)
                } else {
                    EmptyContent(paddingValues)
                }
            } else {
                ItemsListContent(
                    params =
                        ItemsListParams(
                            items = uiState.items,
                            listState = state.listState,
                            getFormattedDaysForItemUseCase = state.getFormattedDaysForItemUseCase,
                            onItemClick = state.onItemClick,
                            onEditClick = state.onEditClick,
                            viewModel = state.viewModel,
                            paddingValues = paddingValues
                        )
                )
            }
        }

        is MainScreenState.Error -> {
            ErrorContent(
                message = uiState.message,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
