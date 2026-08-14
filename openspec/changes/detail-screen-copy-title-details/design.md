## Context

Экран `DetailScreen` (`app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt`) и его контент (`DetailContent.kt`) уже используют общий композабл `ReadSectionView` для трёх секций: `Title`, `Details` (если не пусто) и `Reminder`. Подтверждение пользовательских действий через системный `Toast` уже используется в `AppDataScreen.kt` (`Toast.makeText(context, message, Toast.LENGTH_SHORT).show()` внутри `LaunchedEffect`). Контекстное меню реализовано в `MainScreen.kt` через `DropdownMenu` + `DropdownMenuItem` с `text`/`leadingIcon`/`onClick`. Долгое нажатие — через `Modifier.pointerInput { detectTapGestures(onLongPress = ...) }` (см. `ListItemView.kt`). Буфер обмена ранее в проекте не использовался — это первая точка интеграции.

## Goals / Non-Goals

**Goals:**
- Долгое нажатие на текст в `ReadSectionView` (когда передан `onCopy`) открывает `DropdownMenu` с пунктом «Скопировать»; клик копирует текст в системный буфер обмена и на устройствах с API <33 показывает системный `Toast` с подтверждением.
- На устройствах с API ≥33 системный Toast НЕ показывается — ОС сама показывает системный overlay после копирования, и дублирование перекрывает его визуально.
- `ReadSectionView` остаётся обратно совместимым (Reminder и любые будущие вызовы без `onCopy` работают как раньше).
- `ClipboardHelper` — интерфейс с дефолтной реализацией `SystemClipboardHelper`; handler в Compose получает `ClipboardHelper` через параметр `internal`-функции `rememberCopyToClipboardHandler` (для подмены в androidTest).
- Локализация (en/ru) для пункта меню и сообщений Toast.

**Non-Goals:**
- Расширять контекстное меню (share, edit, etc.) — задача только про копирование.
- Тактильная обратная связь (`hapticFeedback`), `onGloballyPositioned` + ручное смещение меню — стандартного позиционирования `DropdownMenu` внутри `Box` достаточно.
- Копирование для Reminder/Color/Date и других read-only секций — задача только про title и details.
- Поднимать общий state меню на уровень `DetailScreenContent` — двух секций недостаточно, чтобы оправдать lift-state.
- Заменять системный буфер обмена на что-либо ещё.
- Использовать Compose `Snackbar`/`SnackbarHost` для подтверждения — системный `Toast` визуально лучше (тот же стиль, что и в `AppDataScreen`); Compose Snackbar внутри `Scaffold` перекрывался бы системным overlay на API ≥33.

## Decisions

### 1. Абстракция `ClipboardHelper` с самого начала

**Решение:** вводим `interface ClipboardHelper` + `class SystemClipboardHelper` в `util/ClipboardHelper.kt`, а handler в `DetailScreen` принимает `ClipboardHelper` параметром с дефолтом `SystemClipboardHelper()`.

**Альтернативы:**
- Прямой вызов `ClipboardManager.setPrimaryClip(...)` в handler — отказ. Тогда handler-тест в `androidTest` зависит от системного clipboard эмулятора (требует прав, флакит).
- Обёртка над `ClipboardManager` без интерфейса (только класс) — отказ. Нечем подменить в тестах.
- `expect/actual` для KMP — отказ. Проект не KMP, лишняя механика.

**Обоснование:** интерфейс дешёвый (один метод), а handler-тест получает fake-double `FakeClipboardHelper` (androidTest-локальный класс) без зависимости от системного сервиса. Тестируемость — единственная причина абстракции; никаких предположений о будущих feature не требуется.

### 2. Локальный `menuVisible` в каждой `ReadSectionView`, не lift-state

**Решение:** `var menuVisible by remember { mutableStateOf(false) }` живёт внутри `ReadSectionView`. Меню одной секции не влияет на другую: открытие нового меню автоматически вызывает `onDismissRequest` у старого (стандартное поведение `DropdownMenu`), что выставит `menuVisible = false`.

**Альтернативы:**
- Единый `menuState: MenuState?` в `DetailContentInner` с `onMenuShown(sectionId)`.
- `combinedClickable` с `onClick`/`onLongClick`.

**Обоснование:** для двух секций локальный state симметричен паттерну в `MainScreen.kt`, не требует дополнительной координации и не тащит лишний сантехнический код. Если появится третья копируемая секция — lift будет очевидным рефактором.

### 3. Дефолт `onCopy = null` для обратной совместимости

**Решение:** параметр `onCopy: (() -> Unit)? = null` в `ReadSectionView`. Если `null` — `pointerInput` не навешивается, `DropdownMenu` не рендерится, жест длинного нажатия в этой секции не активен.

**Альтернативы:**
- Всегда навешивать `onLongPress` и требовать `onCopy` (без дефолта).

**Обоснование:** существующие вызовы `ReadSectionView` (Reminder) не должны неожиданно получить жест, который ничего не делает. Дефолт `null` = явный opt-in копирования. Из трёх вызовов `ReadSectionView` (Title: DetailContent.kt:96–99, Details: DetailContent.kt:101–104, Reminder: DetailContent.kt:295–299 внутри `UpcomingReminderSection`) меняются только два (Title и Details получают `onCopy = ...`); Reminder остаётся с `onCopy = null` по умолчанию.

### 4. Снекбар и handler живут в `DetailScreen`, а не в `DetailContent`

**Решение:** `SnackbarHostState`, `rememberCoroutineScope()`, `rememberCopyToClipboardHandler(...)` — в `DetailScreen`. Колбэки `onCopyTitle: () -> Unit` / `onCopyDetails: () -> Unit` прокидываются вниз через `DetailScreenParams` → `DetailScreenContent` → `DetailContentByState` → `DetailContentInner` → `ReadSectionView`.

**Альтернативы:**
- Поднять `SnackbarHostState` и `coroutineScope` в `DetailContentInner` — отказ. Усложняет `DetailContent` (ему не нужна инфраструктура снекбара), смешивает ответственности.

**Обоснование:** снекбар-хендлер требует `Context` (для `getString`), `SnackbarHostState` и `CoroutineScope`. Эта инфраструктура уже принадлежит `DetailScreen` (там же, где живёт `Scaffold`). `DetailContent` остаётся чистым презентационным слоем.

### 5. Стандартное позиционирование `DropdownMenu` в `Box`

**Решение:** оборачиваем `Text` в `Box`, рядом кладём `DropdownMenu`. Меню появляется под текстом автоматически.

**Альтернативы:**
- `onGloballyPositioned` + `DpOffset` (как в `MainScreen`).

**Обоснование:** для одной секции внутри `Column` ручное позиционирование — избыточно. `Box` даёт достаточно контекста, чтобы `DropdownMenu` Material3 корректно встал под текстом.

### 6. Колбэк `onCopy` содержит копирование и условный системный Toast

**Решение:** колбэк, который получает `ReadSectionView`, — это «скопировать текст в системный буфер обмена» и при `Result.success` И на устройстве с API <33 — показать системный `Toast` (`Toast.makeText(context, message, Toast.LENGTH_SHORT).show()`). На API ≥33 системный `Toast` НЕ показывается: ОС сама показывает системный overlay после копирования, и дублирование перекрывает его визуально. Используем именно системный `Toast`, а не Compose `Snackbar`: визуально системный Toast лучше (тот же стиль, что и в `AppDataScreen`), плюс Compose `Snackbar` внутри `Scaffold` всё равно перекрывался бы системным overlay на API ≥33. `ReadSectionView` не знает ни о clipboard, ни о Toast'е.

**Сигнатура handler'а:** `(label: String, message: String, text: String) -> Unit` — `message` уже готовый `String`, резолвленный вызывающим в composable-скоупе через `stringResource(R.string.title_copied)`. Резолвить строку в composable-скоупе обязательно: иначе `context.getString()` внутри callback возвращает stale-значение при смене локали/dark-mode (Android Studio lint `ConfigurationLocale`/`ResourceValuesConfigurationAware`). Handler остаётся свободным от конфигурационных зависимостей.

**Альтернативы:**
- Передавать `onCopy: (text: String) -> Unit` в `ReadSectionView`, чтобы он сам передавал текст.
- Compose `Snackbar` через `SnackbarHostState` + `Scaffold(snackbarHost = ...)` — отказ. На API ≥33 системный overlay перекрывает наш снекбар (визуальный конфликт); на API <33 системный Toast выглядит лучше Compose-снекбара.

**Обоснование:** `ReadSectionView` уже знает `bodyText` (значение `Text`), но не должен владеть метой «это title-секция vs details-секция» (для разных `label` и `messageResId`). Поэтому логика выбора label + messageResId живёт в caller'е (`DetailScreen`), а в `ReadSectionView` приходит готовый `() -> Unit` через `DetailScreenParams` → `DetailScreenContent` → `DetailContentByState` → `DetailContentInner`.

## Risks / Trade-offs

- **[Reminder может случайно получить контекстное меню при рефакторинге]** → Митигация: `onCopy = null` по умолчанию + существующий caller на Reminder ничего не передаёт; Compose UI-тест `long_press_ignored_when_on_copy_is_null_then_no_menu_shown` фиксирует контракт.
- **[Handler-тест зависит от системного clipboard]** → Митигация: `ClipboardHelper` интерфейс + `FakeClipboardHelper` в androidTest-файле. Юнит-тест `SystemClipboardHelper` тестирует реальный сервис через MockK.
- **[Имя теста `copy_*` конфликтует с `ClipboardHelperTest`]** → Митигация: handler-тесты используют префикс `remembercopytoclipboardhandler_*` (lowercase SUT-имя).
- **[Toast не показывается на экранах Loading/Error]** → Допустимо: меню копирования недоступно в этих стейтах (нет `ReadSectionView` с контентом), соответственно handler не вызывается. Для `Loading`/`Error` передаются no-op `onCopyTitle = {}` / `onCopyDetails = {}` для стабильной сигнатуры `DetailScreenParams`.
- **[Race condition: пользователь успевает нажать на пункт меню после `onDismissRequest`]** → Стандартное поведение `DropdownMenuItem.onClick` корректно обрабатывает это: при клике `menuVisible = false` + `onCopy?.invoke()` синхронно; порядок важен — закрытие первым, чтобы пользователь не видел «застрявшее» меню.
- **[Toast — системный, не Compose, поэтому его сложно тестировать в androidTest]** → Митигация: handler-тесты в androidTest проверяют, что `clipboardHelper.copy` вызван с правильными `label`/`text` (через `FakeClipboardHelper.copyInvocations`); условный показ Toast'а — `if (result.isSuccess && SDK_INT < TIRAMISU)` — это одна строка без рантайм-эффектов для теста; сама системная Toast-обвязка доверена Android и проверяется вручную на эмуляторе (этап 5.6).