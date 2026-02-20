package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

private const val TAG = "ExportBackupUseCase"

/**
 * Use Case для экспорта данных в резервную копию.
 *
 * Экспортирует все записи в JSON формат, совместимый с iOS-приложением.
 *
 * @property repository Repository для работы с данными
 * @property context Контекст приложения
 * @property logger Logger для логирования (по умолчанию AndroidLogger)
 */
class ExportBackupUseCase(
    private val repository: ItemRepository,
    private val context: Context,
    private val logger: Logger = com.dayscounter.util.AndroidLogger(),
) {
    private val json =
        Json {
            prettyPrint = true
        }

    /**
     * Экспортирует все записи в файл.
     *
     * @param uri URI файла для сохранения
     * @return Result с количеством экспортированных записей или ошибкой
     */
    suspend operator fun invoke(uri: Uri): Result<Int> =
        try {
            logger.d(TAG, "Начало экспорта данных")

            // Получаем все записи из репозитория
            val items =
                repository
                    .getAllItems()
                    .map { it }
                    .first()

            logger.d(TAG, "Получено ${items.size} записей для экспорта")

            // Конвертируем в формат JSON, совместимый с iOS
            val backupItems = items.map { it.toBackupItem() }
            val jsonString = json.encodeToString(backupItems)

            // Записываем в файл
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw BackupException("Не удалось открыть OutputStream для записи")

            logger.d(TAG, "Экспорт завершен успешно, экспортировано ${items.size} записей")
            Result.success(items.size)
        } catch (e: IOException) {
            logger.e(TAG, "Ошибка при экспорте данных", e)
            Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
        } catch (e: SerializationException) {
            logger.e(TAG, "Ошибка при экспорте данных", e)
            Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
        }
}
