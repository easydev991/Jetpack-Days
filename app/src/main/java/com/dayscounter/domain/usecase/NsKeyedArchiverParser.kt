package com.dayscounter.domain.usecase

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64
import kotlin.math.round

/**
 * Парсер NSKeyedArchiver для извлечения UIColor из iOS-бекапов.
 *
 * NSKeyedArchiver кодирует UIColor как бинарный plist (bplist00),
 * содержащий RGBA компоненты как float значения (0.0-1.0).
 *
 * Структура bplist00:
 * - Заголовок: "bplist00" (8 байт)
 * - Объекты: различные типы данных
 * - Offset table: смещения объектов
 * - Trailer: метаданные (32 байта в конце)
 */
@Suppress(
    "MagicNumber", // Бинарный формат bplist00 использует специфицированные байтовые маркеры
    "TooManyFunctions", // Low-level парсер требует много функций для каждого типа данных
    "CyclomaticComplexMethod", // Сложность обусловлена спецификацией бинарного формата
    "LongMethod", // Методы парсинга требуют обработки множества случаев
    "NestedBlockDepth", // Вложенность обусловлена структурой бинарного формата
    "ReturnCount", // Early returns необходимы для валидации бинарных данных
    "TooGenericExceptionCaught", // Парсер должен быть устойчив к любым ошибкам
    "SwallowedException", // Ошибки парсинга возвращают null вместо выброса исключений
    "LoopWithTooManyJumpStatements", // Continue используется для early exit в циклах парсинга
)
object NsKeyedArchiverParser {
    private const val BPLIST_MAGIC = "bplist00"
    private const val TRAILER_SIZE = 32
    private const val MAX_OBJECTS = 1000 // Защита от OOM
    private const val COLOR_MAX = 255
    private const val COLOR_MIN = 0

    // Специальный объект для представления null в bplist
    private object BplistNull

    // MARK: - Public API

    /**
     * Проверяет, является ли строка Base64-закодированным NSKeyedArchiver.
     *
     * @param base64String Строка для проверки
     * @return true если это валидный Base64 и начинается с "bplist00"
     */
    fun isNsKeyedArchiver(base64String: String): Boolean {
        if (base64String.isBlank()) return false

        return try {
            val bytes = Base64.getDecoder().decode(base64String)
            bytes.size >= BPLIST_MAGIC.length && String(
                bytes,
                0,
                BPLIST_MAGIC.length
            ) == BPLIST_MAGIC
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    /**
     * Парсит Base64 NSKeyedArchiver и извлекает цвет в hex-формате.
     *
     * @param base64String Base64-закодированный NSKeyedArchiver с UIColor
     * @return Hex-строка "#RRGGBB" или null если парсинг не удался
     */
    fun parseHexColor(base64String: String): String? {
        if (!isNsKeyedArchiver(base64String)) return null

        return try {
            val bytes = Base64.getDecoder().decode(base64String)
            println("DEBUG: bytes size = ${bytes.size}")
            val rgba = parseBplistColor(bytes)
            println("DEBUG: rgba = $rgba")
            rgba?.let { rgbaToHex(it) }
        } catch (e: Exception) {
            println("DEBUG: exception = ${e.message}")
            null
        }
    }

    // MARK: - Bplist Parsing

    private data class BplistTrailer(
        val offsetIntSize: Int,
        val objectRefSize: Int,
        val numObjects: Long,
        val topObject: Long,
        val offsetTableOffset: Long,
    )

    private data class Rgba(
        val red: Float,
        val green: Float,
        val blue: Float,
        val alpha: Float,
    )

    private fun parseBplistColor(bytes: ByteArray): Rgba? {
        if (bytes.size < BPLIST_MAGIC.length + TRAILER_SIZE) return null
        if (String(bytes, 0, BPLIST_MAGIC.length) != BPLIST_MAGIC) return null

        val trailer = parseTrailer(bytes) ?: run {
            println("DEBUG: parseTrailer returned null")
            return null
        }
        println("DEBUG: trailer = $trailer")
        val offsetTable = parseOffsetTable(bytes, trailer) ?: run {
            println("DEBUG: parseOffsetTable returned null")
            return null
        }
        println("DEBUG: offsetTable size = ${offsetTable.size}")
        val objects = parseObjects(bytes, offsetTable, trailer)
        println("DEBUG: objects count = ${objects.size}")
        objects.forEachIndexed { index, obj ->
            println("DEBUG: object[$index] = ${obj?.javaClass?.simpleName} = $obj")
        }

        return extractColorFromObjects(objects)
    }

    private fun parseTrailer(bytes: ByteArray): BplistTrailer? {
        if (bytes.size < TRAILER_SIZE) return null

        val trailerStart = bytes.size - TRAILER_SIZE

        // Структура trailer (32 bytes):
        // [0-5]: unused (всегда 0)
        // [6]: offsetIntSize (1-8)
        // [7]: objectRefSize (1-8)
        // [8-15]: numObjects (8 bytes, big-endian)
        // [16-23]: topObject (8 bytes, big-endian)
        // [24-31]: offsetTableOffset (8 bytes, big-endian)

        val offsetIntSize = bytes[trailerStart + 6].toInt() and 0xFF
        val objectRefSize = bytes[trailerStart + 7].toInt() and 0xFF

        // Читаем 8-байтные значения (big-endian)
        fun readLong8(offset: Int): Long {
            var value = 0L
            for (i in 0 until 8) {
                value = (value shl 8) or (bytes[trailerStart + offset + i].toLong() and 0xFF)
            }
            return value
        }

        val numObjects = readLong8(8)
        val topObject = readLong8(16)
        val offsetTableOffset = readLong8(24)

        // Защита от некорректных данных
        if (numObjects !in 0..MAX_OBJECTS) return null
        if (offsetTableOffset < 0 || offsetTableOffset >= bytes.size) return null

        return BplistTrailer(
            offsetIntSize = offsetIntSize,
            objectRefSize = objectRefSize,
            numObjects = numObjects,
            topObject = topObject,
            offsetTableOffset = offsetTableOffset,
        )
    }

    private fun parseOffsetTable(
        bytes: ByteArray,
        trailer: BplistTrailer,
    ): LongArray? {
        // Проверка разумности offsetIntSize (1-8 байт)
        if (trailer.offsetIntSize !in 1..8) return null

        val numObjects = trailer.numObjects.toInt()
        val offsetTable = LongArray(numObjects)
        val buffer = ByteBuffer.wrap(bytes)
        buffer.position(trailer.offsetTableOffset.toInt())

        for (i in 0 until numObjects) {
            if (buffer.remaining() < trailer.offsetIntSize) return null
            offsetTable[i] = readInt(buffer, trailer.offsetIntSize)
            // Проверка, что смещение в пределах файла
            if (offsetTable[i] < 0 || offsetTable[i] >= bytes.size) return null
        }

        return offsetTable
    }

    private fun parseObjects(
        bytes: ByteArray,
        offsetTable: LongArray,
        trailer: BplistTrailer,
    ): List<Any?> {
        val objects = mutableListOf<Any?>()
        val buffer = ByteBuffer.wrap(bytes)

        for (offset in offsetTable) {
            buffer.position(offset.toInt())
            // parseObject может вернуть BplistNull для null-object - это нормально
            val obj = parseObject(buffer, trailer.objectRefSize)
            objects.add(if (obj === BplistNull) null else obj)
        }

        return objects
    }

    private fun parseObject(buffer: ByteBuffer, objectRefSize: Int): Any? {
        if (buffer.remaining() < 1) return null

        val marker = buffer.get().toInt() and 0xFF
        val highNibble = (marker shr 4) and 0x0F
        val lowNibble = marker and 0x0F

        // Возвращаем BplistNull для null-object (маркер 0x00)
        // Это позволяет отличить "null объект в bplist" от "ошибка парсинга"
        return when (highNibble) {
            0x0 -> when (lowNibble) {
                0x0 -> BplistNull // null object в bplist
                0x8 -> false // false
                0x9 -> true // true
                else -> null // неизвестный тип - ошибка
            }

            0x1 -> parseInteger(buffer, lowNibble)
            0x2 -> parseReal(buffer, lowNibble)
            0x3 -> parseDate(buffer)
            0x4 -> parseData(buffer, lowNibble)
            0x5 -> parseAsciiString(buffer, lowNibble)
            0x6 -> parseUtf16String(buffer, lowNibble)
            0x7 -> parseUtf8String(buffer, lowNibble)
            0x8 -> parseUid(buffer, lowNibble + 1)
            0xA -> parseArray(buffer, lowNibble, objectRefSize)
            0xC -> parseSet(buffer, lowNibble, objectRefSize)
            0xD -> parseDict(buffer, lowNibble, objectRefSize)
            else -> null
        }
    }

    // MARK: - Object Parsers

    private fun parseInteger(buffer: ByteBuffer, sizeHint: Int): Long {
        val size = when (sizeHint) {
            0x0 -> 1
            0x1 -> 2
            0x2 -> 4
            0x3 -> 8
            else -> 1 shl sizeHint
        }

        var value = 0L
        repeat(size) {
            value = (value shl 8) or (buffer.get().toLong() and 0xFF)
        }
        return value
    }

    private fun parseReal(buffer: ByteBuffer, sizeHint: Int): Float {
        return when (sizeHint) {
            0x2 -> {
                buffer.order(ByteOrder.BIG_ENDIAN)
                buffer.float
            }

            0x3 -> {
                buffer.order(ByteOrder.BIG_ENDIAN)
                val double = buffer.double
                double.toFloat()
            }

            else -> 0f
        }
    }

    private fun parseDate(buffer: ByteBuffer): Double {
        buffer.order(ByteOrder.BIG_ENDIAN)
        return buffer.double
    }

    private fun parseData(buffer: ByteBuffer, sizeHint: Int): ByteArray {
        val size = readCount(buffer, sizeHint)
        val data = ByteArray(size)
        buffer.get(data)
        return data
    }

    private fun parseAsciiString(buffer: ByteBuffer, sizeHint: Int): String {
        val size = readCount(buffer, sizeHint)
        val bytes = ByteArray(size)
        buffer.get(bytes)
        return String(bytes, Charsets.US_ASCII)
    }

    private fun parseUtf16String(buffer: ByteBuffer, sizeHint: Int): String {
        val size = readCount(buffer, sizeHint)
        val bytes = ByteArray(size * 2)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_16BE)
    }

    private fun parseUtf8String(buffer: ByteBuffer, sizeHint: Int): String {
        val size = readCount(buffer, sizeHint)
        val bytes = ByteArray(size)
        buffer.get(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun parseUid(buffer: ByteBuffer, size: Int): Long {
        var value = 0L
        repeat(size) {
            value = (value shl 8) or (buffer.get().toLong() and 0xFF)
        }
        return value
    }

    private fun parseArray(buffer: ByteBuffer, sizeHint: Int, objectRefSize: Int): List<Any?> {
        val size = readCount(buffer, sizeHint)
        val array = mutableListOf<Any?>()
        repeat(size) {
            val ref = readInt(buffer, objectRefSize).toInt()
            array.add(ref) // Сохраняем как ссылку, разрешим позже
        }
        return array
    }

    private fun parseSet(buffer: ByteBuffer, sizeHint: Int, objectRefSize: Int): Set<Any?> {
        val size = readCount(buffer, sizeHint)
        val set = mutableSetOf<Any?>()
        repeat(size) {
            val ref = readInt(buffer, objectRefSize).toInt()
            set.add(ref)
        }
        return set
    }

    private fun parseDict(
        buffer: ByteBuffer,
        sizeHint: Int,
        objectRefSize: Int
    ): Map<String, Any?> {
        val size = readCount(buffer, sizeHint)
        val keys = mutableListOf<Int>()
        val values = mutableListOf<Int>()

        repeat(size) {
            keys.add(readInt(buffer, objectRefSize).toInt())
        }
        repeat(size) {
            values.add(readInt(buffer, objectRefSize).toInt())
        }

        // Возвращаем как мапу ссылок (ключ -> значение)
        val dict = mutableMapOf<String, Any?>()
        for (i in 0 until size) {
            dict["key_${keys[i]}"] = values[i]
        }
        return dict
    }

    // MARK: - Helpers

    private fun readCount(buffer: ByteBuffer, hint: Int): Int {
        return if (hint == 0xF) {
            // Extended count - следующий байт определяет размер типа и значение
            val extMarker = buffer.get().toInt() and 0xFF
            val extType = (extMarker shr 4) and 0x0F
            val extSize = when (extType) {
                0x1 -> 1 // 1-byte count
                0x2 -> 2 // 2-byte count
                0x4 -> 4 // 4-byte count
                0x8 -> 8 // 8-byte count
                else -> return 0
            }
            readInt(buffer, extSize).toInt()
        } else {
            hint
        }
    }

    private fun readInt(buffer: ByteBuffer, size: Int): Long {
        var value = 0L
        repeat(size) {
            value = (value shl 8) or (buffer.get().toLong() and 0xFF)
        }
        return value
    }

    // MARK: - Color Extraction

    private fun extractColorFromObjects(objects: List<Any?>): Rgba? {
        // Ищем UIColor объект в $objects
        // UIColor - это dict с ключами: UIRed, UIGreen, UIBlue, UIAlpha, UIColorComponentCount

        for (obj in objects) {
            if (obj is Map<*, *>) {
                // Проверяем есть ли в этом dict цветовые ключи
                val hasColorKeys = obj.entries.any { (key, _) ->
                    val keyStr = key.toString()
                    if (keyStr.startsWith("key_")) {
                        val keyRef = keyStr.removePrefix("key_").toIntOrNull()
                        if (keyRef != null && keyRef < objects.size) {
                            val keyObj = objects[keyRef]
                            keyObj is String && keyObj in
                                listOf(
                                    "UIRed",
                                    "UIGreen",
                                    "UIBlue",
                                    "UIColorComponentCount",
                                    "UIGree",
                                    "UIGren"
                                )
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                }

                if (hasColorKeys) {
                    val color = tryExtractColor(obj, objects)
                    if (color != null) {
                        return color
                    }
                }
            }
        }

        return null
    }

    private fun tryExtractColor(
        dict: Map<*, *>,
        objects: List<Any?>,
    ): Rgba? {
        var red: Float? = null
        var green: Float? = null
        var blue: Float? = null
        var alpha = 1.0f

        // Ищем UIColorComponentCount = 4, чтобы подтвердить что это UIColor
        val hasComponentCount = dict.entries.any { (key, value) ->
            val keyStr = key.toString()
            if (keyStr.startsWith("key_")) {
                val keyRef = keyStr.removePrefix("key_").toIntOrNull()
                if (keyRef != null && keyRef < objects.size) {
                    val keyObj = objects[keyRef]
                    if (keyObj == "UIColorComponentCount") {
                        val valueRef = (value as? Int) ?: return@any false
                        if (valueRef < objects.size) {
                            val numValue = when (val valueObj = objects[valueRef]) {
                                is Long -> valueObj
                                is Int -> valueObj.toLong()
                                else -> return@any false
                            }
                            numValue == 4L
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
        }

        if (!hasComponentCount) {
            // Альтернативно, проверяем наличие $class = UIColor
            val hasUIClass = dict.entries.any { (key, value) ->
                val keyStr = key.toString()
                if (keyStr.startsWith("key_")) {
                    val keyRef = keyStr.removePrefix("key_").toIntOrNull()
                    if (keyRef != null && keyRef < objects.size) {
                        val keyObj = objects[keyRef]
                        if (keyObj == $$"$class") {
                            val valueRef = (value as? Int) ?: return@any false
                            if (valueRef < objects.size) {
                                val classDict = objects[valueRef] as? Map<*, *>
                                return@any checkClassIsUIColor(classDict, objects)
                            }
                        }
                        false
                    } else {
                        false
                    }
                } else {
                    false
                }
            }

            if (!hasUIClass) return null
        }

        // Извлекаем RGBA компоненты
        for ((key, value) in dict.entries) {
            val keyStr = key.toString()
            if (!keyStr.startsWith("key_")) continue

            val keyRef = keyStr.removePrefix("key_").toIntOrNull() ?: continue
            if (keyRef >= objects.size) continue

            val keyObj = objects[keyRef] as? String ?: continue
            val valueRef = (value as? Int) ?: continue
            if (valueRef >= objects.size) continue

            val floatValue = when (val valueObj = objects[valueRef]) {
                is Float -> valueObj
                is Double -> valueObj.toFloat()
                is Long -> valueObj.toFloat()
                else -> continue
            }

            when (keyObj) {
                "UIRed" -> red = floatValue
                "UIGreen", "UIGree", "UIGren" -> green =
                    floatValue // "UIGree"/"UIGren" - обрезанные имена в iOS bplist
                "UIBlue" -> blue = floatValue
                "UIAlpha" -> alpha = floatValue
            }
        }

        if (red != null && green != null && blue != null) {
            return Rgba(red, green, blue, alpha)
        }

        return null
    }

    private fun checkClassIsUIColor(
        classDict: Map<*, *>?,
        objects: List<Any?>,
    ): Boolean {
        if (classDict == null) return false

        // Ищем $classname = "UIColor"
        for ((key, value) in classDict.entries) {
            val keyStr = key.toString()
            if (!keyStr.startsWith("key_")) continue

            val keyRef = keyStr.removePrefix("key_").toIntOrNull() ?: continue
            if (keyRef >= objects.size) continue

            val keyObj = objects[keyRef] as? String ?: continue
            if (keyObj != $$"$classname") continue

            val valueRef = (value as? Int) ?: continue
            if (valueRef >= objects.size) continue

            val className = objects[valueRef] as? String
            return className == "UIColor"
        }

        return false
    }

    // MARK: - Color Conversion

    private fun rgbaToHex(rgba: Rgba): String {
        val r = round(rgba.red * COLOR_MAX).toInt().coerceIn(COLOR_MIN, COLOR_MAX)
        val g = round(rgba.green * COLOR_MAX).toInt().coerceIn(COLOR_MIN, COLOR_MAX)
        val b = round(rgba.blue * COLOR_MAX).toInt().coerceIn(COLOR_MIN, COLOR_MAX)
        return "#%02X%02X%02X".format(r, g, b)
    }
}
