package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
        composeTestRule.setContent {
            val stateHolder = remember { mutableStateOf(CreateEditUiState(title = "Demo", details = "Details")) }

            JetpackDaysTheme {
                val params =
                    CreateEditFormParams(
                        itemId = null,
                        paddingValues = PaddingValues(),
                        uiStates = stateHolder.value,
                        onShowDatePickerChange = {},
                        onTitleChange = { title -> stateHolder.value = stateHolder.value.copy(title = title) },
                        onDetailsChange = { details -> stateHolder.value = stateHolder.value.copy(details = details) },
                        onColorChange = { color -> stateHolder.value = stateHolder.value.copy(selectedColor = color) },
                        onDisplayOptionChange = { option ->
                            stateHolder.value = stateHolder.value.copy(selectedDisplayOption = option)
                        },
                        onReminderChange = { reminder -> stateHolder.value = stateHolder.value.copy(reminder = reminder) },
                        onValueChange = {},
                        viewModel = createTestViewModel(),
                        onBackClick = {},
                        onReminderNotificationsUnavailable = {}
                    )

                CreateEditFormContent(params)
            }
        }

        composeTestRule
            .onNodeWithText(context.getString(R.string.add_reminder))
            .assertIsDisplayed()
    }
}
