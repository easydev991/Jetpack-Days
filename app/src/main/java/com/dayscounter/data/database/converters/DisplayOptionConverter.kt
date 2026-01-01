package com.dayscounter.data.database.converters

import androidx.room.TypeConverter
import com.dayscounter.domain.model.DisplayOption

/**
 * Конвертер для преобразования DisplayOption в строку и обратно для Room.
 */
class DisplayOptionConverter {
    companion object {
        private const val TAG = "DisplayOptionConverter"
    }

    /**
     * Преобразует DisplayOption в строку для хранения в базе данных.
     *
     * @param value DisplayOption для преобразования
     * @return Строковое представление (имя enum)
     */
    @TypeConverter
    fun fromDisplayOption(value: DisplayOption): String = value.name

    /**
     * Преобразует строку в DisplayOption из базы данных.
     *
     * @param value Строковое представление (имя enum)
     * @return DisplayOption или DEFAULT, если значение неизвестно
     */
    @TypeConverter
    fun toDisplayOption(value: String): DisplayOption =
        try {
            DisplayOption.valueOf(value)
        } catch (e: IllegalArgumentException) {
            android.util.Log.w(
                TAG,
                "Неизвестное значение DisplayOption: '$value', используется DEFAULT",
                e,
            )
            DisplayOption.DEFAULT
        }
}
