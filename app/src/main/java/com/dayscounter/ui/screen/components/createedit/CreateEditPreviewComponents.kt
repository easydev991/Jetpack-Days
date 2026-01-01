package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.component.DaysCountTextStyle
import com.dayscounter.ui.component.daysCountText
import com.dayscounter.ui.theme.jetpackDaysTheme

/**
 * Внутренний компонент предпросмотра.
 *
 * @param selectedDate Выбранная дата события
 * @param displayOption Опция отображения дней
 */
@Composable
internal fun previewDaysContentInner(
    selectedDate: java.time.LocalDate,
    displayOption: DisplayOption = DisplayOption.DAY,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Создаем use cases для форматирования
    val resourceProvider =
        com.dayscounter.di.FormatterModule
            .createResourceProvider(context)
    val daysFormatter =
        com.dayscounter.di.FormatterModule
            .createDaysFormatter()
    val formatDaysTextUseCase = FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
    val getFormattedDaysForItemUseCase =
        GetFormattedDaysForItemUseCase(
            calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
            formatDaysTextUseCase = formatDaysTextUseCase,
            resourceProvider = resourceProvider,
        )

    // Вычисляем разницу между датами
    val currentDate = java.time.LocalDate.now()
    val daysBetween =
        java.time.temporal.ChronoUnit.DAYS
            .between(selectedDate, currentDate)
            .toInt()

    // Создаем временный Item для форматирования
    val timestamp =
        selectedDate
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    val item =
        com.dayscounter.domain.model.Item(
            id = 0L,
            title = "",
            details = "",
            timestamp = timestamp,
            colorTag = null,
            displayOption = displayOption,
        )

    // Получаем форматированный текст
    val daysText =
        when {
            daysBetween == 0 -> stringResource(R.string.today)
            else ->
                getFormattedDaysForItemUseCase(
                    item = item,
                    currentDate = currentDate,
                    defaultDisplayOption = displayOption,
                )
        }

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
                text = stringResource(R.string.preview),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            daysCountText(
                formattedText = daysText,
                textStyle = DaysCountTextStyle.EMPHASIZED,
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

            Text(
                text = if (daysBetween > 0) stringResource(R.string.elapsed) else stringResource(R.string.remaining),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Предпросмотр дней (сегодня)")
@Composable
fun previewDaysContentTodayPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            previewDaysContentInner(java.time.LocalDate.now())
        }
    }
}

@Preview(showBackground = true, name = "Предпросмотр дней (прошедшее событие)")
@Composable
fun previewDaysContentPastPreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            previewDaysContentInner(
                java.time.LocalDate
                    .now()
                    .minusDays(5),
            )
        }
    }
}

@Preview(showBackground = true, name = "Предпросмотр дней (будущее событие)")
@Composable
fun previewDaysContentFuturePreview() {
    jetpackDaysTheme {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            previewDaysContentInner(
                java.time.LocalDate
                    .now()
                    .plusDays(10),
            )
        }
    }
}
