package com.dayscounter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dayscounter.data.database.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

// Data Access Object для записей событий. timestamp первичен, id — defensive tie-breaker.
@Dao
interface ItemDao {
    /** Получает все записи от новых к старым. См. KDoc интерфейса. */
    @Query("SELECT * FROM items ORDER BY timestamp DESC, id DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    /** Получает все записи по возрастанию (старые первые). См. KDoc интерфейса. */
    @Query("SELECT * FROM items ORDER BY timestamp ASC, id ASC")
    fun getAllItemsAsc(): Flow<List<ItemEntity>>

    /** Синоним [getAllItems]. */
    @Query("SELECT * FROM items ORDER BY timestamp DESC, id DESC")
    fun getAllItemsDesc(): Flow<List<ItemEntity>>

    /**
     * Получает запись по идентификатору.
     *
     * @param id Идентификатор записи
     * @return Запись или null, если не найдена
     */
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?

    /**
     * Получает запись по идентификатору в виде Flow.
     * Поток автоматически обновляется при изменениях записи в базе данных.
     *
     * @param id Идентификатор записи
     * @return Flow с записью или null, если не найдена
     */
    @Query("SELECT * FROM items WHERE id = :id")
    fun getItemByIdFlow(id: Long): Flow<ItemEntity?>

    /** Ищет записи в title/details. См. KDoc интерфейса. */
    @Query(
        "SELECT * FROM items WHERE title LIKE '%' || :searchQuery || '%' OR details LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC, id DESC"
    )
    fun searchItems(searchQuery: String): Flow<List<ItemEntity>>

    /**
     * Вставляет новую запись или заменяет существующую при конфликте.
     *
     * @param item Запись для вставки
     * @return Идентификатор вставленной записи
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long

    /**
     * Обновляет существующую запись.
     *
     * @param item Запись для обновления
     */
    @Update
    suspend fun updateItem(item: ItemEntity)

    /**
     * Удаляет запись.
     *
     * @param item Запись для удаления
     */
    @Delete
    suspend fun deleteItem(item: ItemEntity)

    /**
     * Удаляет все записи.
     */
    @Query("DELETE FROM items")
    suspend fun deleteAllItems()

    /**
     * Получает количество записей в базе данных.
     *
     * @return Количество записей
     */
    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemsCount(): Int
}
