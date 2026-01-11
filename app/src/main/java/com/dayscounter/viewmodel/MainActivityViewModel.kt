package com.dayscounter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.domain.model.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private const val STATE_TIMEOUT_MS = 5000L

/**
 * ViewModel для MainActivity. Отслеживает тему приложения из DataStore.
 *
 * Используется для передачи текущей темы в jetpackDaysTheme() на уровне Activity.
 */
class MainActivityViewModel(
    private val dataStore: AppSettingsDataStore,
) : ViewModel() {
    companion object {
        /**
         * Factory для создания MainActivityViewModel. Используется для ручного DI.
         *
         * @param dataStore DataStore для настроек приложения
         * @return Factory для создания ViewModel
         */
        fun factory(dataStore: AppSettingsDataStore): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { MainActivityViewModel(dataStore) }
            }
    }

    /**
     * Текущая тема приложения. Подписывается на изменения из DataStore.
     * При первом запуске возвращает значение по умолчанию (SYSTEM).
     */
    val theme: StateFlow<AppTheme> =
        dataStore.theme.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = AppTheme.SYSTEM,
        )
}
