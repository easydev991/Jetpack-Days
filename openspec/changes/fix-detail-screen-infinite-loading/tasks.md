# Tasks

## 1. Wiring ResourceProvider (поведение не меняется)

- [x] 1.1 Unit-сторона: `DetailScreenViewModel` получает параметр `resourceProvider: ResourceProvider` (конструктор + `factory(...)`); в `RootScreenComponents.kt` в вызов factory передаётся `AppModule.resourceProvider`; в `DetailScreenViewModelTest` добавлен `private class FakeResourceProvider : ResourceProvider` (образец — `CreateEditScreenViewModelTest.kt:1022`) и прокинут во все конструкторы VM. Проверка: `make test` зелёный — прежние ожидания не меняются, поведение экрана прежнее.
- [x] 1.2 Integration-сторона: в `DetailScreenViewModelIntegrationTest` добавлен фейк `ResourceProvider` (образец — `androidTest/.../createedit/TestViewModel.kt:60`), прокинут в конструкторы VM. Проверка: компиляция androidTest-источников `./gradlew compileGithubDebugAndroidTestKotlin`.

## 2. TDD red — тесты на новое ожидание

- [x] 2.1 Unit: `whenitemnotfound_thenremainsinloadingstate` → `whenitemnotfound_thenshowserrorstate` (fake-репозиторий без записи → `DetailScreenState.Error` с детерминированным текстом фейка). Проверка red: отладочный прогон `./gradlew testGithubDebugUnitTest --tests "com.dayscounter.ui.viewmodel.DetailScreenViewModelTest"` падает только на перевёрнутом тесте; отчёт — `python3 scripts/test_report.py` по XML (без повторного `make test`).
- [x] 2.2 Тесты транзита удаления `whenconfirmdelete_thendeletesitem` и `whenstaleemissionafterdelete_thenreminderisnotrestored`: сверить с решением D2 (design.md) — Success-пин остаётся, правки ожиданий не требуются (только wiring из 1.1). Проверка: в прогоне 2.1 оба зелёные.
- [x] 2.3 Integration: `whenItemDoesNotExistInDatabase_thenRemainsInLoadingState` → `whenItemDoesNotExistInDatabase_thenShowsErrorState`. Проверка red: при подключённом эмуляторе — отладочный `make android-test ANDROID_TEST_FILTER=com.dayscounter.ui.viewmodel.DetailScreenViewModelIntegrationTest#whenItemDoesNotExistInDatabase_thenShowsErrorState` падает; обязательный green — группа 4.

## 3. Реализация (green)

- [x] 3.1 `DetailScreenViewModel.observeItem()`: убрать `filterNotNull()`; null-эмиссия → `updateErrorState()`: переход в `DetailScreenState.Error(resourceProvider.getString(ResourceIds.EVENT_NOT_FOUND))` только из состояния `Loading` (guard D2 — идемпотентность повторных null по построению) + `logger.w("DetailScreenViewModel", "Событие не найдено: $itemId")`. Проверка: red-тесты 2.1 и 2.3 становятся зелёными.
- [x] 3.2 `make format` + `make lint`: 0 новых замечаний ktlint/detekt, лимит `maxIssues: 5` не повышается. Проверка: вывод lint без новых предупреждений.
- [x] 3.3 `make test` — один прогон → один отчёт: 0 упавших, включая нетронутые тесты удаления и `DetailScreenStateTest`. Проверка: отчёт `python3 scripts/test_report.py` без фейлов.

## 4. Инструментальная проверка

- [x] 4.1 `make android-test` (эмулятор подключён): 0 упавших, включая `whenItemDoesNotExistInDatabase_thenShowsErrorState` и нетронутые тесты удаления. Проверка: отчёт `python3 scripts/android_test_report.py GithubDebug`.
- [x] 4.2 `make memory-check` (задача трогает ViewModel/UI — чеклист AGENTS.md): leak-тесты зелёные при подключённом эмуляторе.

## Workflow follow-up

- Ручная acceptance на эмуляторе: восстановление back stack после смерти процесса (или deep link с несуществующим id) — экран показывает «Событие не найдено», стрелка назад в TopAppBar работает, спиннер не остаётся. **Выполнена 2026-11-08** (github-debug, emulator-5554): intent `OPEN_FROM_REMINDER` с `extra_item_id=999999` → экран «Детали» показывает «Событие не найдено», стрелка назад видна и тапом возвращает на главный экран; спиннер не остаётся.
- Архивировать change (`openspec archive fix-detail-screen-infinite-loading`) после ревью; активный change `add-rustore-in-app-review` не затрагивать.
