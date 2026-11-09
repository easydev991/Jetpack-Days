package com.dayscounter.domain.exception

/**
 * Доменная классификация ошибок запуска review-флоу.
 *
 * Единый набор исходов, в который flavor-реализация лаунчера маппит иерархию
 * ошибок SDK RuStore («RuStore не установлен», «RuStore устарел», «пользователь
 * не авторизован», «лимит запросов исчерпан», «отзыв уже существует», базовая
 * ошибка SDK). Домен о классах SDK ничего не знает: варианты описывают семантику
 * ошибки доменными словами.
 */
sealed class ReviewException : Exception() {
    /**
     * Приложение-магазин не установлено (в rustore flavor — RuStore не установлен).
     */
    data class NotInstalled(
        override val message: String
    ) : ReviewException()

    /**
     * Приложение-магазин устарело и не поддерживает review-флоу (в rustore flavor — RuStore устарел).
     */
    data class Outdated(
        override val message: String
    ) : ReviewException()

    /**
     * Пользователь не авторизован в приложении-магазине.
     */
    data class NotAuthorized(
        override val message: String
    ) : ReviewException()

    /**
     * Лимит запросов показа формы исчерпан — регулируется на стороне магазина.
     */
    data class RequestLimitReached(
        override val message: String
    ) : ReviewException()

    /**
     * Пользователь уже оставил отзыв на приложение.
     */
    data class ReviewAlreadyExists(
        override val message: String
    ) : ReviewException()

    /**
     * Прочая ошибка запуска флоу (в rustore flavor — базовая ошибка SDK и
     * неожиданные сбои). В отличие от остальных вариантов хранит причину.
     */
    data class Unknown(
        override val message: String,
        override val cause: Throwable? = null
    ) : ReviewException()
}
