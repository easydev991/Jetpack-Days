package com.dayscounter.ui.screen.components.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.component.DaysCountTextStyle
import com.dayscounter.ui.component.daysCountText
import com.dayscounter.ui.util.NumberFormattingUtils
import com.dayscounter.viewmodel.DetailScreenState
import java.time.LocalDate

/**
 * Контент по состоянию.
 */
@Composable
fun detailContentByState(
    uiState: DetailScreenState,
    modifier: Modifier = Modifier,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase? = null,
) {
    when (uiState) {
        is DetailScreenState.Loading -> {
            loadingContent(modifier = modifier)
        }

        is DetailScreenState.Success -> {
            detailContentInner(
                item = uiState.item,
                modifier = modifier,
                getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
            )
        }

        is DetailScreenState.Error -> {
            errorContent(
                message = uiState.message,
                modifier = modifier,
            )
        }
    }
}

/**
 * Внутренний компонент контента деталей.
 */
@Composable
internal fun detailContentInner(
    item: com.dayscounter.domain.model.Item,
    modifier: Modifier = Modifier,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase? = null,
) {
    val scrollState = rememberScrollState()
    val formatter =
        java.time.format.DateTimeFormatter
            .ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("ru"))

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(dimensionResource(R.dimen.spacing_huge)),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_huge)))

        // Цветовая метка
        if (item.colorTag != null) {
            colorTagSection(item.colorTag)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_huge)))
        }

        // Заголовок события
        titleSection(item.title)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        // Дата события
        dateSection(item.timestamp, formatter)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_massive)))

        // Количество дней
        daysCountSection(item, getFormattedDaysForItemUseCase)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_massive)))

        // Детали события (если есть)
        if (item.details.isNotEmpty()) {
            detailsSection(item.details)
        }

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_huge)))

        // Информация об опции отображения
        displayOptionInfoSection(item.displayOption)
    }
}

/**
 * Секция с цветовой меткой.
 */
@Composable
fun colorTagSection(colorTag: Int) {
    Surface(
        modifier = Modifier.size(dimensionResource(R.dimen.size_large)),
        shape = CircleShape,
        color =
            androidx.compose.ui.graphics
                .Color(colorTag),
    ) {}
}

/**
 * Секция с заголовком.
 */
@Composable
fun titleSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )
}

/**
 * Секция с датой.
 */
@Composable
internal fun dateSection(
    timestamp: Long,
    formatter: java.time.format.DateTimeFormatter,
) {
    val eventDate =
        java.time.Instant
            .ofEpochMilli(timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()

    Text(
        text = eventDate.format(formatter),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

/**
 * Секция с количеством дней.
 */
@Composable
fun daysCountSection(
    item: com.dayscounter.domain.model.Item,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase? = null,
) {
    val daysText: String =
        if (getFormattedDaysForItemUseCase != null) {
            // Используем use case для форматирования с учетом displayOption
            getFormattedDaysForItemUseCase(item = item)
        } else {
            // Fallback: простое форматирование по количеству дней (для preview)
            val eventDate =
                java.time.Instant
                    .ofEpochMilli(item.timestamp)
                    .atZone(java.time.ZoneId.systemDefault())
                    .toLocalDate()
            val currentDate = LocalDate.now()
            calculateDaysText(eventDate, currentDate)
        }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        daysCountText(
            formattedText = daysText,
            textStyle = DaysCountTextStyle.EMPHASIZED,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

        Text(
            text = "прошло",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Вычисляет текст с количеством дней.
 */
internal fun calculateDaysText(
    eventDate: LocalDate,
    currentDate: LocalDate,
): String =
    if (eventDate == currentDate) {
        "Сегодня"
    } else {
        val totalDays =
            java.time.temporal.ChronoUnit.DAYS
                .between(eventDate, currentDate)
                .toInt()
        NumberFormattingUtils.formatDaysCount(totalDays)
    }

/**
 * Секция с деталями события.
 */
@Composable
fun detailsSection(details: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_extra_large)),
        ) {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

            Text(
                text = details,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

/**
 * Информация об опции отображения.
 */
@Composable
fun displayOptionInfoSection(displayOption: DisplayOption) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.display_format),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text =
                    when (displayOption) {
                        DisplayOption.DAY -> stringResource(R.string.days_only)
                        DisplayOption.MONTH_DAY -> stringResource(R.string.months_and_days)
                        DisplayOption.YEAR_MONTH_DAY ->
                            stringResource(R.string.years_months_and_days)

                        DisplayOption.DEFAULT ->
                            stringResource(R.string.days_only)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
