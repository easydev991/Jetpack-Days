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

    override fun getQuantityString(
        resId: Int,
        quantity: Int,
        vararg formatArgs: Any,
    ): String {
        // Заглушка для множественного числа
        @Suppress("MagicNumber")
        return when {
            quantity == PLURAL_ONE -> "$quantity день"
            quantity % 10 in PLURAL_FEW_MIN..PLURAL_FEW_MAX &&
                quantity % 100 !in PLURAL_MANY_MIN..PLURAL_MANY_MAX -> "$quantity дня"
            else -> "$quantity дней"
        }
    }

    override fun getString(
        resId: Int,
        vararg formatArgs: Any,
    ): String {
        // Заглушка для обычных строк
        return when (resId) {
            ResourceIds.TODAY -> "Сегодня"
            RES_DAYS_ABBR -> "дн."
            RES_MONTHS_ABBR -> "мес."
            RES_YEARS_ABBR -> "г."
            else -> "Заглушка"
        }
    }
}
