package com.dayscounter.ui.screen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.ui.screen.components.createEditFormContent
import com.dayscounter.ui.screen.components.createEditTopAppBar
import com.dayscounter.ui.screen.components.datePickerDialogSection
import com.dayscounter.ui.screen.components.rememberCreateEditUiStates
import com.dayscounter.viewmodel.CreateEditScreenState
import com.dayscounter.viewmodel.CreateEditScreenViewModel

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
    val uiStates = rememberCreateEditUiStates()
    val showDatePicker = remember { mutableStateOf(false) }

    // Загружаем данные при редактировании
    com.dayscounter.ui.screen.components.loadItemData(itemId, uiState, uiStates)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            createEditTopAppBar(itemId = itemId, onBackClick = onBackClick)
        },
    ) { paddingValues ->
        createEditFormContent(
            itemId = itemId,
            paddingValues = paddingValues,
            uiStates = uiStates,
            showDatePicker = showDatePicker,
            viewModel = viewModel,
            onBackClick = onBackClick,
        )
    }

    // DatePicker Dialog
    if (showDatePicker.value) {
        datePickerDialogSection(
            selectedDate = uiStates.selectedDate,
            showDatePicker = showDatePicker,
        )
    }
}
