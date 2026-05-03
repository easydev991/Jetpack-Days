package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * DatePicker Dialog.
 * Принимает plain-значения и callback'и вместо MutableState.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DatePickerDialogSection(
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState =
        rememberDatePickerState(
            initialSelectedDateMillis =
                selectedDate
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli()
        )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(
                            Instant
                                .ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                        )
                    }
                }
            ) {
                Text(stringResource(R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    ) {
        DatePicker(
            state = datePickerState,
            showModeToggle = false
        )
    }
}

/**
 * Кнопка сохранения в bottomBar.
 */
@Composable
internal fun SaveButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier =
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(all = dimensionResource(R.dimen.spacing_regular)),
        enabled = enabled
    ) {
        Text(stringResource(R.string.save))
    }
}

// ==================== PREVIEWS ====================

@Preview(showBackground = true, name = "Кнопка сохранения")
@Composable
fun SaveButtonPreview() {
    JetpackDaysTheme {
        SaveButton(enabled = true, onClick = {})
    }
}

@Preview(showBackground = true, name = "Кнопка сохранения (disabled)")
@Composable
fun SaveButtonDisabledPreview() {
    JetpackDaysTheme {
        SaveButton(enabled = false, onClick = {})
    }
}
