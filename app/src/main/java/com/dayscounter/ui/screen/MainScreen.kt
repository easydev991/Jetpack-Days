package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.ui.component.listItemView
import com.dayscounter.ui.util.NumberFormattingUtils
import com.dayscounter.viewmodel.MainScreenState
import com.dayscounter.viewmodel.MainScreenViewModel

/**
 * Главный экран со списком событий.
 *
 * Отображает все события из базы данных с количеством прошедших дней.
 * Использует [MainScreenViewModel] для управления состоянием.
 *
 * @param modifier Modifier для экрана
 * @param viewModel ViewModel для управления состоянием
 * @param onItemClick Обработчик клика по событию
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun mainScreen(
    modifier: Modifier = Modifier,
    onItemClick: (Long) -> Unit = {},
) {
    val viewModel: MainScreenViewModel =
        viewModel(
            factory =
                MainScreenViewModel.factory(
                    com.dayscounter.di.AppModule.createItemRepository(
                        com.dayscounter.data.database.DaysDatabase.getDatabase(
                            androidx.compose.ui.platform.LocalContext.current.applicationContext,
                        ),
                    ),
                ),
        )
    mainScreenContent(
        modifier = modifier,
        viewModel = viewModel,
        onItemClick = onItemClick,
    )
}

/**
 * Основной контент экрана.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun mainScreenContent(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel,
    onItemClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.refresh() },
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.add_item),
                )
            }
        },
    ) { paddingValues ->
        when (val state = uiState) {
            is MainScreenState.Loading -> {
                loadingContent(modifier = Modifier.padding(paddingValues))
            }
            is MainScreenState.Success -> {
                if (state.items.isEmpty()) {
                    emptyContent()
                } else {
                    itemsListContent(
                        items = state.items,
                        onItemClick = onItemClick,
                    )
                }
            }
            is MainScreenState.Error -> {
                errorContent(
                    message = state.message,
                    modifier = Modifier.padding(paddingValues),
                )
            }
        }
    }
}

/**
 * Контент с пустым списком.
 */
@Composable
private fun emptyContent() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacing_huge)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.what_should_we_remember),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(dimensionResource(R.dimen.spacing_extra_large)))

        Text(
            text = stringResource(R.string.create_your_first_item),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Контент с списком событий.
 */
@Composable
private fun itemsListContent(
    items: List<com.dayscounter.domain.model.Item>,
    onItemClick: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            // Форматируем количество дней для каждого элемента
            val formattedDays =
                remember(item.timestamp) {
                    val eventDate =
                        java.time.Instant.ofEpochMilli(item.timestamp)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                    val currentDate = java.time.LocalDate.now()

                    if (eventDate == currentDate) {
                        "Сегодня"
                    } else {
                        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(eventDate, currentDate).toInt()
                        NumberFormattingUtils.formatDaysCount(daysDiff)
                    }
                }

            listItemView(
                item = item,
                formattedDaysText = formattedDays,
                onClick = { onItemClick(item.id) },
            )
        }
    }
}

/**
 * Контент при загрузке.
 */
@Composable
private fun loadingContent(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.loading),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Контент при ошибке.
 */
@Composable
private fun errorContent(
    message: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxSize()
                .padding(dimensionResource(R.dimen.spacing_huge)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
    }
}
