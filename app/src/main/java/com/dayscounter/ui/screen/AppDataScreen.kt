package com.dayscounter.ui.screen

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.ui.state.AppDataUiState
import com.dayscounter.viewmodel.AppDataScreenViewModel

/**
 * Параметры для функции AppDataScreenContent.
 *
 * @property uiState Текущее состояние UI
 * @property showDeleteDialog Флаг, указывающий, нужно ли показывать диалог подтверждения удаления
 * @property onExportBackupClick Обработчик нажатия кнопки "Создать резервную копию"
 * @property onImportBackupClick Обработчик нажатия кнопки "Восстановить из резервной копии"
 * @property onDeleteAllDataClick Обработчик нажатия кнопки "Удалить все данные"
 * @property onConfirmDeleteAllData Обработчик подтверждения удаления
 * @property onCancelDeleteAllData Обработчик отмены удаления
 * @property onBackClick Обработчик нажатия кнопки "Назад"
 */
internal data class AppDataScreenParams(
    val uiState: AppDataUiState,
    val showDeleteDialog: Boolean = false,
    val onExportBackupClick: () -> Unit = {},
    val onImportBackupClick: () -> Unit = {},
    val onDeleteAllDataClick: () -> Unit = {},
    val onConfirmDeleteAllData: () -> Unit = {},
    val onCancelDeleteAllData: () -> Unit = {},
    val onBackClick: () -> Unit = {},
)

/**
 * Экран управления данными приложения.
 * Позволяет создавать резервные копии, восстанавливать данные и удалять все записи.
 *
 * @param viewModel ViewModel для управления состоянием экрана
 * @param onBackClick Callback для возврата на предыдущий экран
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDataScreen(
    viewModel: AppDataScreenViewModel,
    onBackClick: () -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()
    val showDeleteDialog by viewModel.showDeleteDialog.collectAsState()
    val context = LocalContext.current

    // Launcher для создания резервной копии (экспорт)
    val exportLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri ->
            if (uri != null) {
                viewModel.exportBackup(uri)
            }
        }

    // Launcher для выбора файла резервной копии (импорт)
    val importLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                viewModel.importBackup(uri)
            }
        }

    // Показываем Toast при наличии сообщения о результате операции
    LaunchedEffect(uiState.resultMessage) {
        uiState.resultMessage?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Очищаем сообщение после показа Toast
    LaunchedEffect(uiState.resultMessage) {
        viewModel.clearResultMessage()
    }

    AppDataScreenContent(
        params =
            AppDataScreenParams(
                uiState = uiState,
                showDeleteDialog = showDeleteDialog,
                onExportBackupClick = {
                    // Запускаем FilePicker для сохранения файла
                    exportLauncher.launch("Days backup")
                },
                onImportBackupClick = {
                    // Запускаем FilePicker для выбора файла
                    importLauncher.launch(arrayOf("*/*"))
                },
                onDeleteAllDataClick = { viewModel.deleteAllData() },
                onConfirmDeleteAllData = { viewModel.confirmDeleteAllData() },
                onCancelDeleteAllData = { viewModel.cancelDeleteAllData() },
                onBackClick = onBackClick,
            ),
    )
}

/**
 * Контент экрана для управления данными приложения.
 *
 * Эта функция используется для UI-тестов в изоляции без ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDataScreenContent(params: AppDataScreenParams) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_data)) },
                navigationIcon = {
                    IconButton(onClick = params.onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        AppDataScreenBody(
            params = params,
            paddingValues = paddingValues,
        )
    }

    // Диалог подтверждения удаления всех данных
    if (params.showDeleteDialog) {
        AppDataScreenDeleteConfirmDialog(params)
    }
}

/**
 * Основной контент экрана управления данными.
 */
@Composable
private fun AppDataScreenBody(
    params: AppDataScreenParams,
    paddingValues: PaddingValues,
) {
    val scrollState = rememberScrollState()
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(paddingValues)
                .padding(dimensionResource(R.dimen.spacing_xsmall)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xxsmall)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))

        if (params.uiState.hasItems) {
            AppDataScreenExportButton(params)
        }

        AppDataScreenImportButton(params)

        if (params.uiState.hasItems) {
            AppDataScreenDeleteButton(params)
        }
    }
}

/**
 * Кнопка "Создать резервную копию".
 */
@Composable
private fun AppDataScreenExportButton(params: AppDataScreenParams) {
    FilledTonalButton(
        onClick = params.onExportBackupClick,
        enabled = !params.uiState.isExporting,
        contentPadding =
            PaddingValues(
                horizontal = dimensionResource(R.dimen.button_horizontal_padding),
            ),
    ) {
        Text(stringResource(R.string.create_a_backup))
    }
}

/**
 * Кнопка "Восстановить из резервной копии".
 */
@Composable
private fun AppDataScreenImportButton(params: AppDataScreenParams) {
    FilledTonalButton(
        onClick = params.onImportBackupClick,
        enabled = !params.uiState.isImporting,
        contentPadding =
            PaddingValues(
                horizontal = dimensionResource(R.dimen.button_horizontal_padding),
            ),
    ) {
        Text(stringResource(R.string.restore_from_backup))
    }
}

/**
 * Кнопка "Удалить все данные" (деструктивный стиль).
 */
@Composable
private fun AppDataScreenDeleteButton(params: AppDataScreenParams) {
    Button(
        onClick = params.onDeleteAllDataClick,
        enabled = !params.uiState.isDeleting,
        colors =
            ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
            ),
        contentPadding =
            PaddingValues(
                horizontal = dimensionResource(R.dimen.button_horizontal_padding),
            ),
    ) {
        Text(stringResource(R.string.delete_all_data))
    }
}

/**
 * Диалог подтверждения удаления всех данных.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDataScreenDeleteConfirmDialog(params: AppDataScreenParams) {
    AlertDialog(
        onDismissRequest = params.onCancelDeleteAllData,
        title = { Text(stringResource(R.string.delete_all_data)) },
        text = {
            Text(stringResource(R.string.do_you_want_to_delete_all_data_permanently))
        },
        confirmButton = {
            Button(
                onClick = params.onConfirmDeleteAllData,
                colors =
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
            ) {
                Text(stringResource(R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = params.onCancelDeleteAllData) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}
