package com.dayscounter.domain.usecase.http

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO ответа GitHub Releases API (endpoint releases/latest).
 * Документация: https://docs.github.com/en/rest/releases/releases#get-the-latest-release
 *
 * Все поля nullable: отсутствие ключа в ответе не должно приводить к падению
 * парсинга ([MissingFieldException]); defensive-проверка выполняется в use case.
 *
 * @property tagName Тег релиза, например "v1.2.3"
 * @property htmlUrl URL страницы релиза на GitHub
 * @property body Текст release notes (может отсутствовать)
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name") val tagName: String? = null,
    @SerialName("html_url") val htmlUrl: String? = null,
    val body: String? = null
)
