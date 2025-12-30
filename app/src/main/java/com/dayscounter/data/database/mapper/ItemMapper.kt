package com.dayscounter.data.database.mapper

import com.dayscounter.data.database.converters.DisplayOptionConverter
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.Item

/**
 * Мапперы для конвертации между Room Entity и Domain Model.
 */
private val displayOptionConverter = DisplayOptionConverter()

/**
 * Преобразует ItemEntity в доменную модель Item.
 *
 * @return Доменная модель Item
 */
fun ItemEntity.toDomain(): Item {
    return Item(
        id = id,
        title = title,
        details = details,
        timestamp = timestamp,
        colorTag = colorTag,
        displayOption = displayOptionConverter.toDisplayOption(displayOption),
    )
}

/**
 * Преобразует доменную модель Item в ItemEntity.
 *
 * @return Room Entity ItemEntity
 */
fun Item.toEntity(): ItemEntity {
    return ItemEntity(
        id = id,
        title = title,
        details = details,
        timestamp = timestamp,
        colorTag = colorTag,
        displayOption = displayOptionConverter.fromDisplayOption(displayOption),
    )
}
