package com.dayscounter.domain.usecase

import com.dayscounter.domain.exception.BackupException
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream
import java.sql.SQLException

/**
 * Use Case для импорта данных из резервной копии.
 *
 * Импортирует записи из JSON файла, поддерживая форматы iOS и Android.
 * Предотвращает дубликаты путем сравнения записей по всем полям.
 *
 * Поддерживаемые форматы:
 * - Новый формат с обёрткой BackupWrapper (с полем format: "ios" или "android")
 * - Старый формат без обёртки (массив BackupItem напрямую)
 *
 * @property repository Repository для работы с данными
 * @property logger Logger для логирования ошибок парсинга
 */
class ImportBackupUseCase(
    private val repository: ItemRepository,
    private val logger: Logger
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    /**
     * Импортирует записи из файла.
     *
     * @param inputStream Поток с JSON данными резервной копии
     * @return Result с количеством импортированных записей или ошибкой
     */
    suspend operator fun invoke(inputStream: InputStream): Result<Int> =
        try {
            val existingItems =
                repository
                    .getAllItems()
                    .first()

            val backupItems: List<BackupItem> =
                inputStream.use {
                    parseBackupFile(it)
                }

            val newItems =
                backupItems
                    .mapNotNull { it.toItem(logger) }
                    .filter { newItem ->
                        !existingItems.any { existingItem ->
                            existingItem.title == newItem.title &&
                                existingItem.details == newItem.details &&
                                existingItem.timestamp == newItem.timestamp &&
                                existingItem.displayOption == newItem.displayOption
                        }
                    }

            var importedCount = 0
            newItems.forEach { item ->
                repository.insertItem(item)
                importedCount++
            }

            Result.success(importedCount)
        } catch (e: IOException) {
            Result.failure(BackupException("Не удалось прочитать файл: ${e.message}", e))
        } catch (e: SerializationException) {
            Result.failure(BackupException("Не удалось распарсить файл: ${e.message}", e))
        } catch (e: SQLException) {
            Result.failure(BackupException("Не удалось сохранить данные: ${e.message}", e))
        }

    /**
     * Парсит файл резервной копии, определяя формат автоматически.
     *
     * Логика определения формата:
     * 1. Попытка декодировать как BackupWrapper (Android формат с timestamp: Long)
     * 2. Попытка декодировать как IosBackupWrapper (iOS формат с timestamp: Double)
     * 3. Fallback на List<BackupItem> (старый формат без обёртки)
     *
     * @param inputStream Поток с JSON данными
     * @return Список BackupItem в Android-формате
     */
    private fun parseBackupFile(inputStream: InputStream): List<BackupItem> {
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val wrapperResult = runCatching { json.decodeFromString<BackupWrapper>(jsonString) }
        val iosWrapperResult = runCatching { json.decodeFromString<IosBackupWrapper>(jsonString) }

        return when {
            wrapperResult.isSuccess -> wrapperResult.getOrThrow().items
            iosWrapperResult.isSuccess ->
                iosWrapperResult.getOrThrow().items.mapNotNull { it.toBackupItem(logger) }
            else -> json.decodeFromString<List<BackupItem>>(jsonString)
        }
    }
}
