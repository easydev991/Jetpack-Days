package com.dayscounter.data.formatter

/**
 * Заглушка для ResourceProvider.
 *
 * Используется ТОЛЬКО для unit-тестов, где нет доступа к Android Context.
 * В продакшн-коде используйте ResourceProviderImpl через FormatterModule.
 *
 * Возвращает локализованные строки для русского языка (для тестов).
 */
class StubResourceProvider : ResourceProvider {
    companion object {
        /** Идентификатор ресурса для "дн." */
        private const val RES_DAYS_ABBR = 1

        /** Идентификатор ресурса для "мес." */
        private const val RES_MONTHS_ABBR = 2

        /** Идентификатор ресурса для "г." */
        private const val RES_YEARS_ABBR = 3

        /** Множественное число: 2-4 (кроме 10-19) */
        private const val PLURAL_FEW_MIN = 2

        /** Множественное число: 2-4 (кроме 10-19) */
        private const val PLURAL_FEW_MAX = 4

        /** Множественное число: исключение (10-19) */
        private const val PLURAL_MANY_MIN = 10

        /** Множественное число: исключение (10-19) */
        private const val PLURAL_MANY_MAX = 19

        /** Сингуляр: 1 */
        private const val PLURAL_ONE = 1
    }

    override fun getMonthsString(quantity: Int): String {
        // Заглушка для множественного числа месяцев
        @Suppress("MagicNumber")
        return when {
            quantity == PLURAL_ONE -> "$quantity"
            quantity % 10 in PLURAL_FEW_MIN..PLURAL_FEW_MAX &&
                quantity % 100 !in PLURAL_MANY_MIN..PLURAL_MANY_MAX -> "$quantity"

            else -> "$quantity"
        }
    }

    override fun getYearsString(quantity: Int): String {
        // Заглушка для множественного числа лет
        @Suppress("MagicNumber")
        return when {
            quantity == PLURAL_ONE -> "$quantity"
            quantity % 10 in PLURAL_FEW_MIN..PLURAL_FEW_MAX &&
                quantity % 100 !in PLURAL_MANY_MIN..PLURAL_MANY_MAX -> "$quantity"

            else -> "$quantity"
        }
    }

    override fun getQuantityString(
        resId: Int,
        quantity: Int,
        vararg formatArgs: Any,
    ): String {
        // Заглушка для множественного числа - возвращает только число (для детального экрана)
        // ВАЖНО: Для детального экрана используем только сокращения,
        // поэтому getQuantityString должен возвращать только ЧИСЛО без слова
        return "$quantity"
    }

    override fun getString(
        resId: Int,
        vararg formatArgs: Any,
    ): String {
        // Заглушка для обычных строк
        return when (resId) {
            ResourceIds.TODAY -> "Сегодня"
            ResourceIds.REMAINING -> "осталось"
            ResourceIds.ELAPSED -> "прошло"
            ResourceIds.DAYS_ABBREVIATED -> "дн."
            ResourceIds.MONTHS_ABBREVIATED -> "мес."
            ResourceIds.YEARS_ABBREVIATED -> "г."
            RES_DAYS_ABBR -> "дн."
            RES_MONTHS_ABBR -> "мес."
            RES_YEARS_ABBR -> "г."
            ResourceIds.DAYS_COUNT,
            ResourceIds.MONTHS_COUNT,
            ResourceIds.YEARS_COUNT,
                -> "Заглушка"

            else -> "Заглушка"
        }
    }
}
