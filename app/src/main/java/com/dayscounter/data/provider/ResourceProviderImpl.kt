package com.dayscounter.data.provider

import android.content.Context

/**
 * Реализация ResourceProvider на основе Android Context.
 *
 * Использует Application context для безопасного доступа к строковым ресурсам.
 *
 * @property context Application context
 */
class ResourceProviderImpl(
    private val context: Context
) : ResourceProvider {
    override fun getString(
        resId: Int,
        vararg formatArgs: Any
    ): String =
        if (formatArgs.isEmpty()) {
            context.getString(resId)
        } else {
            context.getString(resId, *formatArgs)
        }
}
