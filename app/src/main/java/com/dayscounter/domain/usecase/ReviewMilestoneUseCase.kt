package com.dayscounter.domain.usecase

/**
 * Чистый stateless use case milestone-логики авто-запроса оценки
 * (openspec: add-rustore-in-app-review, design.md D2).
 *
 * По снимку состояния счётчиков решает, нужно ли запланировать авто-запрос
 * нативной формы оценки для milestone N. Ничего не хранит и не мутирует:
 * состояние приходит снаружи (хранилище счётчиков + holder сессии из
 * presentation-слоя), а инкремент счётчика, запись решения и отметка
 * milestone пройденным — ответственность вызывающего кода.
 *
 * Правила решения:
 * 1. Не более одного авто-запроса за сессию приложения — флаг передаёт
 *    holder сессии, use case его не хранит.
 * 2. Непоказанный pending не перезаписывается: пока pending существует,
 *    новых решений нет, а более высокий milestone остаётся непройденным
 *    и подхватывается при следующем создании записи после показа pending.
 * 3. Иначе планируется наивысший достигнутый (счётчик >= порог), ещё не
 *    пройденный milestone; после всех порогов решений больше нет.
 */
class ReviewMilestoneUseCase {
    /**
     * Решение по авто-запросу оценки для текущего состояния счётчиков.
     */
    sealed class Decision {
        /**
         * Запланировать авто-запрос для milestone [milestone].
         */
        data class ScheduleMilestone(
            val milestone: Int
        ) : Decision()

        /**
         * Авто-запрос не планируется.
         */
        data object DoNotRequest : Decision()
    }

    /**
     * Иммутабельный снимок состояния счётчиков на момент решения.
     *
     * @property createdItemsCount Кумулятивный счётчик успешно созданных записей
     * (редактирование и импорт бэкапа его не увеличивают — инкремент делает хук
     * создания записи в presentation-слое)
     * @property passedMilestones Milestone-ы, для которых авто-запрос уже
     * запускался (помечаются пройденным в момент запуска SDK-флоу, независимо
     * от того, была ли фактически показана форма)
     * @property pendingMilestone Запланированный, но ещё не показанный milestone
     * @property isAutoRequestAlreadyInitiatedThisSession Флаг «авто-запрос уже
     * выполнялся в этой сессии приложения»; in-memory состояние presentation-слоя
     * (holder сессии), не персистится
     */
    data class State(
        val createdItemsCount: Int,
        val passedMilestones: Set<Int>,
        val pendingMilestone: Int?,
        val isAutoRequestAlreadyInitiatedThisSession: Boolean
    )

    /**
     * Принимает решение по авто-запросу оценки для снимка состояния [state].
     */
    operator fun invoke(state: State): Decision {
        // Лимит сессии (авто-запрос уже выполнялся) и непоказанный pending
        // (не перезаписывается) блокируют любое новое решение
        if (state.isAutoRequestAlreadyInitiatedThisSession || state.pendingMilestone != null) {
            return Decision.DoNotRequest
        }
        // Наивысший достигнутый, ещё не пройденный milestone (пороги по возрастанию)
        val highestUnpassedReached =
            MILESTONE_THRESHOLDS.lastOrNull { threshold ->
                state.createdItemsCount >= threshold && threshold !in state.passedMilestones
            }
        return if (highestUnpassedReached != null) {
            Decision.ScheduleMilestone(highestUnpassedReached)
        } else {
            Decision.DoNotRequest
        }
    }

    private companion object {
        /**
         * Пороги milestone-ов по счётчику успешно созданных записей (по возрастанию).
         */
        val MILESTONE_THRESHOLDS = listOf(1, 5, 15)
    }
}
