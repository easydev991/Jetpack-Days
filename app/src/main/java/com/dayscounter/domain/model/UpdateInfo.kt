package com.dayscounter.domain.model

/**
 * Информация о доступном обновлении приложения.
 *
 * @property version Версия нового релиза (из tag_name без префикса "v")
 * @property releaseUrl URL страницы релиза на GitHub
 * @property notes Текст release notes (может отсутствовать)
 */
data class UpdateInfo(
    val version: String,
    val releaseUrl: String,
    val notes: String?
)
