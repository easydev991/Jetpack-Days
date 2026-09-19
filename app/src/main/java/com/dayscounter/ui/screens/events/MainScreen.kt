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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
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
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.ds.ListItemParams
import com.dayscounter.ui.ds.ListItemView
import com.dayscounter.ui.viewmodel.MainScreenState
import com.dayscounter.ui.viewmodel.MainScreenViewModel
import kotlin.math.roundToInt

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
@Composable
internal fun ScreenHeader(state: MainScreenTopBarState) {
    MainScreenTopBar(state = state)
}

/**
 * Тело экрана со списком событий.
 *
 * SearchField сворачивается при скролле вверх через [CollapsibleSearchField].
 * [NestedScrollConnection] живёт в `MainScreenScaffold` на самом `Scaffold` —
 * это родитель `LazyColumn` через `content`-слот, pre-scroll от списка доходит до колбэка.
 * Scaffold уже резервирует место под topbar через [paddingValues].
 */
@Composable
internal fun ScreenBody(
    paddingValues: PaddingValues,
    state: MainScreenContentState
) {
    val showSearchField =
        state.searchQuery.isNotEmpty() ||
            state.itemsCount >= MIN_ITEMS_FOR_SEARCH
    // Верхний паддинг TopAppBar вынесен на сам Column, чтобы поле поиска жило НИЖЕ topbar
    // (иначе рендерится за ним — семантика есть, пиксели перекрыты), а высота поля менялась
    // внутри AnimatedVisibility без пересчёта contentPadding у списка.
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
        AnimatedVisibility(
            visible = showSearchField,
            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(SEARCH_FIELD_ANIMATION_DURATION_MS)),
            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(SEARCH_FIELD_ANIMATION_DURATION_MS))
        ) {
            CollapsibleSearchField(
                searchQuery = state.searchQuery,
                onSearchQueryChange = state.onSearchQueryChange,
                onSearchFieldHeightPxChange = state.onSearchFieldHeightPxChange,
                searchBarCollapsePx = state.searchBarCollapsePx
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
 * SearchField, сворачивающийся при скролле вверх через [NestedScrollConnection] + [Modifier.layout].
 *
 * Поведение:
 * - При скролле вверх — `onPreScroll` (в родительском [Column]) увеличивает `searchBarCollapsePx` →
 *   `Box` физически сжимается, освобождая место для [LazyColumn].
 * - При скролле вниз — обратный процесс, поле возвращается.
 * - При активном [searchQuery] — `onPreScroll` возвращает [Offset.Zero] (поле не уезжает при наборе).
 * - Состояние `searchBarCollapsePx` сохраняется через [rememberSaveable] для переживания rotation.
 *
 * Реализует UX-критерий «SearchField сворачивается при скролле вверх» без перевода экрана на
 * [androidx.compose.material3.MediumTopAppBar] (который ломает «title всегда в одной строке с
 * SortMenu и PaletteFilter» в [TopAppBar]).
 *
 * Механика: `clipToBounds()` стоит ВНЕ `Modifier.layout` — клип по свернувшейся высоте,
 * которую репортит layout. Контент размещается `placeRelative(0, -collapsePx)` — поле
 * физически уезжает вверх и срезается клипом, `graphicsLayer` не нужен.
 * Constraints передаются как есть (без `Constraints.Infinity`): внутри material3 `SearchBar` —
 * кастомный `Layout` (`SearchBarLayout`, `SearchBar.kt:2206`), который выводит фиксированную
 * высоту inputField через `constrainHeight(minIntrinsicHeight(...))` — с бесконечным
 * `maxHeight` клампа нет и измерение раздувается, поле перестаёт отрисовываться.
 * Высота репортится через [onSizeChanged] на дочернем [SearchField] — запись state внутри
 * measure-блока `layout` запрещена (сайд-эффект в фазе layout ломает exit-анимацию
 * [AnimatedVisibility]).
 *
 * @param onSearchFieldHeightPxChange колбэк реальной высоты поля в px (из [onSizeChanged]).
 *   Нужен в родительском [Column] для клампа `searchBarCollapsePx` в [NestedScrollConnection].
 * @param searchBarCollapsePx px свёрнутой части (0 = expanded, searchFieldHeightPx = collapsed).
 */
@Composable
private fun CollapsibleSearchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSearchFieldHeightPxChange: (Int) -> Unit,
    searchBarCollapsePx: Float
) {
    Box(
        Modifier
            .testTag("collapsibleSearchField")
            .clipToBounds()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                val collapsePx = searchBarCollapsePx.coerceIn(0f, placeable.height.toFloat())
                val visibleHeight = placeable.height - collapsePx.roundToInt()
                layout(placeable.width, visibleHeight) {
                    placeable.placeRelative(0, -collapsePx.roundToInt())
                }
            }
    ) {
        SearchField(
            searchQuery = searchQuery,
            onSearchQueryChange = onSearchQueryChange,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimensionResource(R.dimen.spacing_regular))
                    .onSizeChanged { onSearchFieldHeightPxChange(it.height) }
        )
    }
}

/**
 * Data class for parameters of main screen content by state.
 */
internal data class MainScreenContentState(
    val uiState: MainScreenState,
    val searchQuery: String,
    val itemsCount: Int,
    val onSearchQueryChange: (String) -> Unit,
    val onSearchFieldHeightPxChange: (Int) -> Unit,
    val searchBarCollapsePx: Float,
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
