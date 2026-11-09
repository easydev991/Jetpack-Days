package com.dayscounter.review

/**
 * In-memory holder сессии авто-запроса review-флоу (design D2 change
 * `add-rustore-in-app-review`): флаг «авто-запрос уже выполнялся
 * в этой сессии».
 *
 * Общий singleton presentation-слоя без flavor-вариантов и без
 * SDK-зависимостей: флаг пишет `MainScreenViewModel`, читает
 * `CreateEditScreenViewModel` — оба получают один и тот же экземпляр.
 * Состояние намеренно не переживает процесс: после перезапуска
 * авто-запрос снова разрешён (лимит RuStore SDK всё равно свой).
 */
object ReviewSessionHolder {
    @Volatile
    private var autoRequestShown: Boolean = false

    /**
     * Сообщает, выполнялся ли авто-запрос review-флоу в текущей сессии.
     */
    fun wasAutoRequestShown(): Boolean = autoRequestShown

    /**
     * Отмечает, что авто-запрос review-флоу в текущей сессии выполнен.
     */
    fun markAutoRequestShown() {
        autoRequestShown = true
    }

    /**
     * Сбрасывает флаг сессии: вызывается при полном удалении данных (после
     * сброса триггеров авто-запрос снова разрешён в текущей сессии) и в
     * unit-тестах (singleton общий на весь тестовый JVM, без сброса
     * состояние текло бы между тест-классами).
     */
    fun reset() {
        autoRequestShown = false
    }
}
