package com.dayscounter.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import com.dayscounter.util.ThemeUtils

/**
 * Use Case для управления сменой иконки приложения.
 *
 * Смена иконки реализуется через Activity Aliases и PackageManager API.
 * Поддерживается на Android 8.0 (API 26) и выше.
 *
 * Примечание: Минимальная версия приложения - Android 8.0 (API 26),
 * поэтому проверки версии не требуются.
 *
 * @property context Контекст приложения
 * @property logger Logger для логирования (по умолчанию AndroidLogger)
 */
class IconManager(
    private val context: Context,
    private val logger: Logger = AndroidLogger(),
) {
    companion object {
        private const val TAG = "IconManager"
    }

    /**
     * Проверяет, включена ли тёмная тема для выбора иконки.
     *
     * Для системной темы использует фактические настройки системы.
     * Для явного выбора темы возвращает соответствующее значение.
     *
     * @param theme Выбранная тема приложения
     * @return true, если для иконки должна использоваться тёмная версия
     */
    fun isDarkThemeForIcon(theme: com.dayscounter.domain.model.AppTheme): Boolean =
        when (theme) {
            com.dayscounter.domain.model.AppTheme.DARK -> true
            com.dayscounter.domain.model.AppTheme.LIGHT -> false
            com.dayscounter.domain.model.AppTheme.SYSTEM -> ThemeUtils.isSystemDarkTheme(context)
        }

    /**
     * Изменяет иконку приложения.
     *
     * Метод активирует Activity Alias выбранной иконки и деактивирует остальные.
     * Использует PackageManager.setComponentEnabledSetting() для управления видимостью aliases.
     * Поддерживает светлую и тёмную тему.
     *
     * @param icon Иконка, которую нужно активировать
     * @param isDarkTheme Признак темной темы
     */
    @Suppress("TooManyFunctions", "ThrowsCount")
    fun changeIcon(
        icon: AppIcon,
        isDarkTheme: Boolean,
    ) {
        val packageName = context.packageName

        // Сначала АКТИВИРУЕМ новый Activity Alias
        val targetComponentName = getIconComponentName(icon, isDarkTheme, packageName)
        val targetComponentClassName = icon.getComponentName(isDarkTheme)
        try {
            context.packageManager.setComponentEnabledSetting(
                targetComponentName,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP,
            )
            val themeSuffix = if (isDarkTheme) " (тёмная тема)" else " (светлая тема)"
            logger.d(TAG, "Иконка $targetComponentClassName АКТИВИРОВАНА$themeSuffix")
        } catch (e: SecurityException) {
            logger.e(TAG, "Ошибка при активации иконки: нет прав", e)
            throw e
        } catch (e: PackageManager.NameNotFoundException) {
            logger.e(TAG, "Компонент не найден при активации иконки", e)
            throw e
        } catch (e: IllegalArgumentException) {
            logger.e(TAG, "Неверный аргумент при активации иконки", e)
            throw e
        }

        // Затем деактивируем ВСЕ остальные Activity Aliases
        val allIcons = AppIcon.entries
        allIcons.forEach { currentIcon ->
            // Пропускаем целевую иконку с текущей темой
            if (currentIcon == icon) {
                // Для деактивации нужно проверить светлая и тёмная версии
                val shouldDisableLightTheme = isDarkTheme
                val shouldDisableDarkTheme = !isDarkTheme

                if (shouldDisableLightTheme) {
                    disableComponent(getIconComponentName(currentIcon, false, packageName))
                }
                if (shouldDisableDarkTheme) {
                    disableComponent(getIconComponentName(currentIcon, true, packageName))
                }
            } else {
                // Для всех остальных иконок деактивируем ОБЕ версии
                disableComponent(getIconComponentName(currentIcon, false, packageName))
                disableComponent(getIconComponentName(currentIcon, true, packageName))
            }
        }
    }

    /**
     * Деактивирует Activity Alias с обработкой ошибок.
     *
     * @param componentName Компонент для деактивации
     */
    private fun disableComponent(componentName: android.content.ComponentName) {
        try {
            // Используем KILL_IF_NEEDED вместо DONT_KILL_APP для более надежной деактивации
            context.packageManager.setComponentEnabledSetting(
                componentName,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP,
            )
            val themeSuffix =
                if (componentName.className.endsWith("Dark")) {
                    " (тёмная тема)"
                } else {
                    " (светлая тема)"
                }
            logger.d(TAG, "Иконка ${componentName.className} ДЕАКТИВИРОВАНА$themeSuffix")
        } catch (e: SecurityException) {
            logger.e(TAG, "Ошибка при деактивации иконки: нет прав", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: PackageManager.NameNotFoundException) {
            logger.e(TAG, "Компонент не найден при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: IllegalArgumentException) {
            logger.e(TAG, "Неверный аргумент при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        } catch (e: Exception) {
            logger.e(TAG, "Неожиданная ошибка при деактивации иконки", e)
            // Не выбрасываем исключение - продолжаем деактивацию остальных
        }
    }
}

/**
 * Формирует ComponentName для Activity Alias иконки.
 *
 * @param icon Иконка
 * @param isDarkTheme Признак темной темы
 * @param packageName Имя пакета приложения
 * @return ComponentName для Activity Alias иконки
 */
private fun getIconComponentName(
    icon: AppIcon,
    isDarkTheme: Boolean,
    packageName: String,
): android.content.ComponentName {
    val className = icon.getComponentName(isDarkTheme)
    return android.content.ComponentName(packageName, className)
}
