package com.dayscounter.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

/**
 * Абстракция доступа к системному буферу обмена.
 *
 * Позволяет подменить реальный системный сервис в UI-тестах через
 * `FakeClipboardHelper`, не завися от эмулятора.
 */
interface ClipboardHelper {
    /**
     * Помещает [text] в системный буфер обмена под [label].
     *
     * @return [Result.success] при успешной записи;
     *   [Result.failure] если [ClipboardManager] недоступен или бросил исключение.
     */
    fun copy(
        context: Context,
        label: String,
        text: String
    ): Result<Unit>
}

/**
 * Реализация [ClipboardHelper] поверх системного [ClipboardManager].
 *
 * Не бросает исключения наружу: любые ошибки оборачиваются в [Result.failure].
 */
class SystemClipboardHelper : ClipboardHelper {
    override fun copy(
        context: Context,
        label: String,
        text: String
    ): Result<Unit> =
        runCatching {
            val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: error("ClipboardManager недоступен")
            manager.setPrimaryClip(ClipData.newPlainText(label, text))
        }
}
