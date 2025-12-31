package com.dayscounter.di

import com.dayscounter.DaysCounterApplication
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.formatter.ResourceProvider
import com.dayscounter.data.repository.ItemRepositoryImpl
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.viewmodel.DetailScreenViewModel
import com.dayscounter.viewmodel.MainScreenViewModel

/**
 * Модуль внедрения зависимостей для приложения.
 *
 * Использует ручной подход к DI через factory методы.
 *
 * Создает экземпляры репозиториев, use cases и ViewModel.
 */
object AppModule {
    /**
     * Создает ResourceProvider для работы со строковыми ресурсами.
     *
     * @return Экземпляр ResourceProvider
     */
    val resourceProvider: ResourceProvider by lazy {
        FormatterModule.createResourceProvider(DaysCounterApplication.instance)
    }

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
    ): (
        androidx.lifecycle.SavedStateHandle,
    ) -> DetailScreenViewModel {
        return { savedStateHandle ->
            DetailScreenViewModel(repository, savedStateHandle)
        }
    }

    /**
     * Создает ViewModel для экрана создания/редактирования.
     *
     * @param repository Репозиторий для работы с данными
     * @return Factory для создания CreateEditScreenViewModel с параметрами
     */
    fun createCreateEditScreenViewModelFactory(
        repository: ItemRepository,
    ): (androidx.lifecycle.SavedStateHandle) -> com.dayscounter.viewmodel.CreateEditScreenViewModel {
        return { savedStateHandle ->
            com.dayscounter.viewmodel.CreateEditScreenViewModel(
                repository,
                resourceProvider,
                savedStateHandle,
            )
        }
    }

    /**
     * Возвращает FormatterModule для форматирования дней.
     *
     * @return Объект FormatterModule
     */
    fun getFormatterModule(): FormatterModule {
        return FormatterModule
    }
}
