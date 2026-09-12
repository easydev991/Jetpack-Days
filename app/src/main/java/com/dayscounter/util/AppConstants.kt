package com.dayscounter.util

/** Константы для внешних ссылок приложения. */
object AppConstants {
    /** URL страницы приложения для оценки в RuStore */
    const val APP_RATE_URL = "https://apps.rustore.ru/app/com.dayscounter/reviews"

    /** URL страницы приложения для шеринга в RuStore */
    const val APP_SHARE_URL = "https://apps.rustore.ru/app/com.dayscounter"

    /** URL репозитория приложения на GitHub */
    const val GITHUB_REPOSITORY_URL = "https://github.com/easydev991/Jetpack-Days"

    /** URL GitHub Releases API для проверки наличия новой версии (возвращает JSON, не HTML) */
    const val GITHUB_RELEASES_API_URL =
        "https://api.github.com/repos/easydev991/Jetpack-Days/releases/latest"
}
