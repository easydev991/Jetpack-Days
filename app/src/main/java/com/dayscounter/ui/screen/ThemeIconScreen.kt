package com.dayscounter.ui.screen

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.domain.model.AppIcon
import com.dayscounter.domain.model.AppTheme
import com.dayscounter.ui.screen.components.common.DaysRadioButton
import com.dayscounter.ui.screen.themeicon.IconPreviewItem
import com.dayscounter.ui.theme.DynamicColors
import com.dayscounter.viewmodel.ThemeIconViewModel

/**
 * Экран для выбора темы и иконки приложения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeIconScreen(
    viewModel: ThemeIconViewModel,
    onBackClick: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    ThemeIconScreenContent(
        theme = uiState.theme,
        useDynamicColors = uiState.useDynamicColors,
        icon = uiState.icon,
        onThemeChange = { viewModel.updateTheme(it) },
        onDynamicColorsChange = { viewModel.updateDynamicColors(it) },
        onIconChange = { viewModel.updateIcon(it) },
        onBackClick = onBackClick,
    )
}

/**
 * Контент экрана для выбора темы и иконки приложения.
 *
 * Эта функция используется для UI-тестов в изоляции без ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeIconScreenContent(
    theme: AppTheme = AppTheme.SYSTEM,
    useDynamicColors: Boolean = true,
    icon: AppIcon = AppIcon.DEFAULT,
    onThemeChange: (AppTheme) -> Unit = {},
    onDynamicColorsChange: (Boolean) -> Unit = {},
    onIconChange: (AppIcon) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_theme_and_icon)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ThemeSection(
                theme = theme,
                onThemeChange = onThemeChange,
            )

            HorizontalDivider()

            DynamicColorsSection(
                useDynamicColors = useDynamicColors,
                onDynamicColorsChange = onDynamicColorsChange,
            )

            HorizontalDivider()

            IconSection(
                theme = theme,
                icon = icon,
                onIconChange = onIconChange,
            )
        }
    }
}

/**
 * Секция выбора темы приложения.
 */
@Composable
private fun ThemeSection(
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
) {
    Text(
        text = stringResource(R.string.app_theme),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        modifier = Modifier.fillMaxWidth(),
    )

    Column(modifier = Modifier.selectableGroup()) {
        // Светлая тема
        DaysRadioButton(
            text = stringResource(R.string.light),
            selected = theme == AppTheme.LIGHT,
            onClick = { onThemeChange(AppTheme.LIGHT) },
            onClickable = theme != AppTheme.LIGHT,
        )

        // Тёмная тема
        DaysRadioButton(
            text = stringResource(R.string.dark),
            selected = theme == AppTheme.DARK,
            onClick = { onThemeChange(AppTheme.DARK) },
            onClickable = theme != AppTheme.DARK,
        )

        // Системная тема
        DaysRadioButton(
            text = stringResource(R.string.system),
            selected = theme == AppTheme.SYSTEM,
            onClick = { onThemeChange(AppTheme.SYSTEM) },
            onClickable = theme != AppTheme.SYSTEM,
        )
    }
}

/**
 * Секция выбора динамических цветов.
 */
@Composable
private fun DynamicColorsSection(
    useDynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
) {
    // Секция отображается только если доступны динамические цвета (Android 12+)
    val dynamicColorAvailable = DynamicColors.isDynamicColorAvailable()

    if (dynamicColorAvailable) {
        Text(
            text = stringResource(R.string.dynamic_colors),
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth(),
        )

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.use_dynamic_colors),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = useDynamicColors,
                    onCheckedChange = onDynamicColorsChange,
                )
            }
            Text(
                text = stringResource(R.string.dynamic_colors_hint),
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * Секция выбора иконки приложения.
 */
@Composable
private fun IconSection(
    theme: AppTheme,
    icon: AppIcon,
    onIconChange: (AppIcon) -> Unit,
) {
    val isDarkTheme =
        when (theme) {
            AppTheme.SYSTEM -> isSystemInDarkTheme()
            AppTheme.DARK -> true
            AppTheme.LIGHT -> false
        }

    Text(
        text = stringResource(R.string.app_icon),
        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = dimensionResource(R.dimen.spacing_small)),
    )

    IconGrid(
        icons = AppIcon.entries,
        selectedIcon = icon,
        isDarkTheme = isDarkTheme,
        onIconClick = onIconChange,
    )
}

/**
 * Сетка иконок приложения с адаптивным количеством колонок.
 * При повороте экрана сетка автоматически перестраивается для оптимального размещения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("MagicNumber")
@Composable
private fun IconGrid(
    icons: List<AppIcon>,
    selectedIcon: AppIcon,
    isDarkTheme: Boolean,
    onIconClick: (AppIcon) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        icons.forEach { appIcon ->
            Box(
                modifier = Modifier.width(64.dp),
            ) {
                IconPreviewItem(
                    appIcon = appIcon,
                    isSelected = appIcon == selectedIcon,
                    isDarkTheme = isDarkTheme,
                    onClick = { onIconClick(appIcon) },
                )
            }
        }
    }
}
