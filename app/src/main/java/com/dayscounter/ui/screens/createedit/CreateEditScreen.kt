package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.analytics.AnalyticsEvent
import com.dayscounter.analytics.AnalyticsService
import com.dayscounter.analytics.UserActionType
import com.dayscounter.domain.model.Item
import com.dayscounter.ui.viewmodel.CreateEditChangeInput
import com.dayscounter.ui.viewmodel.CreateEditScreenViewModel
import kotlinx.coroutines.launch
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
    val snackbarHostState = remember { SnackbarHostState() }
    val onReminderNotificationsUnavailable =
        rememberReminderNotificationsUnavailableHandler(snackbarHostState = snackbarHostState)

    loadItemData(itemId, uiState, uiStates)
    val screenActions =
        rememberCreateEditScreenActions(
            params =
                CreateEditScreenActionsParams(
                    itemId = itemId,
                    hasChanges = hasChanges,
                    uiStates = uiStates,
                    viewModel = viewModel,
                    analyticsService = analyticsService,
                    onBackClick = onBackClick
                )
        )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        topBar = {
            CreateEditTopAppBar(itemId = itemId, onBackClick = onBackClick)
        },
        bottomBar = {
            SaveButton(enabled = screenActions.isSaveEnabled, onClick = screenActions.onSaveClick)
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
                    onBackClick = onBackClick,
                    onReminderNotificationsUnavailable = onReminderNotificationsUnavailable
                )
        )
    }

    CreateEditDatePickerIfNeeded(
        shouldShowDatePicker = showDatePicker.value,
        selectedDate = uiStates.selectedDate,
        showDatePicker = showDatePicker,
        onDateSelected = screenActions.onDateSelected
    )
}

@Composable
private fun rememberReminderNotificationsUnavailableHandler(snackbarHostState: SnackbarHostState): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationsDisabledMessage = stringResource(R.string.reminder_notifications_disabled_message)
    val openSettingsActionLabel = stringResource(R.string.open_notification_settings)

    return {
        coroutineScope.launch {
            val snackbarResult =
                snackbarHostState.showSnackbar(
                    message = notificationsDisabledMessage,
                    actionLabel = openSettingsActionLabel,
                    withDismissAction = true,
                    duration = SnackbarDuration.Long
                )

            if (snackbarResult == SnackbarResult.ActionPerformed) {
                context.openReminderNotificationSettings()
            }
        }
    }
}

@Composable
private fun rememberCreateEditSaveAction(params: CreateEditSaveActionParams): () -> Unit =
    {
        if (params.isValidData) {
            params.analyticsService.log(AnalyticsEvent.UserAction(UserActionType.ITEM_SAVED))
            val item = params.uiStates.toItem(itemId = params.itemId)
            val reminderRequest = params.uiStates.reminder.toReminderRequest(item.id)

            params.viewModel.saveItem(
                item = item,
                reminderRequest = reminderRequest,
                onSaved = params.onBackClick
            )
        }
    }

@Composable
private fun rememberCreateEditDateSelectedAction(
    itemId: Long?,
    uiStates: CreateEditUiState,
    viewModel: CreateEditScreenViewModel
): () -> Unit =
    {
        if (itemId != null) {
            val timestamp =
                uiStates.selectedDate.value
                    ?.atStartOfDay(ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: 0L

            viewModel.checkHasChanges(
                CreateEditChangeInput(
                    title = uiStates.title.value,
                    details = uiStates.details.value,
                    timestamp = timestamp,
                    colorTag = uiStates.selectedColor.value?.toArgb(),
                    displayOption = uiStates.selectedDisplayOption.value,
                    reminderFingerprint = uiStates.reminder.toChangeFingerprint()
                )
            )
        }
    }

private fun CreateEditUiState.toItem(itemId: Long?): Item {
    val timestamp =
        selectedDate.value
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli() ?: System.currentTimeMillis()

    return Item(
        id = itemId ?: 0L,
        title = title.value,
        details = details.value,
        timestamp = timestamp,
        colorTag = selectedColor.value?.toArgb(),
        displayOption = selectedDisplayOption.value
    )
}

@Composable
private fun CreateEditDatePickerIfNeeded(
    shouldShowDatePicker: Boolean,
    selectedDate: androidx.compose.runtime.MutableState<java.time.LocalDate?>,
    showDatePicker: androidx.compose.runtime.MutableState<Boolean>,
    onDateSelected: () -> Unit
) {
    if (shouldShowDatePicker) {
        DatePickerDialogSection(
            selectedDate = selectedDate,
            showDatePicker = showDatePicker,
            onDateSelected = onDateSelected
        )
    }
}

private data class CreateEditSaveActionParams(
    val itemId: Long?,
    val isValidData: Boolean,
    val uiStates: CreateEditUiState,
    val viewModel: CreateEditScreenViewModel,
    val analyticsService: AnalyticsService,
    val onBackClick: () -> Unit
)

@Composable
private fun rememberCreateEditScreenActions(params: CreateEditScreenActionsParams): CreateEditScreenActions {
    val isValidData =
        isCreateEditFormValid(
            title = params.uiStates.title.value,
            selectedDate = params.uiStates.selectedDate.value,
            reminderUiState = params.uiStates.reminder
        )
    val isSaveEnabled = if (params.itemId != null) isValidData && params.hasChanges else isValidData
    val onSaveClick =
        rememberCreateEditSaveAction(
            params =
                CreateEditSaveActionParams(
                    itemId = params.itemId,
                    isValidData = isValidData,
                    uiStates = params.uiStates,
                    viewModel = params.viewModel,
                    analyticsService = params.analyticsService,
                    onBackClick = params.onBackClick
                )
        )
    val onDateSelected =
        rememberCreateEditDateSelectedAction(
            itemId = params.itemId,
            uiStates = params.uiStates,
            viewModel = params.viewModel
        )
    return CreateEditScreenActions(
        isSaveEnabled = isSaveEnabled,
        onSaveClick = onSaveClick,
        onDateSelected = onDateSelected
    )
}

private data class CreateEditScreenActions(
    val isSaveEnabled: Boolean,
    val onSaveClick: () -> Unit,
    val onDateSelected: () -> Unit
)

private data class CreateEditScreenActionsParams(
    val itemId: Long?,
    val hasChanges: Boolean,
    val uiStates: CreateEditUiState,
    val viewModel: CreateEditScreenViewModel,
    val analyticsService: AnalyticsService,
    val onBackClick: () -> Unit
)
