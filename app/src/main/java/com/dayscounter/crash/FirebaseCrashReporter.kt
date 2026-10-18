@file:Suppress("TooGenericExceptionCaught", "SwallowedException")

package com.dayscounter.crash

import com.dayscounter.util.CrashReporter
import com.google.firebase.Firebase
import com.google.firebase.crashlytics.crashlytics

/**
 * Crash-канал на Firebase Crashlytics для release-сборок.
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
class FirebaseCrashReporter : CrashReporter {
    /**
     * Логирует исключение в Crashlytics.
     *
     * @param throwable Исключение для логирования
     * @param message Дополнительное сообщение (опционально)
     */
    override fun logException(
        throwable: Throwable,
        message: String?
    ) {
        try {
            Firebase.crashlytics.apply {
                message?.let { setCustomKey("error_message", it) }
                recordException(throwable)
            }
        } catch (e: Exception) {
            // Не логируем ошибки при отправке в Crashlytics,
            // чтобы избежать бесконечного цикла
        }
    }
}
