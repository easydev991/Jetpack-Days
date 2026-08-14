# detail-screen-text-copy

## ADDED Requirements

### Requirement: Долгое нажатие на текст секции Title открывает контекстное меню

При долгом нажатии пользователем на body-текст секции Title компонента `ReadSectionView` на экране `DetailScreen` система MUST отображать контекстное меню `DropdownMenu` с одним пунктом, текст которого загружается из строкового ресурса `R.string.context_menu_copy` ("Скопировать"/"Copy"), а ведущей иконкой служит `Icons.Filled.ContentCopy`.

#### Scenario: Долгое нажатие на текст Title открывает меню с пунктом "Скопировать"

- **WHEN** пользователь выполняет долгое нажатие (long-press) на body-текст секции Title
- **THEN** отображается `DropdownMenu` с одним `DropdownMenuItem`, содержащим текст `R.string.context_menu_copy` и иконку `Icons.Filled.ContentCopy`

#### Scenario: Меню появляется непосредственно под текстом Title в пределах Box-контейнера

- **WHEN** пользователь выполняет долгое нажатие на body-текст секции Title
- **THEN** `DropdownMenu` позиционируется внутри `Box`, оборачивающего `Text`, стандартным для Material3 способом — под текстом

---

### Requirement: Долгое нажатие на текст секции Details открывает контекстное меню

При долгом нажатии пользователем на body-текст секции Details компонента `ReadSectionView` (отображаемой только когда `item.details.isNotEmpty()`) система MUST отображать контекстное меню `DropdownMenu` с идентичным пунктом "Скопировать", как для Title.

#### Scenario: Долгое нажатие на текст Details открывает меню с пунктом "Скопировать" когда details не пусто

- **WHEN** пользователь выполняет долгое нажатие на body-текст секции Details при `item.details.isNotEmpty()`
- **THEN** отображается `DropdownMenu` с одним `DropdownMenuItem`, содержащим текст `R.string.context_menu_copy` и иконку `Icons.Filled.ContentCopy`

#### Scenario: Секция Details не отображается и не реагирует на жесты когда details пусто

- **WHEN** `item.details.isEmpty()`
- **THEN** `ReadSectionView` для Details не рендерится; меню отсутствует; никакая другая секция не получает долгое нажатие

---

### Requirement: Клик по пункту меню копирует текст в буфер обмена и показывает снекбар

При клике пользователем по пункту "Скопировать" в контекстном меню секции Title система MUST вызвать `clipboardHelper.copy(context, "Title", item.title)`. При возврате `Result.success(Unit)` система MUST отобразить снекбар с текстом `R.string.title_copied` ("Название скопировано") длительностью `SnackbarDuration.Short`. При возврате `Result.failure` система MUST NOT отображать снекбар (копирование молча игнорируется). Для секции Details MUST вызываться `clipboardHelper.copy(context, "Details", item.details)` с тем же контрактом: success → снекбар `R.string.details_copied`, failure → без снекбара.

#### Scenario: Клик по пункту меню Title копирует item.title в буфер обмена и показывает снекбар "Название скопировано"

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Title
- **THEN** вызывается `clipboardHelper.copy(context, "Title", item.title)`; при возврате `Result.success(Unit)` показывается снекбар с текстом `R.string.title_copied` ("Название скопировано") длительностью `SnackbarDuration.Short`

#### Scenario: Клик по пункту меню Details копирует item.details в буфер обмена и показывает снекбар "Описание скопировано"

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Details
- **THEN** вызывается `clipboardHelper.copy(context, "Details", item.details)`; при возврате `Result.success(Unit)` показывается снекбар с текстом `R.string.details_copied` ("Описание скопировано") длительностью `SnackbarDuration.Short`

#### Scenario: Клик по пункту меню Title при ошибке копирования не показывает снекбар

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Title и `clipboardHelper.copy(...)` возвращает `Result.failure`
- **THEN** снекбар с текстом `R.string.title_copied` НЕ отображается

#### Scenario: Клик по пункту меню Details при ошибке копирования не показывает снекбар

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Details и `clipboardHelper.copy(...)` возвращает `Result.failure`
- **THEN** снекбар с текстом `R.string.details_copied` НЕ отображается

#### Scenario: Меню закрывается сразу после клика по пункту не дожидаясь копирования

- **WHEN** пользователь нажимает пункт "Скопировать" в меню
- **THEN** `menuVisible` устанавливается в `false` синхронно до вызова `onCopy?.invoke()`; пользователь не видит "застрявшее" меню

---

### Requirement: Тап вне меню закрывает контекстное меню без копирования

При тапе пользователем в область вне открытого контекстного меню `DropdownMenu` система MUST закрыть меню через `onDismissRequest`, MUST установить `menuVisible` в `false`, и MUST NOT вызвать обработчик копирования.

#### Scenario: Тап вне меню закрывает DropdownMenu без вызова копирования

- **WHEN** `DropdownMenu` открыто и пользователь выполняет тап (click/tap) в область вне меню
- **THEN** вызывается `onDismissRequest`, которое устанавливает `menuVisible = false`; `onCopy` не вызывается; текст не копируется

---

### Requirement: Секции без onCopy (Reminder и другие) не реагируют на долгое нажатие и не отображают контекстное меню

При передаче `onCopy = null` (значение по умолчанию) в `ReadSectionView` система MUST NOT навешивать `pointerInput` для долгого нажатия на body-текст, MUST NOT рендерить `DropdownMenu`, и MUST NOT показывать контекстное меню. Это обеспечивает обратную совместимость: секция Reminder, Color, Date и другие существующие вызовы `ReadSectionView` MUST NOT получать контекстное меню.

#### Scenario: ReadSectionView с onCopy = null не показывает меню при долгом нажатии

- **WHEN** `ReadSectionView` отрендерена с `onCopy = null` и пользователь выполняет долгое нажатие на body-текст
- **THEN** `DropdownMenu` не отображается; никакой пункт "Скопировать" не появляется

#### Scenario: ReadSectionView с onCopy = null не рендерит DropdownMenu

- **WHEN** `ReadSectionView` отрендерена с `onCopy = null`
- **THEN** `DropdownMenu` не включается в состав композабла; никакой `pointerInput` для долгого нажатия на body-текст не навешивается

#### Scenario: Секция Reminder остаётся без контекстного меню

- **WHEN** `DetailContentInner` рендерит `ReadSectionView` для секции Reminder
- **THEN** вызывается `ReadSectionView(..., onCopy = null)`; долгое нажатие на текст Reminder не открывает меню

---

### Requirement: ClipboardHelper абстрагирует доступ к системному буферу обмена

Система MUST осуществлять доступ к системному буферу обмена через интерфейс `ClipboardHelper`, имеющий метод `fun copy(context: Context, label: String, text: String): Result<Unit>`. Реализация `SystemClipboardHelper` MUST использовать `ClipboardManager`, MUST возвращать `Result.success(Unit)` при успехе и MUST возвращать `Result.failure` при любых ошибках (null `ClipboardManager`, исключения из `setPrimaryClip`). Compose-обработчик `rememberCopyToClipboardHandler` MUST принимать `ClipboardHelper` параметром с дефолтом `SystemClipboardHelper()` для возможности подмены в тестах.

#### Scenario: ClipboardHelper.copy возвращает Result.success когда ClipboardManager доступен и setPrimaryClip succeeds

- **WHEN** `SystemClipboardHelper.copy(context, "Title", "some text")` вызывается когда ClipboardManager доступен
- **THEN** возвращается `Result.success(Unit)`; текст помещается в системный буфер обмена

#### Scenario: ClipboardHelper.copy возвращает Result.failure когда ClipboardManager равен null

- **WHEN** `SystemClipboardHelper.copy(context, "Title", "some text")` вызывается когда `context.getSystemService(Context.CLIPBOARD_SERVICE)` возвращает null
- **THEN** возвращается `Result.failure`; никакое исключение не пробрасывается наверх

#### Scenario: ClipboardHelper.copy возвращает Result.failure когда setPrimaryClip бросает исключение

- **WHEN** `SystemClipboardHelper.copy(context, "Title", "some text")` вызывается и `clipboardManager.setPrimaryClip(...)` бросает `RuntimeException`
- **THEN** возвращается `Result.failure`; исключение перехватывается и не пробрасывается наверх

#### Scenario: Compose-обработчик принимает ClipboardHelper через параметр для подмены в тестах

- **WHEN** `rememberCopyToClipboardHandler` вызывается с кастомным `ClipboardHelper` (например `FakeClipboardHelper`)
- **THEN** используется переданная реализация, а не `SystemClipboardHelper`; это позволяет тестировать handler без зависимости от системного clipboard

---

### Requirement: Строки локализации присутствуют в обоих языковых файлах

Строки `context_menu_copy`, `title_copied` и `details_copied` MUST присутствовать в `res/values/strings.xml` (английский, по умолчанию) и в `res/values-ru/strings.xml` (русский). Английские значения: "Copy", "Title copied", "Details copied". Русские значения: "Скопировать", "Название скопировано", "Описание скопировано".

#### Scenario: context_menu_copy присутствует в обоих файлах локализации

- **WHEN** загружается строковый ресурс `R.string.context_menu_copy`
- **THEN** в `values/strings.xml` содержится "Copy"; в `values-ru/strings.xml` содержится "Скопировать"

#### Scenario: title_copied присутствует в обоих файлах локализации

- **WHEN** загружается строковый ресурс `R.string.title_copied`
- **THEN** в `values/strings.xml` содержится "Title copied"; в `values-ru/strings.xml` содержится "Название скопировано"

#### Scenario: details_copied присутствует в обоих файлах локализации

- **WHEN** загружается строковый ресурс `R.string.details_copied`
- **THEN** в `values/strings.xml` содержится "Details copied"; в `values-ru/strings.xml` содержится "Описание скопировано"