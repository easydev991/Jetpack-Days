package com.dayscounter.ui.screen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.ui.component.DaysCountTextStyle
import com.dayscounter.ui.component.daysCountText
import com.dayscounter.ui.util.NumberFormattingUtils
import com.dayscounter.ui.screen.CreateEditUiState as ScreenCreateEditUiState
import com.dayscounter.viewmodel.CreateEditScreenViewModel

/**
 * TopAppBar для экрана создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun createEditTopAppBar(
    itemId: Long?,
    onBackClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Text(
                text =
                    if (itemId != null) {
                        stringResource(R.string.new_item)
                    } else {
                        stringResource(R.string.edit_item)
                    },
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

/**
 * Контент формы создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun createEditFormContent(
    itemId: Long?,
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    uiStates: ScreenCreateEditUiState,
    showDatePicker: androidx.compose.runtime.MutableState<Boolean>,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_extra_large)),
    ) {
        titleSection(title = uiStates.title)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
        detailsSection(details = uiStates.details)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
        dateSection(selectedDate = uiStates.selectedDate, showDatePicker = showDatePicker)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        // Предпросмотр дней
        if (uiStates.selectedDate.value != null) {
            previewDaysContent(selectedDate = uiStates.selectedDate.value)
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
        }

        // Выбор цвета
        colorSelector(
            selectedColor = uiStates.selectedColor,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        // Опция отображения
        displayOptionSelector(selectedDisplayOption = uiStates.selectedDisplayOption)

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_huge)))

        // Кнопки
        buttonsSection(
            uiStates = uiStates,
            itemId = itemId,
            viewModel = viewModel,
            onBackClick = onBackClick,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
    }
}

/**
 * Секция с заголовком.
 */
@Composable
internal fun titleSection(title: androidx.compose.runtime.MutableState<String>) {
    OutlinedTextField(
        value = title.value,
        onValueChange = { title.value = it },
        label = { Text(stringResource(R.string.title)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Секция с деталями.
 */
@Composable
internal fun detailsSection(details: androidx.compose.runtime.MutableState<String>) {
    OutlinedTextField(
        value = details.value,
        onValueChange = { details.value = it },
        label = { Text(stringResource(R.string.details)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
}

/**
 * Секция с датой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun dateSection(
    selectedDate: androidx.compose.runtime.MutableState<java.time.LocalDate?>,
    showDatePicker: androidx.compose.runtime.MutableState<Boolean>,
) {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale("ru"))
    // Выбор даты
    OutlinedTextField(
        value =
            selectedDate.value?.format(formatter) ?: "",
        onValueChange = { },
        label = { Text(stringResource(R.string.date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showDatePicker.value = true }) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Выбрать дату",
                )
            }
        },
    )
}

/**
 * Предпросмотр количества дней.
 */
@Composable
internal fun previewDaysContent(selectedDate: java.time.LocalDate) {
    previewDaysContentInner(selectedDate = selectedDate)
}

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

/**
 * Селектор цвета.
 */
@Composable
internal fun colorSelector(selectedColor: androidx.compose.runtime.MutableState<Int?>) {
    val colors =
        listOf(
            colorResource(R.color.color_primary_red),
            colorResource(R.color.color_primary_teal),
            colorResource(R.color.color_primary_blue),
            colorResource(R.color.color_primary_green),
            colorResource(R.color.color_primary_yellow),
            colorResource(R.color.color_primary_purple),
        )

    Text(
        text = stringResource(R.string.color_tag),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_large)),
    ) {
        colors.forEach { color ->
            colorOptionSurface(
                color = color,
                selectedColor = selectedColor,
            )
        }

        // Опция без цвета
        noColorOptionSurface(selectedColor = selectedColor)
    }
}

/**
 * Поверхность для выбора цвета.
 */
@Composable
internal fun colorOptionSurface(
    color: Int,
    selectedColor: androidx.compose.runtime.MutableState<Int?>,
) {
    Surface(
        onClick = {
            if (selectedColor.value == color) {
                selectedColor.value = null
            } else {
                selectedColor.value = color
            }
        },
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size))
                .padding(dimensionResource(R.dimen.spacing_small)),
        shape = CircleShape,
        color = color,
        border =
            if (selectedColor.value == color) {
                OutlinedTextFieldDefaults.borderStroke(
                    focused = true,
                    isError = false,
                )
            } else {
                null
            },
    ) {}
}

/**
 * Поверхность для опции без цвета.
 */
@Composable
internal fun noColorOptionSurface(selectedColor: androidx.compose.runtime.MutableState<Int?>) {
    Surface(
        onClick = { selectedColor.value = null },
        modifier =
            Modifier
                .size(dimensionResource(R.dimen.color_tag_size))
                .padding(dimensionResource(R.dimen.spacing_small)),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border =
            if (selectedColor.value == null) {
                OutlinedTextFieldDefaults.borderStroke(
                    focused = true,
                    isError = false,
                )
            } else {
                null
            },
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "—",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Селектор опции отображения.
 */
@Composable
internal fun displayOptionSelector(
    selectedDisplayOption: androidx.compose.runtime.MutableState<com.dayscounter.domain.model.DisplayOption>,
) {
    Text(
        text = stringResource(R.string.display_format),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_medium)))

    com.dayscounter.domain.model.DisplayOption.values().forEach { option ->
        displayOptionSurface(
            option = option,
            selectedDisplayOption = selectedDisplayOption,
        )
    }
}

/**
 * Поверхность для опции отображения.
 */
@Composable
internal fun displayOptionSurface(
    option: com.dayscounter.domain.model.DisplayOption,
    selectedDisplayOption: androidx.compose.runtime.MutableState<com.dayscounter.domain.model.DisplayOption>,
) {
    Surface(
        onClick = { selectedDisplayOption.value = option },
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(R.dimen.spacing_small)),
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(R.dimen.spacing_large)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val isSelected = selectedDisplayOption.value == option
            val text =
                when (option) {
                    com.dayscounter.domain.model.DisplayOption.DAY -> stringResource(R.string.days_only)
                    com.dayscounter.domain.model.DisplayOption.MONTH_DAY -> stringResource(R.string.months_and_days)
                    com.dayscounter.domain.model.DisplayOption.YEAR_MONTH_DAY ->
                        stringResource(R.string.years_months_and_days)
                    com.dayscounter.domain.model.DisplayOption.DEFAULT ->
                        stringResource(R.string.days_only)
                }

            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            if (isSelected) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Кнопки действий.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun buttonsSection(
    uiStates: ScreenCreateEditUiState,
    itemId: Long?,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
) {
    // Кнопки
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier.weight(1f),
        ) {
            Text(stringResource(R.string.cancel))
        }
        saveButton(
            uiStates = uiStates,
            itemId = itemId,
            viewModel = viewModel,
            onBackClick = onBackClick,
        )
    }
}

/**
 * Кнопка сохранения.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun saveButton(
    uiStates: ScreenCreateEditUiState,
    itemId: Long?,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
) {
    Button(
        onClick = {
            if (uiStates.title.value.isNotEmpty() && uiStates.selectedDate.value != null) {
                val timestamp =
                    uiStates.selectedDate.value
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli() ?: System.currentTimeMillis()

                val item =
                    com.dayscounter.domain.model.Item(
                        id = itemId ?: 0L,
                        title = uiStates.title.value,
                        details = uiStates.details.value,
                        timestamp = timestamp,
                        colorTag = uiStates.selectedColor.value,
                        displayOption = uiStates.selectedDisplayOption.value,
                    )

                if (itemId != null) {
                    viewModel.updateItem(item.copy(id = itemId))
                } else {
                    viewModel.createItem(item)
                }
                onBackClick()
            }
        },
        modifier = Modifier.weight(1f),
        enabled = uiStates.title.value.isNotEmpty() && uiStates.selectedDate.value != null,
    ) {
        Text(stringResource(R.string.save))
    }
}

/**
 * DatePicker Dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun datePickerDialogSection(
    selectedDate: androidx.compose.runtime.MutableState<java.time.LocalDate?>,
    showDatePicker: androidx.compose.runtime.MutableState<Boolean>,
) {
    if (showDatePicker.value) {
        val datePickerState =
            rememberDatePickerState(
                initialSelectedDateMillis =
                    selectedDate.value
                        ?.atStartOfDay(java.time.ZoneId.systemDefault())
                        ?.toInstant()
                        ?.toEpochMilli(),
            )

        DatePickerDialog(
            onDismissRequest = { showDatePicker.value = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate.value =
                                java.time.Instant.ofEpochMilli(millis)
                                    .atZone(java.time.ZoneId.systemDefault())
                                    .toLocalDate()
                        }
                        showDatePicker.value = false
                    },
                ) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker.value = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        ) {
            DatePicker(
                state = datePickerState,
                showModeToggle = false,
            )
        }
    }
}

/**
 * Создает состояния UI.
 */
@Composable
internal fun rememberCreateEditUiStates(): ScreenCreateEditUiState {
    return androidx.compose.runtime.remember {
        ScreenCreateEditUiState(
            title = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") },
            details = androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") },
            selectedDate = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<java.time.LocalDate?>(null) },
            showDatePicker = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) },
            selectedColor = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Int?>(null) },
            selectedDisplayOption = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(com.dayscounter.domain.model.DisplayOption.DAY) },
        )
    }
}

/**
 * Загружает данные при редактировании.
 */
fun loadItemData(
    itemId: Long?,
    uiState: com.dayscounter.viewmodel.CreateEditScreenState,
    uiStates: ScreenCreateEditUiState,
) {
    if (itemId != null && uiState is com.dayscounter.viewmodel.CreateEditScreenState.Success && uiStates.title.value.isEmpty()) {
        val item = (uiState as com.dayscounter.viewmodel.CreateEditScreenState.Success).item
        uiStates.title.value = item.title
        uiStates.details.value = item.details
        uiStates.selectedDate.value =
            java.time.Instant.ofEpochMilli(item.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        uiStates.selectedColor.value = item.colorTag
        uiStates.selectedDisplayOption.value = item.displayOption
    }
}
