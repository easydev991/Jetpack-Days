package com.dayscounter.data.repository

import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.data.database.mapper.toDomain
import com.dayscounter.data.database.mapper.toEntity
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Реализация ItemRepository для работы с Room базой данных.
 *
 * @property itemDao DAO для доступа к данным
 */
class ItemRepositoryImpl(
    private val itemDao: ItemDao,
) : ItemRepository {
    override fun getAllItems(): Flow<List<Item>> =
        itemDao
            .getAllItems()
            .map { entities -> entities.map { it.toDomain() } }

    override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> {
        val flow =
            when (sortOrder) {
                SortOrder.ASCENDING -> itemDao.getAllItemsAsc()
                SortOrder.DESCENDING -> itemDao.getAllItemsDesc()
            }
        return flow.map { entities -> entities.map { it.toDomain() } }
    }

    override suspend fun getItemById(id: Long): Item? = itemDao.getItemById(id)?.toDomain()

    override fun getItemFlow(id: Long): Flow<Item?> =
        itemDao
            .getItemByIdFlow(id)
            .map { entity -> entity?.toDomain() }

    override fun searchItems(query: String): Flow<List<Item>> =
        itemDao
            .searchItems(query)
            .map { entities -> entities.map { it.toDomain() } }

    override suspend fun insertItem(item: Item): Long = itemDao.insertItem(item.toEntity())

    override suspend fun updateItem(item: Item) {
        itemDao.updateItem(item.toEntity())
    }

    override suspend fun deleteItem(item: Item) {
        itemDao.deleteItem(item.toEntity())
    }

    override suspend fun deleteAllItems() {
        itemDao.deleteAllItems()
    }

    override suspend fun getItemsCount(): Int = itemDao.getItemsCount()
}
