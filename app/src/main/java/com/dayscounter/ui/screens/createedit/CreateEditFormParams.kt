package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.MutableState
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel

/**
 * Параметры для формы создания/редактирования события.
 */
data class CreateEditFormParams(
    val itemId: Long?,
    val paddingValues: androidx.compose.foundation.layout.PaddingValues,
    val uiStates: CreateEditUiState,
    val showDatePicker: MutableState<Boolean>,
    val viewModel: CreateEditScreenViewModel,
    val onBackClick: () -> Unit,
)
