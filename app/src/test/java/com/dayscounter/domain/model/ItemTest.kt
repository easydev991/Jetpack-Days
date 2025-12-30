package com.dayscounter.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ItemTest {

    @Test
    fun `createItem_withRequiredFields_thenCreatesSuccessfully`() {
        // Given
        val title = "Тестовое событие"
        val timestamp = 1234567890000L

        // When
        val item = Item(
            title = title,
            timestamp = timestamp
        )

        // Then
        assertEquals(0L, item.id)
        assertEquals(title, item.title)
        assertEquals("", item.details)
        assertEquals(timestamp, item.timestamp)
        assertNull(item.colorTag)
        assertEquals(DisplayOption.DEFAULT, item.displayOption)
    }

    @Test
    fun `createItem_withAllFields_thenCreatesSuccessfully`() {
        // Given
        val id = 1L
        val title = "Тестовое событие"
        val details = "Описание события"
        val timestamp = 1234567890000L
        val colorTag = 0xFFFF0000.toInt() // Красный цвет
        val displayOption = DisplayOption.MONTH_DAY

        // When
        val item = Item(
            id = id,
            title = title,
            details = details,
            timestamp = timestamp,
            colorTag = colorTag,
            displayOption = displayOption
        )

        // Then
        assertEquals(id, item.id)
        assertEquals(title, item.title)
        assertEquals(details, item.details)
        assertEquals(timestamp, item.timestamp)
        assertEquals(colorTag, item.colorTag)
        assertEquals(displayOption, item.displayOption)
    }

    @Test
    fun `createItem_withDefaultValues_thenUsesDefaults`() {
        // Given
        val title = "Событие"
        val timestamp = 1234567890000L

        // When
        val item = Item(
            title = title,
            timestamp = timestamp
        )

        // Then
        assertEquals(0L, item.id)
        assertEquals("", item.details)
        assertNull(item.colorTag)
        assertEquals(DisplayOption.DEFAULT, item.displayOption)
    }

    @Test
    fun `makeDaysCount_whenCalled_thenReturnsStubValue`() {
        // Given
        val item = Item(
            title = "Тест",
            timestamp = 1234567890000L
        )
        val currentDate = System.currentTimeMillis()

        // When
        val result = item.makeDaysCount(currentDate)

        // Then
        // Заглушка, полная реализация в Этапе 6
        assertEquals("0 дней", result)
    }

    @Test
    fun `makeDaysCount_withDifferentCurrentDate_thenReturnsStubValue`() {
        // Given
        val item = Item(
            title = "Тест",
            timestamp = 1000000000000L
        )
        val currentDate1 = 2000000000000L
        val currentDate2 = 3000000000000L

        // When
        val result1 = item.makeDaysCount(currentDate1)
        val result2 = item.makeDaysCount(currentDate2)

        // Then
        // Заглушка всегда возвращает "0 дней"
        assertEquals("0 дней", result1)
        assertEquals("0 дней", result2)
    }
}

