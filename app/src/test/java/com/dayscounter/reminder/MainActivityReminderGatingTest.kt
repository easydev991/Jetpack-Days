package com.dayscounter.reminder

import android.os.Bundle
import com.dayscounter.MainActivity
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * JVM unit-тесты для [MainActivity.shouldHandleReminderIntent] — гейта обработки
 * пуш-интента в [MainActivity.onCreate].
 *
 * Корневой баг: `MainActivity.onCreate` безусловно вызывал `handleReminderIntent`,
 * из-за чего Android при recreation (rotation / theme change / возврате из фона)
 * подсовывал сохранённый пуш-`Intent` с `EXTRA_ITEM_ID`, и `pendingOpenDetailItemId`
 * перезаписывался → `LaunchedEffect` пушил ещё одну копию `ItemDetail` поверх стека.
 *
 * Гейт — pure-функция от `savedInstanceState`, поэтому тестируется без Activity,
 * без Compose, без Robolectric, без эмулятора. Дополнительная интеграционная
 * антирегрессия — `MainActivityDeepLinkRotationUiTest.given_regular_launch_when_activity_recreated_then_open_detail_item_id_remains_null`
 * (androidTest, эмулятор).
 */
class MainActivityReminderGatingTest {
    @Test
    fun cold_start_triggers_handle() {
        // Given: cold-start — savedInstanceState == null (новый процесс или
        // process-restart без сохранённого Bundle).
        val savedInstanceState: Bundle? = null

        // When: проверяем гейт.
        val shouldHandle = MainActivity.shouldHandleReminderIntent(savedInstanceState)

        // Then: handleReminderIntent должен быть вызван.
        assertTrue(
            shouldHandle,
            "Cold-start (savedInstanceState == null) должен обработать reminder-intent"
        )
    }

    @Test
    fun recreate_skips_handle() {
        // Given: recreation — Android сохранил Bundle при rotation / theme change.
        val savedInstanceState: Bundle? = Bundle()

        // When: проверяем гейт.
        val shouldHandle = MainActivity.shouldHandleReminderIntent(savedInstanceState)

        // Then: handleReminderIntent НЕ должен вызываться повторно.
        assertFalse(
            shouldHandle,
            "Recreation (savedInstanceState != null) НЕ должен обрабатывать reminder-intent — " +
                "иначе LaunchedEffect повторно пушнёт ItemDetail поверх стека"
        )
    }
}
