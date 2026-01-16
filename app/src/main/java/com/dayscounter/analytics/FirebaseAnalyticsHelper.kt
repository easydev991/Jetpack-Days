package com.dayscounter.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Помощник для работы с Firebase Analytics.
 *
 * Используется для логирования screen_view событий, которые автоматически становятся
 * breadcrumb logs в Crashlytics.
 * Это позволяет увидеть последовательность экранов, которые пользователь просматривал перед крашем.
 */
object FirebaseAnalyticsHelper {
    private const val TAG = "FirebaseAnalytics"

    /**
     * Логирует screen_view событие.
     *
     * Это событие автоматически станет breadcrumb log в Crashlytics, показывая,
     * какой экран просматривал пользователь перед крашем.
     *
     * @param context Контекст приложения
     * @param screenName Название экрана (например, "EventsScreen", "DetailScreen")
     * @param screenClass Полное имя класса экрана (опционально)
     */
    @Suppress("TooGenericExceptionCaught")
    fun logScreenView(
        context: Context,
        screenName: String,
        screenClass: String? = null,
    ) {
        try {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            val params =
                Bundle().apply {
                    putString(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
                    putString(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
                }

            firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
            Log.d(TAG, "screen_view логирован: $screenName (${screenClass ?: "unknown class"})")
        } catch (e: Exception) {
            // Безопасное логирование - не ломаем приложение при ошибках Analytics
            Log.e(TAG, "Ошибка при логировании screen_view: ${e.message}", e)
        }
    }

    /**
     * Логирует пользовательское событие.
     *
     * Используйте для логирования важных действий пользователя, которые помогут понять контекст краша.
     *
     * @param context Контекст приложения
     * @param eventName Имя события
     * @param parameters Дополнительные параметры события (опционально)
     */
    @Suppress("TooGenericExceptionCaught")
    fun logEvent(
        context: Context,
        eventName: String,
        parameters: Bundle? = null,
    ) {
        try {
            val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
            firebaseAnalytics.logEvent(eventName, parameters)
            Log.d(TAG, "Событие логировано: $eventName")
        } catch (e: Exception) {
            // Безопасное логирование
            Log.e(TAG, "Ошибка при логировании события '$eventName': ${e.message}", e)
        }
    }
}
