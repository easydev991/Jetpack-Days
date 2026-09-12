package com.dayscounter.ui.state

import com.dayscounter.domain.model.UpdateInfo

/**
 * Состояние проверки обновлений на экране "Ещё" (MoreScreen).
 *
 * Диалог показывается для состояний [UpToDate], [UpdateAvailable] и [Error];
 * [Idle] и [Checking] — диалог закрыт.
 */
sealed class MoreScreenUiState {
    /** Проверка не выполнялась (или диалог закрыт). */
    data object Idle : MoreScreenUiState()

    /** Выполняется запрос к GitHub Releases API. */
    data object Checking : MoreScreenUiState()

    /** Установлена последняя версия. */
    data object UpToDate : MoreScreenUiState()

    /** Доступно обновление с информацией о релизе. */
    data class UpdateAvailable(
        val info: UpdateInfo
    ) : MoreScreenUiState()

    /**
     * Ошибка проверки. Текст берётся в диалоге напрямую из `R.string.update_error` —
     * одной ошибки достаточно, `@StringRes`-индирекция не нужна.
     */
    data object Error : MoreScreenUiState()
}
