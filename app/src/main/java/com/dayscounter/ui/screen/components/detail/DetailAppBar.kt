package com.dayscounter.ui.screen.components.detail

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
import com.dayscounter.R
import com.dayscounter.viewmodel.DetailScreenState

/**
 * TopAppBar для экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun detailTopAppBar(
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
                detailActionButtons(onEditClick = onEditClick, onDeleteClick = onDeleteClick, itemId = itemId)
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
internal fun detailActionButtons(
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
