package com.dayscounter.ui.screens.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.dayscounter.R

/**
 * Диалог фильтрации по цветовому тегу.
 *
 * @param availableColors Список доступных цветов для выбора
 * @param currentFilter Текущий применённый фильтр (null если фильтр не установлен)
 * @param onApply Callback при применении фильтра (передаёт выбранный цвет или null для сброса)
 * @param onDismiss Callback при закрытии диалога
 */
@Composable
internal fun ColorTagFilterDialog(
    availableColors: List<Int>,
    currentFilter: Int?,
    onApply: (Int?) -> Unit,
    onDismiss: () -> Unit
) {
    // Черновик выбранного цвета в диалоге
    var draftSelectedColor by remember(currentFilter) { mutableStateOf(currentFilter) }

    // Кнопка "Применить" активна только если выбранный цвет отличается от текущего фильтра
    val canApply = draftSelectedColor != currentFilter

    // Кнопка "Сбросить" активна только если есть активный фильтр
    val canReset = currentFilter != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.filter_by_color))
        },
        text = {
            Column {
                ColorTagFilterGrid(
                    availableColors = availableColors,
                    selectedColor = draftSelectedColor,
                    onColorSelected = { color -> draftSelectedColor = color }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onApply(draftSelectedColor) },
                enabled = canApply
            ) {
                Text(stringResource(R.string.apply))
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onApply(null) },
                enabled = canReset,
                colors =
                    ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
            ) {
                Text(stringResource(R.string.reset))
            }
        }
    )
}

/**
 * Сетка цветов для выбора в фильтре.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorTagFilterGrid(
    availableColors: List<Int>,
    selectedColor: Int?,
    onColorSelected: (Int?) -> Unit
) {
    val colorContentDescription = stringResource(R.string.color)

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
    ) {
        availableColors.forEach { colorInt ->
            ColorTagFilterOption(
                colorInt = colorInt,
                isSelected = selectedColor == colorInt,
                contentDescription = colorContentDescription,
                onClick = {
                    if (selectedColor == colorInt) {
                        // Повторное нажатие на тот же цвет — снимаем выделение
                        onColorSelected(null)
                    } else {
                        onColorSelected(colorInt)
                    }
                }
            )
        }
    }
}

/**
 * Опция цвета в сетке фильтра.
 */
@Composable
private fun ColorTagFilterOption(
    colorInt: Int,
    isSelected: Boolean,
    contentDescription: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size))
                .padding(dimensionResource(R.dimen.spacing_xxsmall))
                .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color =
            androidx.compose.ui.graphics
                .Color(colorInt),
        border =
            if (isSelected) {
                BorderStroke(
                    dimensionResource(R.dimen.border_width),
                    MaterialTheme.colorScheme.outline
                )
            } else {
                null
            }
    ) {}
}
