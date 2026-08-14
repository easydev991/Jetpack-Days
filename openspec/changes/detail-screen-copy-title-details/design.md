## Context

Экран `DetailScreen` (`app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt`) и его контент (`DetailContent.kt`) уже используют общий композабл `ReadSectionView` для трёх секций: `Title`, `Details` (если не пусто) и `Reminder`. Снекбар уже интегрирован в `CreateEditScreen.kt` через `SnackbarHostState` + `Scaffold(snackbarHost = ...)`. Контекстное меню реализовано в `MainScreen.kt` через `DropdownMenu` + `DropdownMenuItem` с `text`/`leadingIcon`/`onClick`. Долгое нажатие — через `Modifier.pointerInput { detectTapGestures(onLongPress = ...) }` (см. `ListItemView.kt`). Буфер обмена ранее в проекте не использовался — это первая точка интеграции.

## Goals / Non-Goals

**Goals:**
- Долгое нажатие на текст в `ReadSectionView` (когда передан `onCopy`) открывает `DropdownMenu` с пунктом «Скопировать»; клик копирует текст и показывает снекбар.
- `ReadSectionView` остаётся обратно совместимым (Reminder и любые будущие вызовы без `onCopy` работают как раньше).
- `ClipboardHelper` — интерфейс с дефолтной реализацией `SystemClipboardHelper`; handler в Compose получает `ClipboardHelper` через параметр `internal`-функции `rememberCopyToClipboardHandler` (для подмены в androidTest).
- Локализация (en/ru) для пункта меню и сообщений снекбара.

**Non-Goals:**
- Расширять контекстное меню (share, edit, etc.) — задача только про копирование.
- Тактильная обратная связь (`hapticFeedback`), `onGloballyPositioned` + ручное смещение меню — стандартного позиционирования `DropdownMenu` внутри `Box` достаточно.
- Копирование для Reminder/Color/Date и других read-only секций — задача только про title и details.
- Поднимать общий state меню на уровень `DetailScreenContent` — двух секций недостаточно, чтобы оправдать lift-state.
- Заменять системный буфер обмена на что-либо ещё.

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

### 6. Колбэк `onCopy` уже содержит и копирование, и снекбар

**Решение:** колбэк, который получает `ReadSectionView`, — это «скопировать и показать снекбар» как единое действие. `ReadSectionView` не знает ни о clipboard, ни о снекбаре.

**Альтернативы:**
- Передавать `onCopy: (text: String) -> Unit` в `ReadSectionView`, чтобы он сам передавал текст.

**Обоснование:** `ReadSectionView` уже знает `bodyText` (значение `Text`), но не должен владеть метой «это title-секция vs details-секция» (для разных снекбаров). Поэтому логика выбора label + messageResId живёт в caller'е (`DetailScreen`), а в `ReadSectionView` приходит готовый `() -> Unit` через `DetailScreenParams` → `DetailScreenContent` → `DetailContentByState` → `DetailContentInner`.

## Risks / Trade-offs

- **[Reminder может случайно получить контекстное меню при рефакторинге]** → Митигация: `onCopy = null` по умолчанию + существующий caller на Reminder ничего не передаёт; Compose UI-тест `long_press_ignored_when_on_copy_is_null_then_no_menu_shown` фиксирует контракт.
- **[Handler-тест зависит от системного clipboard]** → Митигация: `ClipboardHelper` интерфейс + `FakeClipboardHelper` в androidTest-файле. Юнит-тест `SystemClipboardHelper` тестирует реальный сервис через MockK.
- **[Имя теста `copy_*` конфликтует с `ClipboardHelperTest`]** → Митигация: handler-тесты используют префикс `remembercopytoclipboardhandler_*` (lowercase SUT-имя).
- **[Снекбар не показывается на экранах Loading/Error]** → Митигация: для этих веток `uiState` передаются no-op `onCopyTitle = {}` / `onCopyDetails = {}`. Меню всё равно не показывается в этих стейтах (нет `ReadSectionView` с контентом).
- **[Race condition: пользователь успевает нажать на пункт меню после `onDismissRequest`]** → Стандартное поведение `DropdownMenuItem.onClick` корректно обрабатывает это: при клике `menuVisible = false` + `onCopy?.invoke()` синхронно; порядок важен — закрытие первым, чтобы пользователь не видел «застрявшее» меню.