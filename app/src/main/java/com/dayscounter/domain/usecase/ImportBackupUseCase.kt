package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.FileNotFoundException
import java.io.IOException
import java.sql.SQLException

private const val TAG = "ImportBackupUseCase"

/**
 * Use Case для импорта данных из резервной копии.
 *
 * Импортирует записи из JSON файла, формата, совместимого с iOS-приложением.
 * Предотвращает дубликаты путем сравнения записей по всем полям.
 *
 * @property repository Repository для работы с данными
 * @property context Контекст приложения
 * @property logger Logger для логирования (по умолчанию AndroidLogger)
 */
class ImportBackupUseCase(
    private val repository: ItemRepository,
    private val context: Context,
    private val logger: Logger = com.dayscounter.util.AndroidLogger(),
) {
    /**
     * Импортирует записи из файла.
     *
     * @param uri URI файла для импорта
     * @return Result с количеством импортированных записей или ошибкой
     */
    suspend operator fun invoke(uri: Uri): Result<Int> =
        try {
            logger.d(TAG, "Начало импорта данных")

            // Получаем все существующие записи для проверки дубликатов
            val existingItems =
                repository
                    .getAllItems()
                    .first()

            logger.d(TAG, "В базе данных ${existingItems.size} записей")

            // Читаем JSON из файла
            val backupItems: List<BackupItem> =
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    Json { ignoreUnknownKeys = true }.decodeFromStream(inputStream)
                } ?: throw BackupException("Не удалось открыть InputStream для чтения")

            logger.d(TAG, "В файле ${backupItems.size} записей")

            // Фильтруем дубликаты
            val newItems =
                backupItems
                    .mapNotNull { it.toItem() }
                    .filter { newItem ->
                        // Проверяем, есть ли такая запись уже в базе
                        // Для проверки дубликатов используем null для colorTag, так как
                        // импорт из iOS с Data не конвертируется корректно
                        !existingItems.any { existingItem ->
                            existingItem.title == newItem.title &&
                                existingItem.details == newItem.details &&
                                existingItem.timestamp == newItem.timestamp &&
                                existingItem.displayOption == newItem.displayOption
                        }
                    }

            logger.d(TAG, "Новых записей для импорта: ${newItems.size}")

            // Вставляем новые записи в базу данных
            var importedCount = 0
            newItems.forEach { item ->
                repository.insertItem(item)
                importedCount++
            }

            logger.d(TAG, "Импорт завершен успешно, импортировано $importedCount записей")
            Result.success(importedCount)
        } catch (e: FileNotFoundException) {
            logger.e(TAG, "Ошибка при импорте данных", e)
            Result.failure(BackupException("Не удалось найти файл: ${e.message}", e))
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка при импорте данных", e)
            Result.failure(BackupException("Не удалось прочитать файл: ${e.message}", e))
        } catch (e: SerializationException) {
            logger.e(TAG, "Ошибка при импорте данных", e)
            Result.failure(BackupException("Не удалось распарсить файл: ${e.message}", e))
        } catch (e: SQLException) {
            logger.e(TAG, "Ошибка при импорте данных", e)
            Result.failure(BackupException("Не удалось сохранить данные: ${e.message}", e))
        }
}
