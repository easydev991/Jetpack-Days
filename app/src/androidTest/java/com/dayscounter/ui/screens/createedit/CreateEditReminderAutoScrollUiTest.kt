package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.ui.theme.JetpackDaysTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateEditReminderAutoScrollUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun createEdit_whenReminderEnabledOnSmallScreen_thenNewOptionsBecomeVisibleWithoutManualScroll() {
        var uiState by remember { mutableStateOf(CreateEditUiState(title = "Demo", details = "Details")) }

        composeTestRule.setContent {
            JetpackDaysTheme {
                Box(modifier = Modifier.height(280.dp)) {
                    val params =
                        CreateEditFormParams(
                            itemId = null,
                            paddingValues = PaddingValues(),
                            uiStates = uiState,
                            showDatePicker = false,
                            onShowDatePickerChange = {},
                            onTitleChange = { title -> uiState = uiState.copy(title = title) },
                            onDetailsChange = { details -> uiState = uiState.copy(details = details) },
                            onDateChange = { date -> uiState = uiState.copy(selectedDate = date) },
                            onColorChange = { color -> uiState = uiState.copy(selectedColor = color) },
                            onDisplayOptionChange = { option ->
                                uiState = uiState.copy(selectedDisplayOption = option)
                            },
                            onReminderChange = { reminder -> uiState = uiState.copy(reminder = reminder) },
                            viewModel = createTestViewModel(),
                            onBackClick = {},
                            onReminderNotificationsUnavailable = {}
                        )

                    CreateEditFormContent(params)
                }
            }
        }

        composeTestRule.runOnIdle {
            uiState = uiState.copy(reminder = uiState.reminder.copy(isEnabled = true))
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.reminder_mode_on_date))
            .assertIsDisplayed()
    }
}
