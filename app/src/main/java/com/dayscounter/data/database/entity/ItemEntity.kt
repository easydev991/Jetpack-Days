package com.dayscounter.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.dayscounter.data.database.converters.DisplayOptionConverter
import com.dayscounter.domain.model.DisplayOption

/**
 * Room Entity для хранения событий в базе данных.
 *
 * @property id Уникальный идентификатор (автогенерируемый)
 * @property title Название события
 * @property details Описание события
 * @property timestamp Дата события в миллисекундах
 * @property colorTag ARGB-цвет для цветовой метки (может быть null)
 *   Room поддерживает Int? напрямую, конвертер не требуется
 * @property displayOption Опция отображения дней (хранится как строка)
 */
@Entity(tableName = "items")
@TypeConverters(DisplayOptionConverter::class)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val details: String = "",
    val timestamp: Long,
    val colorTag: Int? = null,
    val displayOption: String = DisplayOption.DAY.name
)

