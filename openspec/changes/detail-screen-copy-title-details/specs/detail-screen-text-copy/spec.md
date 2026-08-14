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

### Requirement: Клик по пункту меню копирует текст в буфер обмена и показывает системный Toast (только на API <33)

При клике пользователем по пункту "Скопировать" в контекстном меню секции Title система MUST вызвать `clipboardHelper.copy(context, "Title", item.title)`. Если копирование завершилось без исключения И устройство работает на `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU` (API <33, Android 12 и старше) — система MUST отобразить системный `Toast` через `Toast.makeText(context, titleCopiedMessage, Toast.LENGTH_SHORT).show()` с текстом `titleCopiedMessage = stringResource(R.string.title_copied)`. Текст Toast'а резолвится в composable-скоупе через `stringResource(R.string.title_copied)` и передаётся в lambda как уже готовый `String`, чтобы Toast оставался конфигурационно-чувствительным (lint `ConfigurationLocale`). На устройствах с API ≥33 (Android 13+) системный Toast НЕ показывается: ОС сама показывает системный overlay после копирования, дублирование перекрывает его визуально. Если `clipboardHelper.copy` бросил исключение (например, при недоступном системном `ClipboardManager` — programming error), система MUST NOT отображать Toast, исключение пробрасывается наверх для диагностики (это не нормальный runtime-fail). Для секции Details MUST вызываться `clipboardHelper.copy(context, "Details", item.details)` с тем же контрактом: success → Toast `R.string.details_copied` (только API <33), исключение → без Toast'а. Лямбды `onCopyTitle`/`onCopyDetails` объявляются инлайн в `DetailScreen.kt` рядом с местом их использования (не выносятся в отдельную фабрику): оба вызова передают константные `label` и строки, специализация уже произошла на месте.

#### Scenario: Клик по пункту меню Title копирует item.title в буфер обмена и показывает Toast "Название скопировано" на API <33

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Title на устройстве с `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU` и `clipboardHelper.copy(...)` выполняется без исключения
- **THEN** вызывается `clipboardHelper.copy(context, "Title", item.title)` И показывается системный `Toast.makeText(context, titleCopiedMessage, Toast.LENGTH_SHORT).show()` где `titleCopiedMessage` — это `stringResource(R.string.title_copied)`, полученный в composable-скоупе

#### Scenario: Клик по пункту меню Title копирует item.title в буфер обмена БЕЗ Toast на API >=33

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Title на устройстве с `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` и `clipboardHelper.copy(...)` выполняется без исключения
- **THEN** вызывается `clipboardHelper.copy(context, "Title", item.title)`; системный Toast НЕ показывается — ОС сама показывает системный overlay

#### Scenario: Клик по пункту меню Details копирует item.details в буфер обмена и показывает Toast "Описание скопировано" на API <33

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Details на устройстве с `Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU` и `clipboardHelper.copy(...)` выполняется без исключения
- **THEN** вызывается `clipboardHelper.copy(context, "Details", item.details)` И показывается системный `Toast.makeText(context, detailsCopiedMessage, Toast.LENGTH_SHORT).show()` где `detailsCopiedMessage` — это `stringResource(R.string.details_copied)`, полученный в composable-скоупе

#### Scenario: Клик по пункту меню Details копирует item.details в буфер обмена БЕЗ Toast на API >=33

- **WHEN** пользователь нажимает пункт "Скопировать" в меню секции Details на устройстве с `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU` и `clipboardHelper.copy(...)` выполняется без исключения
- **THEN** вызывается `clipboardHelper.copy(context, "Details", item.details)`; системный Toast НЕ показывается

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

Система MUST осуществлять доступ к системному буферу обмена через интерфейс `ClipboardHelper`, имеющий метод `fun copy(context: Context, label: String, text: String)`. Реализация `SystemClipboardHelper` MUST использовать `ClipboardManager` и MUST делегировать вызов `setPrimaryClip(ClipData.newPlainText(label, text))`. Метод бросает `IllegalStateException` если `context.getSystemService(Context.CLIPBOARD_SERVICE)` возвращает null — это programming error (системный сервис всегда доступен на поддерживаемых платформах), не runtime-fail. `DetailScreen` использует `SystemClipboardHelper` напрямую (через `remember { SystemClipboardHelper() }`); интерфейс `ClipboardHelper` сохранён для unit-тестирования `SystemClipboardHelper` через MockK.

#### Scenario: ClipboardHelper.copy делегирует в ClipboardManager.setPrimaryClip с правильными параметрами

- **WHEN** `SystemClipboardHelper.copy(context, "Title", "some text")` вызывается когда `context.getSystemService(Context.CLIPBOARD_SERVICE)` возвращает `ClipboardManager`
- **THEN** вызывается `ClipData.newPlainText("Title", "some text")` и `clipboardManager.setPrimaryClip(...)` с этим `ClipData`; метод возвращает `Unit`

#### Scenario: ClipboardHelper.copy бросает IllegalStateException когда ClipboardManager равен null

- **WHEN** `SystemClipboardHelper.copy(context, "Title", "some text")` вызывается когда `context.getSystemService(Context.CLIPBOARD_SERVICE)` возвращает null
- **THEN** метод бросает `IllegalStateException("ClipboardManager недоступен")`; исключение пробрасывается наверх

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
