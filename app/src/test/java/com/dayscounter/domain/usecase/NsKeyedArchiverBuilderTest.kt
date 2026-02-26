package com.dayscounter.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Тесты для NsKeyedArchiverBuilder - генератора UIColor в формате NSKeyedArchiver.
 *
 * Проверяет корректность создания bplist00 из RGBA и hex,
 * а также round-trip конвертацию через NsKeyedArchiverParser.
 */
class NsKeyedArchiverBuilderTest {
    // MARK: - buildFromHexColor Tests

    @Test
    fun buildFromHexColor_fromRedHex_returnsValidBase64() {
        // Given
        val redHex = "#FF0000"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(redHex)

        // Then
        assertNotNull(base64)
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64!!))
    }

    @Test
    fun buildFromHexColor_fromGreenHex_returnsValidBase64() {
        // Given
        val greenHex = "#00FF00"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(greenHex)

        // Then
        assertNotNull(base64)
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64!!))
    }

    @Test
    fun buildFromHexColor_fromBlueHex_returnsValidBase64() {
        // Given
        val blueHex = "#0000FF"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(blueHex)

        // Then
        assertNotNull(base64)
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64!!))
    }

    @Test
    fun buildFromHexColor_returnsNull_forInvalidHex() {
        // Given
        val invalidHex = "INVALID"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(invalidHex)

        // Then
        assertNull(base64)
    }

    @Test
    fun buildFromHexColor_returnsNull_forHexWithoutHash() {
        // Given
        val hexWithoutHash = "FF0000"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(hexWithoutHash)

        // Then
        assertNull(base64)
    }

    // MARK: - buildFromArgb Tests

    @Test
    fun buildFromArgb_fromRedInt_returnsValidBase64() {
        // Given
        val redArgb = 0xFFFF0000.toInt()

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromArgb(redArgb)

        // Then
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64))
    }

    @Test
    fun buildFromArgb_fromGreenInt_returnsValidBase64() {
        // Given
        val greenArgb = 0xFF00FF00.toInt()

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromArgb(greenArgb)

        // Then
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64))
    }

    @Test
    fun buildFromArgb_fromBlueInt_returnsValidBase64() {
        // Given
        val blueArgb = 0xFF0000FF.toInt()

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromArgb(blueArgb)

        // Then
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64))
    }

    // MARK: - Round-trip Tests

    @Test
    fun roundTrip_redColor_preservesColor() {
        // Given
        val originalHex = "#FF0000"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(originalHex)
        val parsedHex = NsKeyedArchiverParser.parseHexColor(base64!!)

        // Then
        assertEquals(originalHex, parsedHex)
    }

    @Test
    fun roundTrip_greenColor_preservesColor() {
        // Given
        val originalHex = "#00FF00"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(originalHex)
        val parsedHex = NsKeyedArchiverParser.parseHexColor(base64!!)

        // Then
        assertEquals(originalHex, parsedHex)
    }

    @Test
    fun roundTrip_blueColor_preservesColor() {
        // Given
        val originalHex = "#0000FF"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(originalHex)
        val parsedHex = NsKeyedArchiverParser.parseHexColor(base64!!)

        // Then
        assertEquals(originalHex, parsedHex)
    }

    @Test
    fun roundTrip_customColor_preservesColor() {
        // Given - iOS system red color
        val originalHex = "#FF3A30"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(originalHex)
        val parsedHex = NsKeyedArchiverParser.parseHexColor(base64!!)

        // Then
        assertEquals(originalHex, parsedHex)
    }

    @Test
    fun roundTrip_blackColor_preservesColor() {
        // Given
        val originalHex = "#000000"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(originalHex)
        val parsedHex = NsKeyedArchiverParser.parseHexColor(base64!!)

        // Then
        assertEquals(originalHex, parsedHex)
    }

    @Test
    fun roundTrip_whiteColor_preservesColor() {
        // Given
        val originalHex = "#FFFFFF"

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromHexColor(originalHex)
        val parsedHex = NsKeyedArchiverParser.parseHexColor(base64!!)

        // Then
        assertEquals(originalHex, parsedHex)
    }

    // MARK: - buildFromRgba Tests

    @Test
    fun buildFromRgba_withFullRed_returnsValidBase64() {
        // Given
        val r = 1.0f
        val g = 0.0f
        val b = 0.0f
        val a = 1.0f

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromRgba(r, g, b, a)

        // Then
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64))
    }

    @Test
    fun buildFromRgba_withHalfAlpha_returnsValidBase64() {
        // Given
        val r = 1.0f
        val g = 0.0f
        val b = 0.0f
        val a = 0.5f

        // When
        val base64 = NsKeyedArchiverBuilder.buildFromRgba(r, g, b, a)

        // Then
        assertTrue(NsKeyedArchiverParser.isNsKeyedArchiver(base64))
    }
}
