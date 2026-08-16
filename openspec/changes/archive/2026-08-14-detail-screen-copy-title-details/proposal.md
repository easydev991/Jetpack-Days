## Why

На экране `DetailScreen` сейчас нет способа быстро скопировать название или описание события — пользователь вынужден выделять текст вручную. Это мешает, когда нужно передать название/описание в мессенджер, заметку или другому приложению. Добавляем стандартный Android-паттерн «долгое нажатие → контекстное меню → копирование» для секций `Title` и `Details`.

## What Changes

- Долгое нажатие на текст в секции `Title` открывает контекстное меню с пунктом «Скопировать»; клик копирует `item.title` в системный буфер обмена и показывает системный `Toast` с подтверждением копирования на устройствах с Android 12 и старше (API <33). На Android 13+ (API ≥33) собственный Toast НЕ показывается — ОС сама показывает системный overlay после копирования, и дублирование перекрывает его визуально.
- Аналогично для секции `Details` (только если `item.details.isNotEmpty()`): копируется `item.details`, показывается Toast с текстом «Описание скопировано» (только на API <33).
- Секция `Reminder` (и остальные read-only секции) контекстного меню не получают — обратная совместимость через дефолт `onCopy = null` в `ReadSectionView`.
- Добавляются строки локализации (en/ru) для пункта меню `context_menu_copy` и для сообщений Toast `title_copied`/`details_copied`.
- Вводится абстракция `ClipboardHelper` (интерфейс) + `SystemClipboardHelper` (реализация) — нужна для unit-тестирования `SystemClipboardHelper` через MockK (на JVM без Robolectric реальный `ClipboardManager` недоступен).

## Capabilities

### New Capabilities

- `detail-screen-text-copy`: пользовательская возможность скопировать название и описание события через долгое нажатие на экране деталей. Включает UX-цепочку (long-press → DropdownMenu → клик → копирование → системный Toast подтверждения на API <33), абстракцию `ClipboardHelper` и соответствующую локализацию.

### Modified Capabilities

Отсутствуют — это первая точка интеграции с буфером обмена в проекте, требования к существующим capability не меняются.

## Impact

- `app/src/main/res/values/strings.xml` и `app/src/main/res/values-ru/strings.xml` — 3 новых строки в каждом файле (`context_menu_copy`, `title_copied`, `details_copied`).
- `app/src/main/java/com/dayscounter/util/ClipboardHelper.kt` — новый файл (`interface ClipboardHelper` + `class SystemClipboardHelper`).
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt` — колбэки `onCopyTitle`/`onCopyDetails` объявляются инлайн (без отдельной фабрики `rememberCopyToClipboardHandler` — два call-site с константными `label`/`message` не нуждаются в обобщении): каждый содержит `clipboardHelper.copy(context, label, text)` + условный `Toast.makeText(context, message, Toast.LENGTH_SHORT).show()` при `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU`. Прокидываются в `DetailScreenParams` для `Success`-ветки `uiState` (для `Loading`/`Error` — no-op через `item?.let { ... }`). Без `SnackbarHostState`/`rememberCoroutineScope`.
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContent.kt` — `ReadSectionView` получает опциональный `onCopy: (() -> Unit)? = null`; `Box` (с `Modifier.onSizeChanged` для замера высоты Text) + `pointerInput`/`detectTapGestures` + `DropdownMenu` оборачивают тело секции. `DropdownMenu.offset = DpOffset(touchOffset.x.toDp(), (touchOffset.y - textHeightPx).toDp())` — компенсирует `topToAnchorBottom` Material3 1.4, чтобы top-left меню попал в точку касания. `DetailContentByState` принимает `onCopyTitle`/`onCopyDetails`.
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContentPreviews.kt` — подгонка превью под новую сигнатуру `ReadSectionView`.
- `app/src/test/java/com/dayscounter/util/ClipboardHelperTest.kt` — новый файл (2 кейса для `SystemClipboardHelper`: успешное делегирование `setPrimaryClip(ClipData.newPlainText(label, text))` через MockK и `IllegalStateException("ClipboardManager недоступен")` при null-сервисе).
- `app/src/androidTest/java/com/dayscounter/ui/screens/detail/ReadSectionViewCopyContextMenuUiTest.kt` — новый файл (2 кейса на меню: клик по пункту вызывает `onCopy`; long-press при `onCopy = null` не показывает меню).
