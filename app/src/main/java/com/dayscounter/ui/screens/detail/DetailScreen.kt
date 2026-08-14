package com.dayscounter.ui.screens.detail

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.ui.viewmodel.DetailScreenState
import com.dayscounter.ui.viewmodel.DetailScreenViewModel
import com.dayscounter.util.SystemClipboardHelper

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
@Suppress("LongMethod") // use-case wiring (~28 строк) выводит функцию за порог 60
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    itemId: Long,
    modifier: Modifier = Modifier,
    viewModel: DetailScreenViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onEditClick: (Long) -> Unit = {}
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
                resourceProvider = resourceProvider
            )
    val getDaysAnalysisTextUseCase =
        com.dayscounter.di.FormatterModule
            .createGetDaysAnalysisTextUseCase(
                calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
                resourceProvider = resourceProvider
            )

    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    RefreshReminderOnResume(viewModel = viewModel)

    // Строки резолвятся в composable-скоупе через stringResource,
    // чтобы Toast на API <33 показывал актуальный перевод при смене локали.
    val titleCopiedMessage = stringResource(R.string.title_copied)
    val detailsCopiedMessage = stringResource(R.string.details_copied)

    val clipboardHelper = remember { SystemClipboardHelper() }
    val item = (uiState as? DetailScreenState.Success)?.item
    val onCopyTitle: () -> Unit = {
        item?.let {
            clipboardHelper.copy(context, "Title", it.title)
            // ponytail: Toast на API >=33 не показывается — ОС сама рисует системный overlay.
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, titleCopiedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }
    val onCopyDetails: () -> Unit = {
        item?.let {
            clipboardHelper.copy(context, "Details", it.details)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Toast.makeText(context, detailsCopiedMessage, Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                onCopyTitle = onCopyTitle,
                onCopyDetails = onCopyDetails,
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase
            ),
        modifier = modifier,
        uiState = uiState
    )
}

@Composable
private fun RefreshReminderOnResume(viewModel: DetailScreenViewModel) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refreshReminder()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}

/**
 * Основной контент экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailScreenContent(
    params: DetailScreenParams,
    modifier: Modifier = Modifier,
    uiState: DetailScreenState
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
                itemId = params.itemId
            )
        }
    ) { paddingValues ->
        DetailContentByState(
            uiState = uiState,
            onCopyTitle = params.onCopyTitle,
            onCopyDetails = params.onCopyDetails,
            getDaysAnalysisTextUseCase = params.getDaysAnalysisTextUseCase,
            modifier = Modifier.padding(paddingValues)
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
                        colors =
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                    ) {
                        Text(stringResource(R.string.delete_item_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { params.onCancelDelete() }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
    }
}
