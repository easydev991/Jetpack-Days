package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.ui.screen.components.detail.detailContentByState
import com.dayscounter.ui.screen.components.detail.detailTopAppBar
import com.dayscounter.ui.theme.jetpackDaysTheme
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

    detailScreenContent(
        itemId = itemId,
        modifier = modifier,
        uiState = uiState,
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
    )
}

/**
 * Основной контент экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun detailScreenContent(
    itemId: Long,
    modifier: Modifier = Modifier,
    uiState: DetailScreenState,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    showDeleteDialog: Boolean,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    val currentItem = (uiState as? DetailScreenState.Success)?.item

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            detailTopAppBar(
                uiState = uiState,
                onBackClick = onBackClick,
                onEditClick = onEditClick,
                onDeleteClick = onDeleteClick,
                itemId = itemId,
            )
        },
    ) { paddingValues ->
        detailContentByState(uiState = uiState, modifier = Modifier.padding(paddingValues))
    }

    // Диалог подтверждения удаления
    if (showDeleteDialog) {
        currentItem?.let { item ->
            AlertDialog(
                onDismissRequest = { onCancelDelete() },
                title = { Text(stringResource(R.string.delete_item_title)) },
                text = {
                    Text(stringResource(R.string.delete_item_message, item.title))
                },
                confirmButton = {
                    TextButton(
                        onClick = { onConfirmDelete() },
                    ) {
                        Text(stringResource(R.string.delete_item_confirm))
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { onCancelDelete() },
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                },
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Экран деталей с цветом")
@Composable
fun detailScreenWithColorPreview() {
    jetpackDaysTheme {
        val item =
            com.dayscounter.domain.model.Item(
                id = 1L,
                title = "День рождения",
                details = "Праздничный день с друзьями",
                timestamp = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L),
                colorTag = android.graphics.Color.RED,
                displayOption = DisplayOption.DAY,
            )

        // Отображаем базовый контент детали
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Preview для DetailScreen")
        }
    }
}

@Preview(showBackground = true, name = "Экран деталей без цвета")
@Composable
fun detailScreenNoColorPreview() {
    jetpackDaysTheme {
        val item =
            com.dayscounter.domain.model.Item(
                id = 2L,
                title = "Начало работы",
                details = "Первый день на новой работе",
                timestamp = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L),
                colorTag = null,
                displayOption = DisplayOption.YEAR_MONTH_DAY,
            )

        // Отображаем базовый контент детали
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Preview для DetailScreen без цвета")
        }
    }
}
