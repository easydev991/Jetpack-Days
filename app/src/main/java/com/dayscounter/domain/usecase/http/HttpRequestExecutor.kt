package com.dayscounter.domain.usecase.http

/**
 * HTTP-запрос к внешнему API.
 *
 * @property url Абсолютный URL запроса
 * @property headers HTTP-заголовки запроса
 */
internal data class HttpRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap()
)

/**
 * HTTP-ответ от внешнего API.
 *
 * @property statusCode HTTP-статус ответа
 * @property body Тело ответа (пустое для ответов со статусом не 200)
 */
internal data class HttpResponse(
    val statusCode: Int,
    val body: String
)

/**
 * Узкая абстракция блокирующего HTTP-вызова.
 *
 * Единственное сетевое взаимодействие в проекте (осознанный carve-out из
 * офлайн-правила): production-реализация — платформенный [javax.net.ssl.HttpsURLConnection]
 * без сторонних сетевых библиотек. В unit-тестах подменяется SAM-лямбдой.
 */
internal fun interface HttpRequestExecutor {
    fun execute(request: HttpRequest): HttpResponse
}
