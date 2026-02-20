package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.domain.model.Item
import com.dayscounter.ui.screen.components.createedit.CreateEditFormParams
import com.dayscounter.ui.screen.components.createedit.SaveButton
import com.dayscounter.ui.screen.components.createedit.createEditFormContent
import com.dayscounter.ui.screen.components.createedit.createEditTopAppBar
import com.dayscounter.ui.screen.components.createedit.datePickerDialogSection
import com.dayscounter.ui.screen.components.createedit.loadItemData
import com.dayscounter.ui.screen.components.createedit.rememberCreateEditUiStates
import com.dayscounter.viewmodel.CreateEditScreenViewModel
import java.time.ZoneId

/**
 * Экран создания/редактирования события.
 *
 * Использует [CreateEditScreenViewModel] для управления состоянием.
 *
 * @param itemId Идентификатор события (null для создания нового)
 * @param modifier Modifier для экрана
 * @param viewModel ViewModel для управления состоянием
 * @param onBackClick Обработчик клика "Назад"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createEditScreen(
    itemId: Long?,
    modifier: Modifier = Modifier,
    viewModel: CreateEditScreenViewModel = viewModel(),
    onBackClick: () -> Unit = {},
) {
    createEditScreenContent(
        itemId = itemId,
        modifier = modifier,
        viewModel = viewModel,
        onBackClick = onBackClick,
    )
}

/**
 * Основной контент экрана создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun createEditScreenContent(
    itemId: Long?,
    modifier: Modifier = Modifier,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    val uiStates = rememberCreateEditUiStates()
    val showDatePicker = rememberSaveable { mutableStateOf(false) }

    // Загружаем данные при редактировании
    loadItemData(itemId, uiState, uiStates)

    val isValidData = uiStates.title.value.isNotEmpty() && uiStates.selectedDate.value != null
    val isEditing = itemId != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            createEditTopAppBar(itemId = itemId, onBackClick = onBackClick)
        },
        bottomBar = {
            SaveButton(
                enabled = if (isEditing) isValidData && hasChanges else isValidData,
                onClick = {
                    if (isValidData) {
                        val timestamp =
                            uiStates.selectedDate.value
                                ?.atStartOfDay(ZoneId.systemDefault())
                                ?.toInstant()
                                ?.toEpochMilli() ?: System.currentTimeMillis()

                        val item =
                            Item(
                                id = itemId ?: 0L,
                                title = uiStates.title.value,
                                details = uiStates.details.value,
                                timestamp = timestamp,
                                colorTag = uiStates.selectedColor.value?.toArgb(),
                                displayOption = uiStates.selectedDisplayOption.value,
                            )

                        if (isEditing) {
                            viewModel.updateItem(item.copy(id = itemId))
                        } else {
                            viewModel.createItem(item)
                        }
                        onBackClick()
                    }
                },
            )
        },
    ) { paddingValues ->
        createEditFormContent(
            params =
                CreateEditFormParams(
                    itemId = itemId,
                    paddingValues = paddingValues,
                    uiStates = uiStates,
                    showDatePicker = showDatePicker,
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                ),
        )
    }

    // DatePicker Dialog
    if (showDatePicker.value) {
        datePickerDialogSection(
            selectedDate = uiStates.selectedDate,
            showDatePicker = showDatePicker,
            onDateSelected = {
                if (itemId != null) {
                    val timestamp =
                        uiStates.selectedDate.value
                            ?.atStartOfDay(ZoneId.systemDefault())
                            ?.toInstant()
                            ?.toEpochMilli() ?: 0L

                    viewModel.checkHasChanges(
                        title = uiStates.title.value,
                        details = uiStates.details.value,
                        timestamp = timestamp,
                        colorTag = uiStates.selectedColor.value?.toArgb(),
                        displayOption = uiStates.selectedDisplayOption.value,
                    )
                }
            },
        )
    }
}
