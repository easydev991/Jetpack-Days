package com.dayscounter.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.ui.theme.jetpackDaysTheme

/**
 * Константы для компонента списка элементов.
 */
private object ListItemViewConstants {
    /** Вес левой колонки с названием и описанием (70% ширины) */
    const val TITLE_WEIGHT = 0.7f

    /** Вес правой колонки с количеством дней (30% ширины) */
    const val DAYS_WEIGHT = 0.3f
}

/**
 * Цветовая метка элемента.
 */
@Composable
private fun itemColorTag(colorTag: Int) {
    Box(
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size_small))
                .clip(CircleShape)
                .background(color = Color(colorTag)),
    )
    Spacer(modifier = Modifier.width(dimensionResource(R.dimen.spacing_regular)))
}

/**
 * Левая колонка с названием и описанием элемента.
 */
@Composable
private fun itemTitleAndDetails(
    modifier: Modifier = Modifier,
    item: Item,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        // Заголовок события (до 2 строк)
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        // Описание события (до 2 строк)
        if (item.details.isNotEmpty()) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))
            Text(
                text = item.details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * Правая колонка с количеством дней.
 */
@Composable
private fun itemDaysBadge(
    modifier: Modifier = Modifier,
    formattedDaysText: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
    ) {
        daysCountText(
            formattedText = formattedDaysText,
            textStyle = DaysCountTextStyle.NORMAL,
            textAlign = TextAlign.End,
        )
    }
}

/**
 * Компонент для отображения элемента списка событий.
 *
 * Показывает заголовок события, количество дней и цветовую метку.
 * Использует [daysCountText] для форматированного отображения количества дней.
 *
 * На главном экране:
 * - Текст с количеством дней справа и занимает не более 30% от ширины экрана
 * - Слева вертикальный стек с названием и описанием (каждое ограничено 2 строками)
 * - Длинный текст обрезается с многоточием
 * - Поддерживает короткий клик и долгое нажатие
 * - Поддерживает визуальное выделение элемента
 *
 * @param params Параметры компонента
 * @param modifier Modifier для компонента
 */
@Composable
fun listItemView(
    params: ListItemParams,
    modifier: Modifier = Modifier,
) {
    val backgroundColor =
        animateColorAsState(
            targetValue =
                if (params.isSelected) {
                    MaterialTheme.colorScheme.surfaceVariant
                } else {
                    MaterialTheme.colorScheme.surface
                },
            label = "backgroundColor",
        ).value

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(color = backgroundColor)
                .pointerInput(Unit) {
                    if (params.onLongClick != null) {
                        detectTapGestures(
                            onTap = { params.onClick(params.item) },
                            onLongPress = { offset -> params.onLongClick.invoke(offset) },
                        )
                    } else {
                        detectTapGestures(
                            onTap = { params.onClick(params.item) },
                        )
                    }
                }
                .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (params.item.colorTag != null) {
            itemColorTag(params.item.colorTag)
        }
        itemTitleAndDetails(
            modifier = Modifier.weight(ListItemViewConstants.TITLE_WEIGHT),
            params.item,
        )
        itemDaysBadge(
            modifier = Modifier.weight(ListItemViewConstants.DAYS_WEIGHT),
            params.formattedDaysText,
        )
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
fun listItemViewPreview() {
    jetpackDaysTheme {
        listItemView(
            params =
                ListItemParams(
                    item =
                        Item(
                            id = 1L,
                            title = "День рождения",
                            details = "Праздничный день",
                            timestamp = System.currentTimeMillis() - (5 * 24 * 60 * 60 * 1000L),
                            colorTag = R.color.color_primary_red,
                            displayOption = DisplayOption.DAY,
                        ),
                    formattedDaysText = "5 дней",
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun listItemViewNoColorPreview() {
    jetpackDaysTheme {
        listItemView(
            params =
                ListItemParams(
                    item =
                        Item(
                            id = 2L,
                            title = "Начало работы",
                            details = "Первый день на новой работе",
                            timestamp = System.currentTimeMillis() - (365 * 24 * 60 * 60 * 1000L),
                            colorTag = null,
                            displayOption = DisplayOption.YEAR_MONTH_DAY,
                        ),
                    formattedDaysText = "1 год",
                ),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun listItemViewTodayPreview() {
    jetpackDaysTheme {
        listItemView(
            params =
                ListItemParams(
                    item =
                        Item(
                            id = 3L,
                            title = "Сегодняшнее событие",
                            details = "Произошло сегодня",
                            timestamp = System.currentTimeMillis(),
                            colorTag = R.color.color_primary_teal,
                            displayOption = DisplayOption.MONTH_DAY,
                        ),
                    formattedDaysText = "Сегодня",
                ),
        )
    }
}

@Preview(showBackground = true, name = "Длинный заголовок")
@Composable
fun listItemViewLongTitlePreview() {
    jetpackDaysTheme {
        listItemView(
            params =
                ListItemParams(
                    item =
                        Item(
                            id = 4L,
                            title = "Очень длинное название события, которое должно переноситься на новую строку",
                            details = "Детали события",
                            timestamp = System.currentTimeMillis() - (100 * 24 * 60 * 60 * 1000L),
                            colorTag = R.color.color_primary_blue,
                            displayOption = DisplayOption.DAY,
                        ),
                    formattedDaysText = "100 дней",
                ),
        )
    }
}
