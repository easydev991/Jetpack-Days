package com.dayscounter.di

import android.content.Context
import com.dayscounter.data.formatter.DaysFormatter
import com.dayscounter.data.formatter.DaysFormatterImpl
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.formatter.ResourceProviderImpl
import com.dayscounter.data.formatter.StubResourceProvider
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase

/**
 * DI модуль для форматирования количества дней.
 *
 * Использует ручной подход к внедрению зависимостей через factory методы.
 * Hilt не используется в проекте, так как ручной DI полностью удовлетворяет потребности.
 *
 * Для создания экземпляров используйте factory методы ниже.
 */
object FormatterModule {
    /**
     * Создает [StubResourceProvider].
     */
    fun createStubResourceProvider(): ResourceProvider = StubResourceProvider()

    /**
     * Создает [ResourceProvider].
     */
    fun createResourceProvider(context: Context): ResourceProvider = ResourceProviderImpl(context)

    /**
     * Создает [DaysFormatter].
     */
    fun createDaysFormatter(): DaysFormatter = DaysFormatterImpl()

    /**
     * Создает [CalculateDaysDifferenceUseCase].
     */
    fun createCalculateDaysDifferenceUseCase(): CalculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()

    /**
     * Создает [FormatDaysTextUseCase].
     */
    @Suppress("MaxLineLength")
    fun createFormatDaysTextUseCase(daysFormatter: DaysFormatter): FormatDaysTextUseCase = FormatDaysTextUseCase(daysFormatter)

    /**
     * Создает [GetFormattedDaysForItemUseCase].
     */
    fun createGetFormattedDaysForItemUseCase(
        calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
        formatDaysTextUseCase: FormatDaysTextUseCase,
    ): GetFormattedDaysForItemUseCase =
        GetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
        )
}
