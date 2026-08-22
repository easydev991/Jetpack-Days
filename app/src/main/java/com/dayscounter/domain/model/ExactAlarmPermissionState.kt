package com.dayscounter.domain.model

/**
 * Состояние permission [android.Manifest.permission.SCHEDULE_EXACT_ALARM].
 *
 * Используется для отображения баннера на экране настройки reminder и для
 * принятия решения, какой fallback применить в `AlarmReminderScheduler`.
 *
 * На API ≤ 30 permission отсутствует и всегда granted — возвращается [Allowed].
 * На API ≥ 31 permission есть в манифесте, но пользователь может его отозвать
 * через системные настройки, поэтому состояние зависит от runtime-проверки
 * `AlarmManager.canScheduleExactAlarms()`.
 *
 * Альтернатива [android.Manifest.permission.USE_EXACT_ALARM] (API 33+) не
 * используется: Play Console может отклонить приложение, не относящееся к
 * категории будильников/календарей. Reminder — не тот кейс.
 */
sealed class ExactAlarmPermissionState {
    /**
     * Разрешение есть: API ≤ 30 (где permission отсутствует) или
     * API ≥ 31 с подтверждённым `SCHEDULE_EXACT_ALARM`.
     */
    data object Allowed : ExactAlarmPermissionState()

    /**
     * Разрешения нет (API ≥ 31 без `SCHEDULE_EXACT_ALARM`).
     *
     * [canRequest] указывает, можно ли открыть системный диалог для запроса:
     * `true` на API 31+ (штатный путь через `ACTION_REQUEST_SCHEDULE_EXACT_ALARM`),
     * `false` на устройствах, где permission технически отсутствует в системе
     * (кастомные прошивки) или был окончательно отозван.
     */
    data class Denied(
        val canRequest: Boolean
    ) : ExactAlarmPermissionState()
}
