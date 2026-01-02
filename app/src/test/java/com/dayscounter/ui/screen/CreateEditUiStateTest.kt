package com.dayscounter.ui.screen

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.dayscounter.domain.model.DisplayOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit-тесты для CreateEditUiState.
 */
class CreateEditUiStateTest {
    private lateinit var uiState: CreateEditUiState

    @BeforeEach
    fun setUp() {
        uiState =
            CreateEditUiState(
                title = mutableStateOf(""),
                details = mutableStateOf(""),
                selectedDate = mutableStateOf(null),
                showDatePicker = mutableStateOf(false),
                selectedColor = mutableStateOf(null),
                selectedDisplayOption = mutableStateOf(DisplayOption.DAY),
            )
    }

    @Test
    fun whenUiStateCreated_thenHasDefaultValues() {
        // Given - UiState создан

        // Then - Значения должны быть по умолчанию
        assertEquals("", uiState.title.value, "Название должно быть пустым")
        assertEquals("", uiState.details.value, "Описание должно быть пустым")
        assertNull(uiState.selectedDate.value, "Дата должна быть null")
        assertEquals(false, uiState.showDatePicker.value, "DatePicker должен быть скрыт")
        assertNull(uiState.selectedColor.value, "Цвет должен быть null")
        assertEquals(DisplayOption.DAY, uiState.selectedDisplayOption.value, "Опция отображения должна быть DAY")
    }

    @Test
    fun whenTitleChanged_thenUpdatesTitleState() {
        // Given - UiState с пустым названием

        // When - Изменяем название
        uiState.title.value = "Новое название"

        // Then - Название должно быть обновлено
        assertEquals("Новое название", uiState.title.value, "Название должно быть обновлено")
    }

    @Test
    fun whenDetailsChanged_thenUpdatesDetailsState() {
        // Given - UiState с пустым описанием

        // When - Изменяем описание
        uiState.details.value = "Новое описание"

        // Then - Описание должно быть обновлено
        assertEquals("Новое описание", uiState.details.value, "Описание должно быть обновлено")
    }

    @Test
    fun whenSelectedDateChanged_thenUpdatesDateState() {
        // Given - UiState с null датой
        val testDate = LocalDate.of(2024, 1, 15)

        // When - Выбираем дату
        uiState.selectedDate.value = testDate

        // Then - Дата должна быть обновлена
        assertEquals(testDate, uiState.selectedDate.value, "Дата должна быть обновлена")
    }

    @Test
    fun whenShowDatePickerChanged_thenUpdatesDatePickerState() {
        // Given - UiState с скрытым DatePicker

        // When - Показываем DatePicker
        uiState.showDatePicker.value = true

        // Then - DatePicker должен быть показан
        assertEquals(true, uiState.showDatePicker.value, "DatePicker должен быть показан")

        // When - Скрываем DatePicker
        uiState.showDatePicker.value = false

        // Then - DatePicker должен быть скрыт
        assertEquals(false, uiState.showDatePicker.value, "DatePicker должен быть скрыт")
    }

    @Test
    fun whenSelectedColorChanged_thenUpdatesColorState() {
        // Given - UiState с null цветом
        val testColor = Color.Red

        // When - Выбираем цвет
        uiState.selectedColor.value = testColor

        // Then - Цвет должен быть обновлен
        assertEquals(testColor, uiState.selectedColor.value, "Цвет должен быть обновлен")
    }

    @Test
    fun whenSelectedColorResetToNull_thenColorIsReset() {
        // Given - UiState с выбранным цветом
        uiState.selectedColor.value = Color.Red

        // When - Сбрасываем цвет
        uiState.selectedColor.value = null

        // Then - Цвет должен быть null
        assertNull(uiState.selectedColor.value, "Цвет должен быть null")
    }

    @Test
    fun whenDisplayOptionChanged_thenUpdatesDisplayOptionState() {
        // Given - UiState с опцией DAY

        // When - Меняем опцию отображения
        uiState.selectedDisplayOption.value = DisplayOption.MONTH_DAY

        // Then - Опция отображения должна быть обновлена
        assertEquals(
            DisplayOption.MONTH_DAY,
            uiState.selectedDisplayOption.value,
            "Опция отображения должна быть обновлена",
        )
    }

    @Test
    fun whenAllFieldsChanged_thenAllStatesUpdated() {
        // Given - UiState с значениями по умолчанию
        val testDate = LocalDate.of(2024, 6, 15)
        val testColor = Color.Blue

        // When - Обновляем все поля
        uiState.title.value = "Тестовое событие"
        uiState.details.value = "Тестовое описание"
        uiState.selectedDate.value = testDate
        uiState.showDatePicker.value = true
        uiState.selectedColor.value = testColor
        uiState.selectedDisplayOption.value = DisplayOption.YEAR_MONTH_DAY

        // Then - Все состояния должны быть обновлены
        assertEquals("Тестовое событие", uiState.title.value, "Название должно быть обновлено")
        assertEquals("Тестовое описание", uiState.details.value, "Описание должно быть обновлено")
        assertEquals(testDate, uiState.selectedDate.value, "Дата должна быть обновлена")
        assertEquals(true, uiState.showDatePicker.value, "DatePicker должен быть показан")
        assertEquals(testColor, uiState.selectedColor.value, "Цвет должен быть обновлен")
        assertEquals(
            DisplayOption.YEAR_MONTH_DAY,
            uiState.selectedDisplayOption.value,
            "Опция отображения должна быть обновлена",
        )
    }

    @Test
    fun whenCreateUiStateWithValues_thenValuesArePreserved() {
        // Given - Значения для создания UiState
        val initialTitle = "Исходное название"
        val initialDetails = "Исходное описание"
        val initialDate = LocalDate.of(2024, 3, 20)
        val initialColor = Color.Green
        val initialDisplayOption = DisplayOption.MONTH_DAY

        // When - Создаем UiState с начальными значениями
        val uiStateWithValues =
            CreateEditUiState(
                title = mutableStateOf(initialTitle),
                details = mutableStateOf(initialDetails),
                selectedDate = mutableStateOf(initialDate),
                showDatePicker = mutableStateOf(false),
                selectedColor = mutableStateOf(initialColor),
                selectedDisplayOption = mutableStateOf(initialDisplayOption),
            )

        // Then - Значения должны быть сохранены
        assertEquals(initialTitle, uiStateWithValues.title.value, "Название должно быть сохранено")
        assertEquals(initialDetails, uiStateWithValues.details.value, "Описание должно быть сохранено")
        assertEquals(initialDate, uiStateWithValues.selectedDate.value, "Дата должна быть сохранена")
        assertEquals(initialColor, uiStateWithValues.selectedColor.value, "Цвет должен быть сохранен")
        assertEquals(
            initialDisplayOption,
            uiStateWithValues.selectedDisplayOption.value,
            "Опция отображения должна быть сохранена",
        )
    }

    @Test
    fun whenUiStateMutableStates_thenAreModifiable() {
        // Given - UiState создан

        // When - Модифицируем все состояния
        uiState.title.value = "Модифицированное название"
        uiState.details.value = "Модифицированное описание"
        uiState.selectedDate.value = LocalDate.now()
        uiState.showDatePicker.value = true
        uiState.selectedColor.value = Color.Yellow
        uiState.selectedDisplayOption.value = DisplayOption.YEAR_MONTH_DAY

        // Then - Все изменения должны примениться
        assertNotNull(uiState.title.value, "Название должно быть не null")
        assertNotNull(uiState.details.value, "Описание должно быть не null")
        assertNotNull(uiState.selectedDate.value, "Дата должна быть не null")
        assertNotNull(uiState.selectedColor.value, "Цвет должен быть не null")
    }

    @Test
    fun whenTitleIsEmpty_thenTitleStateIsNotValid() {
        // Given - UiState с пустым названием

        // When - Проверяем валидность (пустое название)
        val isValid = uiState.title.value.isNotEmpty()

        // Then - Название не должно быть валидным
        assertEquals(false, isValid, "Пустое название не должно быть валидным")
    }

    @Test
    fun whenTitleNotEmpty_thenTitleStateIsValid() {
        // Given - UiState с непустым названием
        uiState.title.value = "Название события"

        // When - Проверяем валидность (непустое название)
        val isValid = uiState.title.value.isNotEmpty()

        // Then - Название должно быть валидным
        assertEquals(true, isValid, "Непустое название должно быть валидным")
    }

    @Test
    fun whenDateNotSelected_thenDateStateIsNotValid() {
        // Given - UiState с null датой

        // When - Проверяем валидность (null дата)
        val isValid = uiState.selectedDate.value != null

        // Then - Дата не должна быть валидной
        assertEquals(false, isValid, "Null дата не должна быть валидной")
    }

    @Test
    fun whenDateSelected_thenDateStateIsValid() {
        // Given - UiState с выбранной датой
        uiState.selectedDate.value = LocalDate.now()

        // When - Проверяем валидность (выбранная дата)
        val isValid = uiState.selectedDate.value != null

        // Then - Дата должна быть валидной
        assertEquals(true, isValid, "Выбранная дата должна быть валидной")
    }
}
