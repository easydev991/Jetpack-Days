package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import com.dayscounter.R
import com.dayscounter.ui.component.DaysCountTextStyle
import com.dayscounter.ui.component.daysCountText
import com.dayscounter.ui.util.NumberFormattingUtils

/**
 * Внутренний компонент предпросмотра.
 */
@Composable
internal fun previewDaysContentInner(selectedDate: java.time.LocalDate) {
    val currentDate = java.time.LocalDate.now()
    val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(selectedDate, currentDate).toInt()

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_extra_large)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Предпросмотр",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            val daysText =
                when {
                    daysBetween == 0 -> "Сегодня"
                    daysBetween > 0 -> NumberFormattingUtils.formatDaysCount(daysBetween)
                    else -> NumberFormattingUtils.formatDaysCount(-daysBetween)
                }

            daysCountText(
                formattedText = daysText,
                textStyle = DaysCountTextStyle.EMPHASIZED,
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

            Text(
                text = if (daysBetween > 0) "прошло" else "осталось",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
