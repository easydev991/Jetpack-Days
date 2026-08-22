package com.dayscounter

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.data.database.DaysDatabase
import com.dayscounter.data.preferences.createAppSettingsDataStore
import com.dayscounter.di.AppModule
import com.dayscounter.domain.model.AppTheme
import com.dayscounter.reminder.ReminderIntentContract
import com.dayscounter.reminder.ReminderManager
import com.dayscounter.reminder.extractReminderOpenItemId
import com.dayscounter.ui.screens.root.RootScreen
import com.dayscounter.ui.theme.JetpackDaysTheme
import com.dayscounter.ui.viewmodel.MainActivityViewModel
import kotlinx.coroutines.launch

/**
 * Главная Activity приложения.
 */
class MainActivity : ComponentActivity() {
    private val pendingOpenDetailItemId = mutableStateOf<Long?>(null)

    // ponytail: state-based проверка фикса `if (savedInstanceState == null)` в onCreate —
    // без фикса recreate Activity повторно устанавливает значение из сохранённого push-интента,
    // и мы проверяем через getter, что после recreate остаётся null.
    @get:VisibleForTesting
    internal val openDetailItemId: Long? get() = pendingOpenDetailItemId.value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val dataStore = createAppSettingsDataStore(applicationContext)
        val analyticsService = AppModule.createAnalyticsService(applicationContext)
        val database = DaysDatabase.getDatabase(applicationContext)
        val reminderManager = AppModule.createReminderManager(applicationContext, database)

        // ponytail: handleReminderIntent должен вызываться ровно один раз на доставку пуша —
        // иначе при rotation / recreation Activity Android подсовывает сохранённый intent
        // с EXTRA_ITEM_ID, и обработчик повторно пушит ItemDetail поверх текущего стека,
        // а также повторно дёргает side-effects (consumeReminder + NotificationManagerCompat.cancel).
        // shouldHandleReminderIntent(null) == true покрывает cold-start и process-restart;
        // onNewIntent (без изменений ниже) покрывает пуш при уже запущенном приложении.
        if (shouldHandleReminderIntent(savedInstanceState)) {
            handleReminderIntent(intent, reminderManager)
        }

        setContent {
            val viewModel: MainActivityViewModel =
                viewModel(
                    factory = MainActivityViewModel.factory(dataStore)
                )

            val theme by viewModel.theme.collectAsState()
            val useDynamicColors by viewModel.useDynamicColors.collectAsState()
            val openDetailItemId by pendingOpenDetailItemId

            val isDarkTheme =
                when (theme) {
                    AppTheme.DARK -> true
                    AppTheme.LIGHT -> false
                    AppTheme.SYSTEM -> isSystemInDarkTheme()
                }

            LaunchedEffect(isDarkTheme) {
                this@MainActivity.enableEdgeToEdge(
                    statusBarStyle =
                        if (isDarkTheme) {
                            SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        } else {
                            SystemBarStyle.light(
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT
                            )
                        }
                )
            }

            AppContent(
                theme = theme,
                useDynamicColors = useDynamicColors,
                analyticsService = analyticsService,
                pendingOpenDetailItemId = openDetailItemId,
                onPendingOpenHandled = { pendingOpenDetailItemId.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val database = DaysDatabase.getDatabase(applicationContext)
        val reminderManager = AppModule.createReminderManager(applicationContext, database)
        handleReminderIntent(intent, reminderManager)
    }

    private fun handleReminderIntent(
        intent: Intent?,
        reminderManager: ReminderManager
    ) {
        val itemId = intent.extractReminderOpenItemId() ?: return

        pendingOpenDetailItemId.value = itemId

        lifecycleScope.launch {
            reminderManager.consumeReminder(itemId)
            NotificationManagerCompat
                .from(this@MainActivity)
                .cancel(ReminderIntentContract.notificationIdForItem(itemId))
        }
    }

    companion object {
        /**
         * Гейт обработки пуш-интента в [onCreate].
         *
         * Возвращает `true`, если нужно обработать reminder-intent при создании Activity:
         * - `savedInstanceState == null` — cold-start (новый процесс) или
         *   process-restart (Android убил процесс, пользователь вернулся по savedInstance).
         *
         * Возвращает `false`, если Activity пересоздана системой при rotation / theme change /
         * возврате из фона после смерти процесса — Android восстанавливает прежний intent из
         * ActivityRecord, но обрабатывать его повторно нельзя: иначе `pendingOpenDetailItemId`
         * перезаписывается и `LaunchedEffect` пушит ещё одну копию `ItemDetail` поверх стека.
         *
         * @see onCreate
         */
        @VisibleForTesting
        internal fun shouldHandleReminderIntent(savedInstanceState: Bundle?): Boolean = savedInstanceState == null
    }
}

/**
 * Основной контент Activity с применённой темой.
 */
@Composable
private fun AppContent(
    theme: com.dayscounter.domain.model.AppTheme,
    useDynamicColors: Boolean,
    analyticsService: AnalyticsService,
    pendingOpenDetailItemId: Long?,
    onPendingOpenHandled: () -> Unit
) {
    JetpackDaysTheme(
        appTheme = theme,
        dynamicColor = useDynamicColors
    ) {
        Surface(
            modifier =
                androidx.compose.ui.Modifier
                    .fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            RootScreen(
                analyticsService = analyticsService,
                pendingOpenDetailItemId = pendingOpenDetailItemId,
                onPendingOpenHandled = onPendingOpenHandled
            )
        }
    }
}
