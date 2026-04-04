package com.dayscounter.domain.usecase

import kotlinx.serialization.Serializable

/**
 * Обёртка для резервной копии с указанием формата.
 *
 * Используется для импорта/экспорта бэкапов с явным указанием платформы-источника.
 * Поле `format` nullable для обратной совместимости со старыми файлами без этого поля.
 *
 * @property format Формат бэкапа (android, ios) или null для старых файлов
 * @property items Список элементов резервной копии
 */
@Serializable
data class BackupWrapper(
    val format: BackupFormat? = null,
    val items: List<BackupItem>
)
