package com.dayscounter.domain.repository

import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow

/**
 * Интерфейс репозитория для работы с записями событий.
 * Определяет контракт для доступа к данным в domain слое.
 */
interface ItemRepository {
    /**
     * Получает все записи, отсортированные по дате (от новых к старым).
     *
     * @return Flow со списком всех записей
     */
    fun getAllItems(): Flow<List<Item>>

    /**
     * Получает все записи с заданным порядком сортировки.
     *
     * @param sortOrder Порядок сортировки
     * @return Flow со списком всех записей
     */
    fun getAllItems(sortOrder: SortOrder): Flow<List<Item>>

    /**
     * Получает запись по идентификатору.
     *
     * @param id Идентификатор записи
     * @return Запись или null, если не найдена
     */
    suspend fun getItemById(id: Long): Item?

    /**
     * Ищет записи по запросу в названии или описании.
     *
     * @param query Поисковый запрос
     * @return Flow со списком найденных записей
     */
    fun searchItems(query: String): Flow<List<Item>>

    /**
     * Вставляет новую запись.
     *
     * @param item Запись для вставки
     * @return Идентификатор вставленной записи
     */
    suspend fun insertItem(item: Item): Long

    /**
     * Обновляет существующую запись.
     *
     * @param item Запись для обновления
     */
    suspend fun updateItem(item: Item)

    /**
     * Удаляет запись.
     *
     * @param item Запись для удаления
     */
    suspend fun deleteItem(item: Item)

    /**
     * Удаляет все записи.
     */
    suspend fun deleteAllItems()

    /**
     * Получает количество записей в базе данных.
     *
     * @return Количество записей
     */
    suspend fun getItemsCount(): Int
}
