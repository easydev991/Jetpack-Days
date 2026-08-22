package com.dayscounter.ui.screens.createedit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dayscounter.R
import com.dayscounter.di.AppModule
import com.dayscounter.domain.model.ExactAlarmPermissionState
import com.dayscounter.ui.viewmodel.ExactAlarmPermissionViewModel

/**
 * Карточка, которую видит пользователь, когда permission `SCHEDULE_EXACT_ALARM`
 * запрещён. Два визуальных состояния:
 * - `canRequest = true` → кнопка «Включить точные напоминания», открывает системный диалог.
 * - `canRequest = false` → только текст-предупреждение (например, MIUI/HyperOS без экрана).
 */
@Composable
internal fun DeniedCard(
    canRequest: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val padding = dimensionResource(R.dimen.spacing_regular)
    val itemSpacing = dimensionResource(R.dimen.spacing_xsmall)
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(padding),
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            Text(
                text = stringResource(R.string.exact_alarm_request_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text =
                    if (canRequest) {
                        stringResource(R.string.exact_alarm_request_text)
                    } else {
                        stringResource(R.string.exact_alarm_unavailable_text)
                    },
                style = MaterialTheme.typography.bodyMedium
            )
            if (canRequest) {
                Button(onClick = onRequestPermission) {
                    Text(stringResource(R.string.exact_alarm_request_button))
                }
            }
        }
    }
}

/**
 * Баннер с собственным [ExactAlarmPermissionViewModel]. Создаёт ViewModel
 * через factory из [AppModule], собирает state и обновляет его на `ON_RESUME`
 * (иначе баннер не пропадёт после возврата из системных настроек).
 *
 * По паттерну `ObserveReminderStateOnResume` — в проекте используется ручной
 * `DisposableEffect` + `LifecycleEventObserver`, не `LifecycleEventEffect`.
 */
@Composable
fun ExactAlarmPermissionInlineSection() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val viewModel: ExactAlarmPermissionViewModel =
        viewModel(factory = AppModule.createExactAlarmPermissionViewModelFactory(context))
    val state by viewModel.state.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    viewModel.refresh()
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (val s = state) {
        is ExactAlarmPermissionState.Allowed -> Unit
        is ExactAlarmPermissionState.Denied ->
            DeniedCard(
                canRequest = s.canRequest,
                onRequestPermission = viewModel::onRequestPermission
            )
    }
}
