package com.dayscounter.ui.screens.createedit

import androidx.compose.ui.graphics.Color
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Unit-тесты для CreateEditUiState.
 *
 * CreateEditUiState — plain data class без MutableState.
 * Мутация через copy(), чтение напрямую через поля.
 */
class CreateEditUiStateTest {
    @Test
    fun whenDefaultState_thenAllFieldsHaveDefaultValues() {
        val state = CreateEditUiState()

        assertEquals("", state.title)
        assertEquals("", state.details)
        assertNull(state.selectedDate)
        assertNull(state.selectedColor)
        assertEquals(DisplayOption.DAY, state.selectedDisplayOption)
    }

    @Test
    fun whenCustomValues_thenFieldsPreserveValues() {
        val date = LocalDate.of(2024, 3, 20)
        val color = Color.Green
        val state =
            CreateEditUiState(
                title = "Исходное название",
                details = "Исходное описание",
                selectedDate = date,
                selectedColor = color,
                selectedDisplayOption = DisplayOption.MONTH_DAY
            )

        assertEquals("Исходное название", state.title)
        assertEquals("Исходное описание", state.details)
        assertEquals(date, state.selectedDate)
        assertEquals(color, state.selectedColor)
        assertEquals(DisplayOption.MONTH_DAY, state.selectedDisplayOption)
    }

    @Test
    fun whenCopyWithTitle_thenNewStateHasUpdatedTitle() {
        val state = CreateEditUiState()
        val newState = state.copy(title = "Новое название")

        assertEquals("Новое название", newState.title)
        assertEquals("", newState.details)
    }

    @Test
    fun whenCopyWithDetails_thenNewStateHasUpdatedDetails() {
        val state = CreateEditUiState()
        val newState = state.copy(details = "Новое описание")

        assertEquals("Новое описание", newState.details)
        assertEquals("", newState.title)
    }

    @Test
    fun whenCopyWithDate_thenNewStateHasUpdatedDate() {
        val date = LocalDate.of(2024, 6, 15)
        val state = CreateEditUiState()
        val newState = state.copy(selectedDate = date)

        assertEquals(date, newState.selectedDate)
        assertNull(state.selectedDate)
    }

    @Test
    fun whenCopyWithColor_thenNewStateHasUpdatedColor() {
        val color = Color.Red
        val state = CreateEditUiState()
        val newState = state.copy(selectedColor = color)

        assertEquals(color, newState.selectedColor)
        assertNull(state.selectedColor)
    }

    @Test
    fun whenCopyWithNullColor_thenColorIsReset() {
        val state = CreateEditUiState(selectedColor = Color.Red)
        val newState = state.copy(selectedColor = null)

        assertNull(newState.selectedColor)
    }

    @Test
    fun whenCopyWithDisplayOption_thenNewStateHasUpdatedOption() {
        val state = CreateEditUiState()
        val newState = state.copy(selectedDisplayOption = DisplayOption.MONTH_DAY)

        assertEquals(DisplayOption.MONTH_DAY, newState.selectedDisplayOption)
        assertEquals(DisplayOption.DAY, state.selectedDisplayOption)
    }

    @Test
    fun whenCopyWithAllFields_thenNewStateHasAllUpdated() {
        val date = LocalDate.of(2024, 6, 15)
        val color = Color.Blue
        val state = CreateEditUiState()
        val newState =
            state.copy(
                title = "Тестовое событие",
                details = "Тестовое описание",
                selectedDate = date,
                selectedColor = color,
                selectedDisplayOption = DisplayOption.YEAR_MONTH_DAY
            )

        assertEquals("Тестовое событие", newState.title)
        assertEquals("Тестовое описание", newState.details)
        assertEquals(date, newState.selectedDate)
        assertEquals(color, newState.selectedColor)
        assertEquals(DisplayOption.YEAR_MONTH_DAY, newState.selectedDisplayOption)
    }

    @Test
    fun whenCopy_thenOriginalIsUnchanged() {
        val state = CreateEditUiState()
        state.copy(title = "Новое название")

        assertEquals("", state.title)
    }

    @Test
    fun whenCopy_thenReturnsDifferentInstance() {
        val state = CreateEditUiState()
        val newState = state.copy(title = "Test")

        assertNotSame(state, newState)
    }

    @Test
    fun whenDefaultState_thenReminderHasDefaults() {
        val state = CreateEditUiState()

        assertEquals(false, state.reminder.isEnabled)
        assertEquals(ReminderMode.AT_DATE, state.reminder.mode)
        assertEquals("", state.reminder.intervalValue)
        assertEquals(ReminderIntervalUnit.DAY, state.reminder.intervalUnit)
    }

    @Test
    fun whenCopyWithReminder_thenNewStateHasUpdatedReminder() {
        val state = CreateEditUiState()
        val updatedReminder =
            state.reminder.copy(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = "5",
                intervalUnit = ReminderIntervalUnit.WEEK
            )
        val newState = state.copy(reminder = updatedReminder)

        assertEquals(true, newState.reminder.isEnabled)
        assertEquals(ReminderMode.AFTER_INTERVAL, newState.reminder.mode)
        assertEquals("5", newState.reminder.intervalValue)
        assertEquals(ReminderIntervalUnit.WEEK, newState.reminder.intervalUnit)
    }

    @Test
    fun whenReminderDefaults_thenSelectedDateIsTomorrow() {
        val expected = LocalDate.now().plusDays(1)
        val state = CreateEditUiState()

        assertEquals(expected, state.reminder.selectedDate)
    }
}
