package com.dayscounter.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore для хранения настроек приложения (тема и иконка).
 *
 * Настройки сохраняются постоянно, чтобы при каждом запуске приложения
 * выбор пользователя не сбрасывался.
 */
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * Ключи для хранения настроек в DataStore.
 */
private object PreferencesKeys {
    val THEME_KEY = stringPreferencesKey("theme")
    val ICON_KEY = stringPreferencesKey("icon")
}

/**
 * Реализация DataStore для настроек приложения.
 */
class AppSettingsDataStore(
    private val context: Context,
) {
    /**
     * Сохраняет выбранную тему приложения.
     *
     * @param theme Выбранная тема
     */
    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_KEY] = theme.name
        }
    }

    /**
     * Сохраняет выбранную иконку приложения.
     *
     * @param icon Выбранная иконка
     */
    suspend fun setIcon(icon: AppIcon) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ICON_KEY] = icon.name
        }
    }

    /**
     * Flow для отслеживания выбранной темы.
     * При первом запуске возвращает значение по умолчанию (SYSTEM).
     */
    val theme: Flow<AppTheme> =
        context.dataStore.data
            .map { preferences ->
                @Suppress("SwallowedException")
                preferences[PreferencesKeys.THEME_KEY]?.let { themeName ->
                    try {
                        AppTheme.valueOf(themeName)
                    } catch (e: IllegalArgumentException) {
                        AppTheme.SYSTEM
                    }
                } ?: AppTheme.SYSTEM
            }

    /**
     * Flow для отслеживания выбранной иконки.
     * При первом запуске возвращает значение по умолчанию (DEFAULT).
     */
    val icon: Flow<AppIcon> =
        context.dataStore.data
            .map { preferences ->
                @Suppress("SwallowedException")
                preferences[PreferencesKeys.ICON_KEY]?.let { iconName ->
                    try {
                        AppIcon.valueOf(iconName)
                    } catch (e: IllegalArgumentException) {
                        AppIcon.DEFAULT
                    }
                } ?: AppIcon.DEFAULT
            }
}

/**
 * Factory метод для создания AppSettingsDataStore.
 * Используется для ручного DI вместо Hilt.
 *
 * @param context Контекст приложения
 * @return Экземпляр AppSettingsDataStore
 */
fun createAppSettingsDataStore(context: Context): AppSettingsDataStore =
    AppSettingsDataStore(
        context.applicationContext,
    )
