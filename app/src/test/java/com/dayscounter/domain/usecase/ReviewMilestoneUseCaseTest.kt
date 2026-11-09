package com.dayscounter.domain.usecase

import com.dayscounter.domain.usecase.ReviewMilestoneUseCase.Decision.DoNotRequest
import com.dayscounter.domain.usecase.ReviewMilestoneUseCase.Decision.ScheduleMilestone
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Unit-тесты для [ReviewMilestoneUseCase].
 *
 * Проверяют milestone-логику авто-запроса оценки: пороги [1, 5, 15], выбор
 * наивысшего достигнутого непройденного milestone-а, не более одного
 * авто-запроса за сессию, семантику непоказанного pending, чистоту use case,
 * исчерпание порогов и сброс счётчиков (openspec: add-rustore-in-app-review,
 * задачи 2.1/2.2, design.md D2).
 */
class ReviewMilestoneUseCaseTest {
    private val useCase = ReviewMilestoneUseCase()

    @Test
    fun invoke_when_first_item_created_then_schedules_milestone_one() {
        // Given
        val state = state(createdItemsCount = 1)

        // When
        val decision = useCase(state)

        // Then
        assertEquals(ScheduleMilestone(1), decision, "Первая созданная запись должна планировать milestone 1")
    }

    @Test
    fun invoke_when_counter_reaches_five_then_schedules_milestone_five() {
        // Given: milestone 1 уже показан, счётчик дошёл до 5
        val state = state(createdItemsCount = 5, passedMilestones = setOf(1))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(ScheduleMilestone(5), decision, "Порог 5 должен планировать milestone 5")
    }

    @Test
    fun invoke_when_counter_reaches_fifteen_then_schedules_milestone_fifteen() {
        // Given: milestones 1 и 5 уже показаны, счётчик дошёл до 15
        val state = state(createdItemsCount = 15, passedMilestones = setOf(1, 5))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(ScheduleMilestone(15), decision, "Порог 15 должен планировать milestone 15")
    }

    @Test
    fun invoke_when_several_thresholds_reached_then_picks_highest_unpassed() {
        // Given: milestone 5 остался непройденным (например, блокировался лимитом сессии),
        // а счётчик уже дошёл до 15
        val state = state(createdItemsCount = 15, passedMilestones = setOf(1))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(
            ScheduleMilestone(15),
            decision,
            "Из достигнутых порогов выбирается наивысший непройденный milestone"
        )
    }

    @Test
    fun invoke_when_auto_request_already_initiated_this_session_then_does_not_request() {
        // Given: в этой сессии авто-запрос для milestone 1 уже запускался, счётчик дошёл до 5
        val state =
            state(
                createdItemsCount = 5,
                passedMilestones = setOf(1),
                isAutoRequestAlreadyInitiatedThisSession = true
            )

        // When
        val decision = useCase(state)

        // Then
        assertEquals(DoNotRequest, decision, "За сессию выполняется не более одного авто-запроса")
    }

    @Test
    fun invoke_when_higher_milestone_reached_while_pending_not_shown_then_does_not_request() {
        // Given: pending=1 запланирован, но ещё не показан, а счётчик уже дошёл до 5
        val state = state(createdItemsCount = 5, pendingMilestone = 1)

        // When
        val decision = useCase(state)

        // Then
        assertEquals(
            DoNotRequest,
            decision,
            "Непоказанный pending не перезаписывается — нового решения быть не должно"
        )
    }

    @Test
    fun invoke_when_pending_shown_and_counter_already_past_next_threshold_then_schedules_it() {
        // Given: pending=1 показан (milestone 1 пройден), за время показа счётчик дошёл
        // до 7 — milestone 5 остался непройденным
        val state = state(createdItemsCount = 7, passedMilestones = setOf(1))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(
            ScheduleMilestone(5),
            decision,
            "Промежуточный milestone подхватывается при следующем создании записи после показа pending"
        )
    }

    @Test
    fun invoke_when_attempted_milestone_already_passed_then_does_not_repeat() {
        // Given: milestone 1 запускался ранее и помечен пройденным (форма могла не
        // показаться из-за лимита на стороне RuStore), сессия новая
        val state = state(createdItemsCount = 1, passedMilestones = setOf(1))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(DoNotRequest, decision, "Попробованный milestone не повторяется")
    }

    @Test
    fun invoke_when_counter_reached_passed_thresholds_then_no_new_decision() {
        // Given: счётчик дошёл до уже пройденного порога (3 ≥ 1, milestone 1 пройден)
        val state = state(createdItemsCount = 3, passedMilestones = setOf(1))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(
            DoNotRequest,
            decision,
            "Достигнутые и уже пройденные пороги нового решения не планируют"
        )
    }

    @Test
    fun invoke_when_called_repeatedly_with_same_state_then_decision_is_deterministic() {
        // Given
        val state = state(createdItemsCount = 5, passedMilestones = setOf(1))

        // When
        val first = useCase(state)
        val second = useCase(state)

        // Then
        assertEquals(first, second, "Use case чистый и stateless — повторный вызов даёт то же решение")
        assertEquals(
            ScheduleMilestone(5),
            first,
            "Решение для состояния «счётчик 5, milestone 1 пройден» — milestone 5"
        )
    }

    @Test
    fun invoke_when_all_milestones_passed_then_does_not_request() {
        // Given: все три порога пройдены, счётчик продолжает расти
        val state = state(createdItemsCount = 20, passedMilestones = setOf(1, 5, 15))

        // When
        val decision = useCase(state)

        // Then
        assertEquals(DoNotRequest, decision, "После всех порогов авто-запросы больше не планируются")
    }

    @Test
    fun invoke_when_counters_reset_then_next_created_item_schedules_milestone_one_again() {
        // Given: состояние после «Удалить все данные» — счётчик 0, пройденных нет, pending нет
        val resetState = state(createdItemsCount = 0)

        // When
        val decisionBeforeNewItems = useCase(resetState)

        // Then: пока новых записей нет, решений нет
        assertEquals(DoNotRequest, decisionBeforeNewItems, "Без новых записей после сброса решений нет")

        // When: после сброса создана первая запись
        val decisionAfterFirstItem = useCase(state(createdItemsCount = 1))

        // Then
        assertEquals(
            ScheduleMilestone(1),
            decisionAfterFirstItem,
            "Сброс должен вернуть авто-запросы к milestone 1"
        )
    }

    @Test
    fun invoke_when_auto_request_already_initiated_and_counters_reset_then_does_not_request() {
        // Given: счётчики сброшены, но авто-запрос в этой сессии уже выполнялся —
        // лимит сессии не зависит от сброса хранилища
        val state =
            state(
                createdItemsCount = 1,
                isAutoRequestAlreadyInitiatedThisSession = true
            )

        // When
        val decision = useCase(state)

        // Then
        assertEquals(
            DoNotRequest,
            decision,
            "Сброс счётчиков не снимает лимит «не более одного авто-запроса за сессию»"
        )
    }

    /**
     * Строит снимок состояния счётчиков с дефолтами «чистой установки»:
     * пройденных milestone-ов нет, pending нет, авто-запрос в сессии не выполнялся.
     */
    private fun state(
        createdItemsCount: Int,
        passedMilestones: Set<Int> = emptySet(),
        pendingMilestone: Int? = null,
        isAutoRequestAlreadyInitiatedThisSession: Boolean = false
    ): ReviewMilestoneUseCase.State =
        ReviewMilestoneUseCase.State(
            createdItemsCount = createdItemsCount,
            passedMilestones = passedMilestones,
            pendingMilestone = pendingMilestone,
            isAutoRequestAlreadyInitiatedThisSession = isAutoRequestAlreadyInitiatedThisSession
        )
}
