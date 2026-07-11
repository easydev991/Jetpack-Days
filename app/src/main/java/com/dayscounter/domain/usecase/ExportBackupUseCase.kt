package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * Use Case для экспорта данных в резервную копию.
 *
 * Экспортирует все записи в JSON формат с обёрткой BackupWrapper,
 * содержащей поле format = "android" для указания платформы-источника.
 *
 * @property repository Repository для работы с данными
 * @property context Контекст приложения
 */
class ExportBackupUseCase(
    private val repository: ItemRepository,
    private val context: Context
) {
    private val json =
        Json {
            prettyPrint = true
        }

    /**
     * Экспортирует все записи в файл (Android формат).
     *
     * @param uri URI файла для сохранения
     * @return Result с количеством экспортированных записей или ошибкой
     */
    suspend operator fun invoke(uri: Uri): Result<Int> =
        try {
            val items =
                repository
                    .getAllItems()
                    .map { it }
                    .first()

            val backupItems = items.map { it.toBackupItem() }

            val wrapper =
                BackupWrapper(
                    format = BackupFormat.ANDROID,
                    items = backupItems
                )

            val jsonString = json.encodeToString(BackupWrapper.serializer(), wrapper)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw BackupException("Не удалось открыть OutputStream для записи")

            Result.success(items.size)
        } catch (e: IOException) {
            Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
        } catch (e: SerializationException) {
            Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
        }
}
