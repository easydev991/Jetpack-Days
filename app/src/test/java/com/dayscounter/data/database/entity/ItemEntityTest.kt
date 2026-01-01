package com.dayscounter.data.database.entity

import com.dayscounter.domain.model.DisplayOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ItemEntityTest {
    @Test
    fun `createItemEntity_withRequiredFields_thenCreatesSuccessfully`() {
        // Given
        val title = "Тестовое событие"
        val timestamp = 1234567890000L

        // When
        val entity =
            ItemEntity(
                title = title,
                timestamp = timestamp,
            )

        // Then
        assertEquals(0L, entity.id)
        assertEquals(title, entity.title)
        assertEquals("", entity.details)
        assertEquals(timestamp, entity.timestamp)
        assertNull(entity.colorTag)
        assertEquals(DisplayOption.DAY.name, entity.displayOption)
    }

    @Test
    fun `createItemEntity_withAllFields_thenCreatesSuccessfully`() {
        // Given
        val id = 1L
        val title = "Тестовое событие"
        val details = "Описание события"
        val timestamp = 1234567890000L
        val colorTag = 0xFFFF0000.toInt() // Красный цвет
        val displayOption = DisplayOption.MONTH_DAY.name

        // When
        val entity =
            ItemEntity(
                id = id,
                title = title,
                details = details,
                timestamp = timestamp,
                colorTag = colorTag,
                displayOption = displayOption,
            )

        // Then
        assertEquals(id, entity.id)
        assertEquals(title, entity.title)
        assertEquals(details, entity.details)
        assertEquals(timestamp, entity.timestamp)
        assertEquals(colorTag, entity.colorTag)
        assertEquals(displayOption, entity.displayOption)
    }

    @Test
    fun `createItemEntity_withDefaultValues_thenUsesDefaults`() {
        // Given
        val title = "Событие"
        val timestamp = 1234567890000L

        // When
        val entity =
            ItemEntity(
                title = title,
                timestamp = timestamp,
            )

        // Then
        assertEquals(0L, entity.id)
        assertEquals("", entity.details)
        assertNull(entity.colorTag)
        assertEquals(DisplayOption.DAY.name, entity.displayOption)
    }
}
