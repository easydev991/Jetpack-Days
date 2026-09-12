package com.dayscounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.domain.usecase.CheckForAppUpdateUseCase
import com.dayscounter.ui.state.MoreScreenUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel экрана "Ещё" (MoreScreen) — ручная проверка обновлений.
 *
 * Запрашивает последнюю версию через [CheckForAppUpdateUseCase] и публикует
 * состояние диалога через [state]. Повторный вызов [checkForUpdate] отменяет
 * предыдущую проверку. Закрытие диалога ([dismissDialog]) всегда возвращает в Idle.
 *
 * @property useCase Use case проверки обновлений
 */
class MoreScreenViewModel(
    private val useCase: CheckForAppUpdateUseCase
) : ViewModel() {
    private val _state = MutableStateFlow<MoreScreenUiState>(MoreScreenUiState.Idle)

    /** Текущее состояние проверки, наблюдается UI. */
    val state: StateFlow<MoreScreenUiState> = _state.asStateFlow()

    private var updateJob: Job? = null

    /**
     * Запускает проверку обновлений и маппит результат в состояние диалога:
     * [MoreScreenUiState.UpdateAvailable] / [MoreScreenUiState.UpToDate] /
     * [MoreScreenUiState.Error]. Повторный вызов отменяет предыдущую корутину.
     */
    fun checkForUpdate() {
        _state.value = MoreScreenUiState.Checking
        updateJob?.cancel()
        updateJob =
            viewModelScope.launch {
                useCase()
                    .fold(
                        onSuccess = { info ->
                            _state.value =
                                if (info != null) {
                                    MoreScreenUiState.UpdateAvailable(info)
                                } else {
                                    MoreScreenUiState.UpToDate
                                }
                        },
                        onFailure = {
                            _state.value = MoreScreenUiState.Error
                        }
                    )
            }
    }

    /**
     * Закрывает диалог, возвращая состояние в [MoreScreenUiState.Idle].
     */
    fun dismissDialog() {
        _state.value = MoreScreenUiState.Idle
    }

    companion object {
        /**
         * Фабрика для `viewModel(factory = ...)`. Конвенция проекта —
         * `companion object factory()` (см. `ExactAlarmPermissionViewModel.factory`).
         * Делегирует `AppModule.createMoreScreenViewModelFactory`.
         */
        fun factory(useCase: CheckForAppUpdateUseCase): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MoreScreenViewModel(useCase)
                }
            }
    }
}
