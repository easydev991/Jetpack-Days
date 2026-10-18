package com.dayscounter.domain.usecase

/**
 * Fake для [DaysTextProvider] в unit-тестах, где нет доступа к Android Context.
 *
 * Заменяет удалённую заглушку провайдера ресурсов один-в-один,
 * чтобы ассерты тестов не изменились:
 * количество возвращается как есть (без слов), служебные строки — константами.
 */
class FakeDaysTextProvider : DaysTextProvider {
    override fun today(): String = "Сегодня"

    override fun remaining(): String = "осталось"

    override fun elapsed(): String = "прошло"

    override fun formattingError(): String = "Заглушка"

    override fun daysCount(quantity: Int): String = "$quantity"

    override fun monthsCount(quantity: Int): String = "$quantity"

    override fun yearsCount(quantity: Int): String = "$quantity"
}
