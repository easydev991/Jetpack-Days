package com.dayscounter.domain.usecase

import kotlinx.serialization.Serializable

private const val MILLIS_PER_SECOND = 1000.0

/**
 * DTO для парсинга JSON резервных копий из iOS-приложения.
 *
 * Отличия от Android-формата (BackupItem):
 * - `timestamp`: Double (секунды с 1970-01-01) вместо Long (миллисекунды)
 * - `colorTag`: Base64-строка (NSKeyedArchiver) вместо hex-строки (#RRGGBB)
 *
 * @property title Название события
 * @property details Описание события (необязательное)
 * @property timestamp Дата события в секундах с 1970-01-01 (Double)
 * @property colorTag Цвет в формате Base64 NSKeyedArchiver (iOS UIColor)
 * @property displayOption Опция отображения (camelCase enum: "day", "monthDay", "yearMonthDay")
 */
@Serializable
data class IosBackupItem(
    val title: String,
    val details: String?,
    val timestamp: Double,
    val colorTag: String?,
    val displayOption: String,
)

/**
 * Конвертирует IosBackupItem в BackupItem (Android формат).
 *
 * Выполняет следующие преобразования:
 * - timestamp: секунды → миллисекунды (умножение на 1000)
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

    // Конвертируем timestamp: секунды → миллисекунды
    val androidTimestamp = (timestamp * MILLIS_PER_SECOND).toLong()

    // Конвертируем colorTag: Base64 NSKeyedArchiver → hex-строка
    val androidColorTag: String? = colorTag?.let { iosColorTag ->
        NsKeyedArchiverParser.parseHexColor(iosColorTag)
    }

    return BackupItem(
        title = title,
        details = details,
        timestamp = androidTimestamp,
        colorTag = androidColorTag,
        displayOption = displayOption,
    )
}
