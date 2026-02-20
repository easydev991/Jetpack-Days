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
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCase
import com.dayscounter.domain.usecase.FormatDaysTextUseCase
import com.dayscounter.domain.usecase.GetFormattedDaysForItemUseCase
import com.dayscounter.ui.component.DaysCountText
import com.dayscounter.ui.component.DaysCountTextStyle

/**
 * Константы для компонента предпросмотра.
 */
internal object PreviewComponentsConstants {
    /** Количество дней для превью прошедшего события */
    const val PREVIEW_PAST_DAYS = 5L

    /** Количество дней для превью будущего события */
    const val PREVIEW_FUTURE_DAYS = 10L
}

/**
 * Создает use cases для форматирования дней.
 */
@Composable
private fun createPreviewUseCases(): GetFormattedDaysForItemUseCase {
    val context = androidx.compose.ui.platform.LocalContext.current
    val resourceProvider =
        com.dayscounter.di.FormatterModule
            .createResourceProvider(context)
    val daysFormatter =
        com.dayscounter.di.FormatterModule
            .createDaysFormatter()
    val formatDaysTextUseCase = FormatDaysTextUseCase(daysFormatter)
    val calculateDaysDifferenceUseCase = CalculateDaysDifferenceUseCase()
    return GetFormattedDaysForItemUseCase(
        calculateDaysDifferenceUseCase = calculateDaysDifferenceUseCase,
        formatDaysTextUseCase = formatDaysTextUseCase,
        resourceProvider = resourceProvider,
    )
}

/**
 * Вычисляет разницу в днях между выбранной датой и текущей датой.
 */
private fun calculatePreviewDays(
    selectedDate: java.time.LocalDate,
    currentDate: java.time.LocalDate,
): Int =
    java.time.temporal.ChronoUnit.DAYS
        .between(selectedDate, currentDate)
        .toInt()

/**
 * Создает временный Item для форматирования.
 */
private fun createPreviewItem(
    selectedDate: java.time.LocalDate,
    displayOption: DisplayOption,
): com.dayscounter.domain.model.Item {
    val timestamp =
        selectedDate
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    return com.dayscounter.domain.model.Item(
        id = 0L,
        title = "",
        details = "",
        timestamp = timestamp,
        colorTag = null,
        displayOption = displayOption,
    )
}

/**
 * Форматирует текст дней для предпросмотра.
 */
@Composable
private fun formatPreviewDaysText(
    daysBetween: Int,
    item: com.dayscounter.domain.model.Item,
    currentDate: java.time.LocalDate,
    displayOption: DisplayOption,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
): String =
    when {
        daysBetween == 0 -> stringResource(R.string.today)
        else ->
            getFormattedDaysForItemUseCase(
                item = item,
                currentDate = currentDate,
                defaultDisplayOption = displayOption,
                showMinus = false,
            )
    }

/**
 * Заголовок предпросмотра.
 */
@Composable
private fun PreviewHeader() {
    Text(
        text = stringResource(R.string.preview),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Отображение дней в предпросмотре.
 */
@Composable
private fun PreviewDays(daysText: String) {
    DaysCountText(
        formattedText = daysText,
        textStyle = DaysCountTextStyle.EMPHASIZED,
    )
}

/**
 * Описание прошедшего/оставшегося времени.
 */
@Composable
private fun PreviewFooter(daysBetween: Int) {
    Text(
        text = if (daysBetween > 0) stringResource(R.string.elapsed) else stringResource(R.string.remaining),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * Внутренний компонент предпросмотра.
 *
 * @param selectedDate Выбранная дата события
 * @param displayOption Опция отображения дней
 */
@Composable
internal fun PreviewDaysContentInner(
    selectedDate: java.time.LocalDate,
    displayOption: DisplayOption = DisplayOption.DAY,
) {
    val getFormattedDaysForItemUseCase = createPreviewUseCases()
    val currentDate = java.time.LocalDate.now()
    val daysBetween = calculatePreviewDays(selectedDate, currentDate)
    val item = createPreviewItem(selectedDate, displayOption)
    val daysText =
        formatPreviewDaysText(
            daysBetween = daysBetween,
            item = item,
            currentDate = currentDate,
            displayOption = displayOption,
            getFormattedDaysForItemUseCase = getFormattedDaysForItemUseCase,
        )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_regular)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PreviewHeader()
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
            PreviewDays(daysText)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xxsmall)))
            PreviewFooter(daysBetween)
        }
    }
}
