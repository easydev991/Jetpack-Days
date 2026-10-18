package com.dayscounter.util

/**
 * Интерфейс репортинга исключений в crash-канал (в release — Firebase Crashlytics).
 *
 * Позволяет domain/data/VM слоям репортить сбои без статической зависимости
 * от Firebase: реализация выбирается в `AppModule.createCrashReporter()`
 * по `BuildConfig.DEBUG` (Noop в debug/тестах, Firebase в release).
 */
interface CrashReporter {
    /**
     * Логирует исключение в crash-канал.
     *
     * @param throwable Исключение для логирования
     * @param message Дополнительное сообщение (опционально)
     */
    fun logException(
        throwable: Throwable,
        message: String? = null
    )
}
