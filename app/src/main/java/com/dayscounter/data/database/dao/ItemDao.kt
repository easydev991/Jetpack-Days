package com.dayscounter.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dayscounter.data.database.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object для работы с записями событий в базе данных.
 *
 * Семантика хранения и сортировки:
 * - `timestamp` хранится с millisecond precision (полный `LocalDateTime` → epoch millis,
 *   включая time-of-day). Это единственный осмысленный критерий порядка для same-date событий.
 * - `id` (rowid SQLite) — defensive tie-breaker, не отражает «новизну» события.
 *   Используется только для стабильности при коллизиях миллисекунд и для legacy-данных
 *   (созданных до миграции на millisecond precision), у которых `timestamp` приведён к началу дня.
 * - Все запросы с `ORDER BY` сортируют по `timestamp` первично и по `id` вторично.
 */
@Dao
interface ItemDao {
    /**
     * Получает все записи, отсортированные по дате (от новых к старым).
     * Первичный критерий — `timestamp` (millisecond precision); при равном `timestamp`
     * порядок определяется по `id DESC` (defensive tie-breaker, не «новизна» события).
     *
     * @return Flow со списком всех записей
     */
    @Query("SELECT * FROM items ORDER BY timestamp DESC, id DESC")
    fun getAllItems(): Flow<List<ItemEntity>>

    /**
     * Получает все записи с заданным порядком сортировки.
     * Первичный критерий — `timestamp` (millisecond precision); при равном `timestamp`
     * порядок определяется по `id ASC` (defensive tie-breaker).
     *
     * @param ascending true для сортировки по возрастанию (старые первые),
     *                  false для сортировки по убыванию (новые первые)
     * @return Flow со списком всех записей
     */
    @Query("SELECT * FROM items ORDER BY timestamp ASC, id ASC")
    fun getAllItemsAsc(): Flow<List<ItemEntity>>

    /**
     * Получает все записи, отсортированные по дате (от новых к старым).
     * Первичный критерий — `timestamp` (millisecond precision); при равном `timestamp`
     * порядок определяется по `id DESC` (defensive tie-breaker).
     * Синоним [getAllItems].
     */
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

    /**
     * Ищет записи по запросу в названии или описании.
     * Результаты сортируются по `timestamp DESC` (millisecond precision) с `id DESC`
     * как defensive tie-breaker.
     *
     * @param searchQuery Поисковый запрос
     * @return Flow со списком найденных записей
     */
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
