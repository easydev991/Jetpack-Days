package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.repository.ItemRepository
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException
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
 * @property context Контекст приложения
 */
class ImportBackupUseCase(
    private val repository: ItemRepository,
    private val context: Context
) {
    private val json =
        Json {
            ignoreUnknownKeys = true
        }

    /**
     * Импортирует записи из файла.
     *
     * @param uri URI файла для импорта
     * @return Result с количеством импортированных записей или ошибкой
     */
    @OptIn(ExperimentalSerializationApi::class)
    suspend operator fun invoke(uri: Uri): Result<Int> =
        try {
            val existingItems =
                repository
                    .getAllItems()
                    .first()

            val backupItems: List<BackupItem> =
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseBackupFile(inputStream)
                } ?: throw BackupException("Не удалось открыть InputStream для чтения")

            val newItems =
                backupItems
                    .mapNotNull { it.toItem() }
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
        } catch (e: FileNotFoundException) {
            Result.failure(BackupException("Не удалось найти файл: ${e.message}", e))
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
    @OptIn(ExperimentalSerializationApi::class)
    private fun parseBackupFile(inputStream: java.io.InputStream): List<BackupItem> {
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        val wrapperResult = runCatching { json.decodeFromString<BackupWrapper>(jsonString) }

        if (wrapperResult.isSuccess) {
            val wrapper = wrapperResult.getOrThrow()
            return wrapper.items
        }

        val iosWrapperResult = runCatching { json.decodeFromString<IosBackupWrapper>(jsonString) }

        if (iosWrapperResult.isSuccess) {
            val iosWrapper = iosWrapperResult.getOrThrow()
            return iosWrapper.items.mapNotNull { iosItem -> iosItem.toBackupItem() }
        }

        return json.decodeFromString<List<BackupItem>>(jsonString)
    }
}
