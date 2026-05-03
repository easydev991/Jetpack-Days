package com.dayscounter.ui.screens.createedit

import androidx.compose.ui.graphics.Color
import com.dayscounter.domain.model.DisplayOption
import java.time.LocalDate

/**
 * Состояние UI для экрана создания/редактирования.
 *
 * Plain data class без MutableState — иммутабельный контракт.
 * Мутация через copy().
 */
data class CreateEditUiState(
    val title: String = "",
    val details: String = "",
    val selectedDate: LocalDate? = null,
    val selectedColor: Color? = null,
    val selectedDisplayOption: DisplayOption = DisplayOption.DAY,
    val showDatePicker: Boolean = false,
    val reminder: ReminderFormUiState = ReminderFormUiState()
)
