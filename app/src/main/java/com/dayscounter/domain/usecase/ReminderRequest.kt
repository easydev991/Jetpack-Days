package com.dayscounter.domain.usecase

import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import java.time.LocalDate
import java.time.LocalTime

/**
 * Параметры для построения напоминания перед сохранением.
 */
data class ReminderRequest(
    val itemId: Long,
    val mode: ReminderMode,
    val atDate: LocalDate? = null,
    val atTime: LocalTime? = null,
    val afterAmount: Int? = null,
    val afterUnit: ReminderIntervalUnit? = null
)
