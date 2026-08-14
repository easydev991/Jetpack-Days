package com.dayscounter.ui.screens.detail

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.Reminder
import com.dayscounter.domain.usecase.GetDaysAnalysisTextUseCase
import com.dayscounter.ui.viewmodel.DetailScreenState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Контент по состоянию.
 *
 * @param uiState Состояние экрана
 * @param onCopyTitle Колбэк копирования title (передаётся в `DetailContentInner` → `ReadSectionView` для title)
 * @param onCopyDetails Колбэк копирования details (передаётся в `DetailContentInner` → `ReadSectionView` для details)
 * @param getDaysAnalysisTextUseCase Use case для получения текста анализа с префиксом
 * @param modifier Modifier для компонента
 */
@Composable
fun DetailContentByState(
    uiState: DetailScreenState,
    onCopyTitle: () -> Unit,
    onCopyDetails: () -> Unit,
    getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is DetailScreenState.Loading -> {
            LoadingContent(modifier = modifier)
        }

        is DetailScreenState.Success -> {
            DetailContentInner(
                item = uiState.item,
                reminder = uiState.reminder,
                onCopyTitle = onCopyTitle,
                onCopyDetails = onCopyDetails,
                getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase,
                modifier = modifier
            )
        }

        is DetailScreenState.Error -> {
            ErrorContent(
                message = uiState.message,
                modifier = modifier
            )
        }
    }
}

/**
 * Внутренний компонент контента деталей.
 * Структура аналогична iOS (VStack с выравниванием по левому краю).
 *
 * @param item Элемент для отображения
 * @param onCopyTitle Колбэк копирования title (передаётся в `ReadSectionView` для title)
 * @param onCopyDetails Колбэк копирования details (передаётся в `ReadSectionView` для details)
 * @param getDaysAnalysisTextUseCase Use case для получения текста анализа с префиксом
 * @param modifier Modifier для компонента
 */
@Suppress("LongParameterList")
@Composable
internal fun DetailContentInner(
    item: Item,
    reminder: Reminder? = null,
    onCopyTitle: () -> Unit,
    onCopyDetails: () -> Unit,
    getDaysAnalysisTextUseCase: GetDaysAnalysisTextUseCase,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_regular)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium))
    ) {
        ReadSectionView(
            headerText = stringResource(R.string.title),
            bodyText = item.title,
            onCopy = onCopyTitle
        )
        if (item.details.isNotEmpty()) {
            ReadSectionView(
                headerText = stringResource(R.string.details),
                bodyText = item.details,
                onCopy = onCopyDetails
            )
        }
        if (item.colorTag != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_small)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.color_tag),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                ColorTagSection(item.colorTag)
            }
        }
        DetailDatePicker(
            item = item,
            getDaysAnalysisTextUseCase = getDaysAnalysisTextUseCase
        )
        DetailDisplayOptionPicker(displayOption = item.displayOption)
        reminder?.let { activeReminder ->
            UpcomingReminderSection(reminder = activeReminder)
        }
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
                .Color(colorTag)
    ) {}
}

/**
 * Компонент ReadSectionView - аналог iOS ReadSectionView.
 * Отображает заголовок и текст секции с выравниванием по левому краю.
 *
 * При ненулевом `onCopy` тело секции реагирует на длинное нажатие и показывает
 * контекстное меню с пунктом «Скопировать» (см. [R.string.context_menu_copy]).
 * При `onCopy == null` секция не реагирует на жесты и меню не отображается.
 *
 * @param headerText Заголовок секции
 * @param bodyText Текст секции
 * @param onCopy Колбэк «скопировать и показать снекбар» для body-текста.
 *               Если `null` (по умолчанию) — секция не реагирует на жесты.
 * @param modifier Modifier для компонента
 */
@Composable
fun ReadSectionView(
    headerText: String,
    bodyText: String,
    onCopy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val onCopyCallback = onCopy
    var menuVisible by remember { mutableStateOf(false) }
    var menuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var textHeightPx by remember { mutableStateOf(0) }
    val density = LocalDensity.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        Text(
            text = headerText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.fillMaxWidth()
        )
        // ponytail: Material3 1.4 DropdownMenu по умолчанию topToAnchorBottom —
        // menu.top = anchor.bottom + offset.y. Чтобы top-left меню попал в точку жеста,
        // вычитаем высоту Text из offset.y: menu.top = Box.top + touchOffset.y = touch.Y.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { textHeightPx = it.height }
        ) {
            Text(
                text = bodyText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .let { base ->
                            if (onCopyCallback != null) {
                                base.pointerInput(Unit) {
                                    detectTapGestures(
                                        onLongPress = { touchOffset ->
                                            menuOffset =
                                                with(density) {
                                                    DpOffset(
                                                        x = touchOffset.x.toDp(),
                                                        y = (touchOffset.y - textHeightPx).toDp()
                                                    )
                                                }
                                            menuVisible = true
                                        }
                                    )
                                }
                            } else {
                                base
                            }
                        }
            )
            if (onCopyCallback != null) {
                DropdownMenu(
                    expanded = menuVisible,
                    offset = menuOffset,
                    onDismissRequest = { menuVisible = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.context_menu_copy)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null
                            )
                        },
                        onClick = {
                            menuVisible = false
                            onCopyCallback()
                        }
                    )
                }
            }
        }
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
    modifier: Modifier = Modifier
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
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        Text(
            text = stringResource(R.string.date),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = daysAnalysisText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
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
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_xsmall))
    ) {
        Text(
            text = stringResource(R.string.display_format),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
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
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun UpcomingReminderSection(
    reminder: Reminder,
    modifier: Modifier = Modifier
) {
    val formatter =
        remember {
            DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(Locale.getDefault())
        }
    val formattedDateTime =
        remember(reminder.targetEpochMillis) {
            Instant
                .ofEpochMilli(reminder.targetEpochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime()
                .format(formatter)
        }

    ReadSectionView(
        headerText = stringResource(R.string.reminder_settings),
        bodyText = formattedDateTime,
        modifier = modifier
    )
}
