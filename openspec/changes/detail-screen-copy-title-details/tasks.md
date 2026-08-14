# Tasks — detail-screen-copy-title-details

## 1. Локализация

- [x] 1.1 Добавить `context_menu_copy` ("Copy"), `title_copied` ("Title copied"), `details_copied` ("Details copied") в `app/src/main/res/values/strings.xml`
- [x] 1.2 Добавить параллельные строки `context_menu_copy` ("Скопировать"), `title_copied` ("Название скопировано"), `details_copied` ("Описание скопировано") в `app/src/main/res/values-ru/strings.xml`
- [x] 1.3 Запустить `make format` — ktlint + detekt проходят без ошибок

## 2. Утилита копирования

- [x] 2.1 Создать `app/src/main/java/com/dayscounter/util/ClipboardHelper.kt` с `interface ClipboardHelper { fun copy(context: Context, label: String, text: String): Result<Unit> }` и `class SystemClipboardHelper : ClipboardHelper` (без `!!`, safe unwrapping, Result-returning). Добавить KDoc на оба публичных типа согласно AGENTS.md «KDoc for public APIs».
- [x] 2.2 Создать `app/src/test/java/com/dayscounter/util/ClipboardHelperTest.kt` с тремя случаями: `copy_when_service_is_null_then_returns_failure`, `copy_when_setPrimaryClip_succeeds_then_returns_success`, `copy_when_setPrimaryClip_throws_then_returns_failure` (MockK)
- [x] 2.3 Запустить `make test` — все 3 кейса ClipboardHelperTest зелёные

## 3. Хендлер и снекбар в DetailScreen

- [ ] 3.1 В `DetailScreen.kt` добавить `snackbarHostState = remember { SnackbarHostState() }` и `coroutineScope = rememberCoroutineScope()`; добавить `SnackbarHost(hostState = snackbarHostState)` в `Scaffold`
- [ ] 3.2 Создать `internal @Composable fun rememberCopyToClipboardHandler(...)` по паттерну `CreateEditScreen.kt:166-187 rememberReminderNotificationsUnavailableHandler` (принимает `ClipboardHelper` интерфейс, возвращает `(label, messageResId, text) -> Unit` lambda)
- [ ] 3.3 Вызвать `rememberCopyToClipboardHandler` в `DetailScreen`, пробросить в `DetailScreenParams` как `onCopyTitle` / `onCopyDetails`
- [ ] 3.4 В `DetailScreen` для `Loading`/`Error` веток использовать `onCopyTitle = {}` / `onCopyDetails = {}` (no-op); для `Success` — реальные колбэки с `R.string.title_copied` / `R.string.details_copied`
- [ ] 3.5 Провинуть `onCopyTitle` / `onCopyDetails` через `DetailScreenParams` → `DetailScreenContent` → `DetailContentByState` → `DetailContentInner`
- [ ] 3.6 Создать `app/src/androidTest/java/com/dayscounter/ui/screens/detail/CopyToClipboardHandlerUiTest.kt` с локальным `FakeClipboardHelper` (метод `copy` управляется из теста через `var nextResult: Result<Unit>`) и тремя случаями: `remembercopytoclipboardhandler_when_invoked_with_title_label_then_shows_title_copied_snackbar` (`nextResult = success`), `remembercopytoclipboardhandler_when_invoked_with_details_label_then_shows_details_copied_snackbar` (`nextResult = success`), `remembercopytoclipboardhandler_when_clipboard_returns_failure_then_no_snackbar_shown` (`nextResult = failure`, проверка отсутствия снекбара через `onNodeWithText(R.string.title_copied).assertDoesNotExist()`)

## 4. Контекстное меню в ReadSectionView

- [ ] 4.1 В `DetailContentByState` добавить параметры `onCopyTitle: () -> Unit`, `onCopyDetails: () -> Unit` и передать в `DetailContentInner` (только для `Success`)
- [ ] 4.2 В `DetailContentInner` передать `onCopy = onCopyTitle` в первый `ReadSectionView` (title), `onCopy = onCopyDetails` во второй (details, только при `item.details.isNotEmpty()`), ничего не передавать для Reminder
- [ ] 4.3 Модифицировать `ReadSectionView`: добавить `onCopy: (() -> Unit)? = null`, обернуть `Text` в `Box`, навесить `Modifier.pointerInput` с `detectTapGestures(onLongPress = { menuVisible = true })` только при `onCopy != null`, рендерить `DropdownMenu` с одним `DropdownMenuItem` ("Скопировать" + ContentCopy иконка). Добавить KDoc на новые публичные параметры `ReadSectionView` и `DetailContentByState` согласно AGENTS.md «KDoc for public APIs».
- [ ] 4.4 Создать `app/src/androidTest/java/com/dayscounter/ui/screens/detail/ReadSectionViewCopyContextMenuUiTest.kt` с двумя случаями: `on_copy_when_menu_item_clicked_then_count_is_one` (long-press → wait → click menu item → `invocationCount == 1`), `long_press_ignored_when_on_copy_is_null_then_no_menu_shown`

## 5. Преview-ы и проверка

- [ ] 5.1 Обновить `DetailContentPreviews.kt` — добавить превью-вариант `ReadSectionView` с `onCopy = {}` для проверки компиляции
- [ ] 5.2 Запустить `make format` — ktlint + detekt проходят
- [ ] 5.3 Запустить `make test` — все unit-тесты проходят
- [ ] 5.4 Запустить `./gradlew assembleDebug` — APK собирается успешно
- [ ] 5.5 (Опционально) Запустить `make android-test`
- [ ] 5.6 Ручная проверка: long-press title → меню "Скопировать" → клик → снекбар "Название скопировано" → вставить совпадает с `item.title`; аналогично для details; tap вне меню закрывает; Reminder не реагирует на long-press
