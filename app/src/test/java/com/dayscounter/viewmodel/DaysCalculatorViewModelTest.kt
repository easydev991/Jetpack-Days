package com.dayscounter.viewmodel

import com.dayscounter.data.formatter.DaysFormatterImpl
import com.dayscounter.data.formatter.StubResourceProvider
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.util.NoOpLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Unit-тесты для [DaysCalculatorViewModel].
 *
 * Проверяют корректность вычисления разницы дат и форматирования текста.
 *
 * Тесты создают реальные экземпляры Use Cases вместо моков,
 * так как ручной DI не требует сложной настройки для unit-тестирования.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Config(sdk = [30])
class DaysCalculatorViewModelTest {
    private lateinit var calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase
    private lateinit var formatDaysTextUseCase: FormatDaysTextUseCase

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `displayOption when created then uses default value`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(testDispatcher)

            // Создаем реальные экземпляры Use Cases вместо моков
            calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
            val formatter = DaysFormatterImpl()
            formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

            // Создаем ViewModel с NoOpLogger для тестов
            val viewModel =
                DaysCalculatorViewModel(
                    calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                    formatDaysTextUseCase = formatDaysTextUseCase,
                    resourceProvider = StubResourceProvider(),
                    defaultDisplayOption = DisplayOption.DAY,
                    logger = NoOpLogger(),
                )

            // Then
            Assertions.assertEquals(
                DisplayOption.DAY,
                viewModel.displayOption.value,
                "displayOption по умолчанию должен быть DAY",
            )
        }

    @Test
    fun `reset when called then clears state`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(testDispatcher)

            // Создаем реальные экземпляры Use Cases вместо моков
            calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
            val formatter = DaysFormatterImpl()
            formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

            // Создаем ViewModel с NoOpLogger для тестов
            val viewModel =
                DaysCalculatorViewModel(
                    calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                    formatDaysTextUseCase = formatDaysTextUseCase,
                    resourceProvider = StubResourceProvider(),
                    defaultDisplayOption = DisplayOption.DAY,
                    logger = NoOpLogger(),
                )

            // Given - устанавливаем состояние
            viewModel.calculateDays(1704067200000L, LocalDate.of(2024, 1, 10))
            advanceUntilIdle() // Ждем выполнения корутин

            // Проверяем, что состояние установилось
            val stateBefore = viewModel.state.value
            Assertions.assertNotNull(stateBefore.formattedText, "Должен быть форматированный текст перед reset")

            // When
            viewModel.reset()

            // Then
            val state = viewModel.state.value
            Assertions.assertEquals(false, state.isLoading, "isLoading должен быть false")
            Assertions.assertNull(state.formattedText, "formattedText должен быть null")
            Assertions.assertNull(state.error, "error должен быть null")
        }

    @Test
    fun `clearError when called then clears error state`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(testDispatcher)

            // Создаем реальные экземпляры Use Cases вместо моков
            calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
            val formatter = DaysFormatterImpl()
            formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

            // Создаем ViewModel с NoOpLogger для тестов
            val viewModel =
                DaysCalculatorViewModel(
                    calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                    formatDaysTextUseCase = formatDaysTextUseCase,
                    resourceProvider = StubResourceProvider(),
                    defaultDisplayOption = DisplayOption.DAY,
                    logger = NoOpLogger(),
                )

            // Given - сначала создаем состояние
            viewModel.calculateDays(1704067200000L, LocalDate.of(2024, 1, 10))
            advanceUntilIdle() // Ждем выполнения корутин

            // Then проверяем, что текст есть
            Assertions.assertNotNull(viewModel.state.value.formattedText, "Должен быть форматированный текст")

            // When
            viewModel.clearError()

            // Then
            val state = viewModel.state.value
            Assertions.assertNull(state.error, "error должен быть null после очистки")
            Assertions.assertNotNull(state.formattedText, "formattedText должен сохраниться")
        }

    @Test
    fun `updateDisplayOption when called then updates display option`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(testDispatcher)

            // Создаем реальные экземпляры Use Cases вместо моков
            calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
            val formatter = DaysFormatterImpl()
            formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

            // Создаем ViewModel с NoOpLogger для тестов
            val viewModel =
                DaysCalculatorViewModel(
                    calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                    formatDaysTextUseCase = formatDaysTextUseCase,
                    resourceProvider = StubResourceProvider(),
                    defaultDisplayOption = DisplayOption.DAY,
                    logger = NoOpLogger(),
                )

            // Given
            val newOption = DisplayOption.MONTH_DAY

            // When
            viewModel.updateDisplayOption(newOption)

            // Then
            Assertions.assertEquals(newOption, viewModel.displayOption.value, "displayOption должен быть MONTH_DAY")
        }

    @Test
    fun `calculateDays when timestamp provided then updates state`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(testDispatcher)

            // Создаем реальные экземпляры Use Cases вместо моков
            calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
            val formatter = DaysFormatterImpl()
            formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

            // Создаем ViewModel с NoOpLogger для тестов
            val viewModel =
                DaysCalculatorViewModel(
                    calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                    formatDaysTextUseCase = formatDaysTextUseCase,
                    resourceProvider = StubResourceProvider(),
                    defaultDisplayOption = DisplayOption.DAY,
                    logger = NoOpLogger(),
                )

            // Given
            val timestamp = 1704067200000L // 2024-01-01 00:00:00 UTC
            val currentDate = LocalDate.of(2024, 1, 10) // Дата через 9 дней после события
            val expectedDays = 9

            // When
            viewModel.calculateDays(timestamp, currentDate)
            advanceUntilIdle() // Ждем выполнения корутин

            // Then
            val state = viewModel.state.value
            Assertions.assertEquals(false, state.isLoading, "isLoading должен быть false после вычисления")
            Assertions.assertNotNull(state.formattedText, "formattedText должен быть не null")
            Assertions.assertTrue(
                state.formattedText!!.contains("$expectedDays"),
                "formattedText должен содержать '$expectedDays дней'",
            )
            Assertions.assertNull(state.error, "error должен быть null")
        }

    @Test
    fun `calculateDays with custom displayOption then uses custom option`() =
        runTest {
            val testDispatcher = UnconfinedTestDispatcher()
            Dispatchers.setMain(testDispatcher)

            // Создаем реальные экземпляры Use Cases вместо моков
            calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
            val formatter = DaysFormatterImpl()
            formatDaysTextUseCase = FormatDaysTextUseCase(formatter)

            // Создаем ViewModel с NoOpLogger для тестов
            val viewModel =
                DaysCalculatorViewModel(
                    calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
                    formatDaysTextUseCase = formatDaysTextUseCase,
                    resourceProvider = StubResourceProvider(),
                    defaultDisplayOption = DisplayOption.DAY,
                    logger = NoOpLogger(),
                )

            // Given
            val timestamp = 1704067200000L // 2024-01-01
            val currentDate = LocalDate.of(2024, 1, 10)
            val customOption = DisplayOption.YEAR_MONTH_DAY

            // When
            viewModel.calculateDays(timestamp, currentDate, displayOption = customOption)
            advanceUntilIdle() // Ждем выполнения корутин

            // Then
            val state = viewModel.state.value
            Assertions.assertNotNull(state.formattedText, "formattedText должен быть не null")
        }
}
