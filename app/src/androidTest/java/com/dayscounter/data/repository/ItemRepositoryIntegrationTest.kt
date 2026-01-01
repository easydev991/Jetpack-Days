package com.dayscounter.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.database.dao.ItemDao
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
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
class ItemRepositoryIntegrationTest {
    private lateinit var database: DaysDatabase
    private lateinit var itemDao: ItemDao
    private lateinit var repository: ItemRepositoryImpl

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    DaysDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
        itemDao = database.itemDao()
        repository = ItemRepositoryImpl(itemDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `fullCycle_createReadUpdateDelete_thenWorksCorrectly`() =
        runBlocking {
            // Given - Create
            val item =
                Item(
                    title = "Тестовое событие",
                    details = "Описание",
                    timestamp = 1234567890000L,
                    colorTag = 0xFFFF0000.toInt(),
                    displayOption = DisplayOption.MONTH_DAY,
                )

            // When - Insert
            val insertedId = repository.insertItem(item)

            // Then - Read
            val retrieved = repository.getItemById(insertedId)
            assertNotNull(retrieved)
            assertEquals("Тестовое событие", retrieved?.title)
            assertEquals("Описание", retrieved?.details)
            assertEquals(0xFFFF0000.toInt(), retrieved?.colorTag)
            assertEquals(DisplayOption.MONTH_DAY, retrieved?.displayOption)

            // When - Update
            val updatedItem =
                retrieved!!.copy(
                    title = "Обновленное событие",
                    details = "Новое описание",
                )
            repository.updateItem(updatedItem)

            // Then - Verify Update
            val updated = repository.getItemById(insertedId)
            assertNotNull(updated)
            assertEquals("Обновленное событие", updated?.title)
            assertEquals("Новое описание", updated?.details)

            // When - Delete
            repository.deleteItem(updated!!)

            // Then - Verify Delete
            val deleted = repository.getItemById(insertedId)
            assertNull(deleted)
        }

    @Test
    fun `searchItems_whenMultipleItems_thenReturnsMatchingItems`() =
        runBlocking {
            // Given
            repository.insertItem(Item(title = "День рождения", timestamp = 1000000000000L))
            repository.insertItem(Item(title = "Новый год", timestamp = 2000000000000L))
            repository.insertItem(Item(title = "День победы", timestamp = 3000000000000L))

            // When
            val results = repository.searchItems("День").first()

            // Then
            assertEquals(2, results.size)
            assertEquals(true, results.any { it.title == "День рождения" })
            assertEquals(true, results.any { it.title == "День победы" })
        }

    @Test
    fun `getAllItems_thenReturnsAllItemsSorted`() =
        runBlocking {
            // Given
            repository.insertItem(Item(title = "Событие 1", timestamp = 1000000000000L))
            repository.insertItem(Item(title = "Событие 2", timestamp = 2000000000000L))
            repository.insertItem(Item(title = "Событие 3", timestamp = 3000000000000L))

            // When
            val allItems = repository.getAllItems().first()

            // Then
            assertEquals(3, allItems.size)
            // Проверяем сортировку по timestamp DESC
            assertEquals("Событие 3", allItems[0].title)
            assertEquals("Событие 2", allItems[1].title)
            assertEquals("Событие 1", allItems[2].title)
        }

    @Test
    fun `deleteAllItems_thenAllItemsAreRemoved`() =
        runBlocking {
            // Given
            repository.insertItem(Item(title = "Событие 1", timestamp = 1000000000000L))
            repository.insertItem(Item(title = "Событие 2", timestamp = 2000000000000L))
            repository.insertItem(Item(title = "Событие 3", timestamp = 3000000000000L))

            // When
            repository.deleteAllItems()
            val allItems = repository.getAllItems().first()

            // Then
            assertEquals(0, allItems.size)
            assertEquals(0, repository.getItemsCount())
        }

    @Test
    fun `getItemsCount_whenEmpty_thenReturnsZero`() =
        runBlocking {
            // When
            val count = repository.getItemsCount()

            // Then
            assertEquals(0, count)
        }

    @Test
    fun `getItemsCount_whenItemsExist_thenReturnsCorrectCount`() =
        runBlocking {
            // Given
            repository.insertItem(Item(title = "Событие 1", timestamp = 1000000000000L))
            repository.insertItem(Item(title = "Событие 2", timestamp = 2000000000000L))

            // When
            val count = repository.getItemsCount()

            // Then
            assertEquals(2, count)
        }

    @Test
    fun `conversionBetweenEntityAndDomain_preservesAllFields`() =
        runBlocking {
            // Given
            // Будет автогенерирован
            val originalItem =
                Item(
                    id = 0L,
                    title = "Оригинальное событие",
                    details = "Детали события",
                    timestamp = 9876543210000L,
                    colorTag = 0xFF00FF00.toInt(),
                    displayOption = DisplayOption.YEAR_MONTH_DAY,
                )

            // When
            val insertedId = repository.insertItem(originalItem)
            val retrieved = repository.getItemById(insertedId)

            // Then
            assertNotNull(retrieved)
            assertEquals(insertedId, retrieved?.id)
            assertEquals(originalItem.title, retrieved?.title)
            assertEquals(originalItem.details, retrieved?.details)
            assertEquals(originalItem.timestamp, retrieved?.timestamp)
            assertEquals(originalItem.colorTag, retrieved?.colorTag)
            assertEquals(originalItem.displayOption, retrieved?.displayOption)
        }

    @Test
    fun `conversionWithNullColorTag_preservesNull`() =
        runBlocking {
            // Given
            val item =
                Item(
                    title = "Событие без цвета",
                    timestamp = 1234567890000L,
                    colorTag = null,
                )

            // When
            val insertedId = repository.insertItem(item)
            val retrieved = repository.getItemById(insertedId)

            // Then
            assertNotNull(retrieved)
            assertNull(retrieved?.colorTag)
        }
}
