package com.dayscounter.domain.usecase

import com.dayscounter.domain.exception.BackupException
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.OutputStream

/**
 * Use Case для экспорта данных в резервную копию.
 *
 * Экспортирует все записи в JSON формат с обёрткой BackupWrapper,
 * содержащей поле format = "android" для указания платформы-источника.
 *
 * @property repository Repository для работы с данными
 */
class ExportBackupUseCase(
    private val repository: ItemRepository
) {
    private val json =
        Json {
            prettyPrint = true
        }

    /**
     * Экспортирует все записи в поток (Android формат).
     *
     * @param outputStream Поток для записи JSON данных резервной копии
     * @return Result с количеством экспортированных записей или ошибкой
     */
    suspend operator fun invoke(outputStream: OutputStream): Result<Int> =
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

            outputStream.use { it.write(jsonString.toByteArray()) }

            Result.success(items.size)
        } catch (e: IOException) {
            Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
        } catch (e: SerializationException) {
            Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
        }
}
