## Why

Приложение "JetpackDays" распространяется в двух вариантах: rustore (RuStore с автообновлениями через стор) и github (APK в GitHub Releases). Пользователи github-сборки сейчас вынуждены самостоятельно заходить в репозиторий, чтобы узнать о новых релизах — это лишний friction и риск пропустить важные исправления. На rustore обновления доставляются автоматически, поэтому ручная проверка там не нужна.

## What Changes

- Добавляется ручная проверка наличия новой версии через GitHub Releases API на экране MoreScreen — ТОЛЬКО для github-сборки (`BuildConfig.RUSTORE_FEATURES == false`), НИКОГДА на rustore.
- Новый use case `CheckForAppUpdateUseCase` с `Result<UpdateInfo?>` (null = установлена последняя версия) использует платформенный `HttpsURLConnection` через узкую абстракцию `HttpRequestExecutor` (БЕЗ Retrofit/OkHttp/Ktor — жёсткое ограничение AGENTS.md).
- Семантическое сравнение версий (major.minor.patch) с учётом префикса `v` в `tag_name` от GitHub.
- Новый `MoreScreenViewModel` с `StateFlow<MoreScreenUiState>` (Idle / Checking / UpToDate / UpdateAvailable / Error).
- Диалог `UpdateCheckDialog` с тремя сценариями: доступно обновление (открыть на GitHub), актуальная версия, ошибка сети.
- DI через ручные factory: use case — в `FormatterModule.kt`, ViewModel — в `AppModule.kt` (без Hilt).
- Локализация: английский — `app/src/main/res/values/strings.xml` (дефолтный `values/`), русский — `app/src/main/res/values-ru/strings.xml` (override для русской локали). 8 ключей × 2 языка, см. `spec.md` Requirement «Локализация диалога и кнопки» для полного списка.
- Тесты: unit (UseCase, ViewModel) + androidTest (видимость кнопки по flavor).

## Capabilities

### New Capabilities

- `app-update-check`: ручная проверка наличия новой версии через GitHub Releases API, отображение диалога с результатом (доступно / актуально / ошибка), открытие страницы релиза в браузере. Доступ ТОЛЬКО на github-сборке.

### Modified Capabilities

Нет. Фича полностью аддитивна: не изменяет существующих требований, только добавляет новый capability и новый UI-блок в `MoreScreen` под уже существующей кнопкой "Страница на GitHub".

## Impact

**Новые файлы (~10):**

- `app/src/main/java/com/dayscounter/domain/model/UpdateInfo.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/http/GitHubRelease.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/http/HttpRequestExecutor.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/AppUpdateException.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/CheckForAppUpdateUseCase.kt`
- `app/src/main/java/com/dayscounter/ui/state/MoreScreenUiState.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/MoreScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/screens/more/UpdateCheckDialog.kt`
- `app/src/test/java/.../CheckForAppUpdateUseCaseTest.kt`
- `app/src/test/java/.../MoreScreenViewModelTest.kt`

**Изменяемые файлы (~10):**

- `app/src/main/java/com/dayscounter/ui/screens/more/MoreScreen.kt` — кнопка + ViewModel + обработчик; **добавление приватной функции `openUrl(context, url)` рядом с существующей `openGitHub`** и миграция `openGitHub` на делегирование в `openUrl` (единая точка URL-handling для будущих ссылок — issue tracker, release-notes URL и т.п.).
- `app/src/main/java/com/dayscounter/di/FormatterModule.kt` — factory `createCheckForAppUpdateUseCase()` (конвенция размещения см. `design.md` Context и D1).
- `app/src/main/java/com/dayscounter/di/AppModule.kt` — только `createMoreScreenViewModelFactory()` (конвенция размещения см. `design.md` Context и D1).
- `app/src/main/java/com/dayscounter/util/AppConstants.kt` — константа `GITHUB_RELEASES_API_URL` (API endpoint, не путать с существующим web-URL `GITHUB_REPOSITORY_URL`).
- `app/src/main/AndroidManifest.xml` — добавить `<uses-permission android:name="android.permission.INTERNET" />` (необходим для сетевого вызова, см. `tasks.md:2.4`).
- `app/src/main/res/values/strings.xml` + `app/src/main/res/values-ru/strings.xml` — 8 ключей × 2 языка (кнопка «Закрыть» использует существующий `R.string.close`).
- `app/src/androidTest/java/.../MoreScreenTest.kt` — кейсы видимости кнопки по flavor.
- `.opencode/rules/tech-stack.md` + `AGENTS.md` — фиксация carve-out для единственного сетевого вызова (`HttpsURLConnection`), см. `tasks.md:7.5`.

**Зависимости:** нет новых. `HttpsURLConnection` — платформенный Android API с API 1, не требует подключения сторонних сетевых библиотек.

**API breaking:** нет.
