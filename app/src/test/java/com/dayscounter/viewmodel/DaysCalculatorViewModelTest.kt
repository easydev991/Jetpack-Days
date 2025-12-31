package com.dayscounter.viewmodel

import com.dayscounter.data.formatter.DaysFormatterImpl
import com.dayscounter.data.formatter.StubResourceProvider
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit-тесты для [DaysCalculatorViewModel].
 *
 * Проверяют корректность вычисления разницы дат и форматирования текста.
 *
 * Тесты создают реальные экземпляры Use Cases вместо моков,
 * так как ручной DI не требует сложной настройки для unit-тестирования.
 */
class DaysCalculatorViewModelTest {
    private lateinit var viewModel: DaysCalculatorViewModel

    private lateinit var calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase
    private lateinit var formatDaysTextUseCase: FormatDaysTextUseCase

    @Before
    fun setUp() {
        // Создаем реальные экземпляры Use Cases вместо моков
        calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
        val formatter = DaysFormatterImpl()
        formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

        // Создаем ViewModel с StubResourceProvider для тестов
        viewModel =
            DaysCalculatorViewModel(
                calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                formatDaysTextUseCase = formatDaysTextUseCase,
                resourceProvider = StubResourceProvider(),
                defaultDisplayOption = DisplayOption.DAY,
            )
    }

    @Test
    fun `displayOption when created then uses default value`() {
        // Then
        Assert.assertEquals(
            "displayOption по умолчанию должен быть DAY",
            DisplayOption.DAY,
            viewModel.displayOption.value,
        )
    }

    @Test
    fun `reset when called then clears state`() {
        // Given - устанавливаем состояние
        viewModel.calculateDays(1704067200000L)

        // Ждем завершения асинхронных операций
        Thread.sleep(100)

        // When
        viewModel.reset()

        // Then
        val state = viewModel.state.value
        Assert.assertEquals("isLoading должен быть false", false, state.isLoading)
        Assert.assertNull("formattedText должен быть null", state.formattedText)
        Assert.assertNull("error должен быть null", state.error)
    }

    @Test
    fun `clearError when called then clears error state`() {
        // Given - сначала создаем состояние
        viewModel.calculateDays(1704067200000L)
        Thread.sleep(100)

        // Then проверяем, что текст есть
        Assert.assertNotNull(viewModel.state.value.formattedText, "Должен быть форматированный текст")

        // When
        viewModel.clearError()

        // Then
        val state = viewModel.state.value
        Assert.assertNull("error должен быть null после очистки", state.error)
        Assert.assertNotNull("formattedText должен сохраниться", state.formattedText)
    }

    @Test
    fun `updateDisplayOption when called then updates display option`() {
        // Given
        val newOption = DisplayOption.MONTH_DAY

        // When
        viewModel.updateDisplayOption(newOption)

        // Then
        Thread.sleep(50)
        Assert.assertEquals("displayOption должен быть MONTH_DAY", newOption, viewModel.displayOption.value)
    }

    @Test
    fun `calculateDays when timestamp provided then updates state`() {
        // Given
        val timestamp = 1704067200000L // 2024-01-01 00:00:00 UTC
        val currentDate = LocalDate.of(2024, 1, 6)
        val expectedDays = 5

        // When
        viewModel.calculateDays(timestamp, currentDate)

        // Ждем завершения асинхронных операций
        Thread.sleep(100)

        // Then
        val state = viewModel.state.value
        Assert.assertEquals("isLoading должен быть false после вычисления", false, state.isLoading)
        Assert.assertNotNull("formattedText должен быть не null", state.formattedText)
        Assert.assertTrue(
            "formattedText должен содержать '$expectedDays дней'",
            state.formattedText!!.contains("$expectedDays"),
        )
        Assert.assertNull("error должен быть null", state.error)
    }

    @Test
    fun `calculateDays with custom displayOption then uses custom option`() {
        // Given
        val timestamp = 1704067200000L // 2024-01-01
        val customOption = DisplayOption.YEAR_MONTH_DAY

        // When
        viewModel.calculateDays(timestamp, displayOption = customOption)

        // Ждем завершения асинхронных операций
        Thread.sleep(100)

        // Then
        val state = viewModel.state.value
        Assert.assertNotNull(state.formattedText, "formattedText должен быть не null")
    }
}
