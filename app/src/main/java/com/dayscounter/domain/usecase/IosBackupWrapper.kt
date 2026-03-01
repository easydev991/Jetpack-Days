package com.dayscounter.domain.usecase

import kotlinx.serialization.Serializable

/**
 * Обёртка для резервной копии в iOS формате.
 *
 * Используется для импорта бэкапов от iOS-приложения.
 * Отличие от [BackupWrapper]: items используют [IosBackupItem] с timestamp в секундах с 2001-01-01.
 *
 * @property format Формат бэкапа (всегда "ios" для этого wrapper'а)
 * @property items Список элементов в iOS формате
 */
@Serializable
data class IosBackupWrapper(
    val format: String? = null,
    val items: List<IosBackupItem>,
)
