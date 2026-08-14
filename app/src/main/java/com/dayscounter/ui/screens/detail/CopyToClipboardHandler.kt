package com.dayscounter.ui.screens.detail

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.dayscounter.util.ClipboardHelper
import com.dayscounter.util.SystemClipboardHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Фабрика Compose-обработчика копирования текста в системный буфер обмена
 * со снекбаром подтверждения.
 *
 * Возвращает лямбду `(label, messageResId, text) -> Unit`, которую можно передать
 * в `ReadSectionView.onCopy` или вызвать напрямую из UI:
 *
 * - `label` — метка для системного буфера обмена ("Title"/"Details"),
 * - `messageResId` — ресурс сообщения снекбара ([com.dayscounter.R.string.title_copied]
 *   или [com.dayscounter.R.string.details_copied]),
 * - `text` — копируемый текст.
 *
 * Контракт (см. `specs/detail-screen-text-copy/spec.md`, requirement «Клик по
 * пункту меню копирует текст в буфер обмена и показывает снекбар»):
 * при `Result.success(Unit)` от `ClipboardHelper.copy` отображается снекбар
 * длительностью [SnackbarDuration.Short]; при `Result.failure` снекбар НЕ
 * показывается (копирование молча игнорируется). Возвращаемая лямбда НЕ
 * пробрасывает исключения.
 *
 * [ClipboardHelper] передаётся параметром с дефолтом [SystemClipboardHelper] —
 * для подмены в androidTest на [com.dayscounter.util.ClipboardHelper] fake.
 *
 * @param clipboardHelper Реализация интерфейса доступа к буферу обмена
 * @param snackbarHostState Хост снекбара, в котором показывается подтверждение
 * @param coroutineScope Scope для асинхронной работы (копирование + снекбар)
 * @return Лямбда `(label, messageResId, text) -> Unit` для вызова из UI
 */
@Composable
internal fun rememberCopyToClipboardHandler(
    clipboardHelper: ClipboardHelper = SystemClipboardHelper(),
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): (label: String, messageResId: Int, text: String) -> Unit {
    val context = LocalContext.current
    return { label, messageResId, text ->
        coroutineScope.launch {
            val result = clipboardHelper.copy(context, label, text)
            if (result.isSuccess) {
                snackbarHostState.showSnackbar(
                    message = context.getString(messageResId),
                    duration = SnackbarDuration.Short
                )
            }
            // Failure: намеренно игнорируется — копирование молча.
        }
    }
}
