package com.dayscounter.ui.screens.themeicon

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.domain.model.AppIcon

/**
 * Компонент для отображения превью иконки приложения.
 *
 * @param appIcon Иконка приложения для отображения
 * @param isSelected Выбрана ли эта иконка
 * @param isDarkTheme Признак темной темы для выбора правильного ресурса иконки
 * @param onClick Обработчик клика
 * @param modifier Modifier для настройки внешнего вида
 */
@Composable
fun IconPreviewItem(
    appIcon: AppIcon,
    isSelected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconResource = appIcon.iconResource(isDarkTheme)
    val borderColor =
        if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outline
        }
    val borderWidth = if (isSelected) 4.dp else 2.dp
    Box(
        modifier = modifier
            .size(64.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        // Иконка с закруглением и бордюром (аналог iOS overlay)
        Image(
            painter = painterResource(id = iconResource),
            contentDescription = "Icon preview",
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .drawWithContent {
                        // Сначала рисуем контент (картинку)
                        drawContent()
                        // Потом поверх рисуем бордюр
                        drawRoundRect(
                            color = borderColor,
                            style = Stroke(width = borderWidth.toPx()),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                        )
                    },
        )

        // Галочка на выбранной иконке (аналог iOS .overlay(alignment: .topTrailing))
        AnimatedVisibility(
            visible = isSelected,
            enter =
                scaleIn(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ),
            exit =
                scaleOut(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(
                        x = 6.dp,
                        y = (-6).dp,
                    ),
        ) { CheckIcon() }
    }
}

/** Иконка галочки. */
@Composable
private fun CheckIcon() {
    Icon(
        imageVector = Icons.Default.Check,
        contentDescription = "Selected",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
    )
}

/** Расширение для получения ресурса иконки по значению AppIcon и теме. */
private fun AppIcon.iconResource(isDarkTheme: Boolean): Int =
    when (this) {
        AppIcon.DEFAULT -> R.drawable.icon_preview_1
        AppIcon.ICON_2 -> if (isDarkTheme) R.drawable.icon_preview_2_dark else R.drawable.icon_preview_2
        AppIcon.ICON_3 -> if (isDarkTheme) R.drawable.icon_preview_3_dark else R.drawable.icon_preview_3
        AppIcon.ICON_4 -> if (isDarkTheme) R.drawable.icon_preview_4_dark else R.drawable.icon_preview_4
        AppIcon.ICON_5 -> if (isDarkTheme) R.drawable.icon_preview_5_dark else R.drawable.icon_preview_5
        AppIcon.ICON_6 -> if (isDarkTheme) R.drawable.icon_preview_6_dark else R.drawable.icon_preview_6
    }
