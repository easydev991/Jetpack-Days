package com.dayscounter.ui.screen

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.ui.screen.components.detail.detailContentByState
import com.dayscounter.ui.screen.components.detail.detailTopAppBar
import com.dayscounter.viewmodel.DetailScreenState
import com.dayscounter.viewmodel.DetailScreenViewModel

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
fun detailScreen(
    itemId: Long,
    modifier: Modifier = Modifier,
    viewModel: DetailScreenViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()

    // Создаем use case для форматирования с учетом displayOption
    val resourceProvider =
        com.dayscounter.di.FormatterModule.createResourceProvider(
            context =
                androidx.compose.ui.platform.LocalContext.current,
        )
    val calculateDaysDifferenceUseCase =
        com.dayscounter.di.FormatterModule
            .createCalculateDaysDifferenceUseCase()
    val daysFormatter =
        com.dayscounter.di.FormatterModule
            .createDaysFormatter()
    val formatDaysTextUseCase =
        com.dayscounter.di.FormatterModule
            .createFormatDaysTextUseCase(daysFormatter)
    val getFormattedDaysForItemUseCase =
        com.dayscounter.di.FormatterModule.createGetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )

    detailScreenContent(
        params =
            DetailScreenParams(
                itemId = itemId,
                getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
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
private fun detailScreenContent(
    params: DetailScreenParams,
    modifier: Modifier = Modifier,
    uiState: DetailScreenState,
) {
    val currentItem = (uiState as? DetailScreenState.Success)?.item

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            detailTopAppBar(
                uiState = uiState,
                onBackClick = params.onBackClick,
                onEditClick = params.onEditClick,
                onDeleteClick = params.onDeleteClick,
                itemId = params.itemId,
            )
        },
    ) { paddingValues ->
        detailContentByState(
            uiState = uiState,
            modifier = Modifier.padding(paddingValues),
            getFormattedDaysForItemUseCase = params.getFormattedDaysForItemUseCase,
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
