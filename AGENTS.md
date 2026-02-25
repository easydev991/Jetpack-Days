# AGENTS.md - Guidelines for AI Coding Agents

## Project Overview

"Days Counter" - Android app for tracking days since events. Fully offline. Kotlin + Jetpack Compose.

**Key Constraints:**
- Offline mode: no network features
- Backup model compatible with iOS app
- Logs in Russian by default
- Safety: never use `!!`, safely unwrap optionals

---

## Build/Lint/Test Commands

### Build

```bash
./gradlew assembleDebug          # Debug APK
./gradlew assembleRelease        # Release APK (requires secrets)
make build                       # Same as assembleDebug
```

### Lint & Format

```bash
make format                      # Run ktlint + detekt with auto-fix (REQUIRED after code changes)
make lint                        # Check ktlint + detekt (no auto-fix)
./gradlew ktlintCheck            # ktlint only
./gradlew ktlintFormat           # ktlint auto-fix only
./gradlew app:detekt             # detekt only
```

### Test

```bash
make test                        # All unit tests with report
./gradlew test                   # Unit tests only
./gradlew test --tests "com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCaseTest"  # Single test class
./gradlew test --tests "*DaysDifferenceTest"  # Pattern matching
./gradlew test --tests "com.dayscounter.domain.usecase.CalculateDaysDifferenceUseCaseTest.calculate when same day then returns Today"  # Single test method
make android-test                # Instrumentation tests (requires device)
```

### Full Check

```bash
make check                       # Build + test + lint
```

---

## Code Style

### Kotlin

- Data classes for models
- Sealed classes for states/results
- Extension functions for readability
- **NEVER use `!!`** - use `?`, `?:`, `let`, `checkNotNull`

### Safe Unwrapping (MANDATORY)

```kotlin
// WRONG: val itemId = savedStateHandle["itemId"]!!

// CORRECT: checkNotNull with message
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) {
    "ItemId parameter is required"
}

// CORRECT: let for null-safe call
repository.getItemById(itemId)?.let { item -> ... }

// CORRECT: Elvis operator
val icon = screen.icon ?: defaultIcon
```

### Error Handling

**In Use Cases** - use standard `Result<T>`:

```kotlin
suspend operator fun invoke(uri: Uri): Result<Int> =
    try {
        Result.success(items.size)
    } catch (e: IOException) {
        Result.failure(BackupException("Failed: ${e.message}", e))
    }
```

**For UI States** - use sealed classes:

```kotlin
sealed class DetailScreenState {
    data object Loading : DetailScreenState()
    data class Success(val item: Item) : DetailScreenState()
    data class Error(val message: String) : DetailScreenState()
}
```

### Compose

- `State`/`MutableState` for UI state
- Unidirectional data flow (state flows down, events flow up)
- `ViewModel` for UI state management
- `CompositionLocal` only for theme/global config

```kotlin
@Preview
@Composable
fun ComponentPreview() {
    JetpackDaysTheme {
        Component()
    }
}
```

### Naming Conventions

- Classes: `PascalCase`
- Functions/variables: `camelCase`
- Constants: `UPPER_SNAKE_CASE`
- Packages: `lowercase.with.dots`

### Navigation

```kotlin
sealed class Screen(val route: String, val icon: ImageVector? = null, val titleResId: Int? = null) {
    object Events : Screen(route = "events", icon = Icons.AutoMirrored.Filled.List, titleResId = R.string.events)
    object ItemDetail : Screen(route = "item_detail/{itemId}") {
        fun createRoute(itemId: Long) = "item_detail/$itemId"
    }
}
```

---

## Testing

### Test Naming

```kotlin
@Test
fun functionName_whenCondition_thenExpectedResult() {
    // Given
    // When
    // Then
}
```

### Test Example

```kotlin
@Test
fun `calculate when same day then returns Today`() {
    // Given
    val today = LocalDate.now()
    val timestamp = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    // When
    val result = useCase(eventTimestamp = timestamp)

    // Then
    assertTrue(result is DaysDifference.Today, "Result should be Today")
}
```

### Test Pyramid

- Unit tests: 70%
- Integration tests: 20%
- UI tests: 10%

### TDD Order

**1.** Tests → **2.** Logic → **3.** UI

---

## Project Structure

```
app/src/main/java/com/dayscounter/
├── data/
│   ├── database/         # Room entities, DAO, DB, converters, mappers
│   ├── provider/         # DaysFormatter, ResourceProvider
│   ├── preferences/      # AppSettingsDataStore
│   └── repository/       # ItemRepositoryImpl
├── domain/
│   ├── exception/        # ItemException
│   ├── model/            # Domain entities (Item, DaysDifference, TimePeriod, etc.)
│   ├── repository/       # ItemRepository interface
│   └── usecase/          # Use cases, IconManager, BackupException
├── ui/
│   ├── ds/               # Design System components (reusable)
│   ├── screens/          # Compose screens (events/, detail/, createedit/, more/, etc.)
│   ├── state/            # UI state classes
│   ├── theme/            # App theme
│   └── viewmodel/        # ViewModels
├── navigation/           # Navigation routes
├── analytics/            # FirebaseAnalyticsHelper
├── crash/                # CrashlyticsHelper
├── di/                   # AppModule, FormatterModule (manual DI, no Hilt)
├── util/                 # AndroidLogger, AppConstants, Logger
├── DaysCounterApplication.kt
└── MainActivity.kt
```

Tests: `test/` (unit), `androidTest/` (integration/UI) - structure mirrors source

---

## Architecture

- **MVVM**: Model (Room, repositories) → ViewModel → View (Compose)
- **Clean Architecture**: Presentation → Domain → Data
- **Manual DI**: Factory methods in `FormatterModule`, `AppModule` (no Hilt)
- **Offline-only**: No Retrofit, OkHttp, Ktor

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| UI | Jetpack Compose |
| Navigation | Navigation Compose |
| State | ViewModel |
| Database | Room |
| Preferences | DataStore |
| Async | Coroutines |
| Serialization | kotlinx-serialization |
| Tests | JUnit 5, MockK, Espresso |
| Crash Reporting | Firebase Crashlytics (release only) |

### Versions

- AGP: 9.0.0
- Kotlin: 2.3.0
- Compile SDK: 36
- Target SDK: 35
- Min SDK: 26

---

## Performance

- `viewModelScope.launch` for coroutines with auto-cancellation
- `StateFlow` with `SharingStarted.WhileSubscribed(5000)`
- `rememberSaveable` for state across configuration changes
- `LazyColumn` with `key = { it.id }` for stable item identification
- Room DAO with Flow for reactive queries

---

## Checklist Before Commit

1. `make format` - fix all lint issues
2. `make test` - all tests pass
3. No `!!` operators
4. KDoc for public APIs
5. No deprecated APIs
