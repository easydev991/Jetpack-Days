package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.dayscounter.ui.ds.SelectableColorTag
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
 * Принимает plain-значение и callback вместо MutableState.
 */
@Composable
internal fun ColorSelector(
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit
) {
    val presetColors = rememberPresetColors()
    val showCustomColor = isCustomColor(selectedColor, presetColors)
    val colorContentDescription = stringResource(R.string.color)

    val spacingSmall = dimensionResource(R.dimen.spacing_small)
    val spacingXsmall = dimensionResource(R.dimen.spacing_xsmall)

    Text(
        text = stringResource(R.string.color_tag),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(spacingXsmall))

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(spacingSmall)
    ) {
        if (showCustomColor && selectedColor != null) {
            ColorOptionSurface(
                color = selectedColor,
                selectedColor = selectedColor,
                onColorSelected = onColorSelected,
                contentDescription = colorContentDescription
            )
        }

        presetColors.forEach { color ->
            ColorOptionSurface(
                color = color,
                selectedColor = selectedColor,
                onColorSelected = onColorSelected,
                contentDescription = colorContentDescription
            )
        }
    }
}

/**
 * Поверхность для выбора цвета.
 *
 * @param color Цвет для отображения
 * @param selectedColor Текущий выбранный цвет (plain)
 * @param onColorSelected Callback при выборе цвета
 * @param contentDescription Описание для accessibility и тестов
 */
@Composable
internal fun ColorOptionSurface(
    color: Color,
    selectedColor: Color?,
    onColorSelected: (Color?) -> Unit,
    contentDescription: String = ""
) {
    val outerPadding = dimensionResource(R.dimen.spacing_xxsmall)
    val isSelected = selectedColor == color

    SelectableColorTag(
        color = color,
        isSelected = isSelected,
        modifier =
            Modifier
                .padding(outerPadding)
                .semantics { this.contentDescription = contentDescription },
        onClick = {
            if (isSelected) {
                onColorSelected(null)
            } else {
                onColorSelected(color)
            }
        }
    )
}

/**
 * Селектор опции отображения.
 * Принимает plain-значение и callback вместо MutableState.
 */
@Composable
internal fun DisplayOptionSelector(
    selectedDisplayOption: DisplayOption,
    onDisplayOptionSelected: (DisplayOption) -> Unit
) {
    Text(
        text = stringResource(R.string.display_format),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
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
                selected = selectedDisplayOption == option,
                onClick = {
                    onDisplayOptionSelected(option)
                }
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Селектор цвета с выбранным красным")
@Composable
fun ColorSelectorRedPreview() {
    JetpackDaysTheme {
        var selectedColor by remember { mutableStateOf<Color?>(Color.Red) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ColorSelector(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
        }
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true, name = "Селектор цвета с кастомным цветом")
@Composable
fun ColorSelectorCustomColorPreview() {
    JetpackDaysTheme {
        val customColor = Color(0xFFFF6600)
        var selectedColor by remember { mutableStateOf<Color?>(customColor) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            ColorSelector(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )
        }
    }
}

@Preview(showBackground = true, name = "Селектор опции отображения")
@Composable
fun DisplayOptionSelectorPreview() {
    JetpackDaysTheme {
        var selectedDisplayOption by remember { mutableStateOf(DisplayOption.DAY) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center
        ) {
            DisplayOptionSelector(
                selectedDisplayOption = selectedDisplayOption,
                onDisplayOptionSelected = { selectedDisplayOption = it }
            )
        }
    }
}
