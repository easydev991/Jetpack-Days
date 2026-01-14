package com.dayscounter.ui.screen.components.createedit

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.dayscounter.R
import com.dayscounter.ui.screen.CreateEditUiState as ScreenCreateEditUiState

/**
 * TopAppBar для экрана создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun createEditTopAppBar(
    itemId: Long?,
    onBackClick: () -> Unit,
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
                style = MaterialTheme.typography.titleLarge,
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cancel),
                )
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
    )
}

/**
 * Основные секции формы (название, детали, дата).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainFormSections(
    params: CreateEditFormParams,
    onValueChange: () -> Unit,
) {
    titleSection(
        title = params.uiStates.title,
        onValueChange = { onValueChange() },
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
    detailsSection(
        details = params.uiStates.details,
        onValueChange = { onValueChange() },
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
    dateSection(
        selectedDate = params.uiStates.selectedDate,
        showDatePicker = params.showDatePicker,
    )
}

/**
 * Секция выбора цвета и опции отображения.
 */
@Composable
private fun colorAndDisplayOptionSection(
    params: CreateEditFormParams,
    onValueChange: () -> Unit,
) {
    colorSelector(
        selectedColor = params.uiStates.selectedColor,
        onValueChange = onValueChange,
    )
    Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
    displayOptionSelector(
        selectedDisplayOption = params.uiStates.selectedDisplayOption,
        onValueChange = onValueChange,
    )
}

/**
 * Контент формы создания/редактирования.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun createEditFormContent(params: CreateEditFormParams) {
    // Функция для отслеживания изменений
    val onValueChange: () -> Unit = {
        if (params.itemId != null) {
            val timestamp =
                params.uiStates.selectedDate.value
                    ?.atStartOfDay(java.time.ZoneId.systemDefault())
                    ?.toInstant()
                    ?.toEpochMilli() ?: 0L

            params.viewModel.checkHasChanges(
                title = params.uiStates.title.value,
                details = params.uiStates.details.value,
                timestamp = timestamp,
                colorTag =
                    params.uiStates.selectedColor.value
                        ?.toArgb(),
                displayOption = params.uiStates.selectedDisplayOption.value,
            )
        }
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(params.paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(dimensionResource(R.dimen.spacing_extra_large)),
    ) {
        mainFormSections(params, onValueChange)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        // Предпросмотр дней (закомментировано)
        // if (params.uiStates.selectedDate.value != null) {
        //     previewDaysContentInner(
        //         selectedDate = params.uiStates.selectedDate.value!!,
        //         displayOption = params.uiStates.selectedDisplayOption.value,
        //     )
        //     Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
        // }

        colorAndDisplayOptionSection(params, onValueChange)
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_huge)))
        buttonsSection(
            uiStates = params.uiStates,
            itemId = params.itemId,
            viewModel = params.viewModel,
            onBackClick = params.onBackClick,
        )
        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))
    }
}

/**
 * Секция с заголовком.
 */
@Composable
internal fun titleSection(
    title: MutableState<String>,
    onValueChange: (String) -> Unit = {},
) {
    OutlinedTextField(
        value = title.value,
        onValueChange = {
            title.value = it
            onValueChange(it)
        },
        label = { Text(stringResource(R.string.title)) },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * Секция с деталями.
 */
@Composable
internal fun detailsSection(
    details: MutableState<String>,
    onValueChange: (String) -> Unit = {},
) {
    OutlinedTextField(
        value = details.value,
        onValueChange = {
            details.value = it
            onValueChange(it)
        },
        label = { Text(stringResource(R.string.details)) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 3,
    )
}

/**
 * Секция с датой.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun dateSection(
    selectedDate: MutableState<java.time.LocalDate?>,
    showDatePicker: MutableState<Boolean>,
) {
    val formatter =
        java.time.format.DateTimeFormatter
            .ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag("ru"))
    // Выбор даты
    OutlinedTextField(
        value =
            selectedDate.value?.format(formatter) ?: "",
        onValueChange = { },
        label = { Text(stringResource(R.string.date)) },
        readOnly = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(
                onClick = {
                    showDatePicker.value = true
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.DateRange,
                    contentDescription = stringResource(R.string.select_date),
                )
            }
        },
    )
}

/**
 * Создает состояния UI.
 */
@Composable
internal fun rememberCreateEditUiStates(): ScreenCreateEditUiState =
    ScreenCreateEditUiState(
        title = rememberSaveable { mutableStateOf("") },
        details = rememberSaveable { mutableStateOf("") },
        selectedDate = remember { mutableStateOf(null) },
        showDatePicker = remember { mutableStateOf(false) },
        selectedColor = remember { mutableStateOf(null) },
        selectedDisplayOption =
            remember {
                mutableStateOf(
                    com.dayscounter.domain.model.DisplayOption.DAY,
                )
            },
    )

/**
 * Загружает данные при редактировании.
 */
fun loadItemData(
    itemId: Long?,
    uiState: com.dayscounter.viewmodel.CreateEditScreenState,
    uiStates: ScreenCreateEditUiState,
) {
    val isEditingExistingItem = itemId != null
    val isStateSuccess = uiState is com.dayscounter.viewmodel.CreateEditScreenState.Success
    val isTitleEmpty = uiStates.title.value.isEmpty()

    if (isEditingExistingItem && isStateSuccess && isTitleEmpty) {
        val item = uiState.item
        uiStates.title.value = item.title
        uiStates.details.value = item.details
        uiStates.selectedDate.value =
            java.time.Instant
                .ofEpochMilli(item.timestamp)
                .atZone(java.time.ZoneId.systemDefault())
                .toLocalDate()
        uiStates.selectedColor.value = item.colorTag?.let { Color(it) }
        uiStates.selectedDisplayOption.value = item.displayOption
    }
}
