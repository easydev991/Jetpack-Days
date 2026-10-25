package com.dayscounter.navigation

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ShortcutLaunchResolverTest {
    @Test
    fun whenActionIsCreateItem_thenIsCreateItemActionReturnsTrue() {
        // Given
        val action = ShortcutLaunchResolver.ACTION_CREATE_ITEM

        // When
        val result = ShortcutLaunchResolver.isCreateItemAction(action)

        // Then
        assertTrue(result, "Наш action шортката должен распознаваться")
    }

    @Test
    fun whenActionIsForeign_thenIsCreateItemActionReturnsFalse() {
        // Given
        val action = "com.dayscounter.action.OPEN_FROM_REMINDER"

        // When
        val result = ShortcutLaunchResolver.isCreateItemAction(action)

        // Then
        assertFalse(result, "Чужой action не должен распознаваться как шорткат создания")
    }

    @Test
    fun whenActionIsNull_thenIsCreateItemActionReturnsFalse() {
        // Given
        val action: String? = null

        // When
        val result = ShortcutLaunchResolver.isCreateItemAction(action)

        // Then
        assertFalse(result, "null action не должен распознаваться как шорткат создания")
    }
}
