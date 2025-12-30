package com.dayscounter.ui.util

/**
 * Утилиты для форматирования чисел с учетом русского языка.
 */
object NumberFormattingUtils {
    // Константы для проверки окончаний
    private const val MOD_100 = 100
    private const val MOD_10 = 10
    private const val ONE = 1
    private const val TWO = 2
    private const val FOUR = 4
    private const val ELEVEN = 11
    private const val NINETEEN = 19

    /**
     * Получает окончание для множественного числа слова "день".
     *
     * @param days Количество дней
     * @return Правильное окончание ("день", "дня", "дней")
     */
    fun getDaysEnding(days: Int): String {
        val lastTwoDigits = days % MOD_100
        val lastDigit = days % MOD_10

        return when {
            lastTwoDigits in ELEVEN..NINETEEN -> "дней"
            lastDigit == ONE -> "день"
            lastDigit in TWO..FOUR -> "дня"
            else -> "дней"
        }
    }

    /**
     * Форматирует количество дней с правильным окончанием.
     *
     * @param days Количество дней
     * @return Строка с количеством дней и правильным окончанием (например, "5 дней")
     */
    fun formatDaysCount(days: Int): String {
        return "$days ${getDaysEnding(days)}"
    }
}
