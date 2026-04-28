package com.dayscounter.reminder

import android.content.Intent

internal fun resolveReminderOpenItemId(
    action: String?,
    hasReminderExtra: Boolean,
    itemIdExtra: Long
): Long? {
    val hasReminderAction = action == ReminderIntentContract.ACTION_OPEN_FROM_REMINDER
    val hasReminderPayload = hasReminderAction || hasReminderExtra
    if (!hasReminderPayload) {
        return null
    }

    return itemIdExtra.takeIf { it > 0L }
}

internal fun Intent?.extractReminderOpenItemId(): Long? {
    val currentIntent = this ?: return null
    return resolveReminderOpenItemId(
        action = currentIntent.action,
        hasReminderExtra = currentIntent.hasExtra(ReminderIntentContract.EXTRA_ITEM_ID),
        itemIdExtra = currentIntent.getLongExtra(ReminderIntentContract.EXTRA_ITEM_ID, -1L)
    )
}
