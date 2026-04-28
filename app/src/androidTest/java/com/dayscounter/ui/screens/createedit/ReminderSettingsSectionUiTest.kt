package com.dayscounter.ui.screens.createedit

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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

@RunWith(AndroidJUnit4::class)
class ReminderSettingsSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun reminderSection_whenToggleOffByDefault_thenSettingsAreHidden() {
        val reminderState = createReminderUiState(isEnabled = false)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ReminderSettingsSection(reminderUiState = reminderState, onValueChange = {})
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.add_reminder)).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(context.getString(R.string.reminder_mode_on_date)).assertCountEquals(0)
        composeTestRule.onAllNodesWithText(context.getString(R.string.reminder_mode_after)).assertCountEquals(0)
    }

    @Test
    fun reminderSection_whenToggleTurnedOn_thenModeOptionsBecomeVisible() {
        val reminderState = createReminderUiState(isEnabled = false)

        composeTestRule.setContent {
            JetpackDaysTheme {
                ReminderSettingsSection(reminderUiState = reminderState, onValueChange = {})
            }
        }

        composeTestRule.runOnIdle {
            reminderState.isEnabled.value = true
        }

        composeTestRule.onNodeWithText(context.getString(R.string.reminder_mode_on_date)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.reminder_mode_after)).assertIsDisplayed()
    }

    @Test
    fun reminderSection_whenAtDateModeSelected_thenDateAndTimeFieldsAreVisible() {
        val reminderState =
            createReminderUiState(
                isEnabled = true,
                mode = ReminderMode.AT_DATE
            )

        composeTestRule.setContent {
            JetpackDaysTheme {
                ReminderSettingsSection(reminderUiState = reminderState, onValueChange = {})
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.reminder_date)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.reminder_time)).assertIsDisplayed()
    }

    @Test
    fun reminderSection_whenAfterModeSelected_thenShowsIntervalAndUnitFields() {
        val reminderState =
            createReminderUiState(
                isEnabled = true,
                mode = ReminderMode.AFTER_INTERVAL,
                intervalValue = ""
            )

        composeTestRule.setContent {
            JetpackDaysTheme {
                ReminderSettingsSection(reminderUiState = reminderState, onValueChange = {})
            }
        }

        composeTestRule.onNodeWithText(context.getString(R.string.remind_after_label)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.reminder_period_unit)).assertIsDisplayed()
    }

    private fun createReminderUiState(
        isEnabled: Boolean,
        mode: ReminderMode = ReminderMode.AT_DATE,
        intervalValue: String = "3"
    ): ReminderFormUiState =
        ReminderFormUiState(
            isEnabled = mutableStateOf(isEnabled),
            mode = mutableStateOf(mode),
            selectedDate = mutableStateOf(LocalDate.of(2026, 4, 27)),
            showDatePicker = mutableStateOf(false),
            hour = mutableStateOf(16),
            minute = mutableStateOf(8),
            intervalValue = mutableStateOf(intervalValue),
            intervalUnit = mutableStateOf(ReminderIntervalUnit.DAY)
        )
}
