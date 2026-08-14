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
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для SystemClipboardHelper.
 *
 * Проверяет, что `copy` делегирует системному `ClipboardManager` с правильными
 * параметрами и бросает `IllegalStateException` при недоступном сервисе.
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
    fun copy_when_set_primary_clip_succeeds_then_delegates_to_manager() {
        // When
        helper.copy(context, "Title", "some text")

        // Then
        verify(exactly = 1) { ClipData.newPlainText("Title", "some text") }
        verify(exactly = 1) { clipboardManager.setPrimaryClip(clipData) }
    }

    @Test
    fun copy_when_service_is_null_then_throws() {
        // Given
        every { context.getSystemService(Context.CLIPBOARD_SERVICE) } returns null

        // When / Then
        val exception =
            assertThrows(IllegalStateException::class.java) {
                helper.copy(context, "Title", "some text")
            }
        assertEquals("ClipboardManager недоступен", exception.message)
    }
}
