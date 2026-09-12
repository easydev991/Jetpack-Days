## Context

См. `proposal.md` (Why) для мотивации. Здесь только технические вводные, необходимые для описания подхода.

Текущее состояние:

- Экран `MoreScreen` (`app/src/main/java/com/dayscounter/ui/screens/more/MoreScreen.kt`) уже имеет блок `if (BuildConfig.RUSTORE_FEATURES) { ... }` для RuStore-специфичных кнопок "Оценить приложение" и "Поделиться приложением". Существующая кнопка "GitHub" вынесена за этот блок и видна на обоих flavor. Открытие URL делается через приватную функцию `openGitHub(context)` с `Intent.ACTION_VIEW`.
- `AppConstants.kt` содержит константу `GITHUB_REPOSITORY_URL = "https://github.com/easydev991/Jetpack-Days"`.
- DI — ручной через factory методы в `FormatterModule` и `AppModule`. Конвенция размещения: **use case factories — в `FormatterModule`** (`CalculateDaysDifferenceUseCase`, `FormatDaysTextUseCase`, `GetFormattedDaysForItemUseCase`, `GetDaysAnalysisTextUseCase`); **ViewModel factories + инфраструктура — в `AppModule`** (`ItemRepository`, `ReminderRepository`, `ReminderManager`, `DataStore`, `AnalyticsService`, `ExactAlarmPermissionHelper`, `ExactAlarmPermissionViewModel.factory`). Новый `createCheckForAppUpdateUseCase()` пойдёт в `FormatterModule`, `createMoreScreenViewModelFactory()` — в `AppModule`.
- ViewModel-паттерн проекта: `MutableStateFlow` + `companion object { fun factory(...) }` + фабрика регистрируется в `AppModule` через `viewModelFactory { initializer { ... } }`. См. `ExactAlarmPermissionViewModel` как образец.
- Use cases возвращают `Result<T>` и маппят исключения в доменные (`BackupException`, `ItemException`). Для нового use case введём `AppUpdateException`.
- AGENTS.md жёстко запрещает сетевые библиотеки (Retrofit/OkHttp/Ktor). Для этой фичи фиксируем решение: используем платформенный `javax.net.ssl.HttpsURLConnection` (нативный Android API, доступен с API 1, без новых зависимостей и без desugaring). Это расширение офлайн-first модели, не прецедент для других сетевых фич. `java.net.http.HttpClient` (JDK 11) — **не** входит в Android SDK ни на одном API-уровне и **не** покрывается `desugar_jdk_libs`, поэтому не используется.

## Goals / Non-Goals

**Goals:**

- Минимальный аддитивный UI: одна кнопка и один диалог на `MoreScreen`.
- Чистое разделение слоёв: `domain/usecase` → `ui/viewmodel` → `ui/screens/more`, без протекания `HttpsURLConnection` в UI.
- Тестируемость use case без сети: подмена HTTP-клиента через конструктор (см. Decisions).
- Корректная обработка отсутствия сети/ошибок парсинга без краша.

**Non-Goals:**

- Автоматическая фоновая проверка обновлений (YAGNI — пользователь может нажать кнопку).
- Поддержка pre-release / draft релизов GitHub (только `releases/latest`).
- In-app обновление (только переход в браузер на страницу релиза).
- Поддержка flavor rustore (явно отключено).
- Расширение списка сетевых зависимостей — сетевой вызов строго через платформенный `HttpsURLConnection`.
- Индикатор загрузки / дизейбл кнопки во время `Checking` (осознанно: проверка ручная, ≤10 с; см. `tasks.md:4.3` — повторный тап отменяет предыдущую корутину и запускает новую).

## Decisions

### D1. HTTP-вызов через `HttpRequestExecutor`, реализация — `HttpsURLConnection`

**Решение:** `CheckForAppUpdateUseCase` принимает абстракцию HTTP-вызова (см. D4) в конструкторе. Factory в `FormatterModule.createCheckForAppUpdateUseCase()` (use case factory — конвенция `FormatterModule`, см. Context) создаёт executor (через `HttpRequestExecutor`-лямбду поверх `HttpsURLConnection`, см. D4) и передаёт его в `CheckForAppUpdateUseCase`. HTTP-запрос (`HttpRequest` с URL + headers) строится внутри `CheckForAppUpdateUseCase.invoke()` на каждый вызов. Реализация executor'а — `HttpsURLConnection`: таймауты подключения/чтения по 10 секунд (`connectTimeout` / `readTimeout`), `instanceFollowRedirects = false` (GitHub API endpoint финальный, редиректы не ожидаются и не должны маскировать ошибку). Никакого state у executor'а нет — каждый вызов создаёт новое соединение через `URI.create(url).toURL().openConnection() as HttpsURLConnection`; это безопасно: state нет у экземпляра executor'а, а keep-alive пул (если активен) ведёт сама платформа (libcore), код его не создаёт и не очищает.

**Альтернативы:**

- `java.net.http.HttpClient` (JDK 11) → не входит в Android SDK ни на одном API-уровне; код не скомпилируется.
- Создавать `HttpsURLConnection` внутри use case → не тестируется без реальной сети.
- Singleton через `object` → нарушает конвенцию проекта (DI через фабрики).

**Почему так:** `HttpsURLConnection` — нативный API с API 1, нулевые зависимости, идеологически чище для офлайн-first проекта. Соответствует DI-паттерну проекта, легко мокается в unit-тестах через `HttpRequestExecutor`-лямбду (D4).

### D2. Сравнение версий без сторонних semver-библиотек

**Решение:** Hand-rolled парсер `major.minor.patch`: `version.removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }`. Сравнение покомпонентное: оба списка добиваются нулями до равной длины (`zip` с правилом «отсутствующий компонент = 0», соответствует semver `1.2 ≡ 1.2.0`), затем сравниваются поэлементно. Возврат `-1 / 0 / 1`.

**Альтернативы:** Подключить `io.github.z4kn4fein:semver` или подобное. **Отклонено:** over-engineering для двухстрочного парсинга.

**Почему так:** semver-сравнение укладывается в ~10 строк Kotlin; дополнительная зависимость не оправдана. BuildConfig.VERSION_NAME всегда в формате `major.minor.patch` (см. `gradle.properties`).

### D3. Состояние диалога — `sealed class MoreScreenUiState` в `ui/state/`

**Решение:** Отдельный файл `MoreScreenUiState.kt` с sealed-классом по требованию proposal. Содержит `Idle`, `Checking`, `UpToDate`, `UpdateAvailable(info)`, `Error` (без параметров — YAGNI: одна error-строка `R.string.update_error` берётся в `UpdateCheckDialog` напрямую через `stringResource`, см. tasks.md:4.1).

**Альтернативы:** Enum с nullable `UpdateInfo` полем → менее выразительно, нет exhaustive when.

**Почему так:** соответствует code-style проекта (sealed-классы для UI-состояний, см. `DetailScreenState`).

### D4. `HttpRequestExecutor` через функциональный интерфейс

**Решение:** три простых типа в `domain/usecase/http/` (пакет рядом с `CheckForAppUpdateUseCase.kt`):

```kotlin
internal data class HttpRequest(val url: String, val headers: Map<String, String> = emptyMap())
internal data class HttpResponse(val statusCode: Int, val body: String)
internal fun interface HttpRequestExecutor { fun execute(request: HttpRequest): HttpResponse }
```

Видимость `internal`: `app/src/test/` находится в том же Gradle-модуле и имеет доступ — в тестах мокается через `mockk<HttpRequestExecutor>()` либо через SAM-лямбду. В production: реализация через `HttpsURLConnection` (см. D1). Свои типы `HttpRequest`/`HttpResponse` (а не JDK-классы) — у абстракции нет зависимости от `java.net.http`/`HttpURLConnection`, executor тривиально мокается через `every { executor.execute(ofType<HttpRequest>()) } returns HttpResponse(...)`.

**Альтернативы:**

- Подменять сам `HttpsURLConnection` через MockK → контракт широкий (10+ методов), большая поверхность для случайных возвратов.
- Делать интерфейс `GitHubApi { fun fetchLatestRelease(): String }` → лишний слой абстракции для одного вызова, не отделяет HTTP-вызов от GitHub-специфики.

**Почему так:** узкий контракт (один метод, два параметра) → достаточно SAM-лямбды `fun interface`, нет нужды в `mockk-static`/bytecode-перехвате. `execute` — блокирующий (как `HttpsURLConnection.getResponseCode()`/`inputStream`), в use case оборачивается в `withContext(Dispatchers.IO)` (см. tasks.md:3.2). Тесты: `every { executor.execute(any()) } returns HttpResponse(200, body)` (без `coEvery` — `execute` не `suspend`).

<!-- ponytail: SAM-обёртка только чтобы тестировать use case без сети; inline-соединение вернём, если появится второй endpoint. -->

### D5. `AppUpdateException` — простой класс с `message` и `cause` (по аналогии с `BackupException`)

**Решение:** `class AppUpdateException(message: String, cause: Throwable? = null) : Exception(message, cause)` — plain class в `app/src/main/java/com/dayscounter/domain/usecase/AppUpdateException.kt` (рядом с `BackupException.kt`, по аналогии с которым и сделана структура). Размещение в `domain/usecase/` (а не в `domain/exception/`) — для единообразия с `BackupException` (тоже plain class, тоже в `domain/usecase/`); `domain/exception/` зарезервирован для sealed-иерархий с типизированными case'ами (например, `ItemException` со `SaveFailed`/`DeleteFailed`/`LoadFailed`/`UpdateFailed`). UI не различает типы ошибок — показывает единое сообщение `R.string.update_error` (см. `spec.md`, Scenario «Ошибка проверки»), sealed-иерархия не нужна (YAGNI, см. Альтернативы).

**Альтернативы:** Sealed-иерархия (`NetworkError` / `ParseError`) → отклонено (YAGNI): UI использует одно сообщение, различие case'ов нигде не нужно; case'ы были бы мёртвой структурой.

**Почему так:** соответствует конвенции plain-class для доменных исключений с одной общей причиной (см. `BackupException`). `cause` сохраняет исходное исключение для диагностики в `Logger`.

### D6. Кнопка только в `MoreScreen`, без изменений в `RootScreen`

**Решение:** Кнопка добавляется в `MoreScreen.kt → ActionButtons(context)` внутри нового блока `if (!BuildConfig.RUSTORE_FEATURES) { ... }`, после существующей кнопки "GitHub". Используется существующий `MoreButton` composable.

**Альтернативы:** Делать отдельный `UpdateSection` composable → лишний компонент ради одной кнопки.

**Почему так:** минимум изменений в существующем composable, единый стиль кнопок экрана.

### D7. `MoreScreenViewModel` создаётся через `viewModel(factory = ...)`, не через `remember`

**Решение:** Внутри `MoreScreen` создаём `val viewModel: MoreScreenViewModel = viewModel(factory = AppModule.createMoreScreenViewModelFactory())` (без `LocalContext.current` — текущей реализации `context` не нужен, см. tasks.md:4.4). ViewModel живёт в scope NavBackStackEntry экрана.

**Альтернативы:** `remember { MoreScreenViewModel(...) }` → не переживает configuration changes и process death.

**Почему так:** конвенция проекта (см. `ExactAlarmPermissionInlineSection`).

### D8. Диалог `UpdateCheckDialog` — отдельный `@Composable` в `ui/screens/more/`

**Решение:** `UpdateCheckDialog(state: MoreScreenUiState, onDismiss: () -> Unit, onOpenRelease: (url: String) -> Unit)` — composable-функция с `@Preview` в `JetpackDaysTheme`. Принимает уже готовое состояние, не знает про ViewModel.

**Альтернативы:** Диалог внутри `MoreScreen` → god composable; без `Preview` → нет визуальной проверки в Android Studio.

**Почему так:** чистое разделение ответственности, тестируемо, переиспользуемо.

### D9. Trim release notes до 500 символов с многоточием

**Решение:** Внутри `UpdateCheckDialog` (UI-слой, не domain) — trim release notes до 500 символов с многоточием: `info.notes?.trim()?.takeIf { it.isNotEmpty() }?.take(500)?.let { if (it.length == 500) "$it…" else it }`. `takeIf { it.isNotEmpty() }` обязателен (GitHub может вернуть `body = ""`, иначе блок notes отрисуется пустым — нарушение `spec.md` Scenario «Release notes отсутствуют в ответе GitHub»). Реализация — `tasks.md:5.1`.

**Альтернативы:** Делать trim в use case → это presentation concern, утекает в domain.

**Почему так:** use case возвращает сырые данные, UI отвечает за отображение.

### D10. Data Flow: маппинг GitHub Releases API → доменная модель

Use case выполняет явный маппинг полей JSON-ответа GitHub Releases API на поля доменной модели `UpdateInfo`. Соответствие фиксировано:

| Поле JSON ответа | Поле `UpdateInfo` | Назначение |
|---|---|---|
| `tag_name` | `version` (после `removePrefix("v")`) | Сравнивается с `BuildConfig.VERSION_NAME` |
| `html_url` | `releaseUrl` | Передаётся в `Intent.ACTION_VIEW` |
| `body` | `notes` (nullable) | Release notes для отображения в диалоге |

Парсинг через `kotlinx.serialization` (`@Serializable data class GitHubRelease(@SerialName("tag_name") val tagName: String?, @SerialName("html_url") val htmlUrl: String?, val body: String?)` в `domain/usecase/http/` — рядом с `HttpRequestExecutor`, по прецеденту `BackupItem` в `domain/usecase/`, см. tasks.md:1.3), `Json { ignoreUnknownKeys = true }` для устойчивости к новым полям API. При отсутствии `tag_name` или `html_url` в ответе (теоретически невозможно по контракту API, но defensive — на случай частично-повреждённого JSON или будущих изменений API) — `Result.failure(AppUpdateException("Поле 'tag_name'/'html_url' отсутствует в ответе GitHub"))`. Nullable-поля позволяют парсеру не упасть на отсутствии ключа (иначе `kotlinx-serialization` бросит `MissingFieldException`, которая ловится общим `runCatching` без указания поля).

**Альтернативы:** Map-парсинг через `Json.parseToJsonElement(...)` без sealed data class → менее типизированно, ручной доступ к полям через `json["tag_name"]` без type-safety.

**Почему так:** явный `@Serializable data class` даёт типобезопасный доступ к полям через `release.tagName`/`release.htmlUrl` вместо ручного `json["tag_name"] as String?`; `ignoreUnknownKeys = true` обеспечивает устойчивость к новым полям API (прозрачно игнорируются, парсинг не падает). `@SerialName` обязателен для `tag_name`/`html_url` — ktlint `property-naming` и detekt `ConstructorParameterNaming` (`config/detekt/detekt.yml:316–318`, pattern `[a-z][A-Za-z0-9]*` на стр. 318) запрещают snake_case в primary constructor, camelCase-поля через `@SerialName` сохраняют JSON-контракт. Nullable-поля DTO отделяют «GitHub API реально не вернул поле» (защитная проверка в use case, см. п. 3.2) от «kotlinx-serialization не нашёл поле» (`MissingFieldException`, ловится общим `runCatching`).

### D11. Проверка HTTP-статуса и явный запрет редиректов

**Решение:** Use case **до** парсинга JSON проверяет `response.statusCode == 200`. Любой другой статус (4xx, 5xx, 3xx) → `Result.failure(AppUpdateException("HTTP ${response.statusCode}"))` (подставляется реальный код, например `"HTTP 403"`) без попытки десериализации. На `HttpsURLConnection` явно задаётся `instanceFollowRedirects = false` (механизм и rationale — см. D1).

**Альтернативы:** Положиться на `Json { ignoreUnknownKeys = true }` и парсить тело ошибки `{"message": "..."}` как валидный JSON → хрупко (GitHub может вернуть HTML на 5xx или пустое тело); следовать редиректам по умолчанию → небезопасно (endpoint может быть подменён) и противоречит GitHub API, который не редиректит.

**Почему так:** явная проверка statusCode делает поведение предсказуемым при любых ответах GitHub (rate limit 403, maintenance 503, JSON в неожиданном формате) и не зависит от структуры тела ответа. Запрет редиректов закрывает целый класс багов «работает через редирект, но упадёт при его изменении».

## Risks / Trade-offs

- **GitHub API rate limit (60 req/hour для неавторизованных)** → при интенсивном использовании пользователь увидит ошибку. Mitigation: use case не делает повторных попыток, кнопка ручная — пользователь сам контролирует частоту. YAGNI: не реализуем кеширование ответа.
- **Без авторизации GitHub может вернуть 403 при превышении rate limit** → маппится в `Error`, диалог показывает "Не удалось проверить обновления". Пользователь не видит деталей (это намеренно — security: не показывать API-лимиты).
- **Сравнение версий только major.minor.patch** → если в BuildConfig.VERSION_NAME появится `-beta1` или `+build`, парсинг даст 0 для невалидных компонентов. Mitigation: BuildConfig.VERSION_NAME в проекте всегда в формате `major.minor.patch` (см. `gradle.properties`).
- **`tag_name` из GitHub — только канонический `vX.Y.Z`** → парсер рассчитан на формат `v?major.minor.patch` (lowercase `v`, затем 3 числовых компонента). Невалидные форматы (`V1.2.3` с заглавной, `v 1.2.3` с пробелом, `1.2.3-rc.1` с пререлизом, 2 или 4 компонента) → компоненты с ошибкой заменяются на 0 через `toIntOrNull() ?: 0`, что может дать ложноотрицательное сравнение. Out of scope: реальные релизы GitHub ([easydev991/Jetpack-Days/releases](https://github.com/easydev991/Jetpack-Days/releases)) всегда соответствуют канону; тесты на edge cases намеренно не пишутся (YAGNI).
- **HTTP-вызов stateless** → обоснование (state нет, keep-alive ведёт платформа) — см. D1.
- **Network на main thread** → блокирующее сетевое I/O запрещено на main thread с API 11+ (`NetworkOnMainThreadException`). Mitigation: HTTP-вызов обёрнут в `withContext(Dispatchers.IO) { ... }` внутри `CheckForAppUpdateUseCase.invoke()`, ViewModel запускает через `viewModelScope.launch` без явного диспетчера (Main по умолчанию), состояние `Checking` отображается немедленно.
- **Cleartext HTTP не требуется** → `https://` GitHub API поддерживает TLS, `usesCleartextTraffic="false"` (default) в AndroidManifest не проблема.

## Migration Plan

- Feature additive, миграция не требуется.
- Rollout: merge в main → следующий релиз на github flavor. На rustore кнопка не отображается (build-time gate).
- Rollback: revert PR. Кнопка управляется `BuildConfig.RUSTORE_FEATURES` — на rustore-сборке её и так не будет, удаление файлов не критично для прод-сборки rustore.

## Open Questions

Нет. Все технические решения зафиксированы в Decisions.
