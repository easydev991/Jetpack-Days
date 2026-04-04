package com.dayscounter.domain.usecase

import android.content.Context
import android.net.Uri
import com.dayscounter.domain.repository.ItemRepository
import com.dayscounter.util.Logger
import kotlinx.coroutines.flow.first
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException
import java.sql.SQLException

private const val TAG = "ImportBackupUseCase"

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
 * @property logger Logger для логирования (по умолчанию AndroidLogger)
 */
class ImportBackupUseCase(
    private val repository: ItemRepository,
    private val context: Context,
    private val logger: Logger = com.dayscounter.util.AndroidLogger()
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
            logger.d(TAG, "Начало импорта данных")

            // Получаем все существующие записи для проверки дубликатов
            val existingItems =
                repository
                    .getAllItems()
                    .first()

            logger.d(TAG, "В базе данных ${existingItems.size} записей")

            // Читаем JSON из файла и определяем формат
            val backupItems: List<BackupItem> =
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parseBackupFile(inputStream)
                } ?: throw BackupException("Не удалось открыть InputStream для чтения")

            logger.d(TAG, "В файле ${backupItems.size} записей")

            // Фильтруем дубликаты
            val newItems =
                backupItems
                    .mapNotNull { it.toItem() }
                    .filter { newItem ->
                        // Проверяем, есть ли такая запись уже в базе
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
        // Считываем весь поток в строку для повторного использования
        val jsonString = inputStream.bufferedReader().use { it.readText() }

        // Попытка декодировать как BackupWrapper (Android формат с полем format)
        val wrapperResult = runCatching { json.decodeFromString<BackupWrapper>(jsonString) }

        if (wrapperResult.isSuccess) {
            val wrapper = wrapperResult.getOrThrow()
            logger.d(TAG, "Обнаружен Android формат: ${wrapper.format ?: "не указан"}")
            // Android формат или формат не указан — используем как есть
            return wrapper.items
        }

        // Попытка декодировать как IosBackupWrapper (iOS формат с timestamp: Double)
        val iosWrapperResult = runCatching { json.decodeFromString<IosBackupWrapper>(jsonString) }

        if (iosWrapperResult.isSuccess) {
            val iosWrapper = iosWrapperResult.getOrThrow()
            logger.d(TAG, "Обнаружен iOS формат: ${iosWrapper.format}")
            // iOS формат: конвертируем каждый IosBackupItem в BackupItem
            return iosWrapper.items.mapNotNull { iosItem -> iosItem.toBackupItem() }
        }

        // Fallback: попытка декодировать как List<BackupItem> (старый формат без обёртки)
        logger.d(TAG, "Fallback: декодирование как List<BackupItem> (старый формат)")
        return json.decodeFromString<List<BackupItem>>(jsonString)
    }
}
