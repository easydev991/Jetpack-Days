package com.dayscounter.ui.screens.detail

import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dayscounter.util.ClipboardHelper
import com.dayscounter.util.SystemClipboardHelper

/**
 * Фабрика Compose-обработчика копирования текста в системный буфер обмена
 * с системным `Toast` подтверждения.
 *
 * Возвращает лямбду `(label, message, text) -> Unit`, которую можно передать
 * в `ReadSectionView.onCopy` или вызвать напрямую из UI:
 *
 * - `label` — метка для системного буфера обмена ("Title"/"Details"),
 * - `message` — текст Toast'а (например `"Название скопировано"`); строки
 *   резолвятся вызывающим в composable-скоупе через [stringResource], поэтому
 *   handler остаётся свободным от конфигурационных зависимостей,
 * - `text` — копируемый текст.
 *
 * Контракт (см. `specs/detail-screen-text-copy/spec.md`, requirement «Клик по
 * пункту меню копирует текст в буфер обмена и показывает системный Toast
 * (только на API <33)»):
 * при `Result.success(Unit)` от `ClipboardHelper.copy` И на устройстве с
 * `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU` (API <33, Android 12
 * и старше) отображается системный `Toast` через
 * `Toast.makeText(context, message, Toast.LENGTH_SHORT).show()`.
 * На устройствах с API ≥33 (Android 13+) системный Toast НЕ показывается —
 * ОС сама показывает системный overlay после копирования, и дублирование
 * перекрывает его визуально.
 * При `Result.failure` Toast НЕ показывается ни на какой версии API
 * (копирование молча игнорируется).
 * Возвращаемая лямбда НЕ пробрасывает исключения.
 *
 * [ClipboardHelper] передаётся параметром с дефолтом [SystemClipboardHelper] —
 * для подмены в androidTest на [com.dayscounter.util.ClipboardHelper] fake.
 *
 * @param clipboardHelper Реализация интерфейса доступа к буферу обмена
 * @return Лямбда `(label, message, text) -> Unit` для вызова из UI
 */
@Composable
internal fun rememberCopyToClipboardHandler(
    clipboardHelper: ClipboardHelper = SystemClipboardHelper()
): (label: String, message: String, text: String) -> Unit {
    val context = LocalContext.current
    return { label, message, text ->
        val result = clipboardHelper.copy(context, label, text)
        if (result.isSuccess && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            // ponytail: Toast на API >=33 не показывается — ОС сама рисует системный overlay.
            Toast
                .makeText(context, message, Toast.LENGTH_SHORT)
                .show()
        }
        // Failure: намеренно игнорируется — копирование молча.
    }
}
