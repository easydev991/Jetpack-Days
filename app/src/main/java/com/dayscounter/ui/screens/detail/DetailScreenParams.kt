package com.dayscounter.ui.screens.detail

import com.dayscounter.domain.usecase.GetDaysAnalysisTextUseCase

/**
 * Параметры для экрана деталей события.
 *
 * @property onCopyTitle Колбэк копирования title (для `ReadSectionView` секции Title)
 * @property onCopyDetails Колбэк копирования details (для `ReadSectionView` секции Details)
 */
data class DetailScreenParams(
    val itemId: Long,
    val onBackClick: () -> Unit,
    val onEditClick: (Long) -> Unit,
    val onDeleteClick: () -> Unit,
    val showDeleteDialog: Boolean,
    val onConfirmDelete: () -> Unit,
    val onCancelDelete: () -> Unit,
    val onCopyTitle: () -> Unit,
    val onCopyDetails: () -> Unit,
    val getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase
)
