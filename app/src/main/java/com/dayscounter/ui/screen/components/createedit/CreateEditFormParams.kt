package com.dayscounter.ui.screen.components.createedit

import androidx.compose.runtime.MutableState
import com.dayscounter.viewmodel.CreateEditScreenViewModel
import com.dayscounter.ui.screen.CreateEditUiState as ScreenCreateEditUiState

/**
 * Параметры для формы создания/редактирования события.
 */
data class CreateEditFormParams(
    val itemId: Long?,
    val paddingValues: androidx.compose.foundation.layout.PaddingValues,
    val uiStates: ScreenCreateEditUiState,
    val showDatePicker: MutableState<Boolean>,
    val viewModel: CreateEditScreenViewModel,
    val onBackClick: () -> Unit,
)
