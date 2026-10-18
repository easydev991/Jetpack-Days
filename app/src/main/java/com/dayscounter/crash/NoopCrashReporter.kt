package com.dayscounter.crash

import com.dayscounter.util.CrashReporter

/**
 * Noop crash-канал для debug и test окружений.
 *
 * Не выполняет никаких действий при логировании исключений
 * (зеркально `NoopAnalyticsProvider`).
 */
class NoopCrashReporter : CrashReporter {
    override fun logException(
        throwable: Throwable,
        message: String?
    ) {
        // Намеренный no-op: не отправляем ничего
    }
}
