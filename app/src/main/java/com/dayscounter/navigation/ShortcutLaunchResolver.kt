package com.dayscounter.navigation

/**
 * Разбор launch-intent статического шортката «Добавить запись».
 */
object ShortcutLaunchResolver {
    /** Action intent, который лаунчер доставляет при выборе шортката. */
    const val ACTION_CREATE_ITEM = "com.dayscounter.action.CREATE_ITEM"

    /**
     * Определяет, является ли action intent запуском от шортката создания записи.
     *
     * @param action action доставленного intent (может быть null — например, у системных launch-intent)
     * @return `true` только для [ACTION_CREATE_ITEM]
     */
    fun isCreateItemAction(action: String?): Boolean = action == ACTION_CREATE_ITEM
}
