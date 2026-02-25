package com.dayscounter.di

import android.content.Context
import com.dayscounter.data.provider.DaysFormatter
import com.dayscounter.data.provider.DaysFormatterImpl
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.data.provider.ResourceProviderImpl
import com.dayscounter.data.provider.StubResourceProvider
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetDaysAnalysisTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase

/**
 * DI модуль для форматирования количества дней.
 *
 * Использует ручной подход к внедрению зависимостей через factory методы.
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
    fun createCalculateDaysDifferenceUseCase(): CalculateDaysDifferenceUseCase =
        CalculateDaysDifferenceUseCase()

    /**
     * Создает [FormatDaysTextUseCase].
     */
    @Suppress("MaxLineLength")
    fun createFormatDaysTextUseCase(daysFormatter: DaysFormatter): FormatDaysTextUseCase =
        FormatDaysTextUseCase(daysFormatter)

    /**
     * Создает [GetFormattedDaysForItemUseCase].
     */
    fun createGetFormattedDaysForItemUseCase(
        calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
        formatDaysTextUseCase: FormatDaysTextUseCase,
        resourceProvider: ResourceProvider,
    ): GetFormattedDaysForItemUseCase =
        GetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            resourceProvider,
        )

    /**
     * Создает [GetDaysAnalysisTextUseCase].
     *
     * @param calculateDaysDifferenceUseCase Use case для вычисления разницы дат
     * @param getFormattedDaysForItemUseCase Use case для получения форматированного текста
     * @param resourceProvider Провайдер строковых ресурсов для локализации
     * @return Экземпляр GetDaysAnalysisTextUseCase
     */
    @Suppress("MaxLineLength")
    fun createGetDaysAnalysisTextUseCase(
        calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
        getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
        resourceProvider: ResourceProvider,
    ): GetDaysAnalysisTextUseCase =
        GetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
            resourceProvider = resourceProvider,
        )
}
