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

    override fun today(): String = appContext.getString(ResourceIds.TODAY)

    override fun remaining(): String = appContext.getString(ResourceIds.REMAINING)

    override fun elapsed(): String = appContext.getString(ResourceIds.ELAPSED)

    override fun formattingError(): String = appContext.getString(ResourceIds.ERROR_FORMATTING)

    override fun daysCount(quantity: Int): String = appContext.resources.getQuantityString(ResourceIds.DAYS_COUNT, quantity, quantity)

    override fun monthsCount(quantity: Int): String = appContext.resources.getQuantityString(ResourceIds.MONTHS_COUNT, quantity, quantity)

    override fun yearsCount(quantity: Int): String = appContext.resources.getQuantityString(ResourceIds.YEARS_COUNT, quantity, quantity)
}
