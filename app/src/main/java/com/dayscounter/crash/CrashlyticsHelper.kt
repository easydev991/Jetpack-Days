@file:Suppress("TooGenericExceptionCaught", "SwallowedException")

package com.dayscounter.crash

import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

/**
 * Helper-класс для работы с Crashlytics.
 *
 * Обеспечивает безопасное логирование ошибок.
 *
 * Crashlytics автоматически собирает:
 * - Полные стек-трейсы исключений
 * - Модель устройства (device model)
 * - Версию Android (OS version)
 * - Разрешение экрана
 * - Другие метрики девайса
 *
 * Примечание: Мы используем общий перехват исключений здесь намеренно, чтобы избежать бесконечного
 * цикла при ошибках в самом Crashlytics.
 */
object CrashlyticsHelper {
    /**
     * Логирует исключение в Crashlytics.
     *
     * @param exception Исключение для логирования
     * @param message Дополнительное сообщение (опционально)
     */
    fun logException(
        exception: Throwable,
        message: String? = null,
    ) {
        try {
            Firebase.crashlytics.apply {
                message?.let { setCustomKey("error_message", it) }
                recordException(exception)
            }
        } catch (e: Exception) {
            // Не логируем ошибки при отправке в Crashlytics,
            // чтобы избежать бесконечного цикла
        }
    }
}
