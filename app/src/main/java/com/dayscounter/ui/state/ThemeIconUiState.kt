package com.dayscounter.ui.state

import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme

/**
 * UI State для экрана Theme and Icon Screen.
 *
 * @property theme Текущая выбранная тема приложения
 * @property icon Текущая выбранная иконка приложения
 * @property isLoading Индикатор загрузки настроек
 */
data class ThemeIconUiState(
    val theme: AppTheme = AppTheme.SYSTEM,
    val icon: AppIcon = AppIcon.DEFAULT,
    val isLoading: Boolean = false,
)
