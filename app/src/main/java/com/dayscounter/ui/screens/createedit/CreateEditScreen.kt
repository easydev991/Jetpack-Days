package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.UserActionType
import com.dayscounter.domain.model.Item
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel
import java.time.ZoneId

/**
 * Экран создания/редактирования события.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditScreen(
    itemId: Long?,
    modifier: Modifier = Modifier,
    viewModel: CreateEditScreenViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    analyticsService: AnalyticsService
) {
    CreateEditScreenContent(
        itemId = itemId,
        modifier = modifier,
        viewModel = viewModel,
        onBackClick = onBackClick,
        analyticsService = analyticsService
    )
}

/**
 * Основной контент экрана создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Suppress("LongMethod")
private fun CreateEditScreenContent(
    itemId: Long?,
    modifier: Modifier = Modifier,
    viewModel: CreateEditScreenViewModel,
    onBackClick: () -> Unit,
    analyticsService: AnalyticsService
) {
    val uiState by viewModel.uiState.collectAsState()
    val hasChanges by viewModel.hasChanges.collectAsState()
    val uiStates = rememberCreateEditUiStates()
    val showDatePicker = rememberSaveable { mutableStateOf(false) }

    loadItemData(itemId, uiState, uiStates)

    val isValidData =
        isCreateEditFormValid(
            title = uiStates.title.value,
            selectedDate = uiStates.selectedDate.value,
            reminderUiState = uiStates.reminder
        )
    val isEditing = itemId != null

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CreateEditTopAppBar(itemId = itemId, onBackClick = onBackClick)
        },
        bottomBar = {
            SaveButton(
                enabled = if (isEditing) isValidData && hasChanges else isValidData,
                onClick = {
                    if (!isValidData) {
                        return@SaveButton
                    }

                    analyticsService.log(AnalyticsEvent.UserAction(UserActionType.ITEM_SAVED))

                    val timestamp =
                        uiStates.selectedDate.value
                            ?.atStartOfDay(ZoneId.systemDefault())
                            ?.toInstant()
                            ?.toEpochMilli() ?: System.currentTimeMillis()

                    val item =
                        Item(
                            id = itemId ?: 0L,
                            title = uiStates.title.value,
                            details = uiStates.details.value,
                            timestamp = timestamp,
                            colorTag = uiStates.selectedColor.value?.toArgb(),
                            displayOption = uiStates.selectedDisplayOption.value
                        )

                    val reminderRequest = uiStates.reminder.toReminderRequest(item.id)

                    viewModel.saveItem(
                        item = item,
                        reminderRequest = reminderRequest,
                        onSaved = onBackClick
                    )
                }
            )
        }
    ) { paddingValues ->
        CreateEditFormContent(
            params =
                CreateEditFormParams(
                    itemId = itemId,
                    paddingValues = paddingValues,
                    uiStates = uiStates,
                    showDatePicker = showDatePicker,
                    viewModel = viewModel,
                    onBackClick = onBackClick
                )
        )
    }

    if (showDatePicker.value) {
        DatePickerDialogSection(
            selectedDate = uiStates.selectedDate,
            showDatePicker = showDatePicker,
            onDateSelected = {
                if (itemId != null) {
                    val timestamp =
                        uiStates.selectedDate.value
                            ?.atStartOfDay(ZoneId.systemDefault())
                            ?.toInstant()
                            ?.toEpochMilli() ?: 0L

                    viewModel.checkHasChanges(
                        title = uiStates.title.value,
                        details = uiStates.details.value,
                        timestamp = timestamp,
                        colorTag = uiStates.selectedColor.value?.toArgb(),
                        displayOption = uiStates.selectedDisplayOption.value,
                        reminderFingerprint = uiStates.reminder.toChangeFingerprint()
                    )
                }
            }
        )
    }
}
