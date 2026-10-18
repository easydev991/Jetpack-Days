package com.dayscounter.di

import android.content.Context
import com.dayscounter.BuildConfig
import com.dayscounter.data.provider.DaysTextProviderImpl
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.data.provider.ResourceProviderImpl
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.CheckForAppUpdateUseCase
import com.dayscounter.domain.usecase.DaysFormatter
import com.dayscounter.domain.usecase.DaysTextProvider
import com.dayscounter.domain.usecase.DefaultDaysFormatter
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetDaysAnalysisTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.domain.usecase.http.HttpRequestExecutor
import com.dayscounter.domain.usecase.http.HttpResponse
import com.dayscounter.util.CrashReporter
import com.dayscounter.util.Logger
import java.net.HttpURLConnection
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/**
 * DI модуль для форматирования количества дней.
 *
 * Использует ручной подход к внедрению зависимостей через factory методы.
 *
 * Для создания экземпляров используйте factory методы ниже.
 */
object FormatterModule {
    /** Таймаут подключения к GitHub API, мс */
    private const val CONNECT_TIMEOUT_MS = 10_000

    /** Таймаут чтения ответа GitHub API, мс */
    private const val READ_TIMEOUT_MS = 10_000

    /**
     * HTTP-executor поверх платформенного [HttpsURLConnection] — единственное
     * сетевое взаимодействие в проекте (carve-out из офлайн-правила, см. design.md D1).
     * Каждый вызов создаёт новое соединение, state у executor'а нет.
     */
    private val httpsExecutor: HttpRequestExecutor =
        HttpRequestExecutor { request ->
            val connection = URI.create(request.url).toURL().openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "GET"
                request.headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.connect()
                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    HttpResponse(connection.responseCode, body)
                } else {
                    // Тело ошибки не читаем: use case маппит не-200 в AppUpdateException без парсинга
                    HttpResponse(connection.responseCode, "")
                }
            } finally {
                connection.disconnect()
            }
        }

    /**
     * Создает [ResourceProvider].
     */
    fun createResourceProvider(context: Context): ResourceProvider = ResourceProviderImpl(context)

    /**
     * Создает [DaysTextProvider].
     */
    fun createDaysTextProvider(context: Context): DaysTextProvider = DaysTextProviderImpl(context)

    /**
     * Создает [DaysFormatter].
     */
    fun createDaysFormatter(): DaysFormatter = DefaultDaysFormatter()

    /**
     * Создает [CalculateDaysDifferenceUseCase].
     */
    fun createCalculateDaysDifferenceUseCase(crashReporter: CrashReporter): CalculateDaysDifferenceUseCase =
        CalculateDaysDifferenceUseCase(crashReporter)

    /**
     * Создает [CheckForAppUpdateUseCase] с executor'ом на [HttpsURLConnection].
     */
    fun createCheckForAppUpdateUseCase(): CheckForAppUpdateUseCase =
        CheckForAppUpdateUseCase(
            httpsExecutor,
            currentVersion = BuildConfig.VERSION_NAME,
            logger = AppModule.createLogger()
        )

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
        textProvider: DaysTextProvider,
        crashReporter: CrashReporter,
        logger: Logger
    ): GetFormattedDaysForItemUseCase =
        GetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase,
            formatDaysTextUseCase,
            textProvider,
            crashReporter,
            logger
        )

    /**
     * Создает [GetDaysAnalysisTextUseCase].
     *
     * @param calculateDaysDifferenceUseCase Use case для вычисления разницы дат
     * @param getFormattedDaysForItemUseCase Use case для получения форматированного текста
     * @param textProvider Провайдер семантических строк для локализации
     * @return Экземпляр GetDaysAnalysisTextUseCase
     */
    @Suppress("MaxLineLength")
    fun createGetDaysAnalysisTextUseCase(
        calculateDaysDifferenceUseCase: CalculateDaysDifferenceUseCase,
        getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
        textProvider: DaysTextProvider
    ): GetDaysAnalysisTextUseCase =
        GetDaysAnalysisTextUseCase(
            calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
            getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
            textProvider = textProvider
        )
}
