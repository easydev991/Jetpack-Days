package com.dayscounter.domain.usecase

import com.dayscounter.crash.CrashlyticsHelper
import com.dayscounter.domain.model.DaysDifference
import com.dayscounter.domain.model.TimePeriod
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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
        currentDate: LocalDate = LocalDate.now(),
    ): DaysDifference {
        return try {
            // Конвертируем timestamp в LocalDate
            val eventDate =
                LocalDateTime
                    .ofInstant(
                        Instant.ofEpochMilli(eventTimestamp),
                        ZoneId.systemDefault(),
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
                timestamp = eventTimestamp,
            )
        } catch (e: Exception) {
            // Логируем критическую ошибку в Crashlytics
            CrashlyticsHelper.logException(
                e,
                "Ошибка при вычислении разницы дат: $eventTimestamp - $currentDate",
            )
            // Возвращаем Today как fallback
            DaysDifference.Today(timestamp = eventTimestamp)
        }
    }

    /**
     * Вычисляет период времени между двумя датами.
     *
     * Алгоритм:
     * 1. Определяем количество полных лет
     * 2. Определяем количество полных месяцев
     * 3. Определяем оставшиеся дни
     *
     * @param startDate Начальная дата
     * @param endDate Конечная дата
     * @return [TimePeriod] с годами, месяцами и днями
     */
    private fun calculateTimePeriod(
        startDate: LocalDate,
        endDate: LocalDate,
    ): TimePeriod {
        var years = 0
        var months = 0
        var days: Int

        // Вычисляем полные годы
        var tempDate = startDate
        while (true) {
            val nextYearDate = tempDate.plusYears(1)
            if (!nextYearDate.isAfter(endDate)) {
                tempDate = nextYearDate
                years++
            } else {
                break
            }
        }

        // Вычисляем полные месяцы
        while (true) {
            val nextMonthDate = tempDate.plusMonths(1)
            if (!nextMonthDate.isAfter(endDate)) {
                tempDate = nextMonthDate
                months++
            } else {
                break
            }
        }

        // Вычисляем оставшиеся дни
        days = ChronoUnit.DAYS.between(tempDate, endDate).toInt()

        return TimePeriod(
            years = years,
            months = months,
            days = days,
        )
    }
}
