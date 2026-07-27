package com.dayscounter.data.database.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.entity.ItemEntity
import com.dayscounter.domain.model.DisplayOption
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {
    private fun timestamp(hour: Int): Long =
        LocalDateTime
            .of(2026, 1, 15, hour, 0, 0)
            .atZone(ZoneId.of("UTC"))
            .toInstant()
            .toEpochMilli()

    private lateinit var database: DaysDatabase
    private lateinit var itemDao: ItemDao

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    DaysDatabase::class.java
                ).allowMainThreadQueries()
                .build()
        itemDao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertItem_thenRetrieveIt() =
        runBlocking {
            // Given
            val item =
                ItemEntity(
                    title = "Тестовое событие",
                    details = "Описание",
                    timestamp = 1234567890000L,
                    colorTag = 0xFFFF0000.toInt(),
                    displayOption = DisplayOption.MONTH_DAY.name
                )

            // When
            val insertedId = itemDao.insertItem(item)
            val retrieved = itemDao.getItemById(insertedId)

            // Then
            assertNotNull(retrieved)
            assertEquals(insertedId, retrieved?.id)
            assertEquals("Тестовое событие", retrieved?.title)
            assertEquals("Описание", retrieved?.details)
            assertEquals(1234567890000L, retrieved?.timestamp)
            assertEquals(0xFFFF0000.toInt(), retrieved?.colorTag)
            assertEquals(DisplayOption.MONTH_DAY.name, retrieved?.displayOption)
        }

    @Test
    fun getAllItems_thenReturnsAllItems() =
        runBlocking {
            // Given
            val item1 = ItemEntity(title = "Событие 1", timestamp = 1000000000000L)
            val item2 = ItemEntity(title = "Событие 2", timestamp = 2000000000000L)
            val item3 = ItemEntity(title = "Событие 3", timestamp = 3000000000000L)

            itemDao.insertItem(item1)
            itemDao.insertItem(item2)
            itemDao.insertItem(item3)

            // When
            val allItems = itemDao.getAllItems().first()

            // Then
            assertEquals(3, allItems.size)
            // Проверяем сортировку по timestamp DESC
            assertEquals("Событие 3", allItems[0].title)
            assertEquals("Событие 2", allItems[1].title)
            assertEquals("Событие 1", allItems[2].title)
        }

    @Test
    fun getAllItems_whenSameTimestamp_thenSortedByIdDesc() =
        runBlocking {
            // Given — items with same timestamp but inserted later = higher id
            val timestamp = 2000000000000L
            val itemA = ItemEntity(title = "A (старое)", timestamp = timestamp)
            val itemB = ItemEntity(title = "B (новое)", timestamp = timestamp)
            val idA = itemDao.insertItem(itemA)
            val idB = itemDao.insertItem(itemB)

            // When
            val allItems = itemDao.getAllItems().first()

            // Then — DESC: larger id first
            assertEquals(2, allItems.size)
            assertEquals(itemB.title, allItems[0].title)
            assertEquals(itemA.title, allItems[1].title)
        }

    @Test
    fun getAllItemsAsc_whenSameTimestamp_thenSortedByIdAsc() =
        runBlocking {
            // Given
            val timestamp = 2000000000000L
            val itemA = ItemEntity(title = "A (старое)", timestamp = timestamp)
            val itemB = ItemEntity(title = "B (новое)", timestamp = timestamp)
            val idA = itemDao.insertItem(itemA)
            val idB = itemDao.insertItem(itemB)

            // When
            val allItems = itemDao.getAllItemsAsc().first()

            // Then — ASC: smaller id first
            assertEquals(2, allItems.size)
            assertEquals(itemA.title, allItems[0].title)
            assertEquals(itemB.title, allItems[1].title)
        }

    @Test
    fun getAllItemsDesc_whenSameTimestamp_thenSortedByIdDesc() =
        runBlocking {
            // Given
            val timestamp = 2000000000000L
            val itemA = ItemEntity(title = "A (старое)", timestamp = timestamp)
            val itemB = ItemEntity(title = "B (новое)", timestamp = timestamp)
            val idA = itemDao.insertItem(itemA)
            val idB = itemDao.insertItem(itemB)

            // When
            val allItems = itemDao.getAllItemsDesc().first()

            // Then — DESC: larger id first
            assertEquals(2, allItems.size)
            assertEquals(itemB.title, allItems[0].title)
            assertEquals(itemA.title, allItems[1].title)
        }

    @Test
    fun getAllItems_whenDifferentTimestamps_thenTimestampDominates() =
        runBlocking {
            // Given — A (same date, older id), B (same date, newer id), C (different date, between them in id)
            val timestampA = 1000000000000L
            val timestampB = 3000000000000L
            val itemOld = ItemEntity(title = "Старое", timestamp = timestampA)
            val itemNew = ItemEntity(title = "Новое", timestamp = timestampB)
            val itemSame = ItemEntity(title = "Среднее", timestamp = timestampA)
            itemDao.insertItem(itemOld)
            itemDao.insertItem(itemNew)
            itemDao.insertItem(itemSame)

            // When
            val allItems = itemDao.getAllItems().first()

            // Then — timestamp dominates, id is only tiebreaker within same timestamp
            assertEquals("Новое", allItems[0].title) // newest timestamp first
            assertEquals("Среднее", allItems[1].title) // same timestamp as Old, but inserted later → larger id
            assertEquals("Старое", allItems[2].title)
        }

    @Test
    fun getItemById_whenExists_thenReturnsItem() =
        runBlocking {
            // Given
            val item = ItemEntity(title = "Событие", timestamp = 1234567890000L)
            val insertedId = itemDao.insertItem(item)

            // When
            val retrieved = itemDao.getItemById(insertedId)

            // Then
            assertNotNull(retrieved)
            assertEquals(insertedId, retrieved?.id)
            assertEquals("Событие", retrieved?.title)
        }

    @Test
    fun getItemById_whenNotExists_thenReturnsNull() =
        runBlocking {
            // When
            val retrieved = itemDao.getItemById(999L)

            // Then
            assertNull(retrieved)
        }

    @Test
    fun searchItems_whenTitleMatches_thenReturnsItems() =
        runBlocking {
            // Given
            val item1 = ItemEntity(title = "День рождения", timestamp = 1000000000000L)
            val item2 = ItemEntity(title = "Новый год", timestamp = 2000000000000L)
            val item3 = ItemEntity(title = "День победы", timestamp = 3000000000000L)

            itemDao.insertItem(item1)
            itemDao.insertItem(item2)
            itemDao.insertItem(item3)

            // When
            val results = itemDao.searchItems("День").first()

            // Then
            assertEquals(2, results.size)
            assertEquals(true, results.any { it.title == "День рождения" })
            assertEquals(true, results.any { it.title == "День победы" })
        }

    @Test
    fun searchItems_whenDetailsMatches_thenReturnsItems() =
        runBlocking {
            // Given
            val item1 =
                ItemEntity(
                    title = "Событие 1",
                    details = "Важное событие",
                    timestamp = 1000000000000L
                )
            val item2 =
                ItemEntity(
                    title = "Событие 2",
                    details = "Обычное событие",
                    timestamp = 2000000000000L
                )

            itemDao.insertItem(item1)
            itemDao.insertItem(item2)

            // When
            val results = itemDao.searchItems("Важное").first()

            // Then
            assertEquals(1, results.size)
            assertEquals("Событие 1", results[0].title)
        }

    @Test
    fun updateItem_thenChangesAreSaved() =
        runBlocking {
            // Given
            val item = ItemEntity(title = "Старое название", timestamp = 1234567890000L)
            val insertedId = itemDao.insertItem(item)

            // When
            val updatedItem =
                ItemEntity(
                    id = insertedId,
                    title = "Новое название",
                    timestamp = 1234567890000L
                )
            itemDao.updateItem(updatedItem)
            val retrieved = itemDao.getItemById(insertedId)

            // Then
            assertNotNull(retrieved)
            assertEquals("Новое название", retrieved?.title)
        }

    @Test
    fun deleteItem_thenItemIsRemoved() =
        runBlocking {
            // Given
            val item = ItemEntity(title = "Событие", timestamp = 1234567890000L)
            val insertedId = itemDao.insertItem(item)

            // When
            val itemToDelete =
                ItemEntity(
                    id = insertedId,
                    title = "Событие",
                    timestamp = 1234567890000L
                )
            itemDao.deleteItem(itemToDelete)
            val retrieved = itemDao.getItemById(insertedId)

            // Then
            assertNull(retrieved)
        }

    @Test
    fun deleteAllItems_thenAllItemsAreRemoved() =
        runBlocking {
            // Given
            itemDao.insertItem(ItemEntity(title = "Событие 1", timestamp = 1000000000000L))
            itemDao.insertItem(ItemEntity(title = "Событие 2", timestamp = 2000000000000L))
            itemDao.insertItem(ItemEntity(title = "Событие 3", timestamp = 3000000000000L))

            // When
            itemDao.deleteAllItems()
            val allItems = itemDao.getAllItems().first()

            // Then
            assertEquals(0, allItems.size)
        }

    @Test
    fun getItemsCount_thenReturnsCorrectCount() =
        runBlocking {
            // Given
            itemDao.insertItem(ItemEntity(title = "Событие 1", timestamp = 1000000000000L))
            itemDao.insertItem(ItemEntity(title = "Событие 2", timestamp = 2000000000000L))

            // When
            val count = itemDao.getItemsCount()

            // Then
            assertEquals(2, count)
        }

    @Test
    fun getItemsCount_whenEmpty_thenReturnsZero() =
        runBlocking {
            // When
            val count = itemDao.getItemsCount()

            // Then
            assertEquals(0, count)
        }

    @Test
    fun getAllItems_getAllItemsDesc_getAllItemsAsc_whenSameDateDifferentTimeOfDay() =
        runBlocking {
            // Given — same day, different time-of-day
            val timestampA = timestamp(9)
            val timestampB = timestamp(18)
            val itemA = ItemEntity(title = "A (09:00)", timestamp = timestampA)
            val itemB = ItemEntity(title = "B (18:00)", timestamp = timestampB)

            itemDao.insertItem(itemA)
            itemDao.insertItem(itemB)

            // When
            val allDesc = itemDao.getAllItems().first()
            val allDescExplicit = itemDao.getAllItemsDesc().first()
            val allAsc = itemDao.getAllItemsAsc().first()

            // Then — DESC: larger timestamp (B) first
            assertEquals(2, allDesc.size)
            assertEquals(itemB.title, allDesc[0].title)
            assertEquals(itemA.title, allDesc[1].title)

            assertEquals(2, allDescExplicit.size)
            assertEquals(itemB.title, allDescExplicit[0].title)
            assertEquals(itemA.title, allDescExplicit[1].title)

            // Then — ASC: smaller timestamp (A) first
            assertEquals(2, allAsc.size)
            assertEquals(itemA.title, allAsc[0].title)
            assertEquals(itemB.title, allAsc[1].title)
        }

    @Test
    fun searchItems_whenSameDateDifferentTimeOfDay_returnsByTimeOfDay() =
        runBlocking {
            // Given — same day, different time-of-day, both titles contain "Событие"
            val timestampMorning = timestamp(9)
            val timestampEvening = timestamp(18)
            val itemMorning = ItemEntity(title = "Событие утро", timestamp = timestampMorning)
            val itemEvening = ItemEntity(title = "Событие вечер", timestamp = timestampEvening)

            itemDao.insertItem(itemMorning)
            itemDao.insertItem(itemEvening)

            // When
            val results = itemDao.searchItems("Событие").first()

            // Then — DESC: вечер (larger timestamp) first, then утро
            assertEquals(2, results.size)
            assertEquals(itemEvening.title, results[0].title)
            assertEquals(itemMorning.title, results[1].title)
        }

    @Test
    fun deleteAllItems_then_reinsert_preservesTimestampOrder() =
        runBlocking {
            // Given — same day, different time-of-day
            val timestampA = timestamp(9)
            val timestampB = timestamp(18)

            // Insert both and capture original order
            itemDao.insertItem(ItemEntity(title = "A (09:00)", timestamp = timestampA))
            itemDao.insertItem(ItemEntity(title = "B (18:00)", timestamp = timestampB))

            val beforeDelete = itemDao.getAllItems().first()
            val expectedOrder = beforeDelete.map { it.title }

            // When — delete all, then re-insert with same timestamps (new instances, different ids)
            itemDao.deleteAllItems()

            itemDao.insertItem(ItemEntity(title = "A (09:00)", timestamp = timestampA))
            itemDao.insertItem(ItemEntity(title = "B (18:00)", timestamp = timestampB))

            val afterReinsert = itemDao.getAllItems().first()

            // Then — same relative order as before (B before A in DESC)
            assertEquals(2, afterReinsert.size)
            assertEquals(expectedOrder[0], afterReinsert[0].title)
            assertEquals(expectedOrder[1], afterReinsert[1].title)
        }
}
