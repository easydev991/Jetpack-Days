package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import java.time.Clock
import java.time.ZonedDateTime

/**
 * Строит валидную доменную модель напоминания из параметров формы.
 */
class BuildReminderUseCase(
    private val clock: Clock = Clock.systemDefaultZone()
) {
    operator fun invoke(request: ReminderRequest): Result<Reminder> =
        runCatching {
            val nowDateTime = ZonedDateTime.now(clock)
            val nowMillis = nowDateTime.toInstant().toEpochMilli()

            when (request.mode) {
                ReminderMode.AT_DATE -> buildAtDateReminder(request, nowMillis)
                ReminderMode.AFTER_INTERVAL -> buildAfterIntervalReminder(request, nowDateTime, nowMillis)
            }
        }

    private fun buildAtDateReminder(
        request: ReminderRequest,
        nowMillis: Long
    ): Reminder {
        val date = requireNotNull(request.atDate) { "Reminder date is required" }
        val time = requireNotNull(request.atTime) { "Reminder time is required" }

        val targetMillis =
            date
                .atTime(time)
                .atZone(clock.zone)
                .toInstant()
                .toEpochMilli()

        require(targetMillis > nowMillis) { "Reminder target should be in the future" }

        val selectedDateEpochMillis =
            date
                .atStartOfDay(clock.zone)
                .toInstant()
                .toEpochMilli()

        return Reminder(
            itemId = request.itemId,
            mode = ReminderMode.AT_DATE,
            targetEpochMillis = targetMillis,
            selectedDateEpochMillis = selectedDateEpochMillis,
            selectedHour = time.hour,
            selectedMinute = time.minute,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )
    }

    private fun buildAfterIntervalReminder(
        request: ReminderRequest,
        nowDateTime: ZonedDateTime,
        nowMillis: Long
    ): Reminder {
        val amount = requireNotNull(request.afterAmount) { "Reminder amount is required" }
        require(amount >= 1) { "Reminder amount should be positive" }

        val unit = requireNotNull(request.afterUnit) { "Reminder interval unit is required" }

        val targetDateTime =
            when (unit) {
                ReminderIntervalUnit.DAY -> nowDateTime.plusDays(amount.toLong())
                ReminderIntervalUnit.WEEK -> nowDateTime.plusWeeks(amount.toLong())
                ReminderIntervalUnit.MONTH -> nowDateTime.plusMonths(amount.toLong())
                ReminderIntervalUnit.YEAR -> nowDateTime.plusYears(amount.toLong())
            }

        return Reminder(
            itemId = request.itemId,
            mode = ReminderMode.AFTER_INTERVAL,
            targetEpochMillis = targetDateTime.toInstant().toEpochMilli(),
            intervalAmount = amount,
            intervalUnit = unit,
            createdAt = nowMillis,
            updatedAt = nowMillis
        )
    }
}
