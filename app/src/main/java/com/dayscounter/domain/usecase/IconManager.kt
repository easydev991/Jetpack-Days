package com.dayscounter.domain.usecase

import android.content.Context
import android.content.pm.PackageManager
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger

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
     * Изменяет иконку приложения.
     *
     * Метод активирует Activity Alias выбранной иконки и деактивирует остальные.
     * Использует PackageManager.setComponentEnabledSetting() для управления видимостью aliases.
     *
     * @param icon Иконка, которую нужно активировать
     */
    @Suppress("TooManyFunctions", "ThrowsCount")
    fun changeIcon(icon: AppIcon) {
        val packageName = context.packageName
        val allIcons = AppIcon.entries

        // Активируем выбранную иконку и деактивируем остальные
        allIcons.forEach { currentIcon ->
            val isEnabled = currentIcon == icon
            val componentName = getIconComponentName(currentIcon, packageName)
            val newState =
                if (isEnabled) {
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                } else {
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                }

            try {
                context.packageManager.setComponentEnabledSetting(
                    componentName,
                    newState,
                    PackageManager.DONT_KILL_APP,
                )
                logger.d(
                    TAG,
                    "Иконка ${currentIcon.name} установлена в состояние ${if (isEnabled) "ENABLED" else "DISABLED"}",
                )
            } catch (e: SecurityException) {
                logger.e(
                    TAG,
                    "Ошибка при смене иконки: нет прав для изменения состояния компонента",
                    e,
                )
                throw e
            } catch (e: PackageManager.NameNotFoundException) {
                logger.e(TAG, "Компонент не найден при смене иконки", e)
                throw e
            } catch (e: IllegalArgumentException) {
                logger.e(TAG, "Неверный аргумент при смене иконки", e)
                throw e
            }
        }

        logger.d(TAG, "Иконка изменена на ${icon.name}")
    }
}

/**
 * Формирует ComponentName для Activity Alias иконки.
 *
 * @param icon Иконка
 * @param packageName Имя пакета приложения
 * @return ComponentName для Activity Alias иконки
 */
private fun getIconComponentName(
    icon: AppIcon,
    packageName: String,
): android.content.ComponentName {
    val className =
        when (icon) {
            AppIcon.DEFAULT -> "com.dayscounter.MainActivityAliasIcon1"
            AppIcon.ICON_2 -> "com.dayscounter.MainActivityIcon2"
            AppIcon.ICON_3 -> "com.dayscounter.MainActivityIcon3"
            AppIcon.ICON_4 -> "com.dayscounter.MainActivityIcon4"
            AppIcon.ICON_5 -> "com.dayscounter.MainActivityIcon5"
            AppIcon.ICON_6 -> "com.dayscounter.MainActivityIcon6"
        }

    return android.content.ComponentName(packageName, className)
}
