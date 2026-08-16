# search-bar-main-screen Specification

## Purpose

TBD - created by archiving change redesign-search-bar. Update Purpose after archive.

## Requirements

### Requirement: Search Field Visibility

Поле поиска `SearchField` на главном экране `MainScreen` **MUST** отображаться тогда и только тогда, когда в отфильтрованном списке есть ≥5 элементов (`itemsCount >= MIN_ITEMS_FOR_SEARCH`) или пользователь уже начал вводить поисковый запрос (`searchQuery.isNotEmpty()`).

При уменьшении списка ниже порога и при пустом запросе поле **MUST** плавно скрыться. При обратном увеличении или при вводе запроса — плавно появиться.

#### Scenario: Search field hidden below visibility threshold

- **WHEN** `itemsCount < MIN_ITEMS_FOR_SEARCH` (после фильтрации) **AND** `searchQuery.isEmpty()`
- **THEN** `SearchField` не отображается на `MainScreen`

#### Scenario: Search field shown at or above visibility threshold

- **WHEN** `itemsCount >= MIN_ITEMS_FOR_SEARCH`
- **THEN** `SearchField` отображается на `MainScreen` с анимацией появления

#### Scenario: Search field persists with active query even if item count drops

- **WHEN** пользователь ввёл непустой поисковый запрос `searchQuery.isNotEmpty() == true`
- **AND** позднее `itemsCount` стал меньше `MIN_ITEMS_FOR_SEARCH` (например, после удаления элементов)
- **THEN** `SearchField` остаётся видимым до очистки запроса

### Requirement: Search Field Animation

Появление и скрытие `SearchField` **MUST** быть анимированы средствами Compose. На входе — расширение по верху с одновременным появлением, на выходе — сжатие по верху с одновременным затуханием. Длительность — 200 миллисекунд (tween). Список ниже поля **MUST** плавно следовать за изменением высоты поля, а не «дёргаться» скачком.

#### Scenario: Search field appears with expand-and-fade animation

- **WHEN** `showSearchField` становится `true` (например, `itemsCount` пересёк порог снизу вверх)
- **THEN** `AnimatedVisibility` выполняет `expandVertically(expandFrom = Alignment.Top)` + `fadeIn(tween(durationMillis = 200))`
- **AND** список ниже плавно смещается вниз

#### Scenario: Search field hides with shrink-and-fade animation

- **WHEN** `showSearchField` становится `false` (например, `itemsCount` упал ниже порога при пустом запросе)
- **THEN** `AnimatedVisibility` выполняет `shrinkVertically(shrinkTowards = Alignment.Top)` + `fadeOut(tween(durationMillis = 200))`
- **AND** список плавно занимает освободившееся место

### Requirement: Search Query Clear

Кнопка очистки (`trailingIcon`) **SHALL** показываться только при непустом `searchQuery`. По нажатию на кнопку `searchQuery` **MUST** становиться пустой строкой.

#### Scenario: Clear button only shown with non-empty query

- **WHEN** `searchQuery.isNotEmpty() == true`
- **THEN** внутри `SearchField.trailingIcon` отображается `IconButton(onClick = { onSearchQueryChange("") })` с иконкой `Icons.Filled.Close`
- **AND** элемент доступен для пользователя (стандартный `Role.Button`)

#### Scenario: Clear button hidden with empty query

- **WHEN** `searchQuery.isEmpty() == true`
- **THEN** `trailingIcon`-слот `SearchField` пуст

#### Scenario: Tapping clear button empties the query

- **WHEN** пользователь нажимает на `trailingIcon` `IconButton` при непустом `searchQuery`
- **THEN** вызывается `onSearchQueryChange("")` **AND** `searchQuery` становится пустой строкой **AND** фильтрация сбрасывается

### Requirement: Search Bar Material 3 Component

`SearchField` **MUST** быть реализован через `SearchBar` из material3 1.4.0 (не `DockedSearchBar`, не `OutlinedTextField`, не собственная реализация). Используется сигнатура `SearchBar(inputField, expanded, onExpandedChange, ...)` с `expanded = false` и пустой `content {}`. Внутри `inputField` — `SearchBarDefaults.InputField(query, onQueryChange, onSearch = { }, expanded = false, onExpandedChange = { }, ...)`.

`windowInsets` **MUST** быть явно передан как `WindowInsets(0, 0, 0, 0)` для подавления дефолтного top status-bar inset (`SearchBarDefaults.windowInsets`).

`@OptIn(ExperimentalMaterial3Api::class)` **SHALL** применяться на уровне `SearchField`.

Реализация **MUST NOT** содержать overlay-`Box` или другое абсолютное позиционирование вокруг `SearchBar` — весь UI реализуется через стандартные слоты Material3 (`leadingIcon`, `trailingIcon`).

#### Scenario: SearchField composed with SearchBar material3 1.4.0

- **WHEN** поле видимости `showSearchField == true`
- **THEN** `SearchField` компонуется через `SearchBar` из material3 1.4.0 (сигнатура `SearchBar.kt:534`)
- **AND** передаётся `windowInsets = WindowInsets(0, 0, 0, 0)`
- **AND** передаётся `expanded = false`
- **AND** `content` — пустая лямбда `{ }`

#### Scenario: No overlay wrapper around SearchBar

- **WHEN** `SearchField` компонуется
- **THEN** вокруг `SearchBar` нет overlay-`Box`, нет абсолютного позиционирования элементов, нет `zIndex`-хаков
- **AND** весь UI реализован стандартными слотами Material3 (`leadingIcon`, `trailingIcon`)

### Requirement: Click Area After Rotation

Зона кликабельности внутри `SearchField` (поле ввода `BasicTextField` внутри Material3 `SearchBar`) **MUST** корректно пересчитываться при повороте экрана устройства. После поворота пользователь **MUST** иметь возможность снять и заново установить фокус на поле кликом в любой части поля.

#### Scenario: Rotation does not break click area

- **WHEN** поле видимости `showSearchField == true`
- **AND** устройство поворачивается (portrait → landscape или наоборот) во время или после активного поискового запроса
- **THEN** пользователь может заново кликнуть в любую часть `BasicTextField` для установки фокуса
- **AND** границы кликабельной зоны соответствуют текущей ориентации

### Requirement: Bounded Visible State by Filtered Item Count

Инвариант видимости опирается на **отфильтрованный** размер списка (`itemsCount` после применения цветового фильтра и поиска), а не на полный размер хранилища. Если фильтр оставляет ≤4 элементов, поле **SHALL** скрываться даже при наличии 6+ в хранилище.

#### Scenario: Color filter narrows list below threshold

- **WHEN** в хранилище 6 элементов
- **AND** активный цветовой фильтр оставляет 4 элемента (`itemsCount == 4 < MIN_ITEMS_FOR_SEARCH`)
- **AND** `searchQuery.isEmpty()`
- **THEN** `SearchField` скрывается

### Requirement: Modifier Styling

`SearchField` **MUST** занимать полную ширину родителя (`fillMaxWidth()`) с боковыми отступами через design-token `R.dimen.spacing_regular` (16dp), без собственного вертикального padding.

#### Scenario: SearchField modifier composition

- **WHEN** `SearchField` компонуется в `MainScreen`
- **THEN** `Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.spacing_regular))` применяется
- **AND** вертикальный padding отсутствует (top inset контролируется через `windowInsets`)
