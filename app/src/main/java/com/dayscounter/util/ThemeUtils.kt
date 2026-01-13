package com.dayscounter.util

import android.app.UiModeManager
import android.content.Context

/**
 * Утилита для работы с темой приложения.
 *
 * Предоставляет методы для определения системной темы, которые можно использовать вне Compose
 * (например, в ViewModel).
 */
object ThemeUtils {
    /**
     * Проверяет, включена ли тёмная тема в настройках системы.
     *
     * Использует UiModeManager для определения режима интерфейса. Метод можно использовать в
     * бизнес-логике (ViewModel), а не только в Compose UI.
     *
     * @param context Контекст приложения
     * @return true, если в системе включена тёмная тема
     */
    fun isSystemDarkTheme(context: Context): Boolean {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val nightMode = uiModeManager?.nightMode ?: UiModeManager.MODE_NIGHT_AUTO
        return nightMode == UiModeManager.MODE_NIGHT_YES
    }
}
