package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.dayscounter.R
import com.dayscounter.domain.model.DisplayOption
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
        lateinit var uiStates: CreateEditUiState

        composeTestRule.setContent {
            JetpackDaysTheme {
                Box(modifier = Modifier.height(280.dp)) {
                    val title = rememberSaveable { mutableStateOf("Demo") }
                    val details = rememberSaveable { mutableStateOf("Details") }
                    val selectedDate = rememberSaveable { mutableStateOf(java.time.LocalDate.now()) }
                    val selectedColor = remember { mutableStateOf<androidx.compose.ui.graphics.Color?>(null) }
                    val selectedDisplayOption = rememberSaveable { mutableStateOf(DisplayOption.DAY) }
                    val showDatePicker = remember { mutableStateOf(false) }

                    uiStates =
                        CreateEditUiState(
                            title = title,
                            details = details,
                            selectedDate = selectedDate,
                            selectedColor = selectedColor,
                            selectedDisplayOption = selectedDisplayOption
                        )

                    val params =
                        CreateEditFormParams(
                            itemId = null,
                            paddingValues = PaddingValues(),
                            uiStates = uiStates,
                            showDatePicker = showDatePicker,
                            viewModel = createTestViewModel(),
                            onBackClick = {},
                            onReminderNotificationsUnavailable = {}
                        )

                    CreateEditFormContent(params)
                }
            }
        }

        composeTestRule.runOnIdle {
            uiStates.reminder.isEnabled.value = true
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.reminder_mode_on_date))
            .assertIsDisplayed()
    }
}
