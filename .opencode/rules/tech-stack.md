# Технологический стек

- **UI**: Jetpack Compose, Navigation Compose, ViewModel
- **Данные**: Room, DataStore, Coroutines, kotlinx-serialization (JSON бэкапа,
  совместим с iOS-приложением)
- **Тесты**: JUnit 5, MockK, kotlinx-coroutines-test (unit); Compose Testing
  (androidTest)
- **Telemetry**: Firebase Crashlytics + Analytics — только release

**ВАЖНО:** Сетевые библиотеки (Retrofit, OkHttp, Ktor) НЕ используются —
приложение полностью офлайн.

**Исключение (carve-out):** ручная проверка обновлений
(`CheckForAppUpdateUseCase`) использует платформенный
`javax.net.ssl.HttpsURLConnection` (нативный Android API с API 1, без
сетевых библиотек и новых зависимостей) через узкую абстракцию
`HttpRequestExecutor`. Единственное разрешённое сетевое взаимодействие
в проекте и НЕ прецедент для других фич — новые сетевые фичи по-прежнему
запрещены офлайн-правилом.

Версии — только в `gradle/libs.versions.toml` (source of truth), обновляются
автоматически и отображаются в `README.md`.
