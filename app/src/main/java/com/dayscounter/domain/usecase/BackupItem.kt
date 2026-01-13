package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import kotlinx.serialization.Serializable
import java.util.Locale

private const val HEX_RADIX = 16
private const val ALPHA_CHANNEL = 0xFF
private const val RGB_MASK = 0x00FFFFFF
private const val ALPHA_SHIFT = 24

/**
 * DTO для работы с резервными копиями (экспорт/импорт).
 *
 * Формат совместим с iOS-приложением:
 * - `colorTag`: String? (hex-строка в Android, Base64-строка в iOS)
 * - `displayOption`: String (enum значения в camelCase: "day", "monthDay", "yearMonthDay")
 * - `timestamp`: Long (миллисекунды с 1970-01-01)
 *
 * @property title Название события
 * @property details Описание события (необязательное)
 * @property timestamp Дата события в миллисекундах
 * @property colorTag Цвет в формате строки (hex или Base64)
 * @property displayOption Опция отображения (camelCase enum)
 */
@Serializable
data class BackupItem(
    val title: String,
    val details: String?,
    val timestamp: Long,
    val colorTag: String?,
    val displayOption: String,
)

/**
 * Конвертирует ARGB цвет в hex-строку.
 *
 * @return Hex-строка в формате #RRGGBB
 */
fun Int.toHexColor(): String {
    // Извлекаем RGB компоненты (без альфа-канала)
    val rgb = this and RGB_MASK
    return String.format(Locale.US, "#%06X", rgb)
}

/**
 * Конвертирует hex-строку в ARGB цвет.
 *
 * @param hexColor Hex-строка в формате #RRGGBB
 * @return ARGB цвет (Int) или null, если формат некорректный
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun String.fromHexColor(): Int? =
    try {
        // Убираем символ '#' если он есть
        val hex =
            if (startsWith("#")) {
                substring(1)
            } else {
                this
            }
        // Парсим RGB из hex строки
        val rgb = hex.toInt(HEX_RADIX)
        // Добавляем альфа-канал (255 = 0xFF) для полной непрозрачности
        // Результат: 0xAARRGGBB
        (ALPHA_CHANNEL shl ALPHA_SHIFT) or rgb
    } catch (e: Exception) {
        // Игнорируем ошибки парсинга - некорректный формат hex цвета
        null
    }

/**
 * Расширение для конвертации Item в BackupItem.
 */
fun Item.toBackupItem(): BackupItem =
    BackupItem(
        title = title,
        details = details,
        timestamp = timestamp,
        colorTag = colorTag?.toHexColor(), // Конвертируем Int? в hex-строку
        displayOption =
            when (displayOption) {
                DisplayOption.DAY -> "day"
                DisplayOption.MONTH_DAY -> "monthDay"
                DisplayOption.YEAR_MONTH_DAY -> "yearMonthDay"
            },
    )

/**
 * Расширение для конвертации BackupItem в Item.
 */
fun BackupItem.toItem(): Item? {
    val displayOption =
        when (displayOption) {
            "day" -> DisplayOption.DAY
            "monthDay" -> DisplayOption.MONTH_DAY
            "yearMonthDay" -> DisplayOption.YEAR_MONTH_DAY
            else -> {
                return null
            }
        }

    // Конвертируем colorTag из формата hex-строки в формат Android (Int ARGB)
    // При экспорте мы конвертируем Int в hex-строку (#RRGGBB)
    // При импорте нужно сделать обратную конвертацию
    val androidColorTag: Int? = colorTag?.fromHexColor()

    return Item(
        id = 0, // ID будет присвоен базой данных
        title = title,
        details = details ?: "", // Конвертируем String? в String с дефолтным значением
        timestamp = timestamp,
        colorTag = androidColorTag,
        displayOption = displayOption,
    )
}
