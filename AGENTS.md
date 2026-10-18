# AGENTS.md — руководство для ИИ-агентов

## Обзор проекта

"Days Counter" — Android-приложение (Kotlin + Jetpack Compose) для учёта
дней с событий. Полностью офлайн; формат бэкапа общий с iOS-версией.

**Жёсткие ограничения (нарушать нельзя):**
- Только офлайн: без Retrofit / OkHttp / Ktor (единственное исключение — стек ниже)
- Формат бэкапа должен оставаться совместимым с iOS-приложением (импортёр —
  `domain/usecase/ImportBackupUseCase.kt`; правила — навык `backup`)
- Логи и комментарии для пользователя — по умолчанию на русском
- **Никогда не использовать `!!`** — вместо неё `?`, `?:`, `let`, `checkNotNull`

Операционные правила (грузятся OpenCode автоматически через `opencode.json` →
`instructions` glob): `.opencode/rules/test-execution.md` (прогоны тестов,
подводные камни gradle-задач) и `.opencode/rules/android-emulator.md` (MCP-эмулятор).

---

## Стек

UI — Compose + Navigation Compose + ViewModel; данные — Room, DataStore,
Coroutines, kotlinx-serialization; тесты — JUnit 5 + MockK (unit),
Compose Testing (androidTest); телеметрия — Firebase, только release.

**Исключение:** ручная проверка обновлений (`CheckForAppUpdateUseCase`) —
единственное сетевое взаимодействие в проекте: платформенный
`HttpsURLConnection` через узкую абстракцию `HttpRequestExecutor`.
Не прецедент — новые сетевые фичи запрещены офлайн-правилом.

---

## Архитектура

MVVM + Clean: Presentation (UI, ViewModel) → Domain (Use Cases, entities)
→ Data (Room, repositories — только локальные источники).

DI — ручной, factory-методы: `FormatterModule`, `AppModule` (в `di/`).
Hilt не используется; пересмотреть при росте (>10 ViewModel, сложные графы).

Домен чист: `domain/` не импортирует `android.*`/`androidx.*`/`data`/`crash`/`analytics`/`di`/`reminder`/`ui`/`navigation`/`R`/`BuildConfig`/`util.AndroidLogger` — ловит guard-тест `DomainPurityTest`.

---

## Где что искать

| Что | Источник истины |
| --- | --- |
| Версии библиотек / плагинов | `gradle/libs.versions.toml` |
| `compileSdk` / `minSdk` / `targetSdk` | `app/build.gradle.kts` |
| Версия приложения (`VERSION_NAME`, `VERSION_CODE`) | `gradle.properties` |
| Бейджи версий в README | `<!-- BEGIN_VERSIONS -->` (обновляет `make update_readme_versions`) |
| Флейворы сборки / релизный процесс | [docs/deployment.md](docs/deployment.md) + `Makefile` |
| Конфигурация Detekt | `config/detekt/detekt.yml` |

Не фиксируй версии в этом файле — они часто меняются. Читай файлы выше.

---

## Сборка / Линт / Тесты

Все команды — через `make` (`make help` — полный список); правила прогонов
и подводные камни gradle-задач — `.opencode/rules/test-execution.md`. Цели
`make build` / `make test` / `make install` / `make android-test` /
`make memory-check` сами подтягивают секреты через SSH.

**Осторожно:** прямые gradle-вызовы мимо `make` не подцепляют
`_ensure_secrets` — на чистом чекауте упадут с «google-services.json not
found» без пояснения.

`make lint` = ktlint + detekt + Android Lint (оба флейвора) + markdownlint
(CLI опционален, ставится `make setup`).

**Detekt**: `maxIssues: 5`. Повышать лимит **без согласования с владельцем
репо запрещено**; новые предупреждения исправляются до завершения задачи.

---

## Тестирование

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

## Стиль кода

Проектные конвенции (сверх дефолтов Kotlin/Compose):

- Use Cases с I/O возвращают `Result<T>`, исключения мапятся в доменные
  (`BackupException`, `ItemException`); чистые расчёты отдают значение напрямую
- UI-состояния: взаимоисключающие визуальные состояния — sealed-классы
  (`Loading`/`Success`/`Error`); простое состояние — data class
- Маршруты — `navigation/Screen.kt`: sealed-класс + хелперы
  `createRoute(...)` (живой пример — `Screen.ItemDetail`)
- Сообщения пользователю — через `ResourceProvider` (UI-слои) или
  `DaysTextProvider` (домен); строки — в `strings.xml` (навык `localization`)

---

## Производительность и безопасность

Нюансы текущего состояния (не дефолты платформы):

- `searchItems` — LIKE без индексов (известное ограничение)
- `TextField` с `minLines` ограничивает ввод (CreateEditFormContent.kt)
- Импорт бэкапа: толерантный парсинг (`ignoreUnknownKeys`), фильтрация
  дубликатов, стримы через `use` — детали в навыке `backup`

---

## Структура проекта

```
app/src/main/java/com/dayscounter/
├── data/        # database/ (Room), icon/, provider/, preferences/, repository/
├── domain/      # model/, repository/ (interface), usecase/, exception/
├── ui/          # ds/ (переиспользуемые компоненты), screens/, state/, theme/, viewmodel/
├── navigation/  # Screen.kt — маршруты
├── reminder/    # воркер, уведомления
├── analytics/   # Firebase, только release
├── crash/       # CrashReporter-реализации (Firebase — release-only)
├── di/          # AppModule, FormatterModule — ручной DI
└── util/        # Logger, AndroidLogger/NoOpLogger, CrashReporter, AppConstants, ClipboardHelper
```

Тесты — `app/src/test/` (unit), `app/src/androidTest/` (инструментальные,
структура зеркалит код) и модуль `screenshot-tests/` (скриншот-тесты,
детали — docs/deployment.md).

---

## Чеклист перед коммитом

1. `make format` — чинит проблемы ktlint + detekt + markdown
2. `make test` — читай отчёт; не доверяй только коду выхода
3. Все замечания ktlint/detekt устранены; новый код не добавляет проблем
4. KDoc на публичных API; осмысленные имена
5. Никаких устаревших API (`./gradlew lint` подсвечивает их)
6. `make memory-check` — если задача трогала UI, навигацию или ViewModel
