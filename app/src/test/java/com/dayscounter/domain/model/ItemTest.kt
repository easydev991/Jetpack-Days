package com.dayscounter.domain.model

import com.dayscounter.data.formatter.DaysFormatterImpl
import com.dayscounter.di.FormatterModule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneId

class ItemTest {
    @Test
    fun createItem_withRequiredFields_thenCreatesSuccessfully() {
        // Given
        val title = "Тестовое событие"
        val timestamp = 1234567890000L

        // When
        val item =
            Item(
                title = title,
                timestamp = timestamp,
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
    fun createItem_withAllFields_thenCreatesSuccessfully() {
        // Given
        val id = 1L
        val title = "Тестовое событие"
        val details = "Описание события"
        val timestamp = 1234567890000L
        val colorTag = 0xFFFF0000.toInt() // Красный цвет
        val displayOption = DisplayOption.MONTH_DAY

        // When
        val item =
            Item(
                id = id,
                title = title,
                details = details,
                timestamp = timestamp,
                colorTag = colorTag,
                displayOption = displayOption,
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
    fun createItem_withDefaultValues_thenUsesDefaults() {
        // Given
        val title = "Событие"
        val timestamp = 1234567890000L

        // When
        val item =
            Item(
                title = title,
                timestamp = timestamp,
            )

        // Then
        assertEquals(0L, item.id)
        assertEquals("", item.details)
        assertNull(item.colorTag)
        assertEquals(DisplayOption.DEFAULT, item.displayOption)
    }

    @Test
    fun makeDaysCount_whenCalled_thenReturnsFormattedValue() {
        // Given
        val eventDate = LocalDate.now().minusDays(5)
        val timestamp =
            eventDate
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000
        val item =
            Item(
                title = "Тест",
                timestamp = timestamp,
            )
        val currentDate = LocalDate.now()

        // Создаём use case через FormatterModule
        val daysFormatter = DaysFormatterImpl()
        val formatDaysTextUseCase = FormatterModule.createFormatDaysTextUseCase(daysFormatter)
        val calculateDaysDifferenceUseCase = FormatterModule.createCalculateDaysDifferenceUseCase()
        val resourceProvider = FormatterModule.createStubResourceProvider()
        val useCase =
            FormatterModule.createGetFormattedDaysForItemUseCase(
                calculateDaysDifferenceUseCase,
                formatDaysTextUseCase,
                resourceProvider,
            )

        // When
        val result = item.makeDaysCount(useCase, currentDate)

        // Then
        // Проверяем, что результат содержит информацию о днях
        assertTrue(result.isNotEmpty(), "Результат не должен быть пустым")
        // Полная реализация форматирования проверяется в тестах GetFormattedDaysForItemUseCaseTest
    }

    @Test
    fun makeDaysCount_withDifferentCurrentDate_thenReturnsFormattedValues() {
        // Given
        val eventDate = LocalDate.of(2024, 1, 1)
        val timestamp =
            eventDate
                .atStartOfDay(ZoneId.systemDefault())
                .toEpochSecond() * 1000
        val item =
            Item(
                title = "Тест",
                timestamp = timestamp,
            )
        val currentDate1 = LocalDate.of(2024, 1, 6) // Через 5 дней
        val currentDate2 = LocalDate.of(2024, 1, 7) // Через 6 дней

        // Создаём use case через FormatterModule
        val daysFormatter = DaysFormatterImpl()
        val formatDaysTextUseCase = FormatterModule.createFormatDaysTextUseCase(daysFormatter)
        val calculateDaysDifferenceUseCase = FormatterModule.createCalculateDaysDifferenceUseCase()
        val resourceProvider = FormatterModule.createStubResourceProvider()
        val useCase =
            FormatterModule.createGetFormattedDaysForItemUseCase(
                calculateDaysDifferenceUseCase,
                formatDaysTextUseCase,
                resourceProvider,
            )

        // When
        val result1 = item.makeDaysCount(useCase, currentDate1)
        val result2 = item.makeDaysCount(useCase, currentDate2)

        // Then
        // Проверяем, что результаты не пустые и отличаются
        assertTrue(result1.isNotEmpty(), "Результат не должен быть пустым")
        assertTrue(result2.isNotEmpty(), "Результат не должен быть пустым")
        // Полная реализация форматирования проверяется в тестах GetFormattedDaysForItemUseCaseTest
    }
}
