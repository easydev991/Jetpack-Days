package com.dayscounter.ui.screens.createedit

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imeNestedScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.ui.viewmodel.CreateEditScreenState
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * TopAppBar для экрана создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateEditTopAppBar(
    itemId: Long?,
    onBackClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text =
                    if (itemId != null) {
                        stringResource(R.string.edit_item)
                    } else {
                        stringResource(R.string.new_item)
                    },
                style = MaterialTheme.typography.titleLarge
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel)
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
    )
}

/**
 * Основные секции формы (название, детали, дата).
 * Принимает plain-значения и callback'и.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainFormSections(
    params: CreateEditFormParams,
    onValueChange: () -> Unit
) {
    TitleSection(
        title = params.uiStates.title,
        onValueChange = { newTitle ->
            params.onTitleChange(newTitle)
            onValueChange()
        }
    )
    androidx.compose.foundation.layout
        .Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
    DetailsSection(
        details = params.uiStates.details,
        onValueChange = { newDetails ->
            params.onDetailsChange(newDetails)
            onValueChange()
        }
    )
    androidx.compose.foundation.layout
        .Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
    DateSection(
        selectedDate = params.uiStates.selectedDate,
        onShowDatePickerChange = params.onShowDatePickerChange
    )
}

/**
 * Секция выбора цвета и опции отображения.
 * Принимает plain-значения и callback'и.
 */
@Composable
private fun ColorAndDisplayOptionSection(
    params: CreateEditFormParams,
    onValueChange: () -> Unit
) {
    ColorSelector(
        selectedColor = params.uiStates.selectedColor,
        onColorSelected = { newColor ->
            params.onColorChange(newColor)
            onValueChange()
        }
    )
    androidx.compose.foundation.layout
        .Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
    DisplayOptionSelector(
        selectedDisplayOption = params.uiStates.selectedDisplayOption,
        onDisplayOptionSelected = { newOption ->
            params.onDisplayOptionChange(newOption)
            onValueChange()
        }
    )
}

/**
 * Контент формы создания/редактирования.
 * Принимает CreateEditFormParams с plain-значениями и callback'ами.
 */
@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreateEditFormContent(params: CreateEditFormParams) {
    val reminderSettingsBringIntoViewRequester = remember { BringIntoViewRequester() }
    var previousReminderEnabled by remember { mutableStateOf(params.uiStates.reminder.isEnabled) }
    val onValueChange =
        rememberOnCreateEditValueChange(
            itemId = params.itemId,
            uiState = params.uiStates,
            viewModel = params.viewModel
        )

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(params.paddingValues)
                .imeNestedScroll()
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_regular))
    ) {
        MainFormSections(params, onValueChange)
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        ColorAndDisplayOptionSection(params, onValueChange)
        androidx.compose.foundation.layout
            .Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_regular)))
        ReminderSettingsSection(
            reminder = params.uiStates.reminder,
            onReminderChange = { newReminder ->
                params.onReminderChange(newReminder)
                onValueChange()
            },
            onReminderToggleRequested =
                rememberReminderToggleHandler(
                    reminder = params.uiStates.reminder,
                    onReminderChange = params.onReminderChange,
                    onValueChange = onValueChange,
                    onReminderNotificationsUnavailable = params.onReminderNotificationsUnavailable
                ),
            expandedContentModifier = Modifier.bringIntoViewRequester(reminderSettingsBringIntoViewRequester)
        )
    }

    val isReminderEnabled = params.uiStates.reminder.isEnabled
    LaunchedEffect(isReminderEnabled) {
        if (!previousReminderEnabled && isReminderEnabled) {
            reminderSettingsBringIntoViewRequester.bringIntoView()
        }
        previousReminderEnabled = isReminderEnabled
    }

    ObserveReminderStateOnResume(
        isReminderEnabled = params.uiStates.reminder.isEnabled,
        onReminderDisabled = {
            params.onReminderChange(params.uiStates.reminder.copy(isEnabled = false))
            onValueChange()
        },
        onReminderNotificationsUnavailable = params.onReminderNotificationsUnavailable
    )

    if (params.uiStates.reminder.showDatePicker) {
        DatePickerDialogSection(
            selectedDate = params.uiStates.reminder.selectedDate,
            onDateSelected = { date ->
                params.onReminderChange(
                    params.uiStates.reminder.copy(selectedDate = date, showDatePicker = false)
                )
                onValueChange()
            },
            onDismiss = {
                params.onReminderChange(
                    params.uiStates.reminder.copy(showDatePicker = false)
                )
            }
        )
    }
}

@Composable
private fun rememberReminderToggleHandler(
    reminder: ReminderFormUiState,
    onReminderChange: (ReminderFormUiState) -> Unit,
    onValueChange: () -> Unit,
    onReminderNotificationsUnavailable: () -> Unit
): (Boolean) -> Unit {
    val context = LocalContext.current
    val reminderPermissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            val activationDecision =
                decideReminderActivation(
                    hasPostNotificationsPermission = isGranted,
                    areReminderNotificationsEnabled = context.areReminderNotificationsEnabled()
                )

            when (activationDecision) {
                ReminderActivationDecision.ENABLE -> {
                    onReminderChange(reminder.copy(isEnabled = true))
                }

                ReminderActivationDecision.SHOW_NOTIFICATION_SETTINGS_FEEDBACK -> {
                    onReminderChange(reminder.copy(isEnabled = false))
                    onReminderNotificationsUnavailable()
                }
            }

            onValueChange()
        }

    return { isChecked ->
        when (
            decideReminderToggle(
                isChecked = isChecked,
                sdkInt = Build.VERSION.SDK_INT,
                hasPostNotificationsPermission = context.hasPostNotificationsPermission()
            )
        ) {
            ReminderToggleDecision.ENABLE -> {
                when (
                    decideReminderActivation(
                        hasPostNotificationsPermission = true,
                        areReminderNotificationsEnabled = context.areReminderNotificationsEnabled()
                    )
                ) {
                    ReminderActivationDecision.ENABLE -> {
                        onReminderChange(reminder.copy(isEnabled = true))
                    }

                    ReminderActivationDecision.SHOW_NOTIFICATION_SETTINGS_FEEDBACK -> {
                        onReminderChange(reminder.copy(isEnabled = false))
                        onReminderNotificationsUnavailable()
                    }
                }
                onValueChange()
            }

            ReminderToggleDecision.DISABLE -> {
                onReminderChange(reminder.copy(isEnabled = false))
                onValueChange()
            }

            ReminderToggleDecision.REQUEST_PERMISSION -> {
                reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

/**
 * Секция с заголовком.
 * Принимает plain String и callback вместо MutableState.
 */
@Composable
internal fun TitleSection(
    title: String,
    onValueChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = title,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.title)) },
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * Секция с деталями.
 * Принимает plain String и callback вместо MutableState.
 */
@Composable
internal fun DetailsSection(
    details: String,
    onValueChange: (String) -> Unit = {}
) {
    OutlinedTextField(
        value = details,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.details)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3
    )
}

/**
 * Секция с датой.
 * Принимает plain-значения и callback'и вместо MutableState.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateSection(
    selectedDate: LocalDate?,
    onShowDatePickerChange: (Boolean) -> Unit
) {
    val configuration = LocalConfiguration.current
    val formatter =
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(configuration.locales[0])

    OutlinedTextField(
        value = selectedDate?.format(formatter).orEmpty(),
        onValueChange = {},
        label = { Text(stringResource(R.string.date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = { onShowDatePickerChange(true) }) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.select_date)
                )
            }
        }
    )
}

/**
 * Создает единственное состояние UI через rememberSaveable.
 * Возвращает MutableState<CreateEditUiState> для использования на уровне Screen.
 */
@Composable
internal fun rememberCreateEditUiState(): MutableState<CreateEditUiState> =
    rememberSaveable(stateSaver = CreateEditUiStateSaver) {
        mutableStateOf(CreateEditUiState())
    }

/**
 * Загружает данные при редактировании.
 * Работает с MutableState<CreateEditUiState> через copy().
 */
fun loadItemData(
    itemId: Long?,
    uiState: CreateEditScreenState,
    uiStateMutable: MutableState<CreateEditUiState>
) {
    val successState = uiState as? CreateEditScreenState.Success ?: return

    if (itemId != null && uiStateMutable.value.title.isEmpty()) {
        val item = successState.item
        uiStateMutable.value =
            uiStateMutable.value.copy(
                title = item.title,
                details = item.details,
                selectedDate =
                    java.time.Instant
                        .ofEpochMilli(item.timestamp)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate(),
                selectedColor = item.colorTag?.let { Color(it) },
                selectedDisplayOption = item.displayOption
            )
    }

    if (itemId != null && !uiStateMutable.value.reminder.isInitializedFromSource) {
        uiStateMutable.value =
            uiStateMutable.value.copy(
                reminder = uiStateMutable.value.reminder.applyReminder(successState.reminder)
            )
    }
}
