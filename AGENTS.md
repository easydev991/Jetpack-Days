# AGENTS.md - Guidelines for AI Coding Agents

## Project Overview

"Days Counter" — Android app (Kotlin + Jetpack Compose) for tracking days
since events. Fully offline; backup format is shared with the iOS
counterpart.

**Hard constraints (do not violate):**
- Offline only: no Retrofit / OkHttp / Ktor — see `.agents/rules/tech-stack.mdc`
- Backup format must stay compatible with the iOS app (iOS uses
  `NSKeyedArchiver`; importer lives in `domain/usecase/ImportBackupUseCase.kt`)
- Logs and user-facing comments are Russian by default
- **Never use `!!`** — use `?`, `?:`, `let`, `checkNotNull`

Per-area rules (auto-loaded by OpenCode) live in `.agents/rules/*.mdc`:
`overview`, `architecture`, `code-style`, `code-quality`, `tech-stack`,
`project-structure`, `performance-security`, `tdd`. Load them when in
doubt — do not duplicate their content here.

---

## Where to find things

| Concern | Source of truth |
| --- | --- |
| Library / plugin versions | `gradle/libs.versions.toml` |
| `compileSdk` / `minSdk` / `targetSdk` | `app/build.gradle.kts` |
| App version (`VERSION_NAME`, `VERSION_CODE`) | `gradle.properties` |
| Version badges rendered in README | `<!-- BEGIN_VERSIONS -->` block in `README.md` (kept current by `make update_readme_versions`) |
| Build flavors | `app/build.gradle.kts` (`productFlavors`) — каналы дистрибуции в [docs/deployment.md → Каналы дистрибуции](docs/deployment.md#каналы-дистрибуции) |
| Release / signing flow | `docs/deployment.md` + `Makefile` |
| Detekt config | `config/detekt/detekt.yml` |
| ktlint / detekt plugins | `app/build.gradle.kts` |

Do not pin versions in this file — they change often, and stale values
have caused wrong answers before. Read the files above.

---

## Build / Lint / Test

All user-facing commands live in the `Makefile`; run `Makefile` (`make help` lists the full set — no need to duplicate here). Direct gradle entrypoints for finer control:

```bash
./gradlew test --tests "com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCaseTest"      # single class
./gradlew test --tests "*DaysDifferenceTest"                                                   # pattern
./gradlew test --tests "…UseCaseTest.calculate_when_same_day_then_returns_today"               # single method
./gradlew ktlintCheck && ./gradlew app:detekt                                                 # lint only
./gradlew :app:assembleRustoreDebug :screenshot-tests:assembleDebug --quiet                    # used by screenshots target
```

**Осторожно:** прямые gradle-вызовы мимо `make` не подцепляют prerequisite `_ensure_secrets` (он инлайнен в `Makefile` напрямую в prereq-листы 11 gradle-целей: `build`/`install`/`test`/`android-test`/`_build_screenshots_apk`/`screenshots*`/`rustore`/`apk`/`_rustore_build_aab`). На чистом чекауте без `.secrets/keystore` и `app/google-services.json` они упадут с «google-services.json not found» без пояснения. Для локальной разработки предпочтительны цели `make build` / `make test` / `make install` / `make android-test` — они подтягивают секреты через SSH автоматически.

**Gotcha:** `make test` runs `./gradlew test || true` — the exit code is
**not** a failure signal. Read `scripts/test_report.py` output (it
parses `app/build/test-results/`) and treat any failing/non-passing test
as a failure, even if `make` exits 0.

`make lint` skips `markdownlint` with a yellow warning when the CLI is
missing — install it (`npm i -g markdownlint-cli`) or run `make setup`
to get the full check.

---

## Testing

Before writing tests, load the matching skill:

- `kotlin-testing` (`.opencode/skills/kotlin-testing/SKILL.md`) — for
  `app/src/test/` (unit). JUnit 5, MockK, kotlinx-coroutines-test,
  Turbine, Fake repos on `MutableStateFlow`, AAA structure with
  `// Given / // When / // Then` markers, snake_case test names without
  backticks. ViewModel integration tests are **forbidden** here.
- `kotlin-ui-testing` (`.opencode/skills/kotlin-ui-testing/SKILL.md`) —
  for `app/src/androidTest/`. JUnit 4, Compose Testing v2, Room
  in-memory, Turbine, real `AlarmManager`. No Espresso.

TDD order (tests → logic → UI) and the 70/20/10 pyramid are defined in
`.agents/rules/tdd.mdc` — read it before starting a new feature.

---

## Project Structure

Full tree and placement rules live in `.agents/rules/project-structure.mdc`.
Compact view of `app/src/main/java/com/dayscounter/`:

```
data/        # Room (database/, mappers), provider/, preferences/, repository/
domain/      # model/, repository/ (interface), usecase/, exception/
ui/          # ds/ (reusable), screens/<feature>/, state/, theme/, viewmodel/
reminder/    # AlarmReminderScheduler, ReminderBootReceiver, ReminderAlarmReceiver
navigation/  # Screen.kt (sealed class with createRoute helpers)
analytics/   # FirebaseAnalyticsHelper (release-only)
crash/       # CrashlyticsHelper (release-only)
di/          # AppModule, FormatterModule — manual factory DI, no Hilt
util/        # AndroidLogger, NoOpLogger, ClipboardHelper, ThemeUtils, AppConstants
```

DI rationale + module breakdown: `.agents/rules/architecture.mdc`.

---

## Release / Signing

- Signing secrets live in a private repo (`easydev991/android-secrets`),
  fetched over SSH by `make rustore` / `make apk` into a temp `.secrets/`
  dir. Configure SSH access with `make setup_ssh` first.
- `make rustore` increments `VERSION_CODE`, builds a signed AAB
  (`rustore` flavor → `dayscounter{N}.aab`), and uploads Crashlytics
  mapping files. For a two-step workflow (draft + manual moderation),
  use `make rustore-draft` then `make rustore-commit VID=<vid>`
  after checking release notes in the RuStore Console.
  Use `make apk FLAVOR=github` when you want a signed APK
  (`github` flavor → `dayscounter{N}.apk`) without bumping the build
  number.
- Fastlane uses the Ruby version pinned in `.ruby-version` (rbenv);
  `make setup` installs the full toolchain. Screenshots live in
  `fastlane/metadata/android/<locale>/images/phoneScreenshots/`.

---

## Code Style (summary)

Full rules: `.agents/rules/code-style.mdc`. Top reminders worth keeping
in mind while editing:

- Data classes for models; sealed classes for UI states / `Result<T>`
- Use Cases return `Result<T>`, mapping exceptions to domain failures
  (`BackupException`, `ItemException`)
- Navigation routes live in `navigation/Screen.kt` (sealed class with
  `createRoute(...)` helpers); screen entries take an optional `icon`
  and `titleResId`
- KDoc for public APIs; comment *why*, not *what*
- Logs in Russian, error messages user-facing only when localized via
  `ResourceProvider`

Safe-unwrapping patterns (mandatory):

```kotlin
// ❌ val itemId = savedStateHandle["itemId"]!!

// ✅ checkNotNull with informative message
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) {
    "ItemId parameter is required"
}

// ✅ let for null-safe call
repository.getItemById(itemId)?.let { item -> /* ... */ }

// ✅ Elvis for default
val icon = screen.icon ?: defaultIcon
```

---

## Performance

Full notes in `.agents/rules/performance-security.mdc`. Defaults used
across the codebase:

- `viewModelScope.launch` for coroutines (auto-cancellation)
- `StateFlow` with `SharingStarted.WhileSubscribed(5000)`
- `rememberSaveable` for state across config changes
- `LazyColumn` with `key = { it.id }`; `rememberLazyListState()` for
  scroll position
- Room DAO via `Flow` for reactive queries
- Crashlytics + Analytics enabled only in `release` build type
  (`manifestPlaceholders["crashlyticsCollectionEnabled"]`)

---

## Checklist Before Commit

1. `make format` — fixes ktlint + detekt + markdown issues
2. `make test` — read the report; do not trust exit code alone
3. No `!!` operators
4. KDoc on public APIs
5. No deprecated APIs (`./gradlew lint` flags them)
