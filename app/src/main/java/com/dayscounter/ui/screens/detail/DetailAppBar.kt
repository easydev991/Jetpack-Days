package com.dayscounter.ui.screens.detail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme
import com.dayscounter.ui.viewmodel.DetailScreenState

/**
 * TopAppBar для экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailTopAppBar(
    uiState: DetailScreenState,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    itemId: Long,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        actions = {
            if (uiState is DetailScreenState.Success) {
                DetailActionButtons(
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                    itemId = itemId
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

/**
 * Кнопки действий в TopAppBar.
 */
@Composable
internal fun DetailActionButtons(
    onEditClick: (Long) -> Unit,
    onDeleteClick: () -> Unit,
    itemId: Long,
) {
    IconButton(onClick = { onEditClick(itemId) }) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.edit),
        )
    }
    IconButton(onClick = onDeleteClick) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.delete),
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "TopAppBar экрана деталей")
@Composable
fun DetailTopAppBarPreview() {
    JetpackDaysTheme {
        DetailTopAppBar(
            uiState =
                DetailScreenState.Success(
                    com.dayscounter.domain.model.Item(
                        id = 1L,
                        title = "День рождения",
                        details = "Праздничный день",
                        timestamp = System.currentTimeMillis(),
                        colorTag = android.graphics.Color.RED,
                        displayOption = com.dayscounter.domain.model.DisplayOption.DAY,
                    ),
                ),
            onBackClick = {},
            onEditClick = {},
            onDeleteClick = {},
            itemId = 1L,
        )
    }
}
