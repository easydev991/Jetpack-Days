package com.dayscounter.reminder

/**
 * Ключи intent/extras для alarm и открытия экрана из уведомления.
 */
object ReminderIntentContract {
    private const val INT_BITS_HALF = 32

    const val ACTION_FIRE_REMINDER = "com.dayscounter.action.FIRE_REMINDER"
    const val ACTION_OPEN_FROM_REMINDER = "com.dayscounter.action.OPEN_FROM_REMINDER"

    const val EXTRA_ITEM_ID = "extra_item_id"
    const val EXTRA_ITEM_TITLE = "extra_item_title"

    const val CHANNEL_ID = "dayscounter_reminders"

    fun requestCodeForItem(itemId: Long): Int = (itemId xor (itemId ushr INT_BITS_HALF)).toInt()

    fun notificationIdForItem(itemId: Long): Int = requestCodeForItem(itemId)
}
