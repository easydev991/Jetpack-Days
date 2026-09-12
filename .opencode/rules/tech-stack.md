# Технологический стек

## Зависимости

- **Jetpack Compose** - UI
- **Navigation Compose** - навигация
- **ViewModel** - управление состоянием UI
- **Room** - локальная БД
- **DataStore** - простое хранение
- **Coroutines** - асинхронность
- **kotlinx-serialization** - JSON для резервного копирования
- **Firebase Crashlytics** - сбор ошибок (только release)
- **Firebase Analytics** - breadcrumb logs для отладки крашей
- **JUnit 5** - unit-тесты
- **MockK** - мокирование
- **Espresso** - UI тесты

**ВАЖНО:** Сетевые библиотеки (Retrofit, OkHttp, Ktor) НЕ используются.

**Исключение (carve-out):** ручная проверка обновлений (`CheckForAppUpdateUseCase`) использует платформенный `javax.net.ssl.HttpsURLConnection` (нативный Android API с API 1, без сетевых библиотек и новых зависимостей) через узкую абстракцию `HttpRequestExecutor`. Это единственное разрешённое сетевое взаимодействие в проекте и НЕ прецедент для других фич — новые сетевые фичи по-прежнему запрещены офлайн-правилом.

Версии зависимостей автоматически обновляются в `gradle/libs.versions.toml` и отображаются в `README.md`. Ручное указание версий здесь не требуется.
