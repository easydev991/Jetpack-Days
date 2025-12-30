package com.dayscounter.ui.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.dayscounter.R
import com.dayscounter.ui.component.DaysCountTextStyle
import com.dayscounter.ui.component.daysCountText
import com.dayscounter.ui.util.NumberFormattingUtils
import com.dayscounter.viewmodel.DetailScreenState
import java.time.LocalDate

/**
 * TopAppBar для экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun detailTopAppBar(
    uiState: DetailScreenState,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
    itemId: Long,
) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.details),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.close),
                )
            }
        },
        actions = {
            if (uiState is DetailScreenState.Success) {
                DetailActionButtons(onEditClick = onEditClick, itemId = itemId)
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

/**
 * Кнопки действий в TopAppBar.
 */
@Composable
internal fun DetailActionButtons(
    onEditClick: (Long) -> Unit,
    itemId: Long,
) {
    IconButton(onClick = { onEditClick(itemId) }) {
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = stringResource(R.string.edit),
        )
    }
    IconButton(onClick = { }) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = stringResource(R.string.delete),
        )
    }
}

/**
 * Контент по состоянию.
 */
@Composable
fun detailContentByState(
    uiState: DetailScreenState,
    modifier: Modifier = Modifier,
) {
    when (val state = uiState) {
        is DetailScreenState.Loading -> {
            loadingContent(modifier = modifier)
        }
        is DetailScreenState.Success -> {
            DetailContentInner(
                item = state.item,
                modifier = modifier,
            )
        }
        is DetailScreenState.Error -> {
            errorContent(
                message = state.message,
                modifier = modifier,
            )
        }
        is DetailScreenState.Deleted -> {
            deletedContent(modifier = modifier)
        }
    }
}

/**
 * Внутренний компонент контента деталей.
 */
@Composable
internal fun DetailContentInner(
    item: com.dayscounter.domain.model.Item,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru"))

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
        DateSection(item.timestamp, formatter)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_massive)))

        // Количество дней
        daysCountSection(item)

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
        color = androidx.compose.ui.graphics.Color(colorTag),
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
internal fun DateSection(
    timestamp: Long,
    formatter: java.time.format.DateTimeFormatter,
) {
    val eventDate =
        java.time.Instant.ofEpochMilli(timestamp)
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
fun daysCountSection(item: com.dayscounter.domain.model.Item) {
    val eventDate =
        java.time.Instant.ofEpochMilli(item.timestamp)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
    val currentDate = LocalDate.now()
    val daysText = calculateDaysText(eventDate, currentDate)

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
): String {
    return if (eventDate == currentDate) {
        "Сегодня"
    } else {
        val totalDays = java.time.temporal.ChronoUnit.DAYS.between(eventDate, currentDate).toInt()
        NumberFormattingUtils.formatDaysCount(totalDays)
    }
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
fun displayOptionInfoSection(displayOption: com.dayscounter.domain.model.DisplayOption) {
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
                        com.dayscounter.domain.model.DisplayOption.DAY -> stringResource(R.string.days_only)
                        com.dayscounter.domain.model.DisplayOption.MONTH_DAY -> stringResource(R.string.months_and_days)
                        com.dayscounter.domain.model.DisplayOption.YEAR_MONTH_DAY ->
                            stringResource(R.string.years_months_and_days)
                        com.dayscounter.domain.model.DisplayOption.DEFAULT ->
                            stringResource(R.string.days_only)
                    },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/**
 * Контент при загрузке.
 */
@Composable
internal fun loadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Загрузка...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Контент при ошибке.
 */
@Composable
internal fun errorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacing_huge)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Контент после удаления.
 */
@Composable
internal fun deletedContent(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacing_huge)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Событие удалено",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_huge)))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primary,
            onClick = modifier,
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(dimensionResource(R.dimen.spacing_extra_large)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Назад",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

