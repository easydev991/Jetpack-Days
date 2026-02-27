package com.dayscounter.ui.screens.createedit

import androidx.lifecycle.SavedStateHandle
import com.dayscounter.data.provider.ResourceProvider
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Создаёт тестовый ViewModel для UI тестов CreateEditScreen.
 *
 * Предоставляет минимальную реализацию с фейковыми зависимостями.
 */
fun createTestViewModel(): CreateEditScreenViewModel =
    CreateEditScreenViewModel(
        repository = createTestItemRepository(),
        resourceProvider = createTestResourceProvider(),
        savedStateHandle = SavedStateHandle(),
    )

/**
 * Создаёт тестовый репозиторий с пустой реализацией.
 */
private fun createTestItemRepository(): ItemRepository =
    object : ItemRepository {
        override fun getAllItems(): Flow<List<Item>> = flowOf()

        override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> = flowOf()

        override suspend fun getItemById(id: Long): Item? = null

        override fun getItemFlow(id: Long): Flow<Item?> = flowOf()

        override fun searchItems(query: String): Flow<List<Item>> = flowOf()

        override suspend fun insertItem(item: Item): Long = 0L

        override suspend fun updateItem(item: Item) {}

        override suspend fun deleteItem(item: Item) {}

        override suspend fun deleteAllItems() {}

        override suspend fun getItemsCount(): Int = 0
    }

/**
 * Создаёт тестовый провайдер ресурсов с пустой реализацией.
 */
private fun createTestResourceProvider(): ResourceProvider =
    object : ResourceProvider {
        override fun getString(
            resId: Int,
            vararg formatArgs: Any,
        ): String = ""

        override fun getQuantityString(
            resId: Int,
            quantity: Int,
            vararg formatArgs: Any,
        ): String = ""

        override fun getYearsString(quantity: Int): String = ""

        override fun getMonthsString(quantity: Int): String = ""
    }
