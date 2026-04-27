package com.dayscounter.ui.screens.createedit

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dayscounter.R
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.ui.screens.common.DaysRadioButton
import com.dayscounter.ui.theme.JetpackDaysTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val PREVIEW_YEAR = 2026
private const val PREVIEW_MONTH = 4
private const val PREVIEW_DAY = 27
private const val PREVIEW_HOUR = 16
private const val PREVIEW_MINUTE = 8
private const val PREVIEW_INTERVAL = "3"

@Composable
internal fun ReminderSettingsSection(
    reminderUiState: ReminderFormUiState,
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.add_reminder),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = reminderUiState.isEnabled.value,
            onCheckedChange = {
                reminderUiState.isEnabled.value = it
                onValueChange()
            }
        )
    }

    if (!reminderUiState.isEnabled.value) {
        return
    }

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

    DaysRadioButton(
        text = stringResource(R.string.reminder_mode_on_date),
        selected = reminderUiState.mode.value == ReminderMode.AT_DATE,
        onClick = {
            reminderUiState.mode.value = ReminderMode.AT_DATE
            onValueChange()
        }
    )
    DaysRadioButton(
        text = stringResource(R.string.reminder_mode_after),
        selected = reminderUiState.mode.value == ReminderMode.AFTER_INTERVAL,
        onClick = {
            reminderUiState.mode.value = ReminderMode.AFTER_INTERVAL
            onValueChange()
        }
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

    when (reminderUiState.mode.value) {
        ReminderMode.AT_DATE -> {
            ReminderDateTimeSection(reminderUiState = reminderUiState, onValueChange = onValueChange)
        }

        ReminderMode.AFTER_INTERVAL -> {
            ReminderAfterSection(reminderUiState = reminderUiState, onValueChange = onValueChange)
        }
    }
}

@Composable
private fun ReminderDateTimeSection(
    reminderUiState: ReminderFormUiState,
    onValueChange: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val formatter =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(configuration.locales[0])

    OutlinedTextField(
        value =
            reminderUiState.selectedDate.value
                ?.format(formatter)
                .orEmpty(),
        onValueChange = {},
        label = { Text(stringResource(R.string.reminder_date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { reminderUiState.showDatePicker.value = true }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
    ReminderTimeField(reminderUiState = reminderUiState, onValueChange = onValueChange)
}

@Composable
private fun ReminderTimeField(
    reminderUiState: ReminderFormUiState,
    onValueChange: () -> Unit
) {
    val context = LocalContext.current
    val reminderTime =
        remember(reminderUiState.hour.value, reminderUiState.minute.value) {
            String.format(Locale.getDefault(), "%02d:%02d", reminderUiState.hour.value, reminderUiState.minute.value)
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
                            reminderUiState.hour.value = hour
                            reminderUiState.minute.value = minute
                            onValueChange()
                        },
                        reminderUiState.hour.value,
                        reminderUiState.minute.value,
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
    reminderUiState: ReminderFormUiState,
    onValueChange: () -> Unit
) {
    val showUnitsMenu = remember { mutableStateOf(false) }

    OutlinedTextField(
        value = reminderUiState.intervalValue.value,
        onValueChange = { newValue ->
            val onlyDigits = newValue.filter { it.isDigit() }
            if (onlyDigits != reminderUiState.intervalValue.value) {
                reminderUiState.intervalValue.value = onlyDigits
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
            value = reminderUiState.intervalUnit.value.toDisplayText(),
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
            ReminderIntervalUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.toDisplayText()) },
                    onClick = {
                        reminderUiState.intervalUnit.value = unit
                        showUnitsMenu.value = false
                        onValueChange()
                    }
                )
            }
        }
    }
}

@Composable
private fun ReminderIntervalUnit.toDisplayText(): String =
    when (this) {
        ReminderIntervalUnit.DAY -> stringResource(R.string.reminder_unit_days)
        ReminderIntervalUnit.WEEK -> stringResource(R.string.reminder_unit_weeks)
        ReminderIntervalUnit.MONTH -> stringResource(R.string.reminder_unit_months)
        ReminderIntervalUnit.YEAR -> stringResource(R.string.reminder_unit_years)
    }

@Preview(showBackground = true, name = "Reminder Section")
@Composable
internal fun ReminderSettingsSectionPreview() {
    JetpackDaysTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ReminderSettingsSection(
                reminderUiState =
                    ReminderFormUiState(
                        isEnabled = remember { mutableStateOf(true) },
                        mode = remember { mutableStateOf(ReminderMode.AT_DATE) },
                        selectedDate =
                            remember {
                                mutableStateOf(LocalDate.of(PREVIEW_YEAR, PREVIEW_MONTH, PREVIEW_DAY))
                            },
                        showDatePicker = remember { mutableStateOf(false) },
                        hour = remember { mutableStateOf(PREVIEW_HOUR) },
                        minute = remember { mutableStateOf(PREVIEW_MINUTE) },
                        intervalValue = remember { mutableStateOf(PREVIEW_INTERVAL) },
                        intervalUnit = remember { mutableStateOf(ReminderIntervalUnit.DAY) }
                    ),
                onValueChange = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Reminder Section Disabled")
@Composable
internal fun ReminderSettingsSectionDisabledPreview() {
    JetpackDaysTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            ReminderSettingsSection(
                reminderUiState =
                    ReminderFormUiState(
                        isEnabled = remember { mutableStateOf(false) },
                        mode = remember { mutableStateOf(ReminderMode.AT_DATE) },
                        selectedDate =
                            remember {
                                mutableStateOf(LocalDate.of(PREVIEW_YEAR, PREVIEW_MONTH, PREVIEW_DAY))
                            },
                        showDatePicker = remember { mutableStateOf(false) },
                        hour = remember { mutableStateOf(PREVIEW_HOUR) },
                        minute = remember { mutableStateOf(PREVIEW_MINUTE) },
                        intervalValue = remember { mutableStateOf(PREVIEW_INTERVAL) },
                        intervalUnit = remember { mutableStateOf(ReminderIntervalUnit.DAY) }
                    ),
                onValueChange = {}
            )
        }
    }
}
