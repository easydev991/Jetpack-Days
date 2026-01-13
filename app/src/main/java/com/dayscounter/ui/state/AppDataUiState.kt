package com.dayscounter.ui.state

/**
 * UI State для экрана App Data Screen.
 *
 * @property hasItems Признак наличия записей в базе данных
 * @property isExporting Индикатор экспорта данных
 * @property isImporting Индикатор импорта данных
 * @property isDeleting Индикатор удаления данных
 * @property showDeleteDialog Признак отображения диалога подтверждения удаления
 * @property resultMessage Сообщение о результате операции для Toast (null если нет сообщения)
 * @property isError Признак ошибки операции (true = ошибка, false = успех)
 */
data class AppDataUiState(
    val hasItems: Boolean = false,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isDeleting: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val resultMessage: String? = null,
    val isError: Boolean = false,
)
