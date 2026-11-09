package com.dayscounter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore для хранения счётчиков review-триггеров
 * (openspec: add-rustore-in-app-review, design.md D3).
 *
 * Отдельное хранилище, а не расширение AppSettingsDataStore: служебные счётчики
 * не смешиваются с пользовательскими настройками UI, а сброс при полном удалении
 * данных трогает только этот файл. Имя файла "review_counters" уникально в
 * процессе — делегат preferencesDataStore держит синглтон DataStore (второе
 * свойство над тем же файлом бросило бы IllegalStateException).
 */
private val Context.reviewCountersDataStore: DataStore<Preferences> by preferencesDataStore(name = "review_counters")

/**
 * Ключи для хранения счётчиков review-триггеров в DataStore.
 * (Своё имя, а не PreferencesKeys: одноимённый приватный объект в
 * AppSettingsDataStore.kt того же пакета дал бы коллизию redeclaration.)
 */
private object ReviewCountersKeys {
    val CREATED_ITEMS_COUNT_KEY = intPreferencesKey("created_items_count")
    val PASSED_MILESTONES_KEY = stringSetPreferencesKey("passed_milestones")
    val PENDING_MILESTONE_KEY = intPreferencesKey("pending_milestone")
}

/**
 * Снимок состояния счётчиков review-триггеров.
 *
 * Поля совпадают с первыми тремя полями
 * [com.dayscounter.domain.usecase.ReviewMilestoneUseCase.State]; флаг «авто-запрос
 * уже выполнялся в этой сессии» туда не входит — это in-memory состояние
 * presentation-слоя, в хранилище оно не персистится (design.md D2).
 */
data class ReviewCountersSnapshot(
    val createdItemsCount: Int,
    val passedMilestones: Set<Int>,
    val pendingMilestone: Int?
)

/**
 * Хранилище счётчиков review-триггеров поверх preferences DataStore.
 *
 * Принимает [DataStore] в конструкторе (а не Context), чтобы работать и
 * тестироваться на JVM: в проде инстанс строит фабрика
 * [createReviewCountersDataStore] с синглтоном-делегатом, в тестах —
 * PreferenceDataStoreFactory.create над временным файлом.
 */
class ReviewCountersDataStore(
    private val dataStore: DataStore<Preferences>
) {
    /**
     * Flow снимка счётчиков: счётчик созданных записей, пройденные milestones
     * и запланированный, но ещё не показанный milestone.
     */
    val state: Flow<ReviewCountersSnapshot> =
        dataStore.data
            .map { preferences ->
                ReviewCountersSnapshot(
                    createdItemsCount = preferences[ReviewCountersKeys.CREATED_ITEMS_COUNT_KEY] ?: 0,
                    passedMilestones =
                        preferences[ReviewCountersKeys.PASSED_MILESTONES_KEY]
                            ?.mapNotNull(String::toIntOrNull)
                            ?.toSet()
                            .orEmpty(),
                    pendingMilestone = preferences[ReviewCountersKeys.PENDING_MILESTONE_KEY]
                )
            }

    /**
     * Увеличивает счётчик успешно созданных записей на 1.
     * Редактирование записи и импорт бэкапа этот метод не вызывают.
     *
     * @return Новое значение счётчика
     */
    suspend fun incrementCreatedItemsCount(): Int {
        val updated =
            dataStore.edit { preferences ->
                val current = preferences[ReviewCountersKeys.CREATED_ITEMS_COUNT_KEY] ?: 0
                preferences[ReviewCountersKeys.CREATED_ITEMS_COUNT_KEY] = current + 1
            }
        return checkNotNull(updated[ReviewCountersKeys.CREATED_ITEMS_COUNT_KEY])
    }

    /**
     * Записывает решение milestone-логики: запланированный, но ещё не показанный
     * milestone (design.md D2: запись решения — ответственность вызывающего кода).
     *
     * @param milestone Milestone или null для снятия pending
     */
    suspend fun setPendingMilestone(milestone: Int?) {
        dataStore.edit { preferences ->
            if (milestone != null) {
                preferences[ReviewCountersKeys.PENDING_MILESTONE_KEY] = milestone
            } else {
                preferences.remove(ReviewCountersKeys.PENDING_MILESTONE_KEY)
            }
        }
    }

    /**
     * Отмечает milestone пройденным — вызывается в момент запуска SDK-флоу,
     * независимо от того, была ли фактически показана форма. Pending снимается,
     * когда отмечается именно он: milestone показан и больше не ждёт показа.
     *
     * @param milestone Отмечаемый milestone
     */
    suspend fun markMilestonePassed(milestone: Int) {
        dataStore.edit { preferences ->
            val passed =
                preferences[ReviewCountersKeys.PASSED_MILESTONES_KEY]
                    ?.mapNotNull(String::toIntOrNull)
                    ?.toSet()
                    .orEmpty() + milestone
            preferences[ReviewCountersKeys.PASSED_MILESTONES_KEY] = passed.map(Int::toString).toSet()
            if (preferences[ReviewCountersKeys.PENDING_MILESTONE_KEY] == milestone) {
                preferences.remove(ReviewCountersKeys.PENDING_MILESTONE_KEY)
            }
        }
    }

    /**
     * Полный сброс счётчиков — вызывается из флоу полного удаления данных:
     * после удаления всех записей авто-запрос оценки начинается с нуля.
     */
    suspend fun reset() {
        dataStore.edit { preferences -> preferences.clear() }
    }
}

/**
 * Factory метод для создания ReviewCountersDataStore.
 * Используется для ручного DI вместо Hilt.
 *
 * @param context Контекст приложения
 * @return Экземпляр ReviewCountersDataStore
 */
fun createReviewCountersDataStore(context: Context): ReviewCountersDataStore =
    ReviewCountersDataStore(
        context.reviewCountersDataStore
    )
