package com.dayscounter.viewmodel

import android.app.Application
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme
import com.dayscounter.domain.usecase.IconManager
import com.dayscounter.ui.state.ThemeIconUiState
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val STATE_TIMEOUT_MS = 5000L
private const val TAG = "ThemeIconViewModel"

/**
 * ViewModel для экрана Theme and Icon Screen. Управляет настройками темы и иконки приложения.
 *
 * Настройки загружаются из DataStore при инициализации, чтобы при каждом запуске приложения выбор
 * пользователя не сбрасывался.
 *
 * @property dataStore DataStore для сохранения настроек
 * @property iconManager Use Case для смены иконки приложения
 * @property logger Logger для логирования (по умолчанию AndroidLogger)
 */
class ThemeIconViewModel(
    private val dataStore: AppSettingsDataStore,
    private val iconManager: IconManager,
    private val logger: Logger = AndroidLogger(),
) : ViewModel() {
    companion object {
        /**
         * Factory для создания ThemeIconViewModel. Используется для ручного DI вместо Hilt.
         *
         * @param dataStore DataStore для настроек приложения
         * @param application Application контекст для создания IconManager
         * @return Factory для создания ViewModel
         */
        fun factory(
            dataStore: AppSettingsDataStore,
            application: Application,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                val iconManager = IconManager(application)
                initializer { ThemeIconViewModel(dataStore, iconManager) }
            }
    }

    /** UI State экрана. Объединяет поток настроек из DataStore. */
    val uiState: StateFlow<ThemeIconUiState> =
        combine(
            dataStore.theme,
            dataStore.icon,
        ) { theme, icon ->
            ThemeIconUiState(
                theme = theme,
                icon = icon,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = ThemeIconUiState(),
        )

    /**
     * Обновляет тему приложения. Сохраняет выбор в DataStore.
     *
     * @param theme Новая тема приложения
     */
    fun updateTheme(theme: AppTheme) {
        viewModelScope.launch { dataStore.setTheme(theme) }
    }

    /**
     * Обновляет иконку приложения. Сохраняет выбор в DataStore и применяет через PackageManager.
     *
     * @param icon Новая иконка приложения
     */
    fun updateIcon(icon: AppIcon) {
        viewModelScope.launch {
            try {
                // Сохраняем выбор в DataStore
                dataStore.setIcon(icon)

                // Применяем иконку через PackageManager
                iconManager.changeIcon(icon)

                logger.d(TAG, "Иконка успешно изменена на ${icon.name}")
            } catch (e: SecurityException) {
                logger.e(TAG, "Ошибка безопасности при смене иконки", e)
                // Продолжаем работу, даже если смена иконки не удалась
            } catch (e: PackageManager.NameNotFoundException) {
                logger.e(TAG, "Компонент иконки не найден", e)
                // Продолжаем работу, даже если смена иконки не удалась
            } catch (e: IllegalArgumentException) {
                logger.e(TAG, "Неверный аргумент при смене иконки", e)
                // Продолжаем работу, даже если смена иконки не удалась
            }
        }
    }
}
