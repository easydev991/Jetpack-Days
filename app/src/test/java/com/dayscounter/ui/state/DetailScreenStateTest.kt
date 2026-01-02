package com.dayscounter.ui.state

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.viewmodel.DetailScreenState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для DetailScreenState.
 */
class DetailScreenStateTest {
    @Test
    fun `whenLoadingState_thenIsLoadingInstance`() {
        // Given
        val state = DetailScreenState.Loading

        // Then
        assertTrue(
            state is DetailScreenState.Loading,
            "Loading state должен быть экземпляром DetailScreenState.Loading",
        )
    }

    @Test
    fun `whenSuccessState_thenContainsCorrectItem`() {
        // Given
        val testItem =
            Item(
                id = 1L,
                title = "Тестовое событие",
                details = "Описание",
                timestamp = System.currentTimeMillis(),
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )
        val state = DetailScreenState.Success(testItem)

        // Then
        val successState = state as DetailScreenState.Success
        assertEquals(testItem, successState.item, "Item в состоянии должен совпадать")
        assertEquals(testItem.id, successState.item.id, "ID должен совпадать")
        assertEquals(testItem.title, successState.item.title, "Название должно совпадать")
    }

    @Test
    fun `whenSuccessState_thenCanAccessItemProperties`() {
        // Given
        val testItem =
            Item(
                id = 1L,
                title = "День рождения",
                details = "Праздничный день",
                timestamp = System.currentTimeMillis() - 86400000,
                colorTag = 0xFFFF00,
                displayOption = DisplayOption.DAY,
            )
        val state = DetailScreenState.Success(testItem)

        // When - Доступ к свойствам состояния
        val successState = state as DetailScreenState.Success

        // Then - Все свойства доступны и корректны
        assertEquals(1L, successState.item.id)
        assertEquals("День рождения", successState.item.title)
        assertEquals("Праздничный день", successState.item.details)
        assertEquals(0xFFFF00, successState.item.colorTag)
        assertEquals(DisplayOption.DAY, successState.item.displayOption)
    }

    @Test
    fun `whenErrorState_thenContainsCorrectMessage`() {
        // Given
        val errorMessage = "Ошибка загрузки данных"
        val state = DetailScreenState.Error(errorMessage)

        // Then
        val errorState = state as DetailScreenState.Error
        assertEquals(errorMessage, errorState.message, "Сообщение об ошибке должно совпадать")
    }

    @Test
    fun `whenDifferentSuccessStates_thenAreNotEqual`() {
        // Given
        val item1 =
            Item(
                id = 1L,
                title = "Событие 1",
                details = "",
                timestamp = System.currentTimeMillis(),
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )
        val item2 =
            Item(
                id = 2L,
                title = "Событие 2",
                details = "",
                timestamp = System.currentTimeMillis(),
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )
        val state1 = DetailScreenState.Success(item1)
        val state2 = DetailScreenState.Success(item2)

        // Then
        assertFalse(
            state1 == state2,
            "Разные Success состояния не должны быть равны",
        )
    }

    @Test
    fun `whenSameSuccessState_thenAreEqual`() {
        // Given
        val item =
            Item(
                id = 1L,
                title = "Событие",
                details = "Детали",
                timestamp = System.currentTimeMillis(),
                colorTag = 0xFF0000,
                displayOption = DisplayOption.DAY,
            )
        val state1 = DetailScreenState.Success(item)
        val state2 = DetailScreenState.Success(item)

        // Then
        assertEquals(
            state1,
            state2,
            "Одинаковые Success состояния должны быть равны",
        )
    }

    @Test
    fun `whenSuccessStateWithEmptyDetails_thenIsStillSuccess`() {
        // Given
        val item =
            Item(
                id = 1L,
                title = "Событие",
                details = "",
                timestamp = System.currentTimeMillis(),
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )
        val state = DetailScreenState.Success(item)

        // Then
        val successState = state as DetailScreenState.Success
        assertEquals("", successState.item.details, "Пустые детали допустимы")
    }

    @Test
    fun `whenSuccessStateWithoutColorTag_thenIsStillSuccess`() {
        // Given
        val item =
            Item(
                id = 1L,
                title = "Событие",
                details = "Детали",
                timestamp = System.currentTimeMillis(),
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )
        val state = DetailScreenState.Success(item)

        // Then
        val successState = state as DetailScreenState.Success
        assertEquals(null, successState.item.colorTag, "Отсутствие цветовой метки допустимо")
    }

    @Test
    fun `whenErrorStateWithDifferentMessages_thenAreNotEqual`() {
        // Given
        val state1 = DetailScreenState.Error("Ошибка 1")
        val state2 = DetailScreenState.Error("Ошибка 2")

        // Then
        assertFalse(
            state1 == state2,
            "Error состояния с разными сообщениями не должны быть равны",
        )
    }

    @Test
    fun `whenErrorStateWithSameMessage_thenAreEqual`() {
        // Given
        val errorMessage = "Ошибка загрузки"
        val state1 = DetailScreenState.Error(errorMessage)
        val state2 = DetailScreenState.Error(errorMessage)

        // Then
        assertEquals(
            state1,
            state2,
            "Error состояния с одинаковым сообщением должны быть равны",
        )
    }
}
