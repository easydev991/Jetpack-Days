package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.domain.model.ReminderIntervalUnit
import com.dayscounter.domain.model.ReminderMode
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime

@RunWith(AndroidJUnit4::class)
class CreateEditSaveValidationUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun savebutton_whenReminderEnabledAndIntervalInvalid_thenDisabled() {
        val reminderState =
            ReminderFormUiState(
                isEnabled = mutableStateOf(true),
                mode = mutableStateOf(ReminderMode.AFTER_INTERVAL),
                selectedDate = mutableStateOf(LocalDate.of(2026, 5, 2)),
                showDatePicker = mutableStateOf(false),
                hour = mutableStateOf(12),
                minute = mutableStateOf(0),
                intervalValue = mutableStateOf(""),
                intervalUnit = mutableStateOf(ReminderIntervalUnit.DAY)
            )

        composeTestRule.setContent {
            JetpackDaysTheme {
                val isValid =
                    isCreateEditFormValid(
                        title = "Запись",
                        selectedDate = LocalDate.of(2026, 5, 1),
                        reminderUiState = reminderState,
                        currentDateTime = LocalDateTime.of(2026, 5, 1, 9, 0)
                    )
                SaveButton(enabled = isValid, onClick = {})
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
    }
}
