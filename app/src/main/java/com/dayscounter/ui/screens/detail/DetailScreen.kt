package com.dayscounter.ui.screens.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.ui.viewmodel.DetailScreenState
import com.dayscounter.ui.viewmodel.DetailScreenViewModel

/**
 * Экран деталей события.
 *
 * Отображает полную информацию о событии с количеством прошедших дней.
 * Использует [DetailScreenViewModel] для управления состоянием.
 *
 * @param itemId Идентификатор события
 * @param modifier Modifier для экрана
 * @param viewModel ViewModel для управления состоянием
 * @param onBackClick Обработчик клика "Назад"
 * @param onEditClick Обработчик клика "Редактировать"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: Long,
    modifier: Modifier = Modifier,
    viewModel: DetailScreenViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
) {
    val context = LocalContext.current
    // Создаем use cases для форматирования
    val resourceProvider =
        com.dayscounter.di.FormatterModule
            .createResourceProvider(context)
    val daysFormatter =
        com.dayscounter.di.FormatterModule
            .createDaysFormatter()
    val formatDaysTextUseCase = FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        com.dayscounter.di.FormatterModule
            .createGetFormattedDaysForItemUseCase(
                calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                formatDaysTextUseCase = formatDaysTextUseCase,
                resourceProvider = resourceProvider,
            )
    val getDaysAnalysisTextUseCase =
        com.dayscounter.di.FormatterModule
            .createGetDaysAnalysisTextUseCase(
                calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
                resourceProvider = resourceProvider,
            )

    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    DetailScreenContent(
        params =
            DetailScreenParams(
                itemId = itemId,
                onBackClick = onBackClick,
                onEditClick = onEditClick,
                onDeleteClick = {
                    viewModel.requestDelete()
                },
                showDeleteDialog = showDeleteDialog,
                onConfirmDelete = {
                    viewModel.confirmDelete()
                    onBackClick()
                },
                onCancelDelete = {
                    viewModel.cancelDelete()
                },
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
            ),
        modifier = modifier,
        uiState = uiState,
    )
}

/**
 * Основной контент экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenContent(
    params: DetailScreenParams,
    modifier: Modifier = Modifier,
    uiState: DetailScreenState,
) {
    val currentItem = (uiState as? DetailScreenState.Success)?.item

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DetailTopAppBar(
                uiState = uiState,
                onBackClick = params.onBackClick,
                onEditClick = params.onEditClick,
                onDeleteClick = params.onDeleteClick,
                itemId = params.itemId,
            )
        },
    ) { paddingValues ->
        DetailContentByState(
            uiState = uiState,
            getDaysAnalysisTextUseCase = params.getDaysAnalysisTextUseCase,
            modifier = Modifier.padding(paddingValues),
        )
    }

    // Диалог подтверждения удаления
    if (params.showDeleteDialog) {
        currentItem?.let { item ->
            AlertDialog(
                onDismissRequest = { params.onCancelDelete() },
                title = { Text(stringResource(R.string.delete_item_title)) },
                text = {
                    Text(stringResource(R.string.delete_item_message, item.title))
                },
                confirmButton = {
                    TextButton(
                        onClick = { params.onConfirmDelete() },
                    ) {
                        Text(stringResource(R.string.delete_item_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { params.onCancelDelete() },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}
