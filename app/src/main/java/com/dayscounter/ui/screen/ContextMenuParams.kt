package com.dayscounter.ui.screen

import androidx.compose.ui.unit.DpOffset
import com.dayscounter.domain.model.Item

/**
 * Параметры для контекстного меню.
 */
data class ContextMenuParams(
    val item: Item,
    val menuOffset: DpOffset,
    val onDismiss: () -> Unit,
    val onItemClick: (Long) -> Unit,
    val onEditClick: (Long) -> Unit,
    val onDeleteClick: (Item) -> Unit,
)
