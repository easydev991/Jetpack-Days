package com.dayscounter.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для SystemClipboardHelper.
 *
 * Проверяет Result-контракт: success при корректной работе сервиса,
 * failure при отсутствии сервиса или исключении из setPrimaryClip.
 *
 * Статический [ClipData.newPlainText] замокирован, потому что
 * на JVM без Robolectric реальная реализация бросает RuntimeException.
 */
class ClipboardHelperTest {
    private val context: Context = mockk()
    private val clipboardManager: ClipboardManager = mockk(relaxed = true)
    private val clipData: ClipData = mockk(relaxed = true)
    private val helper = SystemClipboardHelper()

    @BeforeEach
    fun setup() {
        mockkStatic(ClipData::class)
        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns clipboardManager
        every { clipboardManager.setPrimaryClip(any()) } returns Unit
        every { ClipData.newPlainText(any<String>(), any<String>()) } returns clipData
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(ClipData::class)
    }

    @Test
    fun copy_when_set_primary_clip_succeeds_then_returns_success() {
        // When
        val result = helper.copy(context, "Title", "some text")

        // Then
        assertTrue(result.isSuccess, "Ожидался Result.success при корректной работе сервиса")
        verify(exactly = 1) { ClipData.newPlainText("Title", "some text") }
        verify(exactly = 1) { clipboardManager.setPrimaryClip(clipData) }
    }

    @Test
    fun copy_when_service_is_null_then_returns_failure() {
        // Given
        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns null

        // When
        val result = helper.copy(context, "Title", "some text")

        // Then
        assertTrue(result.isFailure, "Ожидался Result.failure при недоступном ClipboardManager")
    }

    @Test
    fun copy_when_set_primary_clip_throws_then_returns_failure() {
        // Given
        every { clipboardManager.setPrimaryClip(any()) } throws RuntimeException("boom")

        // When
        val result = helper.copy(context, "Title", "some text")

        // Then
        assertTrue(result.isFailure, "Ожидался Result.failure при исключении из setPrimaryClip")
    }
}
