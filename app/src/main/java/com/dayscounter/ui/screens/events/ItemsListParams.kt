package com.dayscounter.ui.screens.events

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.viewmodel.MainScreenViewModel

/**
 * Параметры для списка элементов.
 */
data class ItemsListParams(
    val items: List<Item>,
    val listState: LazyListState,
    val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    val onItemClick: (Long) -> Unit,
    val onEditClick: (Long) -> Unit,
    val viewModel: MainScreenViewModel,
    val paddingValues: PaddingValues,
)
