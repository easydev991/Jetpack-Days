package com.dayscounter.domain.repository

import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import kotlinx.coroutines.flow.Flow
import org.junit.jupiter.api.Test

/**
 * Тесты для проверки контракта интерфейса ItemRepository.
 * Эти тесты проверяют только структуру интерфейса, не реализацию.
 */
class ItemRepositoryTest {
    /**
     * Тест проверяет, что интерфейс определен корректно.
     * Реальная реализация будет протестирована в ItemRepositoryImplTest.
     */
    @Test
    fun `interfaceDefinesAllRequiredMethods`() {
        // Этот тест просто проверяет, что интерфейс компилируется
        // Реальная проверка контракта будет в тестах реализации
        val repository: ItemRepository =
            object : ItemRepository {
                override fun getAllItems(): Flow<List<Item>> = throw NotImplementedError()

                override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> = throw NotImplementedError()

                override suspend fun getItemById(id: Long): Item? = throw NotImplementedError()

                override fun searchItems(query: String): Flow<List<Item>> = throw NotImplementedError()

                override suspend fun insertItem(item: Item): Long = throw NotImplementedError()

                override suspend fun updateItem(item: Item): Unit = throw NotImplementedError()

                override suspend fun deleteItem(item: Item): Unit = throw NotImplementedError()

                override suspend fun deleteAllItems(): Unit = throw NotImplementedError()

                override suspend fun getItemsCount(): Int = throw NotImplementedError()
            }

        // Если код компилируется, значит интерфейс определен правильно
        assert(repository != null)
    }
}
