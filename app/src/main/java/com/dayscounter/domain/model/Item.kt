package com.dayscounter.domain.model

/**
 * Доменная модель события для отслеживания дней.
 *
 * @property id Уникальный идентификатор события
 * @property title Название события (обязательное поле)
 * @property details Описание события (необязательное, по умолчанию пустая строка)
 * @property timestamp Дата события в миллисекундах с 1970-01-01
 * @property colorTag ARGB-цвет для цветовой метки (необязательное)
 * @property displayOption Опция отображения дней (по умолчанию DAY)
 */
data class Item(
    val id: Long = 0L,
    val title: String,
    val details: String = "",
    val timestamp: Long,
    val colorTag: Int? = null,
    val displayOption: DisplayOption = DisplayOption.DEFAULT
)
