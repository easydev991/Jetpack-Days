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

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {

    private lateinit var database: DaysDatabase
    private lateinit var itemDao: ItemDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DaysDatabase::class.java
        ).allowMainThreadQueries().build()
        itemDao = database.itemDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertItem_thenRetrieveIt() = runBlocking {
        // Given
        val item = ItemEntity(
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
    fun getAllItems_thenReturnsAllItems() = runBlocking {
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
    fun getItemById_whenExists_thenReturnsItem() = runBlocking {
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
    fun getItemById_whenNotExists_thenReturnsNull() = runBlocking {
        // When
        val retrieved = itemDao.getItemById(999L)

        // Then
        assertNull(retrieved)
    }

    @Test
    fun searchItems_whenTitleMatches_thenReturnsItems() = runBlocking {
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
    fun searchItems_whenDetailsMatches_thenReturnsItems() = runBlocking {
        // Given
        val item1 = ItemEntity(
            title = "Событие 1",
            details = "Важное событие",
            timestamp = 1000000000000L
        )
        val item2 = ItemEntity(
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
    fun updateItem_thenChangesAreSaved() = runBlocking {
        // Given
        val item = ItemEntity(title = "Старое название", timestamp = 1234567890000L)
        val insertedId = itemDao.insertItem(item)

        // When
        val updatedItem = ItemEntity(
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
    fun deleteItem_thenItemIsRemoved() = runBlocking {
        // Given
        val item = ItemEntity(title = "Событие", timestamp = 1234567890000L)
        val insertedId = itemDao.insertItem(item)

        // When
        val itemToDelete = ItemEntity(
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
    fun deleteAllItems_thenAllItemsAreRemoved() = runBlocking {
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
    fun getItemsCount_thenReturnsCorrectCount() = runBlocking {
        // Given
        itemDao.insertItem(ItemEntity(title = "Событие 1", timestamp = 1000000000000L))
        itemDao.insertItem(ItemEntity(title = "Событие 2", timestamp = 2000000000000L))

        // When
        val count = itemDao.getItemsCount()

        // Then
        assertEquals(2, count)
    }

    @Test
    fun getItemsCount_whenEmpty_thenReturnsZero() = runBlocking {
        // When
        val count = itemDao.getItemsCount()

        // Then
        assertEquals(0, count)
    }
}

