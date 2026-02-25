package com.dayscounter.ui.screens.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.usecase.GetDaysAnalysisTextUseCase
import com.dayscounter.ui.viewmodel.DetailScreenState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Контент по состоянию.
 *
 * @param uiState Состояние экрана
 * @param getDaysAnalysisTextUseCase Use case для получения текста анализа с префиксом
 * @param modifier Modifier для компонента
 */
@Composable
fun DetailContentByState(
    uiState: DetailScreenState,
    getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is DetailScreenState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is DetailScreenState.Success -> {
            DetailContentInner(
                item = uiState.item,
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
                modifier = modifier,
            )
        }

        is DetailScreenState.Error -> {
            ErrorContent(
                message = uiState.message,
                modifier = modifier,
            )
        }
    }
}

/**
 * Внутренний компонент контента деталей.
 * Структура аналогична iOS (VStack с выравниванием по левому краю).
 *
 * @param item Элемент для отображения
 * @param getDaysAnalysisTextUseCase Use case для получения текста анализа с префиксом
 * @param modifier Modifier для компонента
 */
@Composable
internal fun DetailContentInner(
    item: Item,
    getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        ReadSectionView(
            headerText = stringResource(R.string.title),
            bodyText = item.title,
        )
        if (item.details.isNotEmpty()) {
            ReadSectionView(
                headerText = stringResource(R.string.details),
                bodyText = item.details,
            )
        }
        if (item.colorTag != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.color_tag),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                ColorTagSection(item.colorTag)
            }
        }
        DetailDatePicker(
            item = item,
            getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
        )
        DetailDisplayOptionPicker(displayOption = item.displayOption)
        Spacer(modifier = Modifier.weight(1f))
    }
}

/**
 * Секция с цветовой меткой.
 */
@Composable
fun ColorTagSection(colorTag: Int) {
    Surface(
        modifier = Modifier.size(dimensionResource(R.dimen.color_tag_size_small)),
        shape = CircleShape,
        color =
            androidx.compose.ui.graphics
                .Color(colorTag),
    ) {}
}

/**
 * Компонент ReadSectionView - аналог iOS ReadSectionView.
 * Отображает заголовок и текст секции с выравниванием по левому краю.
 *
 * @param headerText Заголовок секции
 * @param bodyText Текст секции
 * @param modifier Modifier для компонента
 */
@Composable
fun ReadSectionView(
    headerText: String,
    bodyText: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
    ) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = bodyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Компонент DetailDatePicker - аналог iOS ItemDatePicker.
 * Отображает дату в формате DatePicker, отключенный для редактирования,
 * с дополнительным полем для краткого анализа дней.
 *
 * @param item Элемент для отображения (содержит timestamp и displayOption)
 * @param getDaysAnalysisTextUseCase Use case для получения текста анализа с префиксом
 * @param modifier Modifier для компонента
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailDatePicker(
    item: Item,
    getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase,
    modifier: Modifier = Modifier,
) {
    val formatter =
        remember {
            DateTimeFormatter.ofPattern("MMM d, yyyy")
        }
    val formattedDate =
        remember(item.timestamp) {
            Instant
                .ofEpochMilli(item.timestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(formatter)
        }

    // Используем новый use case для получения полного текста анализа
    val daysAnalysisText = getDaysAnalysisTextUseCase(item = item)

    // Для режима только для чтения отображаем как Text вместо DatePicker
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
    ) {
        Text(
            text = stringResource(R.string.date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = daysAnalysisText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * Компонент DetailDisplayOptionPicker - аналог iOS ItemDisplayOptionPicker.
 * Отображает выбранную опцию отображения, отключенный для редактирования.
 *
 * @param displayOption Опция отображения
 * @param modifier Modifier для компонента
 */
@Composable
fun DetailDisplayOptionPicker(
    displayOption: DisplayOption,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall)),
    ) {
        Text(
            text = stringResource(R.string.display_format),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
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
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
