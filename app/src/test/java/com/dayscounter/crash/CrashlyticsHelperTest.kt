package com.dayscounter.crash

import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.Exception

class CrashlyticsHelperTest {
    @Test
    fun logException_whenValidException_thenDoesNotCrash() {
        // Given
        val exception = Exception("Test exception")

        // When
        CrashlyticsHelper.logException(exception, "Test message")

        // Then
        // Если дошли сюда - тест пройден, исключения не было
        assertTrue(true)
    }

    @Test
    fun logException_whenExceptionWithNullMessage_thenDoesNotCrash() {
        // Given
        val exception = Exception("Test exception without message")

        // When
        CrashlyticsHelper.logException(exception, null)

        // Then
        // Если дошли сюда - тест пройден, исключения не было
        assertTrue(true)
    }

    @Test
    fun logException_whenExceptionWithEmptyMessage_thenDoesNotCrash() {
        // Given
        val exception = Exception("Test exception with empty message")

        // When
        CrashlyticsHelper.logException(exception, "")

        // Then
        // Если дошли сюда - тест пройден, исключения не было
        assertTrue(true)
    }

    @Test
    fun logException_whenRuntimeException_thenDoesNotCrash() {
        // Given
        val exception = RuntimeException("Test runtime exception")

        // When
        CrashlyticsHelper.logException(exception, "Test runtime exception")

        // Then
        // Если дошли сюда - тест пройден, исключения не было
        assertTrue(true)
    }

    @Test
    fun logException_whenNullPointerException_thenDoesNotCrash() {
        // Given
        val exception = NullPointerException("Test null pointer exception")

        // When
        CrashlyticsHelper.logException(exception, "Test NPE")

        // Then
        // Если дошли сюда - тест пройден, исключения не было
        assertTrue(true)
    }
}
