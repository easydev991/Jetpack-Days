package com.dayscounter.analytics

import android.content.Context
import android.os.Bundle
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.Logger
import com.google.firebase.analytics.FirebaseAnalytics

/**
 * Провайдер аналитики Firebase.
 *
 * @param context Контекст приложения
 */
class FirebaseAnalyticsProvider(
    context: Context,
    private val logger: Logger = AndroidLogger()
) : AnalyticsProvider {
    private val firebaseAnalytics = FirebaseAnalytics.getInstance(context.applicationContext)

    private companion object {
        private const val TAG = "FirebaseAnalyticsProvider"
        private const val PARAM_OPERATION = "operation"
        private const val PARAM_ERROR_DOMAIN = "error_domain"
        private const val PARAM_ERROR_CODE = "error_code"
        private const val PARAM_ACTION = "action"
        private const val PARAM_ICON_NAME = "icon_name"
        private const val USER_ACTION_EVENT = "user_action"
        private const val APP_ERROR_EVENT = "app_error"
    }

    override fun log(event: AnalyticsEvent) {
        when (event) {
            is AnalyticsEvent.ScreenView -> logScreenView(event)
            is AnalyticsEvent.UserAction -> logUserAction(event)
            is AnalyticsEvent.AppError -> logAppError(event)
        }
    }

    private fun logScreenView(event: AnalyticsEvent.ScreenView) {
        val params =
            Bundle().apply {
                putString(FirebaseAnalytics.Param.SCREEN_NAME, event.screen.screenName)
                event.screenClass?.let { putString(FirebaseAnalytics.Param.SCREEN_CLASS, it) }
            }
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW, params)
        logger.d(TAG, "screen_view: ${event.screen.screenName}")
    }

    private fun logUserAction(event: AnalyticsEvent.UserAction) {
        val params =
            Bundle().apply {
                putString(PARAM_ACTION, event.action.value)
                if (event.action == UserActionType.ICON_SELECTED) {
                    event.iconName?.let { putString(PARAM_ICON_NAME, it) }
                }
            }
        firebaseAnalytics.logEvent(USER_ACTION_EVENT, params)
        logger.d(TAG, "user_action: ${event.action.value}")
    }

    private fun logAppError(event: AnalyticsEvent.AppError) {
        val params =
            Bundle().apply {
                putString(PARAM_OPERATION, event.operation.value)
                putString(PARAM_ERROR_DOMAIN, event.throwable::class.java.name)
                putLong(PARAM_ERROR_CODE, event.throwable.hashCode().toLong())
            }
        firebaseAnalytics.logEvent(APP_ERROR_EVENT, params)
        logger.d(TAG, "app_error: ${event.operation.value}")
    }
}
