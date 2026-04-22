package com.dayscounter.ui.screens.events

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme

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

    // Кнопка "Сбросить" активна когда есть применённый фильтр ИЛИ в черновике что-то выбрано
    val canReset = currentFilter != null || draftSelectedColor != null

    /**
     * Обработчик сброса:
     * - если есть применённый фильтр — сбросить фильтр и закрыть диалог;
     * - если фильтра нет, но в черновике что-то выбрано — очистить только черновик, диалог остаётся открытым.
     */
    val onResetClick: () -> Unit = {
        if (currentFilter != null) {
            // Есть применённый фильтр — сбросить и закрыть
            onApply(null)
        } else {
            // Фильтра нет, просто очищаем черновик
            draftSelectedColor = null
        }
    }

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
                onClick = onResetClick,
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
@Composable
private fun ColorTagFilterGrid(
    availableColors: List<Int>,
    selectedColor: Int?,
    onColorSelected: (Int?) -> Unit
) {
    val colorContentDescription = stringResource(R.string.color)

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(max = ColorTagFilterDialogConstants.GRID_MAX_HEIGHT)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(ColorTagFilterDialogConstants.GRID_MIN_CELL_SIZE),
            horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small))
        ) {
            items(items = availableColors, key = { it }) { colorInt ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
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
    val colorTagSize = dimensionResource(R.dimen.color_tag_size)
    val borderWidth = dimensionResource(R.dimen.border_width)

    Box(
        modifier =
            Modifier
                .size(colorTagSize)
                .clip(CircleShape)
                .background(Color(colorInt))
                .then(
                    if (isSelected) {
                        Modifier.border(
                            BorderStroke(borderWidth, MaterialTheme.colorScheme.outline),
                            CircleShape
                        )
                    } else {
                        Modifier
                    }
                ).clickable(onClick = onClick)
                .semantics { this.contentDescription = contentDescription }
    )
}

private object ColorTagFilterDialogConstants {
    val GRID_MIN_CELL_SIZE = 56.dp
    val GRID_MAX_HEIGHT = 260.dp
    const val HUE_RANGE = 360f
    const val PREVIEW_SATURATION = 0.75f
    const val PREVIEW_LIGHTNESS = 0.55f
    const val PREVIEW_COLORS_1 = 1
    const val PREVIEW_COLORS_3 = 3
    const val PREVIEW_COLORS_8 = 8
    const val PREVIEW_COLORS_30 = 30
    const val PREVIEW_WIDTH_DP = 360
    const val PREVIEW_HEIGHT_DP = 640
    const val PREVIEW_SELECTED_INDEX_3 = 0
    const val PREVIEW_SELECTED_INDEX_8 = 2
    const val PREVIEW_SELECTED_INDEX_30 = 10
}

private fun previewColors(count: Int): List<Int> =
    List(count) { index ->
        val hue = (index * ColorTagFilterDialogConstants.HUE_RANGE) / count.coerceAtLeast(1)
        Color
            .hsl(
                hue = hue,
                saturation = ColorTagFilterDialogConstants.PREVIEW_SATURATION,
                lightness = ColorTagFilterDialogConstants.PREVIEW_LIGHTNESS
            ).toArgb()
    }

@Suppress("UnusedPrivateMember")
@Preview(
    name = "ColorFilter 1",
    showBackground = true,
    widthDp = ColorTagFilterDialogConstants.PREVIEW_WIDTH_DP,
    heightDp = ColorTagFilterDialogConstants.PREVIEW_HEIGHT_DP
)
@Composable
private fun ColorTagFilterDialogPreviewOneColor() {
    val colors = previewColors(ColorTagFilterDialogConstants.PREVIEW_COLORS_1)
    JetpackDaysTheme {
        ColorTagFilterDialog(
            availableColors = colors,
            currentFilter = null,
            onApply = {},
            onDismiss = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(
    name = "ColorFilter 3",
    showBackground = true,
    widthDp = ColorTagFilterDialogConstants.PREVIEW_WIDTH_DP,
    heightDp = ColorTagFilterDialogConstants.PREVIEW_HEIGHT_DP
)
@Composable
private fun ColorTagFilterDialogPreviewThreeColors() {
    val colors = previewColors(ColorTagFilterDialogConstants.PREVIEW_COLORS_3)
    JetpackDaysTheme {
        ColorTagFilterDialog(
            availableColors = colors,
            currentFilter = colors.getOrNull(ColorTagFilterDialogConstants.PREVIEW_SELECTED_INDEX_3),
            onApply = {},
            onDismiss = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(
    name = "ColorFilter 8",
    showBackground = true,
    widthDp = ColorTagFilterDialogConstants.PREVIEW_WIDTH_DP,
    heightDp = ColorTagFilterDialogConstants.PREVIEW_HEIGHT_DP
)
@Composable
private fun ColorTagFilterDialogPreviewEightColors() {
    val colors = previewColors(ColorTagFilterDialogConstants.PREVIEW_COLORS_8)
    JetpackDaysTheme {
        ColorTagFilterDialog(
            availableColors = colors,
            currentFilter = colors.getOrNull(ColorTagFilterDialogConstants.PREVIEW_SELECTED_INDEX_8),
            onApply = {},
            onDismiss = {}
        )
    }
}

@Suppress("UnusedPrivateMember")
@Preview(
    name = "ColorFilter 30",
    showBackground = true,
    widthDp = ColorTagFilterDialogConstants.PREVIEW_WIDTH_DP,
    heightDp = ColorTagFilterDialogConstants.PREVIEW_HEIGHT_DP
)
@Composable
private fun ColorTagFilterDialogPreviewThirtyColors() {
    val colors = previewColors(ColorTagFilterDialogConstants.PREVIEW_COLORS_30)
    JetpackDaysTheme {
        ColorTagFilterDialog(
            availableColors = colors,
            currentFilter = colors.getOrNull(ColorTagFilterDialogConstants.PREVIEW_SELECTED_INDEX_30),
            onApply = {},
            onDismiss = {}
        )
    }
}
