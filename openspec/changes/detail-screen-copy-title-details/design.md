## Context

Экран `DetailScreen` (`app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt`) и его контент (`DetailContent.kt`) уже используют общий композабл `ReadSectionView` для трёх секций: `Title`, `Details` (если не пусто) и `Reminder`. Подтверждение пользовательских действий через системный `Toast` уже используется в `AppDataScreen.kt` (`Toast.makeText(context, message, Toast.LENGTH_SHORT).show()` внутри `LaunchedEffect`). Контекстное меню реализовано в `MainScreen.kt` через `DropdownMenu` + `DropdownMenuItem` с `text`/`leadingIcon`/`onClick`. Долгое нажатие — через `Modifier.pointerInput { detectTapGestures(onLongPress = ...) }` (см. `ListItemView.kt`). Буфер обмена ранее в проекте не использовался — это первая точка интеграции.

## Goals / Non-Goals

**Goals:**
- Долгое нажатие на текст в `ReadSectionView` (когда передан `onCopy`) открывает `DropdownMenu` с пунктом «Скопировать»; клик копирует текст в системный буфер обмена и на устройствах с API <33 показывает системный `Toast` с подтверждением.
- На устройствах с API ≥33 системный Toast НЕ показывается — ОС сама показывает системный overlay после копирования, и дублирование перекрывает его визуально.
- `ReadSectionView` остаётся обратно совместимым (Reminder и любые будущие вызовы без `onCopy` работают как раньше).
- `ClipboardHelper` — интерфейс с дефолтной реализацией `SystemClipboardHelper`, используется `DetailScreen` напрямую для системных вызовов и тестируется unit-тестом через MockK.
- Локализация (en/ru) для пункта меню и сообщений Toast.

**Non-Goals:**
- Расширять контекстное меню (share, edit, etc.) — задача только про копирование.
- Тактильная обратная связь (`hapticFeedback`), `onGloballyPositioned` + ручное смещение меню — стандартного позиционирования `DropdownMenu` внутри `Box` достаточно.
- Копирование для Reminder/Color/Date и других read-only секций — задача только про title и details.
- Поднимать общий state меню на уровень `DetailScreenContent` — двух секций недостаточно, чтобы оправдать lift-state.
- Заменять системный буфер обмена на что-либо ещё.
- Использовать Compose `Snackbar`/`SnackbarHost` для подтверждения — системный `Toast` визуально лучше (тот же стиль, что и в `AppDataScreen`); Compose Snackbar внутри `Scaffold` перекрывался бы системным overlay на API ≥33.
- Выносить логику «копировать + показать Toast» в отдельный `@Composable`-фабрику или handler-класс — обе лямбды (`onCopyTitle`/`onCopyDetails`) объявляются инлайн в `DetailScreen.kt` рядом с местом использования: оба call-site передают константные `label`/`message`, специализация уже произошла, отдельная фабрика добавляет абстракцию без пользы.

## Decisions

### 1. Абстракция `ClipboardHelper` для unit-тестирования `SystemClipboardHelper`

**Решение:** вводим `interface ClipboardHelper` + `class SystemClipboardHelper` в `util/ClipboardHelper.kt`. `DetailScreen` использует `SystemClipboardHelper` напрямую (через `remember { SystemClipboardHelper() }`), без отдельного handler-фабрики. Интерфейс нужен для unit-теста `SystemClipboardHelper`: MockK замокировывает `ClipboardManager`, и тест проверяет делегирование без зависимости от реального системного сервиса.

**Альтернативы:**
- Прямой вызов `ClipboardManager.setPrimaryClip(...)` в `DetailScreen` без интерфейса — отказ. Тогда unit-тест `SystemClipboardHelper` невозможен (нет mock-точки).
- Обёртка над `ClipboardManager` без интерфейса (только класс) — отказ. Нечем подменить в unit-тесте.
- Отдельный handler `@Composable fun rememberCopyToClipboardHandler(clipboardHelper)` с androidTest-фейком — отказ (см. Decision 4). Оба call-site передают константы, фабрика добавляет слой без пользы.
- `expect/actual` для KMP — отказ. Проект не KMP, лишняя механика.

**Обоснование:** интерфейс дешёвый (один метод), нужен только для unit-теста `SystemClipboardHelper`. Тестируемость — единственная причина абстракции; никаких предположений о будущих feature не требуется.

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

**Обоснование:** существующие вызовы `ReadSectionView` (Reminder) не должны неожиданно получить жест, который ничего не делает. Дефолт `null` = явный opt-in копирования. Из трёх вызовов `ReadSectionView` (Title: DetailContent.kt:118-122, Details: DetailContent.kt:124-128, Reminder: DetailContent.kt внутри `UpcomingReminderSection`) меняются только два (Title и Details получают `onCopy = ...`); Reminder остаётся с `onCopy = null` по умолчанию.

### 4. Лямбды `onCopyTitle`/`onCopyDetails` объявляются инлайн в `DetailScreen`, а не в отдельной фабрике

**Решение:** `onCopyTitle: () -> Unit` и `onCopyDetails: () -> Unit` объявляются прямо в теле `DetailScreen.kt`, рядом с местом их проброса в `DetailScreenParams`. Логика «копировать + показать условный Toast на API <33» пишется инлайн внутри каждой лямбды.

**Альтернативы:**
- `internal @Composable fun rememberCopyToClipboardHandler(clipboardHelper): (label, message, text) -> Unit` — отказ. Оба call-site передают константы (`"Title"`/`"Details"`, `titleCopiedMessage`/`detailsCopiedMessage`), фабрика с 3-арг лямбдой только раздувает поверхность без выигрыша. Удалена отдельным ponytail-ревью.
- Поднять инфраструктуру копирования в `DetailContentInner` — отказ. Усложняет `DetailContent` (ему не нужна инфраструктура clipboard/Toast), смешивает ответственности.

**Обоснование:** `DetailScreen` уже владеет `LocalContext` и `stringResource(...)` — инфраструктура для копирования и Toast'а живёт там же. Лямбды короткие (6–8 строк каждая), специализированы для конкретных `label` + `message`, и читаются на месте использования. Отдельная фабрика с 3-арг параметрами — лишний слой абстракции без будущей переиспользуемости.

### 5. Стандартное позиционирование `DropdownMenu` в `Box`

**Решение:** оборачиваем `Text` в `Box`, рядом кладём `DropdownMenu` (только при `onCopy != null`). Меню появляется под текстом автоматически.

**Альтернативы:**
- `onGloballyPositioned` + `DpOffset` (как в `MainScreen`).

**Обоснование:** для одной секции внутри `Column` ручное позиционирование — избыточно. `Box` даёт достаточно контекста, чтобы `DropdownMenu` Material3 корректно встал под текстом.

### 6. Условный системный Toast по версии API

**Решение:** после успешного `clipboardHelper.copy(context, label, text)` (без исключения) и при `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU` (API <33) — показать системный `Toast` (`Toast.makeText(context, message, Toast.LENGTH_SHORT).show()`). На API ≥33 системный `Toast` НЕ показывается: ОС сама показывает системный overlay после копирования, и дублирование перекрывает его визуально. Используем именно системный `Toast`, а не Compose `Snackbar`: визуально системный Toast лучше (тот же стиль, что и в `AppDataScreen`), плюс Compose `Snackbar` внутри `Scaffold` всё равно перекрывался бы системным overlay на API ≥33.

**Текст Toast'а:** `message: String`, уже готовый, резолвленный в composable-скоупе через `stringResource(R.string.title_copied)` или `stringResource(R.string.details_copied)`. Резолвить строку в composable-скоупе обязательно: иначе `context.getString()` внутри callback возвращает stale-значение при смене локали/dark-mode (Android Studio lint `ConfigurationLocale`/`ResourceValuesConfigurationAware`).

**Альтернативы:**
- `Result<Unit>` от `clipboardHelper.copy` + проверка `result.isSuccess` для гейтинга Toast'а — отказ. Handler игнорировал `Result.failure` (молча), единственное использование `isSuccess` — гейт Toast'а. `ClipboardManager.setPrimaryClip` не реалистично падает в production, а единственный реальный failure-path (`error("ClipboardManager недоступен")`) — programming error, должен пробрасываться как исключение, а не глотаться в `Result`. Упростили сигнатуру до `fun copy(...): Unit` (см. `ClipboardHelper.kt`).
- Compose `Snackbar` через `SnackbarHostState` + `Scaffold(snackbarHost = ...)` — отказ. На API ≥33 системный overlay перекрывает наш снекбар (визуальный конфликт); на API <33 системный Toast выглядит лучше Compose-снекбара.

**Обоснование:** `ReadSectionView` не знает ни о clipboard, ни о Toast'е. Конкретные `label` (`"Title"`/`"Details"`) и тексты Toast'а (`titleCopiedMessage`/`detailsCopiedMessage`) специализируются в `DetailScreen.kt` и передаются как `() -> Unit` через `DetailScreenParams` → `DetailScreenContent` → `DetailContentByState` → `DetailContentInner` → `ReadSectionView`.

## Risks / Trade-offs

- **[Reminder может случайно получить контекстное меню при рефакторинге]** → Митигация: `onCopy = null` по умолчанию + существующий caller на Reminder ничего не передаёт; Compose UI-тест `long_press_ignored_when_on_copy_is_null_then_no_menu_shown` фиксирует контракт.
- **[Toast — системный, не Compose, поэтому его сложно тестировать в androidTest]** → Митигация: системный Toast рендерится через `WindowManager`, а не Compose-дерево, поэтому androidTest его не видит. Compose UI-тест меню `ReadSectionViewCopyContextMenuUiTest` проверяет, что `onCopy` вызывается с правильной семантикой; условный показ Toast'а — `if (SDK_INT < TIRAMISU)` — это одна строка без рантайм-эффектов для теста; сама системная Toast-обвязка доверена Android и проверяется вручную на эмуляторе (этап 5.6).
- **[Race condition: пользователь успевает нажать на пункт меню после `onDismissRequest`]** → Стандартное поведение `DropdownMenuItem.onClick` корректно обрабатывает это: при клике `menuVisible = false` + `onCopy?.invoke()` синхронно; порядок важен — закрытие первым, чтобы пользователь не видел «застрявшее» меню.
- **[Лямбды копирования дублируют одну и ту же структуру (copy + Toast)]** → Допустимо: разные `label` (`"Title"`/`"Details"`) и разные `message` (`titleCopiedMessage`/`detailsCopiedMessage`); извлечение в один helper добавит параметры вместо строк и не сократит код. Ponytail-ревью подтвердило: разделение читается лучше, чем общий 3-арг handler.
