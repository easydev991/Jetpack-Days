package com.dayscounter.navigation

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayscounter.MainActivity
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Интеграционный антирегрессионный тест: проверяет, что `openDetailItemId`
 * остаётся `null` после `scenario.recreate()` БЕЗ предварительного `setIntent`.
 *
 * Это НЕ воспроизводит исходный баг напрямую (для воспроизведения нужен `setIntent`
 * + recreate, что ломает `ActivityScenario.close()` / `recreate()` в instrumentation
 * тестах — см. design.md Decision 2 в `openspec/changes/fix-navigation-on-rotation-after-notification/`).
 *
 * Гейт `MainActivity.shouldHandleReminderIntent(savedInstanceState) == false` для
 * `savedInstanceState != null` покрывается JVM unit-тестом
 * `MainActivityReminderGatingTest` (`app/src/test/`). Здесь — только проверка,
 * что `@get:VisibleForTesting openDetailItemId` корректно отдаёт `pendingOpenDetailItemId`
 * после реального `scenario.recreate()`.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityDeepLinkRotationUiTest {
    @Test
    fun given_regular_launch_when_activity_recreated_then_open_detail_item_id_remains_null() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            // Given: обычный запуск (без push-интента) → openDetailItemId == null.
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                assertNull(
                    "На свежем старте openDetailItemId должен быть null",
                    activity.openDetailItemId
                )
            }

            // When: recreate без смены intent (программный эквивалент поворота).
            scenario.recreate()
            scenario.moveToState(Lifecycle.State.RESUMED)

            // Then: антирегрессия — openDetailItemId остаётся null.
            var actualOpenId: Long? = Long.MIN_VALUE
            scenario.onActivity { activity ->
                actualOpenId = activity.openDetailItemId
            }
            assertNull(
                "После recreate без push-интента openDetailItemId должен оставаться null",
                actualOpenId
            )
        }
    }
}
