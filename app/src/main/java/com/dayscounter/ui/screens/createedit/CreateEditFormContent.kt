package com.dayscounter.ui.screens.createedit

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.ui.screens.common.DaysRadioButton
import com.dayscounter.ui.viewmodel.CreateEditScreenState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * TopAppBar для экрана создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateEditTopAppBar(
    itemId: Long?,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text =
                    if (itemId != null) {
                        stringResource(R.string.edit_item)
                    } else {
                        stringResource(R.string.new_item)
                    },
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
    )
}

/**
 * Основные секции формы (название, детали, дата).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFormSections(
    params: CreateEditFormParams,
    onValueChange: () -> Unit
) {
    TitleSection(
        title = params.uiStates.title,
        onValueChange = { onValueChange() }
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
    DetailsSection(
        details = params.uiStates.details,
        onValueChange = { onValueChange() }
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
    DateSection(
        selectedDate = params.uiStates.selectedDate,
        showDatePicker = params.showDatePicker
    )
}

/**
 * Секция выбора цвета и опции отображения.
 */
@Composable
private fun ColorAndDisplayOptionSection(
    params: CreateEditFormParams,
    onValueChange: () -> Unit
) {
    ColorSelector(
        selectedColor = params.uiStates.selectedColor,
        onValueChange = onValueChange
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
    DisplayOptionSelector(
        selectedDisplayOption = params.uiStates.selectedDisplayOption,
        onValueChange = onValueChange
    )
}

/**
 * Контент формы создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreateEditFormContent(params: CreateEditFormParams) {
    val reminderEnabled = rememberSaveable { mutableStateOf(false) }
    val reminderMode = rememberSaveable { mutableStateOf(ReminderMode.ON_DATE) }
    val reminderDate =
        rememberSaveable(stateSaver = NullableLocalDateSaver) {
            mutableStateOf(LocalDate.now())
        }
    val showReminderDatePicker = rememberSaveable { mutableStateOf(false) }
    val reminderHour = rememberSaveable { mutableStateOf(LocalTime.now().hour) }
    val reminderMinute = rememberSaveable { mutableStateOf(LocalTime.now().minute) }
    val reminderAfterValue = rememberSaveable { mutableStateOf("") }
    val reminderAfterUnit = rememberSaveable { mutableStateOf(ReminderAfterUnit.DAYS) }

    // Функция для отслеживания изменений
    val onValueChange: () -> Unit = {
        if (params.itemId != null) {
            val timestamp =
                params.uiStates.selectedDate.value
                    ?.atStartOfDay(java.time.ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: 0L

            params.viewModel.checkHasChanges(
                title = params.uiStates.title.value,
                details = params.uiStates.details.value,
                timestamp = timestamp,
                colorTag =
                    params.uiStates.selectedColor.value
                        ?.toArgb(),
                displayOption = params.uiStates.selectedDisplayOption.value
            )
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(params.paddingValues)
                .imeNestedScroll()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_regular))
    ) {
        MainFormSections(params, onValueChange)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        ColorAndDisplayOptionSection(params, onValueChange)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        ReminderSettingsSection(
            reminderEnabled = reminderEnabled,
            reminderMode = reminderMode,
            reminderDate = reminderDate,
            showReminderDatePicker = showReminderDatePicker,
            reminderHour = reminderHour,
            reminderMinute = reminderMinute,
            reminderAfterValue = reminderAfterValue,
            reminderAfterUnit = reminderAfterUnit,
            onValueChange = onValueChange
        )
    }

    if (showReminderDatePicker.value) {
        DatePickerDialogSection(
            selectedDate = reminderDate,
            showDatePicker = showReminderDatePicker
        )
    }
}

private enum class ReminderMode {
    ON_DATE,
    AFTER_INTERVAL
}

private enum class ReminderAfterUnit {
    DAYS,
    WEEKS,
    MONTHS,
    YEARS
}

@Composable
private fun ReminderSettingsSection(
    reminderEnabled: MutableState<Boolean>,
    reminderMode: MutableState<ReminderMode>,
    reminderDate: MutableState<LocalDate?>,
    showReminderDatePicker: MutableState<Boolean>,
    reminderHour: MutableState<Int>,
    reminderMinute: MutableState<Int>,
    reminderAfterValue: MutableState<String>,
    reminderAfterUnit: MutableState<ReminderAfterUnit>,
    onValueChange: () -> Unit
) {
    Text(
        text = stringResource(R.string.reminder_settings),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.add_reminder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = reminderEnabled.value,
            onCheckedChange = {
                reminderEnabled.value = it
                onValueChange()
            }
        )
    }

    if (!reminderEnabled.value) {
        return
    }

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
    Column {
        DaysRadioButton(
            text = stringResource(R.string.reminder_mode_on_date),
            selected = reminderMode.value == ReminderMode.ON_DATE,
            onClick = {
                reminderMode.value = ReminderMode.ON_DATE
                onValueChange()
            }
        )
        DaysRadioButton(
            text = stringResource(R.string.reminder_mode_after),
            selected = reminderMode.value == ReminderMode.AFTER_INTERVAL,
            onClick = {
                reminderMode.value = ReminderMode.AFTER_INTERVAL
                onValueChange()
            }
        )
    }

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
    when (reminderMode.value) {
        ReminderMode.ON_DATE -> {
            ReminderDateTimeSection(
                reminderDate = reminderDate,
                showReminderDatePicker = showReminderDatePicker,
                reminderHour = reminderHour,
                reminderMinute = reminderMinute,
                onValueChange = onValueChange
            )
        }

        ReminderMode.AFTER_INTERVAL -> {
            ReminderAfterSection(
                reminderAfterValue = reminderAfterValue,
                reminderAfterUnit = reminderAfterUnit,
                onValueChange = onValueChange
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderDateTimeSection(
    reminderDate: MutableState<LocalDate?>,
    showReminderDatePicker: MutableState<Boolean>,
    reminderHour: MutableState<Int>,
    reminderMinute: MutableState<Int>,
    onValueChange: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val formatter =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(configuration.locales[0])

    OutlinedTextField(
        value = reminderDate.value?.format(formatter) ?: "",
        onValueChange = {},
        label = { Text(stringResource(R.string.reminder_date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { showReminderDatePicker.value = true }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
    ReminderTimeField(
        reminderHour = reminderHour,
        reminderMinute = reminderMinute,
        onValueChange = onValueChange
    )
}

@Composable
private fun ReminderTimeField(
    reminderHour: MutableState<Int>,
    reminderMinute: MutableState<Int>,
    onValueChange: () -> Unit
) {
    val context = LocalContext.current
    val reminderTime =
        remember(reminderHour.value, reminderMinute.value) {
            String.format(Locale.getDefault(), "%02d:%02d", reminderHour.value, reminderMinute.value)
        }

    OutlinedTextField(
        value = reminderTime,
        onValueChange = {},
        label = { Text(stringResource(R.string.reminder_time)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            reminderHour.value = hour
                            reminderMinute.value = minute
                            onValueChange()
                        },
                        reminderHour.value,
                        reminderMinute.value,
                        true
                    ).show()
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = stringResource(R.string.select_time)
                )
            }
        }
    )
}

@Composable
private fun ReminderAfterSection(
    reminderAfterValue: MutableState<String>,
    reminderAfterUnit: MutableState<ReminderAfterUnit>,
    onValueChange: () -> Unit
) {
    val showUnitsMenu = remember { mutableStateOf(false) }
    OutlinedTextField(
        value = reminderAfterValue.value,
        onValueChange = { newValue ->
            val onlyDigits = newValue.filter { it.isDigit() }
            if (onlyDigits != reminderAfterValue.value) {
                reminderAfterValue.value = onlyDigits
                onValueChange()
            }
        },
        label = { Text(stringResource(R.string.remind_after_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = reminderAfterUnit.value.toDisplayText(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reminder_period_unit)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { showUnitsMenu.value = true },
            trailingIcon = {
                IconButton(onClick = { showUnitsMenu.value = true }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.select_period_unit)
                    )
                }
            }
        )
        DropdownMenu(
            expanded = showUnitsMenu.value,
            onDismissRequest = { showUnitsMenu.value = false }
        ) {
            ReminderAfterUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.toDisplayText()) },
                    onClick = {
                        reminderAfterUnit.value = unit
                        showUnitsMenu.value = false
                        onValueChange()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderAfterUnit.toDisplayText(): String =
    when (this) {
        ReminderAfterUnit.DAYS -> stringResource(R.string.reminder_unit_days)
        ReminderAfterUnit.WEEKS -> stringResource(R.string.reminder_unit_weeks)
        ReminderAfterUnit.MONTHS -> stringResource(R.string.reminder_unit_months)
        ReminderAfterUnit.YEARS -> stringResource(R.string.reminder_unit_years)
    }

/**
 * Секция с заголовком.
 */
@Composable
internal fun TitleSection(
    title: MutableState<String>,
    onValueChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = title.value,
        onValueChange = {
            title.value = it
            onValueChange(it)
        },
        label = { Text(stringResource(R.string.title)) },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Секция с деталями.
 */
@Composable
internal fun DetailsSection(
    details: MutableState<String>,
    onValueChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = details.value,
        onValueChange = {
            details.value = it
            onValueChange(it)
        },
        label = { Text(stringResource(R.string.details)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
}

/**
 * Секция с датой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateSection(
    selectedDate: MutableState<java.time.LocalDate?>,
    showDatePicker: MutableState<Boolean>
) {
    val configuration = LocalConfiguration.current
    val formatter =
        java.time.format.DateTimeFormatter
            .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
            .withLocale(configuration.locales[0])
    // Выбор даты
    OutlinedTextField(
        value =
            selectedDate.value?.format(formatter) ?: "",
        onValueChange = { },
        label = { Text(stringResource(R.string.date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(
                onClick = {
                    showDatePicker.value = true
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )
}

/**
 * Создает состояния UI.
 */
@Composable
internal fun rememberCreateEditUiStates(): CreateEditUiState =
    CreateEditUiState(
        title = rememberSaveable { mutableStateOf("") },
        details = rememberSaveable { mutableStateOf("") },
        selectedDate = rememberSaveable(stateSaver = NullableLocalDateSaver) { mutableStateOf(null) },
        selectedColor = rememberSaveable(stateSaver = NullableColorSaver) { mutableStateOf(null) },
        selectedDisplayOption =
            rememberSaveable(stateSaver = DisplayOptionSaver) {
                mutableStateOf(
                    DisplayOption.DAY
                )
            }
    )

/**
 * Загружает данные при редактировании.
 */
fun loadItemData(
    itemId: Long?,
    uiState: CreateEditScreenState,
    uiStates: CreateEditUiState
) {
    val isEditingExistingItem = itemId != null
    val isStateSuccess = uiState is CreateEditScreenState.Success
    val isTitleEmpty = uiStates.title.value.isEmpty()

    if (isEditingExistingItem && isStateSuccess && isTitleEmpty) {
        val item = uiState.item
        uiStates.title.value = item.title
        uiStates.details.value = item.details
        uiStates.selectedDate.value =
            java.time.Instant
                .ofEpochMilli(item.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        uiStates.selectedColor.value = item.colorTag?.let { Color(it) }
        uiStates.selectedDisplayOption.value = item.displayOption
    }
}
