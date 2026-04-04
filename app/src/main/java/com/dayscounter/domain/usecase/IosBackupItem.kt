package com.dayscounter.domain.usecase

import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000.0

/**
 * Разница между 1970-01-01 00:00:00 GMT и 2001-01-01 00:00:00 GMT в секундах.
 *
 * iOS использует `timeIntervalSinceReferenceDate` (секунды с 2001-01-01),
 * а Android использует `System.currentTimeMillis()` (миллисекунды с 1970-01-01).
 */
private const val IOS_REFERENCE_DATE_OFFSET_SECONDS = 978307200L

/**
 * DTO для парсинга JSON резервных копий из iOS-приложения.
 *
 * Отличия от Android-формата (BackupItem):
 * - `timestamp`: Double (секунды с 2001-01-01) вместо Long (миллисекунды с 1970-01-01)
 * - `colorTag`: Base64-строка (NSKeyedArchiver) вместо hex-строки (#RRGGBB)
 *
 * @property title Название события
 * @property details Описание события (необязательное)
 * @property timestamp Дата события в секундах с 2001-01-01 (timeIntervalSinceReferenceDate)
 * @property colorTag Цвет в формате Base64 NSKeyedArchiver (iOS UIColor)
 * @property displayOption Опция отображения (camelCase enum: "day", "monthDay", "yearMonthDay")
 */
@Serializable
data class IosBackupItem(
    val title: String,
    val details: String? = null,
    val timestamp: Double,
    val colorTag: String? = null,
    val displayOption: String
)

/**
 * Конвертирует IosBackupItem в BackupItem (Android формат).
 *
 * Выполняет следующие преобразования:
 * - timestamp: секунды с 2001-01-01 → миллисекунды с 1970-01-01
 *   (добавляем IOS_REFERENCE_DATE_OFFSET_SECONDS и умножаем на 1000)
 * - colorTag: Base64 NSKeyedArchiver → hex-строка (#RRGGBB)
 * - displayOption: проверка валидности
 *
 * @return BackupItem в Android-формате или null, если displayOption невалидный
 */
fun IosBackupItem.toBackupItem(): BackupItem? {
    // Проверяем валидность displayOption
    val validDisplayOptions = setOf("day", "monthDay", "yearMonthDay")
    if (displayOption !in validDisplayOptions) {
        return null
    }

    // Конвертируем timestamp:
    // iOS: секунды с 2001-01-01 → Android: миллисекунды с 1970-01-01
    // Формула: ((seconds + 978307200) * 1000)
    val androidTimestamp =
        ((timestamp + IOS_REFERENCE_DATE_OFFSET_SECONDS) * MILLIS_PER_SECOND).toLong()

    // Конвертируем colorTag: Base64 NSKeyedArchiver → hex-строка
    val androidColorTag: String? =
        colorTag?.let { iosColorTag ->
            NsKeyedArchiverParser.parseHexColor(iosColorTag)
        }

    return BackupItem(
        title = title,
        details = details,
        timestamp = androidTimestamp,
        colorTag = androidColorTag,
        displayOption = displayOption
    )
}
