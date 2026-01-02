package com.dayscounter.util

/**
 * Реализация Logger для тестов.
 * Не выполняет никаких действий при вызове методов логирования.
 * Используется для избежания RuntimeException при вызове android.util.Log в unit-тестах.
 */
class NoOpLogger : Logger {
    override fun d(
        tag: String,
        message: String,
    ) {
        // Ничего не делаем в тестах
    }

    override fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // Ничего не делаем в тестах
    }

    override fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        // Ничего не делаем в тестах
    }
}
