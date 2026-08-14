## Why

На экране `DetailScreen` сейчас нет способа быстро скопировать название или описание события — пользователь вынужден выделять текст вручную. Это мешает, когда нужно передать название/описание в мессенджер, заметку или другому приложению. Добавляем стандартный Android-паттерн «долгое нажатие → контекстное меню → копирование» для секций `Title` и `Details`.

## What Changes

- Долгое нажатие на текст в секции `Title` открывает контекстное меню с пунктом «Скопировать»; клик копирует `item.title` в системный буфер обмена и показывает системный `Toast` с подтверждением копирования на устройствах с Android 12 и старше (API <33). На Android 13+ (API ≥33) собственный Toast НЕ показывается — ОС сама показывает системный overlay после копирования, и дублирование перекрывает его визуально.
- Аналогично для секции `Details` (только если `item.details.isNotEmpty()`): копируется `item.details`, показывается Toast с текстом «Описание скопировано» (только на API <33).
- Секция `Reminder` (и остальные read-only секции) контекстного меню не получают — обратная совместимость через дефолт `onCopy = null` в `ReadSectionView`.
- Добавляются строки локализации (en/ru) для пункта меню `context_menu_copy` и для сообщений Toast `title_copied`/`details_copied`.
- Вводится абстракция `ClipboardHelper` (интерфейс) + `SystemClipboardHelper` (реализация) — нужна, чтобы handler-тесты в `androidTest` не зависели от системного clipboard эмулятора.

## Capabilities

### New Capabilities

- `detail-screen-text-copy`: пользовательская возможность скопировать название и описание события через долгое нажатие на экране деталей. Включает UX-цепочку (long-press → DropdownMenu → клик → копирование → системный Toast подтверждения на API <33), абстракцию `ClipboardHelper` и соответствующую локализацию.

### Modified Capabilities

Отсутствуют — это первая точка интеграции с буфером обмена в проекте, требования к существующим capability не меняются.

## Impact

- `app/src/main/res/values/strings.xml` и `app/src/main/res/values-ru/strings.xml` — 3 новых строки в каждом файле (`context_menu_copy`, `title_copied`, `details_copied`).
- `app/src/main/java/com/dayscounter/util/ClipboardHelper.kt` — новый файл (`interface ClipboardHelper` + `class SystemClipboardHelper`).
- `app/src/main/java/com/dayscounter/ui/screens/detail/CopyToClipboardHandler.kt` — новый файл: `@Composable rememberCopyToClipboardHandler(clipboardHelper = SystemClipboardHelper())` возвращает `(label, messageResId, text) -> Unit`; внутри — копирование через `clipboardHelper.copy` + условный `Toast.makeText(context, ...).show()` при `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU`. Без `SnackbarHostState`/`rememberCoroutineScope`.
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt` — используется `rememberCopyToClipboardHandler`, колбэки `onCopyTitle`/`onCopyDetails` прокидываются в `DetailScreenParams` для `Success`-ветки `uiState` (для `Loading`/`Error` — `no-op`).
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContent.kt` — `ReadSectionView` получает опциональный `onCopy: (() -> Unit)? = null`; `Box` + `pointerInput`/`detectTapGestures` + `DropdownMenu` оборачивают тело секции. `DetailContentByState` принимает `onCopyTitle`/`onCopyDetails`.
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContentPreviews.kt` — подгонка превью под новую сигнатуру `ReadSectionView`.
- `app/src/test/java/com/dayscounter/util/ClipboardHelperTest.kt` — новый файл (3 кейса для `SystemClipboardHelper`).
- `app/src/androidTest/java/com/dayscounter/ui/screens/detail/CopyToClipboardHandlerUiTest.kt` — новый файл (2 кейса на handler с `FakeClipboardHelper`: проверка, что handler вызывает `clipboardHelper.copy` с переданными `label`/`text`; обработка `Result.failure` без падения).
- `app/src/androidTest/java/com/dayscounter/ui/screens/detail/ReadSectionViewCopyContextMenuUiTest.kt` — новый файл (2 кейса на меню).