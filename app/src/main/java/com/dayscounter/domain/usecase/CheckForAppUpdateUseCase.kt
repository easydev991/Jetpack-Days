package com.dayscounter.domain.usecase

import com.dayscounter.BuildConfig
import com.dayscounter.domain.model.UpdateInfo
import com.dayscounter.domain.usecase.http.GitHubRelease
import com.dayscounter.domain.usecase.http.HttpRequest
import com.dayscounter.domain.usecase.http.HttpRequestExecutor
import com.dayscounter.util.AndroidLogger
import com.dayscounter.util.AppConstants
import com.dayscounter.util.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/**
 * Проверяет наличие новой версии приложения через GitHub Releases API.
 *
 * Отправляет GET-запрос на [AppConstants.GITHUB_RELEASES_API_URL], парсит ответ и
 * семантически сравнивает версию релиза с текущей. Любая ошибка (сеть, HTTP != 200,
 * парсинг, отсутствие полей) маппится в [AppUpdateException] — крашей быть не должно.
 *
 * Единственное сетевое взаимодействие в проекте (carve-out из офлайн-правила),
 * HTTP-вызов — через платформенный [HttpRequestExecutor] (см. design.md D1/D4).
 *
 * @property executor Абстракция блокирующего HTTP-вызова
 * @property currentVersion Текущая версия приложения (для тестов переопределяется)
 * @property logger Логгер
 */
@Suppress("TooGenericExceptionCaught")
class CheckForAppUpdateUseCase internal constructor(
    private val executor: HttpRequestExecutor,
    private val currentVersion: String = BuildConfig.VERSION_NAME,
    private val logger: Logger = AndroidLogger()
) {
    companion object {
        private const val TAG = "CheckForAppUpdateUseCase"
        private const val HTTP_OK = 200
        private const val VERSION_PREFIX = "v"
        private val json = Json { ignoreUnknownKeys = true }
    }

    /**
     * Выполняет проверку обновлений.
     *
     * @return [Result.success] с [UpdateInfo] — доступно обновление,
     * [Result.success] с null — установлена последняя версия (или текущая новее),
     * [Result.failure] с [AppUpdateException] — ошибка проверки.
     */
    suspend operator fun invoke(): Result<UpdateInfo?> =
        try {
            val request =
                HttpRequest(
                    url = AppConstants.GITHUB_RELEASES_API_URL,
                    headers = mapOf("Accept" to "application/vnd.github+json")
                )
            // Блокирующий вызов запрещён на main thread — уводим на IO
            val response = withContext(Dispatchers.IO) { executor.execute(request) }
            if (response.statusCode != HTTP_OK) {
                Result.failure(AppUpdateException("HTTP ${response.statusCode}"))
            } else {
                parseRelease(response.body)
            }
        } catch (e: CancellationException) {
            // Отмена корутины не должна превращаться в Result.failure
            throw e
        } catch (e: Exception) {
            logger.e(TAG, "Ошибка проверки обновлений", e)
            Result.failure(AppUpdateException("Не удалось проверить обновления: ${e.message}", e))
        }

    /**
     * Парсит тело ответа GitHub и сравнивает версии (см. design.md D2/D10).
     * Исключения парсинга ловит вызывающий [invoke].
     */
    private fun parseRelease(body: String): Result<UpdateInfo?> {
        val release = json.decodeFromString<GitHubRelease>(body)
        val tagName = release.tagName
        val htmlUrl = release.htmlUrl
        if (tagName == null || htmlUrl == null) {
            return Result.failure(AppUpdateException("Поле 'tag_name'/'html_url' отсутствует в ответе GitHub"))
        }
        val latestVersion = tagName.removePrefix(VERSION_PREFIX)
        return if (compareVersions(latestVersion, currentVersion) > 0) {
            Result.success(UpdateInfo(version = latestVersion, releaseUrl = htmlUrl, notes = release.body))
        } else {
            Result.success(null)
        }
    }

    /**
     * Семантическое сравнение версий major.minor.patch (см. design.md D2).
     * Невалидные компоненты считаются нулём, отсутствующие компоненты добиваются нулями.
     *
     * @return отрицательное число, если [first] < [second]; 0 при равенстве;
     * положительное, если [first] > [second].
     */
    private fun compareVersions(
        first: String,
        second: String
    ): Int {
        val firstParts = first.split(".").map { it.toIntOrNull() ?: 0 }
        val secondParts = second.split(".").map { it.toIntOrNull() ?: 0 }
        val size = maxOf(firstParts.size, secondParts.size)
        for (index in 0 until size) {
            val comparison = firstParts.getOrElse(index) { 0 }.compareTo(secondParts.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
