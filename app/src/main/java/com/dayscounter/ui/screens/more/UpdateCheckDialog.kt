package com.dayscounter.ui.screens.more

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.dayscounter.R
import com.dayscounter.domain.model.UpdateInfo
import com.dayscounter.ui.state.MoreScreenUiState
import com.dayscounter.ui.theme.JetpackDaysTheme

/** Максимальная длина release notes в диалоге (см. design.md D9). */
private const val MAX_RELEASE_NOTES_LENGTH = 500

/**
 * Диалог с результатом проверки обновлений.
 *
 * Показывается для состояний [MoreScreenUiState.UpToDate],
 * [MoreScreenUiState.UpdateAvailable] и [MoreScreenUiState.Error];
 * [MoreScreenUiState.Idle] и [MoreScreenUiState.Checking] — диалог закрыт.
 *
 * @param state Текущее состояние проверки обновлений
 * @param onDismiss Закрытие диалога (возвращает ViewModel в Idle)
 * @param onOpenRelease Открытие страницы релиза в браузере
 */
@Composable
fun UpdateCheckDialog(
    state: MoreScreenUiState,
    onDismiss: () -> Unit,
    onOpenRelease: (String) -> Unit
) {
    when (state) {
        MoreScreenUiState.Idle, MoreScreenUiState.Checking -> Unit

        is MoreScreenUiState.UpdateAvailable ->
            UpdateAvailableDialog(
                info = state.info,
                onDismiss = onDismiss,
                onOpenRelease = onOpenRelease
            )

        MoreScreenUiState.UpToDate ->
            MessageDialog(
                titleResId = R.string.update_up_to_date_title,
                messageResId = R.string.update_up_to_date,
                onDismiss = onDismiss
            )

        MoreScreenUiState.Error ->
            MessageDialog(
                titleResId = R.string.update_error_title,
                messageResId = R.string.update_error,
                onDismiss = onDismiss
            )
    }
}

/**
 * Диалог "Доступно обновление": версия, release notes (если есть)
 * и кнопки "Открыть на GitHub" / "Закрыть".
 */
@Composable
private fun UpdateAvailableDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit,
    onOpenRelease: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.update_available_title)) },
        text = {
            Column {
                Text(stringResource(R.string.update_available_message, info.version))
                trimReleaseNotes(info.notes)?.let { notes ->
                    Spacer(
                        modifier = Modifier.height(dimensionResource(R.dimen.spacing_xsmall))
                    )
                    Text(notes)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onOpenRelease(info.releaseUrl)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.update_open_button))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

/**
 * Диалог с одной кнопкой "Закрыть" — для [MoreScreenUiState.UpToDate]
 * и [MoreScreenUiState.Error].
 */
@Composable
private fun MessageDialog(
    titleResId: Int,
    messageResId: Int,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleResId)) },
        text = { Text(stringResource(messageResId)) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

/**
 * Обрезает release notes до [MAX_RELEASE_NOTES_LENGTH] символов с многоточием.
 *
 * `takeIf { it.isNotEmpty() }` обязателен: GitHub может вернуть пустое `body`,
 * тогда блок notes не должен отрисовываться вовсе.
 */
private fun trimReleaseNotes(notes: String?): String? =
    notes
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?.take(MAX_RELEASE_NOTES_LENGTH)
        ?.let { if (it.length == MAX_RELEASE_NOTES_LENGTH) "$it…" else it }

// ==================== PREVIEWS ====================

private val sampleUpdateInfo =
    UpdateInfo(
        version = "1.2.3",
        releaseUrl = "https://github.com/easydev991/Jetpack-Days/releases/tag/v1.2.3",
        notes = "Исправлены ошибки, улучшена стабильность."
    )

@Preview(showBackground = true, name = "Update Available")
@Composable
fun UpdateAvailablePreview() {
    JetpackDaysTheme {
        UpdateCheckDialog(
            state = MoreScreenUiState.UpdateAvailable(sampleUpdateInfo),
            onDismiss = {},
            onOpenRelease = {}
        )
    }
}

@Preview(showBackground = true, name = "Up To Date")
@Composable
fun UpToDatePreview() {
    JetpackDaysTheme {
        UpdateCheckDialog(
            state = MoreScreenUiState.UpToDate,
            onDismiss = {},
            onOpenRelease = {}
        )
    }
}

@Preview(showBackground = true, name = "Update Check Error")
@Composable
fun ErrorPreview() {
    JetpackDaysTheme {
        UpdateCheckDialog(
            state = MoreScreenUiState.Error,
            onDismiss = {},
            onOpenRelease = {}
        )
    }
}
