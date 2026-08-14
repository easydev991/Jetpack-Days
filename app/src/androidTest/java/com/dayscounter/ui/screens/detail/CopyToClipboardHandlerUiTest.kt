package com.dayscounter.ui.screens.detail

import android.content.Context
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.util.ClipboardHelper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для [rememberCopyToClipboardHandler].
 *
 * Проверяет контракт:
 * - `Result.success` → отображается снекбар с переданным `messageResId`;
 * - `Result.failure` → снекбар НЕ отображается.
 *
 * Использует локальный [FakeClipboardHelper] с управляемым `nextResult`,
 * чтобы изолировать тест от системного `ClipboardManager`.
 */
@RunWith(AndroidJUnit4::class)
class CopyToClipboardHandlerUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val titleCopiedText = context.getString(R.string.title_copied)
    private val detailsCopiedText = context.getString(R.string.details_copied)
    private val fakeClipboardHelper = FakeClipboardHelper()

    @Test
    fun remembercopytoclipboardhandler_when_invoked_with_title_label_then_shows_title_copied_snackbar() {
        // Given
        var handler: ((String, Int, String) -> Unit)? = null
        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            handler =
                rememberCopyToClipboardHandler(
                    clipboardHelper = fakeClipboardHelper,
                    snackbarHostState = snackbarHostState
                )
            SnackbarHost(hostState = snackbarHostState)
        }
        fakeClipboardHelper.nextResult = Result.success(Unit)

        // When
        handler?.invoke("Title", R.string.title_copied, "Какой-то title")

        // Then
        composeTestRule
            .onNodeWithText(titleCopiedText)
            .assertIsDisplayed()
    }

    @Test
    fun remembercopytoclipboardhandler_when_invoked_with_details_label_then_shows_details_copied_snackbar() {
        // Given
        var handler: ((String, Int, String) -> Unit)? = null
        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            handler =
                rememberCopyToClipboardHandler(
                    clipboardHelper = fakeClipboardHelper,
                    snackbarHostState = snackbarHostState
                )
            SnackbarHost(hostState = snackbarHostState)
        }
        fakeClipboardHelper.nextResult = Result.success(Unit)

        // When
        handler?.invoke("Details", R.string.details_copied, "Какое-то описание")

        // Then
        composeTestRule
            .onNodeWithText(detailsCopiedText)
            .assertIsDisplayed()
    }

    @Test
    fun remembercopytoclipboardhandler_when_clipboard_returns_failure_then_no_snackbar_shown() {
        // Given
        var handler: ((String, Int, String) -> Unit)? = null
        composeTestRule.setContent {
            val snackbarHostState = remember { SnackbarHostState() }
            handler =
                rememberCopyToClipboardHandler(
                    clipboardHelper = fakeClipboardHelper,
                    snackbarHostState = snackbarHostState
                )
            SnackbarHost(hostState = snackbarHostState)
        }
        fakeClipboardHelper.nextResult = Result.failure(RuntimeException("boom"))

        // When
        handler?.invoke("Title", R.string.title_copied, "Какой-то title")

        // Then
        composeTestRule
            .onNodeWithText(titleCopiedText)
            .assertDoesNotExist()
    }
}

/**
 * Тестовая реализация [ClipboardHelper] с управляемым результатом.
 *
 * `nextResult` устанавливается из теста перед вызовом `copy` —
 * handler-логика проверяет `result.isSuccess` для отображения снекбара.
 */
private class FakeClipboardHelper : ClipboardHelper {
    var nextResult: Result<Unit> = Result.success(Unit)

    override fun copy(
        context: Context,
        label: String,
        text: String
    ): Result<Unit> = nextResult
}
