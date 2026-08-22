package com.dayscounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.domain.model.ExactAlarmPermissionState
import com.dayscounter.reminder.ExactAlarmPermissionHelper
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel для баннера exact-alarm permission на экране `CreateEditScreen`.
 *
 * Отслеживает состояние permission через [ExactAlarmPermissionHelper]
 * и предоставляет действие [onRequestPermission] для открытия системных настроек.
 *
 * Factory создаётся в `AppModule`. Баннер на экране должен вызывать
 * [refresh] через `DisposableEffect(lifecycleOwner)` + `LifecycleEventObserver`
 * на `Lifecycle.Event.ON_RESUME` — иначе состояние не обновится после возврата
 * из системных настроек. По конвенции проекта используется `DisposableEffect`,
 * а не `LifecycleEventEffect` (см. `ExactAlarmPermissionBanner.kt`).
 */
class ExactAlarmPermissionViewModel(
    private val helper: ExactAlarmPermissionHelper,
    private val logger: Logger = AndroidLogger()
) : ViewModel() {
    private val _state = MutableStateFlow(helper.getState())
    val state: StateFlow<ExactAlarmPermissionState> = _state.asStateFlow()

    /**
     * Обновляет состояние из helper. Вызывается из
     * `DisposableEffect` + `LifecycleEventObserver` на `ON_RESUME`
     * (см. `ExactAlarmPermissionBanner.kt`).
     */
    fun refresh() {
        _state.value = helper.getState()
    }

    /**
     * Открывает системный экран SCHEDULE_EXACT_ALARM.
     *
     * Если helper бросил [android.content.ActivityNotFoundException]
     * (кастомные прошивки MIUI/HyperOS без экрана SCHEDULE_EXACT_ALARM) —
     * исключение логируется и поглощается, refresh() всё равно вызывается.
     */
    fun onRequestPermission() {
        try {
            helper.requestSettings()
        } catch (e: Exception) {
            // Ловим Exception, но не Throwable: Error — неустранимые сбои JVM
            // (OOM, StackOverflow); ловить и логировать их бессмысленно,
            // рантайм сам завершит процесс. По паттерну MoreScreen.kt:214-218
            // (UI-слой ловит системные ошибки запуска intent'а).
            logger.w(TAG, "Не удалось открыть настройки SCHEDULE_EXACT_ALARM", e)
        }
        refresh()
    }

    companion object {
        private const val TAG = "ExactAlarmPermissionVM"

        /**
         * Фабрика для `viewModel(factory = ...)`. Конвенция проекта — `companion object factory()`
         * (см. `CreateEditScreenViewModel.factory`). Делегирует `AppModule.createExactAlarmPermissionViewModelFactory`.
         */
        fun factory(helper: ExactAlarmPermissionHelper): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    ExactAlarmPermissionViewModel(helper = helper)
                }
            }
    }
}
