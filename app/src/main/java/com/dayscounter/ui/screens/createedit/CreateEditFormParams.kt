package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.graphics.Color
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel
import java.time.LocalDate

/**
 * Параметры для формы создания/редактирования события.
 *
 * Plain data class без MutableState.
 * Все мутации — через callback'и.
 */
data class CreateEditFormParams(
    val itemId: Long?,
    val paddingValues: PaddingValues,
    val uiStates: CreateEditUiState,
    val onShowDatePickerChange: (Boolean) -> Unit,
    val onTitleChange: (String) -> Unit,
    val onDetailsChange: (String) -> Unit,
    val onDateChange: (LocalDate?) -> Unit,
    val onColorChange: (Color?) -> Unit,
    val onDisplayOptionChange: (DisplayOption) -> Unit,
    val onReminderChange: (ReminderFormUiState) -> Unit,
    val viewModel: CreateEditScreenViewModel,
    val onBackClick: () -> Unit,
    val onReminderNotificationsUnavailable: () -> Unit
)
