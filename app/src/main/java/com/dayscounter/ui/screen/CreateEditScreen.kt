package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.ui.screen.components.createedit.CreateEditFormParams
import com.dayscounter.ui.screen.components.createedit.createEditFormContent
import com.dayscounter.ui.screen.components.createedit.createEditTopAppBar
import com.dayscounter.ui.screen.components.createedit.datePickerDialogSection
import com.dayscounter.ui.screen.components.createedit.loadItemData
import com.dayscounter.ui.screen.components.createedit.rememberCreateEditUiStates
import com.dayscounter.ui.theme.jetpackDaysTheme
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
    loadItemData(itemId, uiState, uiStates)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            createEditTopAppBar(itemId = itemId, onBackClick = onBackClick)
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
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Экран создания новой записи")
@Composable
fun createEditScreenNewPreview() {
    jetpackDaysTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Preview для CreateEditScreen (создание)")
        }
    }
}

@Preview(showBackground = true, name = "Экран редактирования записи")
@Composable
fun createEditScreenEditPreview() {
    jetpackDaysTheme {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Preview для CreateEditScreen (редактирование)")
        }
    }
}
