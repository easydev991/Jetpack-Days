package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.MutableState
import com.dayscounter.viewmodel.CreateEditScreenViewModel

/**
 * Параметры для формы создания/редактирования события.
 */
data class CreateEditFormParams(
    val itemId: Long?,
    val paddingValues: PaddingValues,
    val uiStates: CreateEditUiState,
    val showDatePicker: MutableState<Boolean>,
    val viewModel: CreateEditScreenViewModel,
    val onBackClick: () -> Unit,
)
