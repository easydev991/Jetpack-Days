package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import java.time.LocalDate
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
 * Использует единственный MutableState<CreateEditUiState> для всей формы.
 */
@Suppress("LongMethod")
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
    val formState = rememberCreateEditUiState()
    val snackbarHostState = remember { SnackbarHostState() }
    val onReminderNotificationsUnavailable =
        rememberReminderNotificationsUnavailableHandler(snackbarHostState = snackbarHostState)

    LaunchedEffect(itemId, uiState) {
        loadItemData(itemId, uiState, formState)
    }

    val onValueChange = {
        if (itemId != null) {
            val s = formState.value
            viewModel.checkHasChanges(
                CreateEditChangeInput(
                    title = s.title,
                    details = s.details,
                    timestamp =
                        s.selectedDate
                            ?.atStartOfDay(ZoneId.systemDefault())
                            ?.toInstant()
                            ?.toEpochMilli() ?: 0L,
                    colorTag = s.selectedColor?.toArgb(),
                    displayOption = s.selectedDisplayOption,
                    reminderFingerprint = s.reminder.toChangeFingerprint()
                )
            )
        }
    }

    val screenActions =
        rememberCreateEditScreenActions(
            params =
                CreateEditScreenActionsParams(
                    itemId = itemId,
                    hasChanges = hasChanges,
                    uiStates = formState.value,
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
                    uiStates = formState.value,
                    onShowDatePickerChange = { formState.value = formState.value.copy(showDatePicker = it) },
                    onTitleChange = { title -> formState.value = formState.value.copy(title = title) },
                    onDetailsChange = { details -> formState.value = formState.value.copy(details = details) },
                    onDateChange = { date -> formState.value = formState.value.copy(selectedDate = date) },
                    onColorChange = { color -> formState.value = formState.value.copy(selectedColor = color) },
                    onDisplayOptionChange = { option ->
                        formState.value = formState.value.copy(selectedDisplayOption = option)
                    },
                    onReminderChange = { reminder -> formState.value = formState.value.copy(reminder = reminder) },
                    onValueChange = onValueChange,
                    viewModel = viewModel,
                    onBackClick = onBackClick,
                    onReminderNotificationsUnavailable = onReminderNotificationsUnavailable
                )
        )
    }

    CreateEditDatePickerIfNeeded(
        shouldShowDatePicker = formState.value.showDatePicker,
        selectedDate = formState.value.selectedDate,
        onDateSelected = { date ->
            formState.value = formState.value.copy(selectedDate = date, showDatePicker = false)
            if (itemId != null) {
                val s = formState.value
                viewModel.checkHasChanges(
                    CreateEditChangeInput(
                        title = s.title,
                        details = s.details,
                        timestamp =
                            s.selectedDate
                                ?.atStartOfDay(ZoneId.systemDefault())
                                ?.toInstant()
                                ?.toEpochMilli() ?: 0L,
                        colorTag = s.selectedColor?.toArgb(),
                        displayOption = s.selectedDisplayOption,
                        reminderFingerprint = s.reminder.toChangeFingerprint()
                    )
                )
            }
        },
        onDismiss = { formState.value = formState.value.copy(showDatePicker = false) }
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

private fun CreateEditUiState.toItem(itemId: Long?): Item {
    val timestamp =
        selectedDate
            ?.atStartOfDay(ZoneId.systemDefault())
            ?.toInstant()
            ?.toEpochMilli() ?: System.currentTimeMillis()

    return Item(
        id = itemId ?: 0L,
        title = title,
        details = details,
        timestamp = timestamp,
        colorTag = selectedColor?.toArgb(),
        displayOption = selectedDisplayOption
    )
}

@Composable
private fun CreateEditDatePickerIfNeeded(
    shouldShowDatePicker: Boolean,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    if (shouldShowDatePicker) {
        DatePickerDialogSection(
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            onDismiss = onDismiss
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
            title = params.uiStates.title,
            selectedDate = params.uiStates.selectedDate,
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
    return CreateEditScreenActions(
        isSaveEnabled = isSaveEnabled,
        onSaveClick = onSaveClick
    )
}

private data class CreateEditScreenActions(
    val isSaveEnabled: Boolean,
    val onSaveClick: () -> Unit
)

private data class CreateEditScreenActionsParams(
    val itemId: Long?,
    val hasChanges: Boolean,
    val uiStates: CreateEditUiState,
    val viewModel: CreateEditScreenViewModel,
    val analyticsService: AnalyticsService,
    val onBackClick: () -> Unit
)
