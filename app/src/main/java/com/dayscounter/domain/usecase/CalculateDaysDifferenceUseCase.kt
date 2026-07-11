package com.dayscounter.domain.usecase

import com.dayscounter.crash.CrashlyticsHelper
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.TimePeriod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Suppress("TooGenericExceptionCaught")
class CalculateDaysDifferenceUseCase {
    /**
     * Вычисляет разницу между датами.
     *
     * @param eventTimestamp Временная метка события в миллисекундах
     * @param currentDate Текущая дата (по умолчанию используется системная)
     * @return [DaysDifference] с результатом вычисления
     */
    operator fun invoke(
        eventTimestamp: Long,
        currentDate: LocalDate = LocalDate.now()
    ): DaysDifference {
        return try {
            // Конвертируем timestamp в LocalDate
            val eventDate =
                LocalDateTime
                    .ofInstant(
                        Instant.ofEpochMilli(eventTimestamp),
                        ZoneId.systemDefault()
                    ).toLocalDate()

            // Вычисляем разницу в днях
            val totalDays = ChronoUnit.DAYS.between(eventDate, currentDate).toInt()

            // Если разница равна 0 дней — сегодня
            if (totalDays == 0) {
                return DaysDifference.Today(timestamp = eventTimestamp)
            }

            // Вычисляем период (годы, месяцы, дни)
            val period = calculateTimePeriod(eventDate, currentDate)

            DaysDifference.Calculated(
                period = period,
                totalDays = totalDays,
                timestamp = eventTimestamp
            )
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при вычислении разницы дат: $eventTimestamp - $currentDate"
            )
            // Возвращаем Today как fallback
            DaysDifference.Today(timestamp = eventTimestamp)
        }
    }

    private fun calculateTimePeriod(
        startDate: LocalDate,
        endDate: LocalDate
    ): TimePeriod {
        val period = Period.between(startDate, endDate)
        return TimePeriod(
            years = period.years,
            months = period.months,
            days = period.days
        )
    }
}
