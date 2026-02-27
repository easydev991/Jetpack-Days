package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.ui.screens.common.DaysRadioButton
import com.dayscounter.ui.theme.JetpackDaysTheme

/**
 * Предустановленные цвета для селектора.
 */
@Suppress("MagicNumber")
internal object PresetColors {
    val Red = Color(0xFFE53935)
    val Teal = Color(0xFF00897B)
    val Blue = Color(0xFF1E88E5)
    val Green = Color(0xFF43A047)
    val Yellow = Color(0xFFFDD835)
    val Purple = Color(0xFF8E24AA)

    val all: List<Color>
        get() = listOf(Red, Teal, Blue, Green, Yellow, Purple)
}

/**
 * Предустановленные цвета для селектора.
 */
@Composable
internal fun rememberPresetColors(): List<Color> = remember { PresetColors.all }

/**
 * Селектор цвета.
 *
 * Поддерживает отображение кастомного цвета (не из preset) в начале списка.
 */
@Composable
internal fun ColorSelector(
    selectedColor: MutableState<Color?>,
    onValueChange: () -> Unit = {},
) {
    val presetColors = rememberPresetColors()
    val showCustomColor = isCustomColor(selectedColor.value, presetColors)
    val colorContentDescription = stringResource(R.string.color)

    Text(
        text = stringResource(R.string.color_tag),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
    ) {
        // Кастомный цвет в начале списка (если есть)
        if (showCustomColor && selectedColor.value != null) {
            item {
                ColorOptionSurface(
                    color = selectedColor.value!!,
                    selectedColor = selectedColor,
                    onValueChange = onValueChange,
                    contentDescription = colorContentDescription,
                )
            }
        }

        // Предустановленные цвета
        items(presetColors) { color ->
            ColorOptionSurface(
                color = color,
                selectedColor = selectedColor,
                onValueChange = onValueChange,
                contentDescription = colorContentDescription,
            )
        }
    }
}

/**
 * Поверхность для выбора цвета.
 *
 * @param color Цвет для отображения
 * @param selectedColor Состояние выбранного цвета
 * @param onValueChange Callback при изменении цвета
 * @param contentDescription Описание для accessibility и тестов
 */
@Composable
internal fun ColorOptionSurface(
    color: Color,
    selectedColor: MutableState<Color?>,
    onValueChange: () -> Unit = {},
    contentDescription: String = "",
) {
    Surface(
        onClick = {
            if (selectedColor.value == color) {
                selectedColor.value = null
            } else {
                selectedColor.value = color
            }
            onValueChange()
        },
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size))
                .padding(dimensionResource(R.dimen.spacing_xxsmall))
                .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = color,
        border =
            if (selectedColor.value == color) {
                BorderStroke(
                    dimensionResource(R.dimen.border_width),
                    MaterialTheme.colorScheme.outline,
                )
            } else {
                null
            },
    ) {}
}

/**
 * Селектор опции отображения.
 */
@Composable
internal fun DisplayOptionSelector(
    selectedDisplayOption: MutableState<DisplayOption>,
    onValueChange: () -> Unit = {},
) {
    Text(
        text = stringResource(R.string.display_format),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))

    Column(modifier = Modifier.selectableGroup()) {
        DisplayOption.entries.forEach { option ->
            val text =
                when (option) {
                    DisplayOption.DAY -> stringResource(R.string.days_only)
                    DisplayOption.MONTH_DAY -> stringResource(R.string.months_and_days)
                    DisplayOption.YEAR_MONTH_DAY ->
                        stringResource(R.string.years_months_and_days)

                    DisplayOption.DEFAULT ->
                        stringResource(R.string.days_only)
                }

            DaysRadioButton(
                text = text,
                selected = selectedDisplayOption.value == option,
                onClick = {
                    selectedDisplayOption.value = option
                    onValueChange()
                },
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Селектор цвета с выбранным красным")
@Composable
fun ColorSelectorRedPreview() {
    JetpackDaysTheme {
        val selectedColor = remember { mutableStateOf<Color?>(Color.Red) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            ColorSelector(selectedColor)
        }
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true, name = "Селектор цвета с кастомным цветом")
@Composable
fun ColorSelectorCustomColorPreview() {
    JetpackDaysTheme {
        val customColor = Color(0xFFFF6600) // Оранжевый — не в preset
        val selectedColor = remember { mutableStateOf<Color?>(customColor) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            ColorSelector(selectedColor)
        }
    }
}

@Preview(showBackground = true, name = "Селектор опции отображения")
@Composable
fun DisplayOptionSelectorPreview() {
    JetpackDaysTheme {
        val selectedDisplayOption = remember { mutableStateOf(DisplayOption.DAY) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            DisplayOptionSelector(selectedDisplayOption)
        }
    }
}
