@file:Suppress("ktlint:standard:property-naming")

package com.dayscounter.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dayscounter.data.formatter.StubResourceProvider
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Состояние UI для DaysCalculatorViewModel.
 *
 * @property isLoading Флаг загрузки
 * @property formattedText Форматированный текст с количеством дней
 * @property error Сообщение об ошибке (если есть)
 */
data class DaysCalculatorState(
    val isLoading: Boolean = false,
    val formattedText: String? = null,
    val error: String? = null,
)

/**
 * ViewModel для вычисления и форматирования разницы между датами.
 *
 * Отвечает за:
 * - Вычисление разницы между датой события и текущим днем
 * - Форматирование результата в соответствии с выбранной опцией отображения
 * - Обработку ошибок и обновление состояния UI
 *
 * @param calculateDaysDifferenceUseCase Use case для вычисления разницы дат
 * @param formatDaysTextUseCase Use case для форматирования текста
 * @param defaultDisplayOption Опция отображения по умолчанию
 */
@Suppress("TooGenericExceptionCaught")
class DaysCalculatorViewModel(
    private val calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
    private val formatDaysTextUseCase: FormatDaysTextUseCase,
    private val defaultDisplayOption: DisplayOption = DisplayOption.DAY,
) : ViewModel() {
    @Suppress("VariableNaming")
    private val TAG = "DaysCalculatorVM"

    private val _state = MutableStateFlow(DaysCalculatorState())
    val state: StateFlow<DaysCalculatorState> = _state.asStateFlow()

    private val currentDisplayOption = MutableStateFlow(defaultDisplayOption)
    val displayOption: StateFlow<DisplayOption> = currentDisplayOption.asStateFlow()

    /**
     * Вычисляет и форматирует разницу между датой события и текущим днем.
     *
     * @param eventTimestamp Таймстамп даты события (в миллисекундах)
     * @param currentDate Текущая дата (опционально, для тестирования)
     * @param displayOption Опция отображения (опционально, используется сохраненное значение)
     */
    fun calculateDays(
        eventTimestamp: Long,
        currentDate: LocalDate? = null,
        displayOption: DisplayOption? = null,
    ) {
        viewModelScope.launch {
            try {
                _state.value =
                    _state.value.copy(
                        isLoading = true,
                        error = null,
                    )

                val option = displayOption ?: currentDisplayOption.value

                Log.d(TAG, "Вычисление разницы для timestamp=$eventTimestamp с опцией $option")

                val difference: DaysDifference =
                    calculateDaysDifferenceUseCase(
                        eventTimestamp = eventTimestamp,
                        currentDate = currentDate ?: LocalDate.now(),
                    )

                @Suppress("TooGenericExceptionCaught")
                val formattedText: String =
                    try {
                        formatDaysTextUseCase(
                            difference = difference,
                            displayOption = option,
                            resourceProvider = StubResourceProvider(),
                        )
                    } catch (e: Exception) {
                        // Обрабатываем исключения при форматировании
                        Log.e(TAG, "Ошибка форматирования: ${e.message}", e)
                        "Ошибка форматирования"
                    }

                _state.value =
                    DaysCalculatorState(
                        isLoading = false,
                        formattedText = formattedText,
                        error = null,
                    )

                Log.d(TAG, "Результат форматирования: $formattedText")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "Ошибка при вычислении дней: ${e.message}", e)
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = "Ошибка при вычислении: ${e.message}",
                    )
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Ошибка форматирования чисел: ${e.message}", e)
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = "Ошибка форматирования: ${e.message}",
                    )
            }
        }
    }

    /**
     * Обновляет опцию отображения и пересчитывает для последнего timestamp.
     *
     * @param newDisplayOption Новая опция отображения
     * @param eventTimestamp Таймстамп события (опционально, если есть сохраненный)
     */
    fun updateDisplayOption(
        newDisplayOption: DisplayOption,
        eventTimestamp: Long? = null,
    ) {
        currentDisplayOption.value = newDisplayOption
        Log.d(TAG, "Опция отображения обновлена на: $newDisplayOption")

        if (eventTimestamp != null && _state.value.formattedText != null) {
            calculateDays(eventTimestamp, displayOption = newDisplayOption)
        }
    }

    /**
     * Пересчитывает разницу для текущего времени (используется для обновления UI).
     *
     * @param eventTimestamp Таймстамп события
     */
    fun refresh(eventTimestamp: Long) {
        calculateDays(eventTimestamp)
    }

    /**
     * Сбрасывает состояние в начальное.
     */
    fun reset() {
        _state.value = DaysCalculatorState()
        Log.d(TAG, "Состояние сброшено")
    }

    /**
     * Очищает ошибку.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
