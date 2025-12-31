package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.viewmodel.CreateEditScreenViewModel
import com.dayscounter.ui.screen.CreateEditUiState as ScreenCreateEditUiState

/**
 * Кнопки действий.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun buttonsSection(
    uiStates: ScreenCreateEditUiState,
    itemId: Long?,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
) {
    // Кнопки
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        OutlinedButton(onClick = onBackClick) {
            Text(stringResource(R.string.cancel))
        }
        saveButton(
            uiStates = uiStates,
            itemId = itemId,
            viewModel = viewModel,
            onBackClick = onBackClick,
        )
    }
}

/**
 * Кнопка сохранения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun saveButton(
    uiStates: ScreenCreateEditUiState,
    itemId: Long?,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
) {
    Button(
        onClick = {
            if (uiStates.title.value.isNotEmpty() && uiStates.selectedDate.value != null) {
                val timestamp =
                    uiStates.selectedDate.value
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli() ?: System.currentTimeMillis()

                val item =
                    com.dayscounter.domain.model.Item(
                        id = itemId ?: 0L,
                        title = uiStates.title.value,
                        details = uiStates.details.value,
                        timestamp = timestamp,
                        colorTag = uiStates.selectedColor.value?.toArgb(),
                        displayOption = uiStates.selectedDisplayOption.value,
                    )

                if (itemId != null) {
                    viewModel.updateItem(item.copy(id = itemId))
                } else {
                    viewModel.createItem(item)
                }
                onBackClick()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        enabled = uiStates.title.value.isNotEmpty() && uiStates.selectedDate.value != null,
    ) {
        Text(stringResource(R.string.save))
    }
}

/**
 * DatePicker Dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun datePickerDialogSection(
    selectedDate: MutableState<java.time.LocalDate?>,
    showDatePicker: MutableState<Boolean>,
) {
    if (showDatePicker.value) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    selectedDate.value
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli(),
            )

        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate.value =
                                java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                        }
                        showDatePicker.value = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
            )
        }
    }
}
