# AGENTS.md - Guidelines for AI Coding Agents

## Project Overview

"Days Counter" — Android app (Kotlin + Jetpack Compose) for tracking days
since events. Fully offline; backup format is shared with the iOS
counterpart.

**Hard constraints (do not violate):**
- Offline only: no Retrofit / OkHttp / Ktor (единственный carve-out — Stack ниже)
- Backup format must stay compatible with the iOS app (importer —
  `domain/usecase/ImportBackupUseCase.kt`; правила — навык `backup`)
- Logs and user-facing comments are Russian by default
- **Never use `!!`** — use `?`, `?:`, `let`, `checkNotNull` (ловит detekt)

Operational rules (auto-loaded by OpenCode via `opencode.json` →
`instructions` glob): `.opencode/rules/test-execution.md` (прогоны тестов,
gotcha gradle-задач) and `.opencode/rules/android-emulator.md` (MCP-эмулятор).

---

## Stack

UI — Compose + Navigation Compose + ViewModel; данные — Room, DataStore,
Coroutines, kotlinx-serialization; тесты — JUnit 5 + MockK (unit),
Compose Testing (androidTest); телеметрия — Firebase, только release.

**Carve-out:** ручная проверка обновлений (`CheckForAppUpdateUseCase`) —
единственное сетевое взаимодействие в проекте: платформенный
`HttpsURLConnection` через узкую абстракцию `HttpRequestExecutor`.
Не прецедент — новые сетевые фичи запрещены офлайн-правилом.

---

## Architecture

MVVM + Clean: Presentation (UI, ViewModel) → Domain (Use Cases, entities)
→ Data (Room, repositories — только локальные источники).

DI — ручной, factory-методы: `FormatterModule`, `AppModule` (в `di/`).
Hilt не используется; пересмотреть при росте (>10 ViewModel, сложные графы).

---

## Where to find things

| Concern | Source of truth |
| --- | --- |
| Library / plugin versions | `gradle/libs.versions.toml` |
| `compileSdk` / `minSdk` / `targetSdk` | `app/build.gradle.kts` |
| App version (`VERSION_NAME`, `VERSION_CODE`) | `gradle.properties` |
| Version badges in README | `<!-- BEGIN_VERSIONS -->` (обновляет `make update_readme_versions`) |
| Build flavors / release flow | [docs/deployment.md](docs/deployment.md) + `Makefile` |
| Detekt config | `config/detekt/detekt.yml` |

Do not pin versions in this file — they change often. Read the files above.

---

## Build / Lint / Test

Все команды — через `make` (`make help` — полный список); правила прогонов
и gotcha gradle-задач — `.opencode/rules/test-execution.md`. Цели
`make build` / `make test` / `make install` / `make android-test` сами
подтягивают секреты через SSH.

**Осторожно:** прямые gradle-вызовы мимо `make` не подцепляют
`_ensure_secrets` — на чистом чекауте упадут с «google-services.json not
found» без пояснения.

`make lint` = ktlint + detekt + Android Lint (оба флейвора) + markdownlint
(CLI опционален, ставится `make setup`).

**Detekt**: `maxIssues: 5`. Повышать лимит **без согласования с владельцем
репо запрещено**; новые предупреждения исправляются до завершения задачи.

---

## Testing

- Unit (`app/src/test/`) — навык `kotlin-testing`; androidTest
  (`app/src/androidTest/`) — навык `kotlin-ui-testing`; прогоны —
  `.opencode/rules/test-execution.md`
- TDD: строго Тесты → Логика (домен и данные) → UI — Compose создаётся
  после покрытия логики тестами; пирамида 70/20/10; именование и AAA —
  навык `kotlin-testing`

Конвенции Room (миграция без теста не принимается):

- каждая `Migration` коммитится в паре с `MigrationTest` (androidTest,
  `MigrationTestHelper`) и новым `N.json` в `app/schemas/`
- простые изменения схемы — `@AutoMigration`; рукописный SQL — для сложных
- мок-юниты на SQL миграций запрещены (`verify { execSQL }` с relaxed не
  ловит синтаксические ошибки) — только интеграционный тест

---

## Code Style

Проектные конвенции (сверх дефолтов Kotlin/Compose):

- Use Cases возвращают `Result<T>`, исключения мапятся в доменные
  (`BackupException`, `ItemException`)
- UI-состояния: взаимоисключающие визуальные состояния — sealed-классы
  (`Loading`/`Success`/`Error`); простое состояние — data class
- Маршруты — `navigation/Screen.kt`: sealed class + хелперы
  `createRoute(...)` (живой пример — `Screen.ItemDetail`)
- Сообщения пользователю — только через `ResourceProvider`; строки — в
  `strings.xml` (навык `localization`)

---

## Performance & Security

Нюансы текущего состояния (не дефолты платформы):

- `searchItems` — LIKE без индексов (известное ограничение)
- `TextField` с `minLines` ограничивает ввод (CreateEditFormContent.kt)
- Импорт бэкапа: толерантный парсинг (`ignoreUnknownKeys`), фильтрация
  дубликатов, стримы через `use` — детали в навыке `backup`

---

## Project Structure

```
app/src/main/java/com/dayscounter/
├── data/        # database/ (Room), provider/, preferences/, repository/
├── domain/      # model/, repository/ (interface), usecase/, exception/
├── ui/          # ds/ (переиспользуемые компоненты), screens/, state/, theme/, viewmodel/
├── navigation/  # Screen.kt — маршруты
├── reminder/    # worker, notification
├── analytics/   # Firebase, release-only
├── crash/       # Crashlytics, release-only
├── di/          # AppModule, FormatterModule — manual DI
└── util/        # Logger, AppConstants
```

Тесты — `app/src/test/` (unit), `app/src/androidTest/` (инструментальные,
структура зеркалит код) и модуль `screenshot-tests/` (скриншот-тесты,
детали — docs/deployment.md).

---

## Checklist Before Commit

1. `make format` — fixes ktlint + detekt + markdown issues
2. `make test` — read the report; do not trust exit code alone
3. Все замечания ktlint/detekt устранены; новый код не добавляет проблем
4. KDoc on public APIs; осмысленные имена
5. No deprecated APIs (`./gradlew lint` flags them)
