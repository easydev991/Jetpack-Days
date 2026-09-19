package com.dayscounter.reminder

import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.ChecksSdkIntAtLeast
import com.dayscounter.domain.model.ExactAlarmPermissionState

/**
 * Абстракция над Android API для работы с permission [android.Manifest.permission.SCHEDULE_EXACT_ALARM].
 *
 * Используется [androidx.lifecycle.ViewModel] баннера на экране `CreateEditScreen` —
 * позволяет тестировать ViewModel без Android-зависимостей.
 *
 * Co-location с [AlarmReminderScheduler] — оба оборачивают один и тот же [AlarmManager].
 */
interface ExactAlarmPermissionHelper {
    /**
     * Текущее состояние permission.
     *
     * Дешёвый вызов: внутри — только проверка `AlarmManager.canScheduleExactAlarms()`
     * без IPC, без блокирующих операций.
     */
    fun getState(): ExactAlarmPermissionState

    /**
     * Открывает системный экран SCHEDULE_EXACT_ALARM.
     *
     * @throws ActivityNotFoundException если intent не может быть запущен
     * (например, на прошивках с переопределённым Settings — MIUI/HyperOS,
     * или на устройствах, где экран SCHEDULE_EXACT_ALARM отсутствует).
     */
    fun requestSettings()
}

/**
 * Реализация [ExactAlarmPermissionHelper] через системные API.
 *
 * @param context Application context — helper переживает смену Activity.
 * @param sdkInt Текущий SDK_INT. Параметр с дефолтом `Build.VERSION.SDK_INT`
 *   для тестируемости (в unit-тестах подставляется нужное значение).
 */
class AndroidExactAlarmPermissionHelper(
    private val context: Context,
    private val sdkInt: Int = Build.VERSION.SDK_INT
) : ExactAlarmPermissionHelper {
    // Линт не доказывает поток от инжектируемого sdkInt к API-31 вызову —
    // аннотированный предикат делает гвард статически видимым для него.
    @ChecksSdkIntAtLeast(parameter = 0, api = Build.VERSION_CODES.S)
    private fun atLeastS(sdk: Int) = sdk >= Build.VERSION_CODES.S

    override fun getState(): ExactAlarmPermissionState {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        // На API < 31 permission отсутствует в системе и считается granted.
        return when {
            !atLeastS(sdkInt) -> ExactAlarmPermissionState.Allowed
            alarmManager == null -> ExactAlarmPermissionState.Denied(canRequest = false)
            alarmManager.canScheduleExactAlarms() -> ExactAlarmPermissionState.Allowed
            else -> ExactAlarmPermissionState.Denied(canRequest = true)
        }
    }

    override fun requestSettings() {
        if (!atLeastS(sdkInt)) {
            // Ponytail: на pre-31 экрана нет — баннер для этих устройств не показывается,
            // но helper публичный, защищаемся на случай вызова.
            throw ActivityNotFoundException("SCHEDULE_EXACT_ALARM requires API 31+")
        }
        val intent =
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        // Бросает ActivityNotFoundException на кастомных прошивках
        // без экрана SCHEDULE_EXACT_ALARM — ловится на стороне ViewModel.
        context.startActivity(intent)
    }
}
