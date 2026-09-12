package com.dayscounter.domain.usecase

/**
 * Исключение при ошибке проверки обновлений приложения.
 *
 * @property message Сообщение об ошибке
 * @property cause Причина ошибки (опционально)
 */
class AppUpdateException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
