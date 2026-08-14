# Tasks — detail-screen-copy-title-details

## 1. Локализация

- [x] 1.1 Добавить `context_menu_copy` ("Copy"), `title_copied` ("Title copied"), `details_copied` ("Details copied") в `app/src/main/res/values/strings.xml`
- [x] 1.2 Добавить параллельные строки `context_menu_copy` ("Скопировать"), `title_copied` ("Название скопировано"), `details_copied` ("Описание скопировано") в `app/src/main/res/values-ru/strings.xml`
- [x] 1.3 Запустить `make format` — ktlint + detekt проходят без ошибок

## 2. Утилита копирования

- [x] 2.1 Создать `app/src/main/java/com/dayscounter/util/ClipboardHelper.kt` с `interface ClipboardHelper { fun copy(context: Context, label: String, text: String) }` и `class SystemClipboardHelper : ClipboardHelper` (без `!!`, safe unwrapping, бросает `IllegalStateException` на null `ClipboardManager`). Добавить KDoc на оба публичных типа согласно AGENTS.md «KDoc for public APIs».
- [x] 2.2 Создать `app/src/test/java/com/dayscounter/util/ClipboardHelperTest.kt` с двумя случаями: `copy_when_service_is_null_then_throws` (проверка `IllegalStateException` с message `"ClipboardManager недоступен"`), `copy_when_set_primary_clip_succeeds_then_delegates_to_manager` (проверка вызовов `ClipData.newPlainText(label, text)` и `clipboardManager.setPrimaryClip(clipData)` через `verify(exactly = 1)`). Статический `ClipData.newPlainText` замокирован (MockK), потому что на JVM без Robolectric реальная реализация бросает RuntimeException.
- [x] 2.3 Запустить `make test` — оба кейса ClipboardHelperTest зелёные

## 3. Лямбды копирования и Toast подтверждения в DetailScreen

- [x] 3.1 В `DetailScreen.kt` НЕ добавлять `SnackbarHostState`/`SnackbarHost`/`rememberCoroutineScope` (Compose Snackbar не используется — системный Toast показывается прямо из lambda, см. `AppDataScreen.kt:107-109`).
- [x] 3.2 В `DetailScreen.kt` объявить `val clipboardHelper = remember { SystemClipboardHelper() }`, `val item = (uiState as? DetailScreenState.Success)?.item`. НЕ создавать отдельный `@Composable fun rememberCopyToClipboardHandler` — обе лямбды инлайн в `DetailScreen`.
- [x] 3.3 Объявить `val onCopyTitle: () -> Unit = { item?.let { clipboardHelper.copy(context, "Title", it.title); if (SDK_INT < TIRAMISU) Toast.makeText(context, titleCopiedMessage, SHORT).show() } }` и `val onCopyDetails` симметрично с `"Details"`, `it.details`, `detailsCopiedMessage`. Пробросить в `DetailScreenParams` как `onCopyTitle`/`onCopyDetails`. `titleCopiedMessage`/`detailsCopiedMessage` — `val` в composable-скоупе через `stringResource(...)` (lint `ConfigurationLocale`).
- [x] 3.4 Для стейтов `Loading`/`Error` лямбды автоматически no-op: `item?.let { ... }` пропускает копирование когда `item == null`, потому что `ReadSectionView` рендерится только в `is DetailScreenState.Success`. Не нужен `when`-блок с пустыми ветками.
- [x] 3.5 Пробросить `onCopyTitle`/`onCopyDetails` через `DetailScreenParams` → `DetailScreenContent` → `DetailContentByState` → `DetailContentInner` → `ReadSectionView.onCopy`.
- [x] 3.6 НЕ создавать `CopyToClipboardHandlerUiTest.kt` — отдельный handler отсутствует, `FakeClipboardHelper` тоже; контракт копирования покрыт unit-тестом `ClipboardHelperTest` (системный сервис через MockK) и UI-тестом `ReadSectionViewCopyContextMenuUiTest` (проверяет, что `onCopy` инвокается при клике по пункту меню).

## 4. Контекстное меню в ReadSectionView

- [x] 4.1 В `DetailContentByState` добавить параметры `onCopyTitle: () -> Unit`, `onCopyDetails: () -> Unit` и передать в `DetailContentInner` (только для `Success`)
- [x] 4.2 В `DetailContentInner` передать `onCopy = onCopyTitle` в первый `ReadSectionView` (title), `onCopy = onCopyDetails` во второй (details, только при `item.details.isNotEmpty()`), ничего не передавать для Reminder
- [x] 4.3 Модифицировать `ReadSectionView`: добавить `onCopy: (() -> Unit)? = null`, обернуть `Text` в `Box`, навесить `Modifier.pointerInput` с `detectTapGestures(onLongPress = { menuVisible = true })` только при `onCopy != null`, рендерить `DropdownMenu` с одним `DropdownMenuItem` ("Скопировать" + ContentCopy иконка) только при `onCopy != null`. Один `Text` рендерится всегда — `pointerInput` и `DropdownMenu` вешаются условно (нет дублирующей `else`-ветки с `Text`). Добавить KDoc на новые публичные параметры `ReadSectionView` и `DetailContentByState` согласно AGENTS.md «KDoc for public APIs».
- [x] 4.4 Создать `app/src/androidTest/java/com/dayscounter/ui/screens/detail/ReadSectionViewCopyContextMenuUiTest.kt` с двумя случаями: `on_copy_when_menu_item_clicked_then_count_is_one` (long-press → wait → click menu item → `invocationCount == 1`), `long_press_ignored_when_on_copy_is_null_then_no_menu_shown`

## 5. Преview-ы и проверка

- [x] 5.1 Обновить `DetailContentPreviews.kt` — добавить превью-вариант `ReadSectionView` с `onCopy = {}` для проверки компиляции
- [x] 5.2 Запустить `make format` — ktlint + detekt проходят (12 weighted issues в detekt — все pre-existing в несвязанных файлах: MainScreen, ThemeIconScreen, CreateEditScreenViewModel и др., ни одной новой)
- [x] 5.3 Запустить `make test` — все unit-тесты проходят (457/457)
- [x] 5.4 Запустить `./gradlew assembleDebug` — APK собирается успешно
- [x] 5.5 (Опционально) Запустить `make android-test` — `compileDebugAndroidTestKotlin` зелёный
- [x] 5.6 Ручная проверка на эмуляторе: long-press title → меню "Скопировать" → клик → вставить совпадает с `item.title`; аналогично для details; на устройстве API <33 дополнительно проверить, что появляется системный Toast "Название скопировано" / "Описание скопировано"; на устройстве API ≥33 проверить, что наш Toast НЕ появляется (есть только системный overlay); tap вне меню закрывает; Reminder не реагирует на long-press — **пройдено пользователем визуально**
