package com.dayscounter.di

import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.formatter.ResourceProviderImpl
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.viewmodel.CreateEditScreenViewModel
import com.dayscounter.viewmodel.DetailScreenViewModel
import com.dayscounter.viewmodel.MainScreenViewModel

/**
 * Модуль внедрения зависимостей для приложения.
 *
 * Использует ручной подход к DI через factory методы.
 * Hilt не используется для упрощения сборки и уменьшения зависимостей.
 *
 * Создает экземпляры репозиториев, use cases и ViewModel.
 */
object AppModule {
    /**
     * Создает репозиторий для работы с событиями.
     *
     * @param database База данных Room
     * @return Экземпляр ItemRepository
     */
    fun createItemRepository(database: DaysDatabase): ItemRepository {
        return ItemRepositoryImpl(database.itemDao())
    }

    /**
     * Создает ViewModel для главного экрана.
     *
     * @param repository Репозиторий для работы с данными
     * @return Экземпляр MainScreenViewModel
     */
    fun createMainScreenViewModel(repository: ItemRepository): MainScreenViewModel {
        return MainScreenViewModel(repository)
    }

    /**
     * Создает ViewModel для экрана деталей.
     *
     * @param repository Репозиторий для работы с данными
     * @return Factory для создания DetailScreenViewModel с параметрами
     */
    fun createDetailScreenViewModelFactory(
        repository: ItemRepository,
    ): (androidx.lifecycle.SavedStateHandle) ->
        DetailScreenViewModel(repository, savedStateHandle)

    /**
     * Создает ViewModel для экрана создания/редактирования.
     *
     * @param repository Репозиторий для работы с данными
     * @return Factory для создания CreateEditScreenViewModel с параметрами
     */
    fun createCreateEditScreenViewModelFactory(
        repository: ItemRepository,
    ): (androidx.lifecycle.SavedStateHandle) -> CreateEditScreenViewModel {
        return { savedStateHandle ->
            CreateEditScreenViewModel(repository, savedStateHandle)
        }
    }

    /**
     * Создает FormatterModule для форматирования дней.
     *
     * @param context Контекст приложения
     * @return Экземпляр FormatterModule
     */
    fun createFormatterModule(context: android.content.Context): FormatterModule {
        return FormatterModule(
            resourceProvider = ResourceProviderImpl(context),
        )
    }
}
