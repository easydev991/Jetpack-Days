package com.dayscounter.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.ui.theme.jetpackDaysTheme

/**
 * Компонент для отображения элемента списка событий.
 *
 * Показывает заголовок события, количество дней и цветовую метку.
 * Использует [daysCountText] для форматированного отображения количества дней.
 *
 * @param item Элемент для отображения
 * @param formattedDaysText Форматированный текст с количеством дней (вычисляется извне)
 * @param modifier Modifier для компонента
 * @param onClick Обработчик клика по элементу
 */
@Composable
fun listItemView(
    item: Item,
    formattedDaysText: String,
    modifier: Modifier = Modifier,
    onClick: (Item) -> Unit = {},
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surface,
                )
                .clickable { onClick(item) }
                .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Цветовая метка
        if (item.colorTag != null) {
            Box(
                modifier =
                    Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(color = Color(item.colorTag)),
            )
            Spacer(modifier = Modifier.width(12.dp))
        }

        // Основной контент
        Column(
            modifier = Modifier.weight(1f),
        ) {
            // Заголовок события
            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Количество дней
            Spacer(modifier = Modifier.height(4.dp))

            daysCountText(
                formattedText = formattedDaysText,
                textStyle = DaysCountTextStyle.NORMAL,
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true)
@Composable
fun listItemViewPreview() {
    jetpackDaysTheme {
        listItemView(
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
        )
    }
}

@Preview(showBackground = true)
@Composable
fun listItemViewNoColorPreview() {
    jetpackDaysTheme {
        listItemView(
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
        )
    }
}

@Preview(showBackground = true)
@Composable
fun listItemViewTodayPreview() {
    jetpackDaysTheme {
        listItemView(
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
        )
    }
}

@Preview(showBackground = true, name = "Длинный заголовок")
@Composable
fun listItemViewLongTitlePreview() {
    jetpackDaysTheme {
        listItemView(
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
        )
    }
}
