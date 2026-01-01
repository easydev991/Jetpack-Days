package com.dayscounter.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dayscounter.data.database.dao.ItemDao
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DaysDatabaseTest {
    private lateinit var database: DaysDatabase

    @Before
    fun setup() {
        database =
            Room
                .inMemoryDatabaseBuilder(
                    ApplicationProvider.getApplicationContext(),
                    DaysDatabase::class.java,
                ).allowMainThreadQueries()
                .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun createDatabase_thenDatabaseIsCreated() {
        // Then
        assertNotNull(database)
    }

    @Test
    fun getItemDao_thenReturnsDao() {
        // When
        val dao: ItemDao = database.itemDao()

        // Then
        assertNotNull(dao)
    }
}
