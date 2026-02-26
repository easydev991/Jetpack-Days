package com.dayscounter.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Тесты для NsKeyedArchiverParser - парсера UIColor из NSKeyedArchiver (iOS).
 *
 * Проверяет корректность извлечения RGBA компонентов из бинарного plist
 * и конвертации в hex-формат (#RRGGBB).
 */
class NsKeyedArchiverParserTest {
    // Реальный пример из iOS: красный цвет (iOS system red)
    // Декодированные RGBA значения: R=1.0, G=0.228, B=0.187, A=1.0
    // Ожидаемый hex: #FF3A30
    @Suppress("MaxLineLength")
    private val redColorBase64 =
        "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
            "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9y" +
            "Q29tcG9uZW50Q291bnRWVUlHcmVlblZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlND" +
            "b2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNs" +
            "YXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAI" +
            "ABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDs" +
            "APcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="

    @Test
    fun parseHexColor_fromRedColorBase64_returnsFF3A30() {
        // Given - красный цвет из iOS
        // When
        println("Parser test - redColorBase64 length: ${redColorBase64.length}")
        val hexColor = NsKeyedArchiverParser.parseHexColor(redColorBase64)
        println("Parser test - hexColor: $hexColor")

        // Then - ожидаем #FF3A30 (R=255, G=58, B=48)
        assertEquals("#FF3A30", hexColor)
    }

    @Test
    fun parseHexColor_returnsNull_forInvalidBase64() {
        // Given
        val invalidBase64 = "not-valid-base64!!!"

        // When
        val hexColor = NsKeyedArchiverParser.parseHexColor(invalidBase64)

        // Then
        assertNull(hexColor)
    }

    @Test
    fun parseHexColor_returnsNull_forEmptyString() {
        // Given
        val emptyString = ""

        // When
        val hexColor = NsKeyedArchiverParser.parseHexColor(emptyString)

        // Then
        assertNull(hexColor)
    }

    @Test
    fun parseHexColor_returnsNull_forNonBplistData() {
        // Given - валидный Base64, но не bplist
        val nonBplist = "SGVsbG8gV29ybGQ=" // "Hello World"

        // When
        val hexColor = NsKeyedArchiverParser.parseHexColor(nonBplist)

        // Then
        assertNull(hexColor)
    }

    @Test
    fun parseHexColor_returnsNull_forBplistWithoutUIColor() {
        // Given - минимальный bplist без UIColor объекта
        // Это Base64 от bplist00 с пустыми объектами
        val bplistWithoutColor =
            "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05T" +
                "S2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGw=="

        // When
        val hexColor = NsKeyedArchiverParser.parseHexColor(bplistWithoutColor)

        // Then
        assertNull(hexColor)
    }

    @Test
    fun isNsKeyedArchiver_returnsTrue_forValidBase64Bplist() {
        // Given
        val validBase64 = redColorBase64

        // When
        val result = NsKeyedArchiverParser.isNsKeyedArchiver(validBase64)

        // Then
        assertEquals(true, result)
    }

    @Test
    fun isNsKeyedArchiver_returnsFalse_forHexColor() {
        // Given
        val hexColor = "#FF0000"

        // When
        val result = NsKeyedArchiverParser.isNsKeyedArchiver(hexColor)

        // Then
        assertEquals(false, result)
    }

    @Test
    fun isNsKeyedArchiver_returnsFalse_forInvalidBase64() {
        // Given
        val invalidBase64 = "not-valid!!!"

        // When
        val result = NsKeyedArchiverParser.isNsKeyedArchiver(invalidBase64)

        // Then
        assertEquals(false, result)
    }

    @Test
    fun isNsKeyedArchiver_returnsFalse_forEmptyString() {
        // Given
        val emptyString = ""

        // When
        val result = NsKeyedArchiverParser.isNsKeyedArchiver(emptyString)

        // Then
        assertEquals(false, result)
    }
}
