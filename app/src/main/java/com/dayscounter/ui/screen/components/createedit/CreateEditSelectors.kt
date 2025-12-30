package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption

/**
 * Селектор цвета.
 */
@Composable
internal fun colorSelector(selectedColor: MutableState<Color?>) {
    val colors =
        listOf(
            colorResource(R.color.color_primary_red),
            colorResource(R.color.color_primary_teal),
            colorResource(R.color.color_primary_blue),
            colorResource(R.color.color_primary_green),
            colorResource(R.color.color_primary_yellow),
            colorResource(R.color.color_primary_purple),
        )

    Text(
        text = stringResource(R.string.color_tag),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large)),
    ) {
        colors.forEach { color ->
            colorOptionSurface(
                color = color,
                selectedColor = selectedColor,
            )
        }

        // Опция без цвета
        noColorOptionSurface(selectedColor = selectedColor)
    }
}

/**
 * Поверхность для выбора цвета.
 */
@Composable
internal fun colorOptionSurface(
    color: Color,
    selectedColor: MutableState<Color?>,
) {
    Surface(
        onClick = {
            if (selectedColor.value == color) {
                selectedColor.value = null
            } else {
                selectedColor.value = color
            }
        },
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size))
                .padding(dimensionResource(R.dimen.spacing_small)),
        shape = CircleShape,
        color = color,
        border =
            if (selectedColor.value == color) {
                null
            } else {
                BorderStroke(
                    dimensionResource(R.dimen.border_width),
                    MaterialTheme.colorScheme.outline,
                )
            },
    ) {}
}

/**
 * Поверхность для опции без цвета.
 */
@Composable
internal fun noColorOptionSurface(selectedColor: MutableState<Color?>) {
    Surface(
        onClick = { selectedColor.value = null },
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size))
                .padding(dimensionResource(R.dimen.spacing_small)),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border =
            if (selectedColor.value == null) {
                null
            } else {
                BorderStroke(
                    dimensionResource(R.dimen.border_width),
                    MaterialTheme.colorScheme.outline,
                )
            },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Селектор опции отображения.
 */
@Composable
internal fun displayOptionSelector(selectedDisplayOption: MutableState<DisplayOption>) {
    Text(
        text = stringResource(R.string.display_format),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

    DisplayOption.values().forEach { option ->
        displayOptionSurface(
            option = option,
            selectedDisplayOption = selectedDisplayOption,
        )
    }
}

/**
 * Поверхность для опции отображения.
 */
@Composable
internal fun displayOptionSurface(
    option: DisplayOption,
    selectedDisplayOption: MutableState<DisplayOption>,
) {
    Surface(
        onClick = { selectedDisplayOption.value = option },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacing_small)),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isSelected = selectedDisplayOption.value == option
            val text =
                when (option) {
                    DisplayOption.DAY -> stringResource(R.string.days_only)
                    DisplayOption.MONTH_DAY -> stringResource(R.string.months_and_days)
                    DisplayOption.YEAR_MONTH_DAY ->
                        stringResource(R.string.years_months_and_days)
                    DisplayOption.DEFAULT ->
                        stringResource(R.string.days_only)
                }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
