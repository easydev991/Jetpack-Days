package com.dayscounter.data.database.mapper

import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ItemMapperTest {

    @Test
    fun `toDomain_whenEntityWithAllFields_thenConvertsCorrectly`() {
        // Given
        val entity = ItemEntity(
            id = 1L,
            title = "Тестовое событие",
            details = "Описание",
            timestamp = 1234567890000L,
            colorTag = 0xFFFF0000.toInt(),
            displayOption = DisplayOption.MONTH_DAY.name
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals(1L, domain.id)
        assertEquals("Тестовое событие", domain.title)
        assertEquals("Описание", domain.details)
        assertEquals(1234567890000L, domain.timestamp)
        assertEquals(0xFFFF0000.toInt(), domain.colorTag)
        assertEquals(DisplayOption.MONTH_DAY, domain.displayOption)
    }

    @Test
    fun `toDomain_whenEntityWithNullColorTag_thenConvertsCorrectly`() {
        // Given
        val entity = ItemEntity(
            id = 2L,
            title = "Событие",
            timestamp = 1234567890000L,
            colorTag = null,
            displayOption = DisplayOption.DAY.name
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals(2L, domain.id)
        assertEquals("Событие", domain.title)
        assertNull(domain.colorTag)
        assertEquals(DisplayOption.DAY, domain.displayOption)
    }

    @Test
    fun `toDomain_whenEntityWithDefaultValues_thenConvertsCorrectly`() {
        // Given
        val entity = ItemEntity(
            title = "Событие",
            timestamp = 1234567890000L
        )

        // When
        val domain = entity.toDomain()

        // Then
        assertEquals(0L, domain.id)
        assertEquals("", domain.details)
        assertNull(domain.colorTag)
        assertEquals(DisplayOption.DEFAULT, domain.displayOption)
    }

    @Test
    fun `toEntity_whenDomainWithAllFields_thenConvertsCorrectly`() {
        // Given
        val domain = Item(
            id = 1L,
            title = "Тестовое событие",
            details = "Описание",
            timestamp = 1234567890000L,
            colorTag = 0xFFFF0000.toInt(),
            displayOption = DisplayOption.MONTH_DAY
        )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals(1L, entity.id)
        assertEquals("Тестовое событие", entity.title)
        assertEquals("Описание", entity.details)
        assertEquals(1234567890000L, entity.timestamp)
        assertEquals(0xFFFF0000.toInt(), entity.colorTag)
        assertEquals(DisplayOption.MONTH_DAY.name, entity.displayOption)
    }

    @Test
    fun `toEntity_whenDomainWithNullColorTag_thenConvertsCorrectly`() {
        // Given
        val domain = Item(
            id = 2L,
            title = "Событие",
            timestamp = 1234567890000L,
            colorTag = null,
            displayOption = DisplayOption.DAY
        )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals(2L, entity.id)
        assertEquals("Событие", entity.title)
        assertNull(entity.colorTag)
        assertEquals(DisplayOption.DAY.name, entity.displayOption)
    }

    @Test
    fun `toEntity_whenDomainWithDefaultValues_thenConvertsCorrectly`() {
        // Given
        val domain = Item(
            title = "Событие",
            timestamp = 1234567890000L
        )

        // When
        val entity = domain.toEntity()

        // Then
        assertEquals(0L, entity.id)
        assertEquals("", entity.details)
        assertNull(entity.colorTag)
        assertEquals(DisplayOption.DAY.name, entity.displayOption)
    }

    @Test
    fun `roundTripConversion_thenPreservesAllFields`() {
        // Given
        val originalDomain = Item(
            id = 5L,
            title = "Оригинальное событие",
            details = "Детали",
            timestamp = 9876543210000L,
            colorTag = 0xFF00FF00.toInt(),
            displayOption = DisplayOption.YEAR_MONTH_DAY
        )

        // When
        val entity = originalDomain.toEntity()
        val convertedDomain = entity.toDomain()

        // Then
        assertEquals(originalDomain.id, convertedDomain.id)
        assertEquals(originalDomain.title, convertedDomain.title)
        assertEquals(originalDomain.details, convertedDomain.details)
        assertEquals(originalDomain.timestamp, convertedDomain.timestamp)
        assertEquals(originalDomain.colorTag, convertedDomain.colorTag)
        assertEquals(originalDomain.displayOption, convertedDomain.displayOption)
    }

    @Test
    fun `roundTripConversion_withNullColorTag_thenPreservesNull`() {
        // Given
        val originalDomain = Item(
            id = 6L,
            title = "Событие без цвета",
            timestamp = 1234567890000L,
            colorTag = null
        )

        // When
        val entity = originalDomain.toEntity()
        val convertedDomain = entity.toDomain()

        // Then
        assertNull(convertedDomain.colorTag)
        assertEquals(originalDomain, convertedDomain)
    }
}

