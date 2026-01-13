package com.dayscounter.domain.usecase

/**
 * Исключение при ошибке работы с резервными копиями (экспорт/импорт).
 *
 * @property message Сообщение об ошибке
 * @property cause Причина ошибки (опционально)
 */
class BackupException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)
