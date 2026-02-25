package com.dayscounter.ui.screens.events

import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.viewmodel.MainScreenViewModel

/**
 * Параметры для главного экрана.
 */
data class MainScreenParams(
    val viewModel: MainScreenViewModel,
    val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    val onItemClick: (Long) -> Unit,
    val onEditClick: (Long) -> Unit,
    val onCreateClick: () -> Unit,
)
