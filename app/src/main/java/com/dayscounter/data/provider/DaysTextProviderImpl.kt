package com.dayscounter.data.provider

import android.content.Context
import com.dayscounter.domain.usecase.DaysTextProvider

/**
 * Android-реализация [DaysTextProvider] поверх `R.plurals`/`R.string`.
 *
 * Адаптер между доменным интерфейсом и ресурсами приложения
 * (зеркально [ResourceProviderImpl]: application-контекст, plural-формы).
 *
 * @property appContext Application-контекст для доступа к ресурсам
 */
class DaysTextProviderImpl(
    context: Context
) : DaysTextProvider {
    private val appContext = context.applicationContext
    private val resources = appContext.resources

    override fun today(): String = appContext.getString(ResourceIds.TODAY)

    override fun remaining(): String = appContext.getString(ResourceIds.REMAINING)

    override fun elapsed(): String = appContext.getString(ResourceIds.ELAPSED)

    override fun formattingError(): String = appContext.getString(ResourceIds.ERROR_FORMATTING)

    override fun daysCount(quantity: Int): String = quantityString(ResourceIds.DAYS_COUNT, quantity)

    override fun monthsCount(quantity: Int): String = quantityString(ResourceIds.MONTHS_COUNT, quantity)

    override fun yearsCount(quantity: Int): String = quantityString(ResourceIds.YEARS_COUNT, quantity)

    /** Локализованная строка множественного числа по идентификатору plurals-ресурса. */
    private fun quantityString(
        id: Int,
        quantity: Int
    ): String = resources.getQuantityString(id, quantity, quantity)
}
