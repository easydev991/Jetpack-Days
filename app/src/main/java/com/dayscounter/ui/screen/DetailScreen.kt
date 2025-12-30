package com.dayscounter.ui.screen

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.ui.screen.components.detail.detailContentByState
import com.dayscounter.ui.screen.components.detail.detailTopAppBar
import com.dayscounter.viewmodel.DetailScreenState
import com.dayscounter.viewmodel.DetailScreenViewModel

/**
 * Экран деталей события.
 *
 * Отображает полную информацию о событии с количеством прошедших дней.
 * Использует [DetailScreenViewModel] для управления состоянием.
 *
 * @param itemId Идентификатор события
 * @param modifier Modifier для экрана
 * @param viewModel ViewModel для управления состоянием
 * @param onBackClick Обработчик клика "Назад"
 * @param onEditClick Обработчик клика "Редактировать"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun detailScreen(
    itemId: Long,
    modifier: Modifier = Modifier,
    viewModel: DetailScreenViewModel = viewModel(),
    onBackClick: () -> Unit = {},
    onEditClick: (Long) -> Unit = {},
) {
    val uiState by viewModel.uiState.collectAsState()

    detailScreenContent(
        itemId = itemId,
        modifier = modifier,
        uiState = uiState,
        onBackClick = onBackClick,
        onEditClick = onEditClick,
    )
}

/**
 * Основной контент экрана деталей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun detailScreenContent(
    itemId: Long,
    modifier: Modifier = Modifier,
    uiState: DetailScreenState,
    onBackClick: () -> Unit,
    onEditClick: (Long) -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            detailTopAppBar(uiState = uiState, onBackClick = onBackClick, onEditClick = onEditClick, itemId = itemId)
        },
    ) { paddingValues ->
        detailContentByState(uiState = uiState, modifier = Modifier.padding(paddingValues))
    }
}
