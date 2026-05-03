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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private const val PREVIEW_YEAR = 2026
private const val PREVIEW_MONTH = 4
private const val PREVIEW_DAY = 27
private const val PREVIEW_HOUR = 16
private const val PREVIEW_MINUTE = 8
private const val PREVIEW_INTERVAL = "3"
internal const val REMINDER_TOGGLE_TEST_TAG = "reminder_toggle"

/**
 * Секция настроек напоминания.
 * Принимает plain-значения и callback'и вместо MutableState.
 */
@Composable
internal fun ReminderSettingsSection(
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit,
    onReminderToggleRequested: ((Boolean) -> Unit)? = null,
    expandedContentModifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.reminder_settings),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
    ReminderToggleRow(
        reminder = reminder,
        onReminderChange = onReminderChange,
        onReminderToggleRequested = onReminderToggleRequested
    )

    if (!reminder.isEnabled) {
        return
    }

    ReminderExpandedContent(
        reminder = reminder,
        onReminderChange = onReminderChange,
        expandedContentModifier = expandedContentModifier
    )
}

@Composable
private fun ReminderToggleRow(
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit,
    onReminderToggleRequested: ((Boolean) -> Unit)?
) {
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
            modifier = Modifier.testTag(REMINDER_TOGGLE_TEST_TAG),
            checked = reminder.isEnabled,
            onCheckedChange = {
                onReminderToggleRequested?.invoke(it) ?: run {
                    onReminderChange(reminder.copy(isEnabled = it))
                }
            }
        )
    }
}

@Composable
private fun ReminderExpandedContent(
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit,
    expandedContentModifier: Modifier
) {
    val validationErrorResId = reminder.validationErrorResId(currentDateTime = LocalDateTime.now())

    Column(modifier = expandedContentModifier) {
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

        DaysRadioButton(
            text = stringResource(R.string.reminder_mode_on_date),
            selected = reminder.mode == ReminderMode.AT_DATE,
            onClick = {
                onReminderChange(reminder.copy(mode = ReminderMode.AT_DATE))
            }
        )
        DaysRadioButton(
            text = stringResource(R.string.reminder_mode_after),
            selected = reminder.mode == ReminderMode.AFTER_INTERVAL,
            onClick = {
                onReminderChange(reminder.copy(mode = ReminderMode.AFTER_INTERVAL))
            }
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

        when (reminder.mode) {
            ReminderMode.AT_DATE -> {
                ReminderDateTimeSection(reminder = reminder, onReminderChange = onReminderChange)
            }

            ReminderMode.AFTER_INTERVAL -> {
                ReminderAfterSection(reminder = reminder, onReminderChange = onReminderChange)
            }
        }

        if (validationErrorResId != null) {
            Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall)))
            Text(
                text = stringResource(validationErrorResId),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ReminderDateTimeSection(
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit
) {
    val configuration = LocalConfiguration.current
    val formatter =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(configuration.locales[0])

    OutlinedTextField(
        value =
            reminder.selectedDate
                ?.format(formatter)
                .orEmpty(),
        onValueChange = {},
        label = { Text(stringResource(R.string.reminder_date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { onReminderChange(reminder.copy(showDatePicker = true)) }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))
    ReminderTimeField(reminder = reminder, onReminderChange = onReminderChange)
}

@Composable
private fun ReminderTimeField(
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit
) {
    val context = LocalContext.current
    val reminderTime =
        remember(reminder.hour, reminder.minute) {
            String.format(Locale.getDefault(), "%02d:%02d", reminder.hour, reminder.minute)
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
                            onReminderChange(reminder.copy(hour = hour, minute = minute))
                        },
                        reminder.hour,
                        reminder.minute,
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
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit
) {
    var showUnitsMenu by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = reminder.intervalValue,
        onValueChange = { newValue ->
            val onlyDigits = newValue.filter { it.isDigit() }
            if (onlyDigits != reminder.intervalValue) {
                onReminderChange(reminder.copy(intervalValue = onlyDigits))
            }
        },
        label = { Text(stringResource(R.string.remind_after_label)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_small)))

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = reminder.intervalUnit.toDisplayText(),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.reminder_period_unit)) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { showUnitsMenu = true },
            trailingIcon = {
                IconButton(onClick = { showUnitsMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = stringResource(R.string.select_period_unit)
                    )
                }
            }
        )

        DropdownMenu(
            expanded = showUnitsMenu,
            onDismissRequest = { showUnitsMenu = false }
        ) {
            ReminderIntervalUnit.entries.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.toDisplayText()) },
                    onClick = {
                        onReminderChange(reminder.copy(intervalUnit = unit))
                        showUnitsMenu = false
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
        var reminder by remember {
            mutableStateOf(
                ReminderFormUiState(
                    isEnabled = true,
                    mode = ReminderMode.AT_DATE,
                    selectedDate = LocalDate.of(PREVIEW_YEAR, PREVIEW_MONTH, PREVIEW_DAY),
                    hour = PREVIEW_HOUR,
                    minute = PREVIEW_MINUTE,
                    intervalValue = PREVIEW_INTERVAL,
                    intervalUnit = ReminderIntervalUnit.DAY
                )
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            ReminderSettingsSection(
                reminder = reminder,
                onReminderChange = { reminder = it }
            )
        }
    }
}

@Preview(showBackground = true, name = "Reminder Section Disabled")
@Composable
internal fun ReminderSettingsSectionDisabledPreview() {
    JetpackDaysTheme {
        var reminder by remember {
            mutableStateOf(
                ReminderFormUiState(
                    isEnabled = false,
                    mode = ReminderMode.AT_DATE,
                    selectedDate = LocalDate.of(PREVIEW_YEAR, PREVIEW_MONTH, PREVIEW_DAY),
                    hour = PREVIEW_HOUR,
                    minute = PREVIEW_MINUTE,
                    intervalValue = PREVIEW_INTERVAL,
                    intervalUnit = ReminderIntervalUnit.DAY
                )
            )
        }
        Column(modifier = Modifier.padding(16.dp)) {
            ReminderSettingsSection(
                reminder = reminder,
                onReminderChange = { reminder = it }
            )
        }
    }
}
