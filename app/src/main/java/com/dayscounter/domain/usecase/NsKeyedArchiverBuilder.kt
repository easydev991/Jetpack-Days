package com.dayscounter.domain.usecase

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Base64

/**
 * Генератор NSKeyedArchiver (bplist00) для экспорта UIColor в iOS-совместимом формате.
 *
 * Создает бинарный plist с UIColor, который iOS может корректно импортировать.
 * Структура совместима с NsKeyedArchiverParser для round-trip конвертации.
 */
@Suppress(
    "MagicNumber",
    "TooManyFunctions",
    "LongMethod",
)
object NsKeyedArchiverBuilder {
    private const val BPLIST_MAGIC = "bplist00"

    // MARK: - Public API

    /**
     * Конвертирует hex-цвет в Base64 NSKeyedArchiver.
     *
     * @param hexColor Hex-строка в формате #RRGGBB
     * @return Base64-закодированный NSKeyedArchiver или null если формат некорректный
     */
    fun buildFromHexColor(hexColor: String): String? {
        val argb = parseHexToArgb(hexColor) ?: return null
        return buildFromArgb(argb)
    }

    /**
     * Конвертирует ARGB Int цвет в Base64 NSKeyedArchiver.
     *
     * @param argb ARGB цвет (Int)
     * @return Base64-закодированный NSKeyedArchiver
     */
    fun buildFromArgb(argb: Int): String {
        val r = ((argb shr 16) and 0xFF) / 255.0f
        val g = ((argb shr 8) and 0xFF) / 255.0f
        val b = (argb and 0xFF) / 255.0f
        val a = ((argb shr 24) and 0xFF) / 255.0f
        return buildFromRgba(r, g, b, a)
    }

    /**
     * Конвертирует RGBA float компоненты в Base64 NSKeyedArchiver.
     *
     * @param r Red (0.0 - 1.0)
     * @param g Green (0.0 - 1.0)
     * @param b Blue (0.0 - 1.0)
     * @param a Alpha (0.0 - 1.0)
     * @return Base64-закодированный NSKeyedArchiver
     */
    fun buildFromRgba(r: Float, g: Float, b: Float, a: Float): String {
        val bytes = buildBplist(r, g, b, a)
        return Base64.getEncoder().encodeToString(bytes)
    }

    // MARK: - Hex Parsing

    private fun parseHexToArgb(hexColor: String): Int? {
        return hexColor
            .takeIf { it.startsWith("#") }
            ?.removePrefix("#")
            ?.takeIf { it.length == 6 }
            ?.toIntOrNull(16)
            ?.let { rgb -> (0xFF000000.toInt()) or rgb }
    }

    // MARK: - Bplist Building

    @Suppress("SimplifiableCall")
    private fun buildBplist(r: Float, g: Float, b: Float, a: Float): ByteArray {
        // Структура основана на реальном iOS примере
        // Содержит NSKeyedArchiver с UIColor

        val objects = mutableListOf<ByteArray>()

        // Object 0: null
        objects.add(byteArrayOf(0x00))

        // Object 1: "$version" -> "100000"
        objects.add(encodeAsciiString("\$version"))
        objects.add(encodeAsciiString("100000"))

        // Object 3: "$archiver" -> "NSKeyedArchiver"
        objects.add(encodeAsciiString("\$archiver"))
        objects.add(encodeAsciiString("NSKeyedArchiver"))

        // Object 5: "$top" -> {root: ref}
        objects.add(encodeAsciiString("\$top"))

        // Object 6: "root"
        objects.add(encodeAsciiString("root"))

        // Object 7: "$objects" array placeholder
        objects.add(encodeAsciiString("\$objects"))

        // Object 8: "$null"
        objects.add(encodeAsciiString("\$null"))

        // Object 9: "$class"
        objects.add(encodeAsciiString("\$class"))

        // Object 10: "$classname"
        objects.add(encodeAsciiString("\$classname"))

        // Object 11: "$classes"
        objects.add(encodeAsciiString("\$classes"))

        // Object 12: "$classhints"
        objects.add(encodeAsciiString("\$classhints"))

        // Object 13: "UIColor"
        objects.add(encodeAsciiString("UIColor"))

        // Object 14: UIColorComponentCount
        objects.add(encodeAsciiString("UIColorComponentCount"))

        // Object 15: UIRed
        objects.add(encodeAsciiString("UIRed"))

        // Object 16: UIGreen
        objects.add(encodeAsciiString("UIGreen"))

        // Object 17: UIBlue
        objects.add(encodeAsciiString("UIBlue"))

        // Object 18: UIAlpha
        objects.add(encodeAsciiString("UIAlpha"))

        // Object 19: 4 (integer - UIColorComponentCount value)
        objects.add(byteArrayOf(0x10, 0x04))

        // Object 20-23: RGBA doubles
        objects.add(encodeDouble(r.toDouble()))
        objects.add(encodeDouble(g.toDouble()))
        objects.add(encodeDouble(b.toDouble()))
        objects.add(encodeDouble(a.toDouble()))

        // Object 24: UIColor class dict {$classname(10) -> "UIColor"(13)}
        objects.add(encodeDict(mapOf(10 to 13)))

        // Object 25: UIColor color dict
        // Keys: $class(9), UIColorComponentCount(14), UIRed(15), UIGreen(16), UIBlue(17), UIAlpha(18)
        // Values: class(24), 4(19), r(20), g(21), b(22), a(23)
        objects.add(
            encodeDict(
                mapOf(
                    9 to 24,   // $class -> UIColor class
                    14 to 19,  // UIColorComponentCount -> 4
                    15 to 20,  // UIRed -> r
                    16 to 21,  // UIGreen -> g
                    17 to 22,  // UIBlue -> b
                    18 to 23,  // UIAlpha -> a
                ),
            ),
        )

        // Теперь собираем финальный bplist
        return assembleBplist(objects)
    }

    private fun assembleBplist(objects: List<ByteArray>): ByteArray {
        val stream = ByteArrayOutputStream()

        // Magic
        stream.write(BPLIST_MAGIC.toByteArray(Charsets.US_ASCII))

        // Пишем все объекты
        val offsets = mutableListOf<Int>()
        var currentOffset = stream.size()

        for (obj in objects) {
            offsets.add(currentOffset)
            stream.write(obj)
            currentOffset += obj.size
        }

        // Offset table
        val offsetTableStart = stream.size()
        val offsetSize = if (currentOffset < 256) 1 else if (currentOffset < 65536) 2 else 4

        for (offset in offsets) {
            when (offsetSize) {
                1 -> stream.write(offset)
                2 -> {
                    stream.write((offset shr 8) and 0xFF)
                    stream.write(offset and 0xFF)
                }

                4 -> {
                    stream.write((offset shr 24) and 0xFF)
                    stream.write((offset shr 16) and 0xFF)
                    stream.write((offset shr 8) and 0xFF)
                    stream.write(offset and 0xFF)
                }
            }
        }

        // Trailer (32 bytes)
        val trailer = ByteArray(32)
        // Bytes 0-5: unused (0)
        // Byte 6: offset int size
        trailer[6] = offsetSize.toByte()
        // Byte 7: object ref size (1 byte for < 256 objects)
        trailer[7] = 1
        // Bytes 8-15: num objects (big-endian)
        val numObjects = objects.size.toLong()
        for (i in 0 until 8) {
            trailer[8 + i] = ((numObjects shr (56 - i * 8)) and 0xFF).toByte()
        }
        // Bytes 16-23: top object (0)
        // Bytes 24-31: offset table offset (big-endian)
        for (i in 0 until 8) {
            trailer[24 + i] = ((offsetTableStart.toLong() shr (56 - i * 8)) and 0xFF).toByte()
        }

        stream.write(trailer)

        return stream.toByteArray()
    }

    // MARK: - Object Encoders

    private fun encodeAsciiString(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.US_ASCII)
        val len = bytes.size
        return if (len < 15) {
            byteArrayOf((0x50 or len).toByte()) + bytes
        } else {
            byteArrayOf(0x5F, 0x10) + encodeInt(len) + bytes
        }
    }

    private fun encodeDouble(value: Double): ByteArray {
        val buffer = ByteBuffer.allocate(9)
        buffer.order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x23.toByte()) // 8-byte float marker
        buffer.putDouble(value)
        return buffer.array()
    }

    private fun encodeDict(refs: Map<Int, Int>): ByteArray {
        val len = refs.size
        val header =
            if (len < 15) {
                byteArrayOf((0xD0 or len).toByte())
            } else {
                byteArrayOf(0xDF.toByte(), 0x10.toByte()) + encodeInt(len)
            }
        val keys = refs.keys.map { it.toByte() }.toByteArray()
        val values = refs.values.map { it.toByte() }.toByteArray()
        return header + keys + values
    }

    private fun encodeInt(value: Int): ByteArray {
        return if (value < 256) {
            byteArrayOf(0x10, value.toByte())
        } else {
            byteArrayOf(
                0x11,
                ((value shr 8) and 0xFF).toByte(),
                (value and 0xFF).toByte(),
            )
        }
    }
}
