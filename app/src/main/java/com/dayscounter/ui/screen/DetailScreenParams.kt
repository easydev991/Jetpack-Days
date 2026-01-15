package com.dayscounter.ui.screen

import com.dayscounter.domain.usecase.GetDaysAnalysisTextUseCase

/**
 * Параметры для экрана деталей события.
 */
data class DetailScreenParams(
    val itemId: Long,
    val onBackClick: () -> Unit,
    val onEditClick: (Long) -> Unit,
    val onDeleteClick: () -> Unit,
    val showDeleteDialog: Boolean,
    val onConfirmDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase,
)
