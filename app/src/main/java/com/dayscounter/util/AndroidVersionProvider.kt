package com.dayscounter.util

/**
 * Интерфейс для получения версии Android.
 * Используется в IconManager для мокирования версии в тестах.
 */
interface AndroidVersionProvider {
    /**
     * Возвращает текущую версию Android SDK.
     *
     * @return Версия Android SDK
     */
    fun getSdkInt(): Int
}

/**
 * Реализация AndroidVersionProvider для продакшена.
 * Возвращает реальную версию Android SDK.
 */
class RealAndroidVersionProvider : AndroidVersionProvider {
    override fun getSdkInt(): Int = android.os.Build.VERSION.SDK_INT
}
