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
     * Бросает [IllegalStateException] если [ClipboardManager] недоступен
     * (не должен случаться в production — это programming error).
     */
    fun copy(
        context: Context,
        label: String,
        text: String
    )
}

/**
 * Реализация [ClipboardHelper] поверх системного [ClipboardManager].
 */
class SystemClipboardHelper : ClipboardHelper {
    override fun copy(
        context: Context,
        label: String,
        text: String
    ) {
        val manager =
            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: error("ClipboardManager недоступен")
        manager.setPrimaryClip(ClipData.newPlainText(label, text))
    }
}
