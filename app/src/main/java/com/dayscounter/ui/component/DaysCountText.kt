package com.dayscounter.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.ui.theme.jetpackDaysTheme

/**
 * Вспомогательная функция для получения стиля для обычного отображения.
 */
@Composable
private fun getNormalStyle(customStyle: TextStyle?): TextStyle =
    if (customStyle != null) {
        customStyle
    } else {
        MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
        )
    }

/**
 * Вспомогательная функция для получения акцентного стиля.
 */
@Composable
private fun getEmphasizedStyle(
    customStyle: TextStyle?,
    color: Color,
): TextStyle {
    val accentColor =
        if (color == Color.Unspecified) {
            MaterialTheme.colorScheme.primary
        } else {
            color
        }

    return if (customStyle != null) {
        customStyle.copy(
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
    } else {
        MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            color = accentColor,
        )
    }
}

/**
 * Вспомогательная функция для получения вторичного стиля.
 */
@Composable
private fun getSecondaryStyle(
    customStyle: TextStyle?,
    color: Color,
): TextStyle {
    val secondaryColor =
        if (color == Color.Unspecified) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            color
        }

    return if (customStyle != null) {
        customStyle.copy(color = secondaryColor)
    } else {
        MaterialTheme.typography.bodyMedium.copy(color = secondaryColor)
    }
}

/**
 * Compose компонент для отображения форматированного количества дней.
 *
 * Поддерживает автоматическое обновление при изменении времени,
 * отображение в разных стилях и корректную работу с Material3 темой.
 *
 * @param formattedText Форматированный текст для отображения
 * @param modifier Modifier для компонента
 * @param textStyle Стиль отображения текста
 * @param customStyle Кастомный TextStyle (переопределяет textStyle)
 * @param color Цвет текста (переопределяет тему)
 */
@Composable
fun daysCountText(
    formattedText: String,
    modifier: Modifier = Modifier,
    textStyle: DaysCountTextStyle = DaysCountTextStyle.NORMAL,
    customStyle: TextStyle? = null,
    color: Color = Color.Unspecified,
) {
    val resolvedStyle =
        when (textStyle) {
            DaysCountTextStyle.NORMAL -> getNormalStyle(customStyle)
            DaysCountTextStyle.EMPHASIZED -> getEmphasizedStyle(customStyle, color)
            DaysCountTextStyle.SECONDARY -> getSecondaryStyle(customStyle, color)
        }

    val contentColor =
        if (color != Color.Unspecified) {
            color
        } else {
            LocalContentColor.current
        }

    Text(
        text = formattedText,
        modifier = modifier,
        style = resolvedStyle,
        color = contentColor,
    )
}

/**
 * Предпросмотры компонента daysCountText.
 */
@Preview(showBackground = true)
@Composable
fun daysCountTextPreview() {
    jetpackDaysTheme {
        daysCountText(
            formattedText = "5 дней",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun daysCountTextTodayPreview() {
    jetpackDaysTheme {
        daysCountText(
            formattedText = "Сегодня",
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun daysCountTextEmphasizedPreview() {
    jetpackDaysTheme {
        daysCountText(
            formattedText = "1 год 2 месяца 5 дней",
            modifier = Modifier.padding(16.dp),
            textStyle = DaysCountTextStyle.EMPHASIZED,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun daysCountTextSecondaryPreview() {
    jetpackDaysTheme {
        daysCountText(
            formattedText = "2 мес. 5 дн.",
            modifier = Modifier.padding(16.dp),
            textStyle = DaysCountTextStyle.SECONDARY,
        )
    }
}

@Preview(showBackground = true)
@Composable
fun daysCountTextFullFormatPreview() {
    jetpackDaysTheme {
        daysCountText(
            formattedText = "1 год 2 месяца 5 дней",
            modifier = Modifier.padding(16.dp),
        )
    }
}
