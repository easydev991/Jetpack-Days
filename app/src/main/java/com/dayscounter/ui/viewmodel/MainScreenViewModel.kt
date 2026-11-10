package com.dayscounter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.ReviewAutoRequestOutcome
import com.dayscounter.analytics.ReviewErrorType
import com.dayscounter.data.preferences.AppSettingsDataStore
import com.dayscounter.data.preferences.ReviewCountersDataStore
import com.dayscounter.domain.exception.ItemException.DeleteFailed
import com.dayscounter.domain.exception.ItemException.UpdateFailed
import com.dayscounter.domain.exception.ReviewException
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.ReviewFlowOutcome
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.domain.usecase.ReviewFlowLauncher
import com.dayscounter.review.ReviewSessionHolder
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

private const val STATE_TIMEOUT_MS = 5000L
private const val TAG = "MainScreenViewModel"

// Задержка перед авто-запросом review-флоу: даём экрану событий успеть отрисоваться
private const val REVIEW_AUTO_REQUEST_DELAY_MS = 800L

/**
 * Review-зависимости авто-запроса оценки для Events-экрана (задача 7.2).
 *
 * Собраны в один nullable-параметр с дефолтом null, чтобы существующие
 * вызовы фабрики и юнит-тесты (без review-части) остались совместимыми:
 * null = фича выключена, авто-запрос не выполняется.
 *
 * @property counters Хранилище счётчиков review-триггеров
 * @property flowLauncher Лаунчер нативной формы оценки (flavor-реализация)
 * @property isReviewEnabled Флаг flavor-фичи ([BuildConfig.RUSTORE_FEATURES])
 * @property analytics Сервис аналитики для логирования исхода
 */
data class ReviewTriggerDependencies(
    val counters: ReviewCountersDataStore,
    val flowLauncher: ReviewFlowLauncher,
    val isReviewEnabled: Boolean,
    val analytics: AnalyticsService
)

/**
 * Маппит доменную классификацию ошибки запуска флоу в тип события аналитики.
 * Доменные классы в аналитику не тянутся — преобразование живёт в presentation-слое.
 */
private fun ReviewException.toReviewErrorType(): ReviewErrorType =
    when (this) {
        is ReviewException.NotInstalled -> ReviewErrorType.NOT_INSTALLED
        is ReviewException.Outdated -> ReviewErrorType.OUTDATED
        is ReviewException.NotAuthorized -> ReviewErrorType.NOT_AUTHORIZED
        is ReviewException.RequestLimitReached -> ReviewErrorType.REQUEST_LIMIT_REACHED
        is ReviewException.ReviewAlreadyExists -> ReviewErrorType.REVIEW_ALREADY_EXISTS
        is ReviewException.Unknown -> ReviewErrorType.UNKNOWN
    }

/**
 * ViewModel для управления состоянием главного экрана со списком событий.
 *
 * Отвечает за загрузку, управление и отображение списка событий.
 * Порядок сортировки сохраняется в DataStore для сохранения выбора пользователя.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class MainScreenViewModel(
    private val repository: ItemRepository,
    private val dataStore: AppSettingsDataStore,
    private val logger: Logger = AndroidLogger(),
    private val reviewDeps: ReviewTriggerDependencies? = null
) : ViewModel() {
    companion object {
        fun factory(
            repository: ItemRepository,
            dataStore: AppSettingsDataStore,
            reviewDeps: ReviewTriggerDependencies? = null
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    MainScreenViewModel(repository, dataStore, AndroidLogger(), reviewDeps)
                }
            }
    }

    /**
     * Состояние экрана.
     */
    private val _uiState = MutableStateFlow<MainScreenState>(MainScreenState.Loading)
    val uiState: StateFlow<MainScreenState> = _uiState.asStateFlow()

    /**
     * Поисковый запрос.
     */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Порядок сортировки (читается из DataStore).
     */
    val sortOrder: StateFlow<SortOrder> =
        dataStore.sortOrder.stateIn(
            scope = viewModelScope,
            started =
                kotlinx.coroutines.flow.SharingStarted
                    .WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = SortOrder.DESCENDING
        )

    /**
     * Количество элементов (для отображения кнопки сортировки).
     */
    private val _itemsCount = MutableStateFlow(0)
    val itemsCount: StateFlow<Int> = _itemsCount.asStateFlow()

    /**
     * Выбранный цветовой фильтр (null = фильтр выключен).
     * Источник данных - DataStore для сохранения между перезапусками.
     */
    val selectedColorTag: StateFlow<Int?> =
        dataStore.mainScreenColorTagFilter.stateIn(
            scope = viewModelScope,
            started =
                kotlinx.coroutines.flow.SharingStarted
                    .WhileSubscribed(STATE_TIMEOUT_MS),
            initialValue = null
        )

    /**
     * Доступные цвета из текущих записей (уникальные).
     */
    private val _availableColorTags = MutableStateFlow<List<Int>>(emptyList())
    val availableColorTags: StateFlow<List<Int>> = _availableColorTags.asStateFlow()

    /**
     * Элемент для удаления в диалоговом окне.
     */
    private val _showDeleteDialog = MutableStateFlow<Item?>(null)
    val showDeleteDialog: StateFlow<Item?> = _showDeleteDialog.asStateFlow()

    /**
     * Показывать ли диалог фильтра по цвету.
     */
    private val _showFilterDialog = MutableStateFlow(false)
    val showFilterDialog: StateFlow<Boolean> = _showFilterDialog.asStateFlow()

    /**
     * Сериализует повторные проверки pending-milestone (init + возврат на экран).
     */
    private val reviewRequestMutex = Mutex()

    /**
     * Загружает события при создании ViewModel.
     */
    init {
        // Observe items and update UI state accordingly
        observeItems()
        maybeRunReviewAutoRequest()
    }

    /**
     * Повторная проверка pending-milestone при возврате на Events-экран:
     * ViewModel скоуплена к back stack entry и переживает навигацию,
     * поэтому pending, запланированный при создании записи в этой же
     * сессии, init уже не увидит.
     */
    fun checkPendingReviewRequest() {
        maybeRunReviewAutoRequest()
    }

    /**
     * Отложенный показ pending-milestone review-флоу (задача 7.2).
     *
     * Если запланирован pending-milestone и авто-запрос в этой сессии ещё
     * не выполнялся, после короткой задержки (даём экрану отрисоваться)
     * запускает flavor-лаунчер формы оценки. Флаг сессии закрывается до
     * запуска (лимит не превышается даже при ошибке), milestone помечается
     * пройденным при запуске флоу, pending снимается. Ошибки лаунчера
     * логируются в аналитику тихо — без UI-состояний и фолбэков.
     */
    private fun maybeRunReviewAutoRequest() {
        val deps = reviewDeps ?: return
        if (!deps.isReviewEnabled) return
        viewModelScope.launch {
            // Мьютекс сериализует init-проверку и повторные проверки при
            // возврате на экран: обе могли пройти holder-проверку до отметки
            // флага и запустить флоу дважды
            reviewRequestMutex.withLock {
                val snapshot =
                    try {
                        deps.counters.state.first()
                    } catch (e: IOException) {
                        // Сбой файла счётчиков не должен ронять экран списка:
                        // попытка повторится при следующем возврате на экран
                        logger.e(TAG, "Не удалось прочитать счётчики review-триггеров", e)
                        return@launch
                    }
                val pendingMilestone = snapshot.pendingMilestone ?: return@launch
                if (ReviewSessionHolder.wasAutoRequestShown()) {
                    logger.d(TAG, "Авто-запрос review-флоу уже выполнялся в этой сессии, пропуск")
                    return@launch
                }
                delay(REVIEW_AUTO_REQUEST_DELAY_MS)
                showReviewFormIfStillPending(deps, pendingMilestone)
            }
        }
    }

    /**
     * Показ нативной формы после задержки: перепроверяет pending (за окно
     * ожидания состояние могло измениться — например, пользователь успел
     * удалить все данные и pending снят), закрывает флаг сессии, помечает
     * milestone пройденным, запускает flavor-лаунчер и логирует исход в
     * аналитику. Все ошибки — тихие, без UI-состояний.
     */
    private suspend fun showReviewFormIfStillPending(
        deps: ReviewTriggerDependencies,
        pendingMilestone: Int
    ) {
        // Перепроверка после задержки: сбой чтения (залогирован в хелпере),
        // снятый или заменённый pending — тихий пропуск; повторная проверка
        // запустится при следующем возврате на экран
        val currentPending = readCurrentPending(deps)
        if (currentPending != pendingMilestone) {
            logger.d(
                TAG,
                "Pending review-триггера изменился за окно ожидания ($pendingMilestone → $currentPending), пропуск"
            )
            return
        }
        // Флаг сессии закрывается до запуска: даже при ошибке повторных
        // авто-запросов в этой сессии не будет
        ReviewSessionHolder.markAutoRequestShown()
        // Milestone помечается пройденным в момент запуска флоу независимо
        // от фактического исхода (design.md Risks)
        // Pending снимается внутри markMilestonePassed при совпадении
        // milestone; повторное планирование в сессии блокирует holder
        if (!markMilestonePassedQuietly(deps, pendingMilestone)) return
        val outcome =
            try {
                deps.flowLauncher.launch()
            } catch (e: CancellationException) {
                // Milestone уже помечен и флаг сессии закрыт — фиксируем
                // потерянный исход в аналитике до проброса отмены
                deps.analytics.log(
                    AnalyticsEvent.ReviewAutoRequest(ReviewAutoRequestOutcome.CANCELLED)
                )
                logger.d(TAG, "Авто-запрос review-флоу отменён: milestone=$pendingMilestone")
                throw e
            }
        val event =
            when (outcome) {
                is ReviewFlowOutcome.Shown ->
                    AnalyticsEvent.ReviewAutoRequest(ReviewAutoRequestOutcome.SHOWN)
                is ReviewFlowOutcome.Error ->
                    AnalyticsEvent.ReviewAutoRequest(
                        ReviewAutoRequestOutcome.ERROR,
                        outcome.error.toReviewErrorType()
                    )
            }
        deps.analytics.log(event)
        logger.d(TAG, "Авто-запрос review-флоу завершён: milestone=$pendingMilestone, исход=$outcome")
    }

    /**
     * Читает текущий pending из хранилища; при сбое файла счётчиков логирует
     * ошибку и возвращает null (вызывающий код трактует как «pending снят»).
     */
    private suspend fun readCurrentPending(deps: ReviewTriggerDependencies): Int? =
        try {
            deps.counters.state
                .first()
                .pendingMilestone
        } catch (e: IOException) {
            logger.e(TAG, "Не удалось перепроверить pending review-триггера", e)
            null
        }

    /**
     * Помечает milestone пройденным; при сбое файла счётчиков логирует ошибку
     * и сообщает неудачу — форма в этом случае не запускается.
     */
    private suspend fun markMilestonePassedQuietly(
        deps: ReviewTriggerDependencies,
        pendingMilestone: Int
    ): Boolean =
        try {
            deps.counters.markMilestonePassed(pendingMilestone)
            true
        } catch (e: IOException) {
            logger.e(TAG, "Не удалось отметить milestone review-триггера пройденным", e)
            false
        }

    /**
     * Наблюдает за изменением элементов и обновляет состояние UI.
     */
    private fun observeItems() {
        viewModelScope.launch {
            combine(
                sortOrder.flatMapLatest { order -> repository.getAllItems(order) },
                _searchQuery,
                selectedColorTag
            ) { items, query, selectedColor ->
                logger.d(
                    TAG,
                    "Обновление списка: количество=${items.size}, " +
                        "запрос='$query', фильтр=$selectedColor"
                )

                // Обновляем доступные цвета
                val availableColors =
                    items
                        .mapNotNull { it.colorTag }
                        .distinct()
                        .sorted()
                _availableColorTags.value = availableColors

                // Проверяем, что выбранный цвет ещё доступен; если нет — сбрасываем
                val effectiveSelectedColor =
                    selectedColor?.takeIf { availableColors.contains(it) }
                if (selectedColor != null && effectiveSelectedColor == null) {
                    logger.d(TAG, "Выбранный цвет $selectedColor более недоступен, сброс фильтра")
                    viewModelScope.launch {
                        dataStore.setMainScreenColorTagFilter(null)
                    }
                }

                // Применяем фильтры: поиск -> цвет
                applyFilters(items, query, effectiveSelectedColor)
            }.collect { items ->
                logger.d(TAG, "Обновление UI: количество элементов=${items.size}")
                _itemsCount.value = items.size
                _uiState.value = MainScreenState.Success(items)
            }
        }
    }

    /**
     * Применяет фильтры поиска и цвета к списку элементов.
     */
    private fun applyFilters(
        items: List<Item>,
        query: String,
        selectedColor: Int?
    ): List<Item> {
        // Фильтр по поиску
        var result =
            if (query.isEmpty()) {
                items
            } else {
                items
                    .filter { item ->
                        val titleContains = item.title.contains(query, ignoreCase = true)
                        val detailsContains = item.details.contains(query, ignoreCase = true)
                        logger.d(
                            TAG,
                            "Фильтрация: элемент='${item.title}', " +
                                "детали='${item.details}', " +
                                "запрос='$query', " +
                                "titleContains=$titleContains, " +
                                "detailsContains=$detailsContains"
                        )
                        titleContains || detailsContains
                    }.also {
                        logger.d(TAG, "После поиска: ${it.size} элементов")
                    }
            }

        // Фильтр по цвету
        if (selectedColor != null) {
            result = result.filter { it.colorTag == selectedColor }
            logger.d(TAG, "После фильтра по цвету $selectedColor: ${result.size} элементов")
        }

        return result
    }

    /**
     * Обновляет поисковый запрос.
     *
     * @param query Новый поисковый запрос
     */
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Обновляет порядок сортировки.
     *
     * @param order Новый порядок сортировки
     */
    fun updateSortOrder(order: SortOrder) {
        viewModelScope.launch {
            dataStore.setSortOrder(order)
            logger.d(TAG, "Порядок сортировки обновлен и сохранен: $order")
        }
    }

    /**
     * Обновляет фильтр по цветовому тегу.
     *
     * @param colorTag Выбранный цвет (null для сброса фильтра)
     */
    fun updateSelectedColorTag(colorTag: Int?) {
        viewModelScope.launch {
            dataStore.setMainScreenColorTagFilter(colorTag)
            logger.d(TAG, "Фильтр по цвету обновлен и сохранен: $colorTag")
        }
    }

    /**
     * Очищает фильтр по цветовому тегу.
     */
    fun clearColorTagFilter() {
        viewModelScope.launch {
            dataStore.setMainScreenColorTagFilter(null)
            logger.d(TAG, "Фильтр по цвету очищен и сохранен")
        }
    }

    /**
     * Переключает видимость диалога фильтра.
     */
    fun toggleFilterDialog() {
        _showFilterDialog.value = !_showFilterDialog.value
        logger.d(TAG, "Диалог фильтра переключён: ${_showFilterDialog.value}")
    }

    /**
     * Удаляет событие.
     *
     * @param item Событие для удаления
     */
    fun deleteItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.deleteItem(item)
                logger.d(TAG, "Событие удалено: ${item.title}")
            } catch (e: DeleteFailed) {
                val message = "Ошибка удаления события: ${e.message}"
                logger.e(TAG, message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }

    /**
     * Запрашивает подтверждение удаления записи.
     *
     * @param item Запись для удаления
     */
    fun requestDelete(item: Item) {
        _showDeleteDialog.value = item
        logger.d(TAG, "Запрос на удаление: ${item.title}")
    }

    /**
     * Подтверждает удаление записи.
     */
    fun confirmDelete() {
        _showDeleteDialog.value?.let { item ->
            deleteItem(item)
            logger.d(TAG, "Удаление подтверждено: ${item.title}")
        }
        _showDeleteDialog.value = null
    }

    /**
     * Отменяет удаление записи.
     */
    fun cancelDelete() {
        _showDeleteDialog.value = null
        logger.d(TAG, "Удаление отменено")
    }

    /**
     * Обновляет событие.
     *
     * @param item Событие для обновления
     */
    fun updateItem(item: Item) {
        viewModelScope.launch {
            try {
                repository.updateItem(item)
                logger.d(TAG, "Событие обновлено: ${item.title}")
            } catch (e: UpdateFailed) {
                val message = "Ошибка обновления события: ${e.message}"
                logger.e(TAG, message, e)
                _uiState.value = MainScreenState.Error(message)
            }
        }
    }
}

/**
 * Состояние главного экрана.
 */
sealed class MainScreenState {
    /** Загрузка данных */
    data object Loading : MainScreenState()

    /** Успешная загрузка */
    data class Success(
        val items: List<Item>
    ) : MainScreenState()

    /** Ошибка загрузки */
    data class Error(
        val message: String
    ) : MainScreenState()
}
