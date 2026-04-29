package com.dayscounter.reminder

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ReminderIntentParserTest {
    @Test
    fun resolveReminderOpenItemId_whenRegularLaunch_thenReturnsNull() {
        val result =
            resolveReminderOpenItemId(
                action = null,
                hasReminderExtra = false,
                itemIdExtra = -1L
            )

        assertNull(result)
    }

    @Test
    fun resolveReminderOpenItemId_whenReminderActionAndValidId_thenReturnsItemId() {
        val result =
            resolveReminderOpenItemId(
                action = ReminderIntentContract.ACTION_OPEN_FROM_REMINDER,
                hasReminderExtra = false,
                itemIdExtra = 42L
            )

        assertEquals(42L, result)
    }

    @Test
    fun resolveReminderOpenItemId_whenReminderExtraWithoutActionAndValidId_thenReturnsItemId() {
        val result =
            resolveReminderOpenItemId(
                action = "android.intent.action.MAIN",
                hasReminderExtra = true,
                itemIdExtra = 51L
            )

        assertEquals(51L, result)
    }

    @Test
    fun resolveReminderOpenItemId_whenReminderPayloadHasInvalidId_thenReturnsNull() {
        val result =
            resolveReminderOpenItemId(
                action = ReminderIntentContract.ACTION_OPEN_FROM_REMINDER,
                hasReminderExtra = true,
                itemIdExtra = 0L
            )

        assertNull(result)
    }
}
