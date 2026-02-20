package com.dayscounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.data.preferences.createAppSettingsDataStore
import com.dayscounter.ui.screen.RootScreen
import com.dayscounter.ui.theme.JetpackDaysTheme
import com.dayscounter.viewmodel.MainActivityViewModel

/**
 * Главная Activity приложения.
 *
 * Проект использует ручной подход к внедрению зависимостей через factory методы.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Создаём DataStore для настроек приложения
        val dataStore = createAppSettingsDataStore(applicationContext)

        setContent {
            // Создаём ViewModel для MainActivity
            val viewModel: MainActivityViewModel =
                viewModel(
                    factory = MainActivityViewModel.factory(dataStore),
                )

            // Получаем текущую тему и настройку динамических цветов из ViewModel
            val theme by viewModel.theme.collectAsState()
            val useDynamicColors by viewModel.useDynamicColors.collectAsState()

            // Применяем тему приложения
            AppContent(
                theme = theme,
                useDynamicColors = useDynamicColors,
            )
        }
    }
}

/**
 * Основной контент Activity с применённой темой.
 *
 * @param theme Тема приложения из DataStore
 */
@Composable
private fun AppContent(
    theme: com.dayscounter.domain.model.AppTheme,
    useDynamicColors: Boolean,
) {
    JetpackDaysTheme(
        appTheme = theme,
        dynamicColor = useDynamicColors,
    ) {
        // A surface container using the 'background' color from the theme
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            RootScreen()
        }
    }
}
