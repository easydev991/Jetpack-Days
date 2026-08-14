package com.dayscounter.ui.screens.detail

import android.content.Context
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.util.ClipboardHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI-тесты для [rememberCopyToClipboardHandler].
 *
 * Проверяет контракт:
 * - вызов handler'а приводит к вызову [ClipboardHelper.copy] с переданными
 *   `label` и `text`;
 * - `Result.failure` от clipboard НЕ пробрасывается наружу как исключение.
 *
 * Появление системного `Toast` НЕ проверяется — Toast живёт в WindowManager,
 * не в Compose-дереве, и Compose UI-тесты его не видят. Условный показ Toast'а
 * (`Build.VERSION.SDK_INT < TIRAMISU`) проверяется вручную на эмуляторе
 * (этап 5.6). Доверяем Android-обвязке `Toast.makeText`.
 *
 * Использует локальный [FakeClipboardHelper] с управляемым `nextResult` и
 * `lastInvocation`, чтобы изолировать тест от системного `ClipboardManager`.
 *
 * Строки Toast'а резолвятся в test setUp из instrumentation target context —
 * production-код делает то же самое в composable-скоупе через [stringResource],
 * чтобы Toast был конфигурационно-чувствительным (см. lint
 * `ConfigurationLocale`).
 */
@RunWith(AndroidJUnit4::class)
class CopyToClipboardHandlerUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val titleCopiedMessage = context.getString(R.string.title_copied)
    private val detailsCopiedMessage = context.getString(R.string.details_copied)
    private val fakeClipboardHelper = FakeClipboardHelper()

    @Test
    fun remembercopytoclipboardhandler_when_invoked_with_title_label_then_calls_clipboard_helper_with_label_and_text() {
        // Given
        var handler: ((String, String, String) -> Unit)? = null
        composeTestRule.setContent {
            handler = rememberCopyToClipboardHandler(clipboardHelper = fakeClipboardHelper)
        }
        fakeClipboardHelper.nextResult = Result.success(Unit)

        // When
        handler?.invoke("Title", titleCopiedMessage, "Какой-то title")

        // Then
        assertNotNull("handler должен вызвать clipboardHelper.copy", fakeClipboardHelper.lastInvocation)
        assertEquals("Title", fakeClipboardHelper.lastInvocation?.label)
        assertEquals("Какой-то title", fakeClipboardHelper.lastInvocation?.text)
    }

    @Test
    fun remembercopytoclipboardhandler_when_clipboard_returns_failure_then_no_exception() {
        // Given
        var handler: ((String, String, String) -> Unit)? = null
        composeTestRule.setContent {
            handler = rememberCopyToClipboardHandler(clipboardHelper = fakeClipboardHelper)
        }
        fakeClipboardHelper.nextResult = Result.failure(RuntimeException("boom"))

        // When / Then: handler не должен пробрасывать исключение
        try {
            handler?.invoke("Title", titleCopiedMessage, "Какой-то title")
        } catch (e: RuntimeException) {
            throw AssertionError("handler пробросил исключение при Result.failure: ${e.message}", e)
        }
        assertNotNull(
            "handler должен вызвать clipboardHelper.copy даже при Result.failure",
            fakeClipboardHelper.lastInvocation
        )
    }
}

/**
 * Тестовая реализация [ClipboardHelper].
 *
 * `nextResult` устанавливается из теста перед вызовом `copy` —
 * handler-логика проверяет `result.isSuccess` для отображения Toast'а.
 *
 * `lastInvocation` записывает последний вызов `copy(label, text)` — handler
 * должен вызвать `clipboardHelper.copy` ровно с теми значениями, которые
 * пришли из UI.
 */
private class FakeClipboardHelper : ClipboardHelper {
    var nextResult: Result<Unit> = Result.success(Unit)
    var lastInvocation: CopyInvocation? = null

    override fun copy(
        context: Context,
        label: String,
        text: String
    ): Result<Unit> {
        lastInvocation = CopyInvocation(label = label, text = text)
        return nextResult
    }
}

/**
 * Запись об одном вызове [ClipboardHelper.copy].
 */
private data class CopyInvocation(
    val label: String,
    val text: String
)
