package com.dayscounter.domain.usecase

import kotlinx.serialization.Serializable

/**
 * Формат резервной копии.
 *
 * Используется для определения платформы-источника бэкапа и правильной конвертации данных.
 * Сериализуется в lowercase строки ("android", "ios") для совместимости с iOS.
 */
@Serializable(with = BackupFormatSerializer::class)
enum class BackupFormat {
    /**
     * Android формат.
     * - timestamp: миллисекунды с 1970-01-01
     * - colorTag: hex-строка (#RRGGBB)
     */
    ANDROID,

    /**
     * iOS формат.
     * - timestamp: секунды с 2001-01-01 (timeIntervalSinceReferenceDate)
     * - colorTag: Base64 NSKeyedArchiver (UIColor)
     */
    IOS,
}
