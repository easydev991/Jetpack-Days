@file:Suppress("TooGenericExceptionCaught")

package com.dayscounter.data.repository

import com.dayscounter.crash.CrashlyticsHelper
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.toDomain
import com.dayscounter.data.database.toEntity
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Реализация ItemRepository для работы с Room базой данных.
 *
 * @property itemDao DAO для доступа к данным
 *
 * Примечание: Мы используем общий перехват исключений здесь намеренно,
 * чтобы логировать любые ошибки базы данных в Crashlytics перед их переброской.
 * Это позволяет получить полную информацию о проблемах в production.
 */
class ItemRepositoryImpl(
    private val itemDao: ItemDao,
) : ItemRepository {
    override fun getAllItems(): Flow<List<Item>> =
        try {
            itemDao
                .getAllItems()
                .map { entities -> entities.map { it.toDomain() } }
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при получении списка элементов",
            )
            throw e
        }

    override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> {
        try {
            val flow =
                when (sortOrder) {
                    SortOrder.ASCENDING -> itemDao.getAllItemsAsc()
                    SortOrder.DESCENDING -> itemDao.getAllItemsDesc()
                }
            return flow.map { entities -> entities.map { it.toDomain() } }
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при получении списка элементов с сортировкой: $sortOrder",
            )
            throw e
        }
    }

    override suspend fun getItemById(id: Long): Item? =
        try {
            itemDao.getItemById(id)?.toDomain()
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при получении элемента с id: $id",
            )
            throw e
        }

    override fun getItemFlow(id: Long): Flow<Item?> =
        try {
            itemDao
                .getItemByIdFlow(id)
                .map { entity -> entity?.toDomain() }
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при получении потока элемента с id: $id",
            )
            throw e
        }

    override fun searchItems(query: String): Flow<List<Item>> =
        try {
            itemDao
                .searchItems(query)
                .map { entities -> entities.map { it.toDomain() } }
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при поиске элементов: $query",
            )
            throw e
        }

    override suspend fun insertItem(item: Item): Long =
        try {
            itemDao.insertItem(item.toEntity())
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при вставке элемента: ${item.title}",
            )
            throw e
        }

    override suspend fun updateItem(item: Item) {
        try {
            itemDao.updateItem(item.toEntity())
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при обновлении элемента: ${item.title}",
            )
            throw e
        }
    }

    override suspend fun deleteItem(item: Item) {
        try {
            itemDao.deleteItem(item.toEntity())
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при удалении элемента: ${item.title}",
            )
            throw e
        }
    }

    override suspend fun deleteAllItems() {
        try {
            itemDao.deleteAllItems()
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при удалении всех элементов",
            )
            throw e
        }
    }

    override suspend fun getItemsCount(): Int =
        try {
            itemDao.getItemsCount()
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при получении количества элементов",
            )
            throw e
        }
}
