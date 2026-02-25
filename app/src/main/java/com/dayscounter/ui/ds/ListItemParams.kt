package com.dayscounter.ui.ds

import androidx.compose.ui.geometry.Offset
import com.dayscounter.domain.model.Item

/**
 * Параметры для компонента списка элементов.
 */
data class ListItemParams(
    val item: Item,
    val formattedDaysText: String,
    val onClick: (Item) -> Unit = {},
    val onLongClick: ((Offset) -> Unit)? = null,
    val isSelected: Boolean = false,
)
