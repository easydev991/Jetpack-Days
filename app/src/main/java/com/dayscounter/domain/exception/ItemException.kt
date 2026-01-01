package com.dayscounter.domain.exception

/**
 * Исключение для ошибок при работе с элементами (Item).
 */
sealed class ItemException : Exception() {
    /**
     * Элемент не найден.
     */
    data class ItemNotFound(
        override val message: String,
    ) : ItemException()

    /**
     * Ошибка при сохранении элемента.
     */
    data class SaveFailed(
        override val message: String,
    ) : ItemException()

    /**
     * Ошибка при удалении элемента.
     */
    data class DeleteFailed(
        override val message: String,
    ) : ItemException()

    /**
     * Ошибка при получении списка элементов.
     */
    data class LoadFailed(
        override val message: String,
    ) : ItemException()

    /**
     * Ошибка при обновлении элемента.
     */
    data class UpdateFailed(
        override val message: String,
    ) : ItemException()
}
