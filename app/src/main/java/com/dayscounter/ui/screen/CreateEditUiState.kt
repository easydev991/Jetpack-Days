package com.dayscounter.ui.screen

import androidx.compose.runtime.MutableState
import androidx.compose.ui.graphics.Color
import com.dayscounter.domain.model.DisplayOption

/**
 * Состояние UI для экрана создания/редактирования.
 */
data class CreateEditUiState(
    val title: MutableState<String>,
    val details: MutableState<String>,
    val selectedDate: MutableState<java.time.LocalDate?>,
    val selectedColor: MutableState<Color?>,
    val selectedDisplayOption: MutableState<DisplayOption>,
)
