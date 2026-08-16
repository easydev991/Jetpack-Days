# Spec: ui-events-screen-collapse-search-field

Поведение сворачивания/разворачивания поля поиска `SearchField` при скролле на главном экране `MainScreen` приложения JetpackDays. Реализовано через `CollapsibleSearchField` в `ScreenBody` + `NestedScrollConnection` в `MainScreenScaffold`. Заголовок «Events» всегда в одной строке с `SortMenu` и `PaletteFilter` в `TopAppBar` (small).

## Requirements

### Requirement: Search Field Collapses on Scroll Up

Поле поиска `CollapsibleSearchField` на главном экране `MainScreen` **MUST** сворачиваться по высоте при скролле списка вверх (палец снизу вверх, reveal контента ниже) и разворачиваться при скролле вниз. Степень сворачивания пропорциональна величине скролла, от `expandedHeight` (полностью видимое) до `0dp` (полностью скрытое).

Реализация: `NestedScrollConnection.onPreScroll` в `MainScreenScaffold.kt` обновляет `searchBarCollapsePx` по формуле `(searchBarCollapsePx - available.y).coerceIn(0f, searchFieldHeightPx.toFloat())`. `Modifier.layout { placeRelative(0, -collapsePx) }` в `CollapsibleSearchField` (`MainScreen.kt`) сдвигает контент вверх на `collapsePx`; `clipToBounds()` снаружи `Modifier.layout` обеспечивает визуальный клиппинг.

#### Scenario: Search field collapses on swipe up in long list

- **WHEN** в списке 30 записей (`itemsCount >= MIN_ITEMS_FOR_SEARCH`) **AND** `SearchField` видимо (порог пройден) **AND** пользователь выполняет swipe up по `LazyColumn`
- **THEN** высота `CollapsibleSearchField` уменьшается пропорционально величине скролла
- **AND** поле полностью скрывается если scroll-offset превышает высоту поля

#### Scenario: Search field expands on swipe down after collapse

- **WHEN** `CollapsibleSearchField` свёрнуто (частично или полностью) **AND** пользователь выполняет swipe down по `LazyColumn`
- **THEN** высота `CollapsibleSearchField` увеличивается пропорционально величине скролла
- **AND** поле возвращается к `expandedHeight` при scroll-offset >= высоты поля

#### Scenario: Short swipe partially collapses

- **WHEN** пользователь выполняет короткий swipe up (fraction 0.2 от высоты экрана)
- **THEN** `CollapsibleSearchField` частично сворачивается на пропорциональную величину (без snap к границам)

### Requirement: Scroll Does Not Collapse When Search Query Active

Сворачивание `CollapsibleSearchField` **MUST** быть заблокировано при активном поисковом запросе. Если пользователь ввёл текст в `SearchField`, скролл списка **SHALL NOT** уменьшать высоту поля.

Реализация: ранний `return Offset.Zero` в `NestedScrollConnection.onPreScroll` (`MainScreenScaffold.kt`) при `state.searchQuery.isNotEmpty()`.

#### Scenario: Active search query blocks collapse

- **WHEN** `searchQuery.isNotEmpty() == true` **AND** пользователь выполняет swipe up по `LazyColumn`
- **THEN** `searchBarCollapsePx` не меняется
- **AND** `CollapsibleSearchField` остаётся полностью видимым
- **AND** `LazyColumn` получает scroll-offset как обычно (scroll не «съедается»)

### Requirement: Scroll Does Not Affect Search Field Below Threshold

Если количество записей ниже порога видимости (`itemsCount < MIN_ITEMS_FOR_SEARCH`) **AND** `searchQuery.isEmpty()`, `SearchField` **MUST** быть скрыто через `AnimatedVisibility` (`MainScreen.kt`). Скролл списка в этом случае **SHALL NOT** влиять на скрытое поле.

Реализация: `AnimatedVisibility(visible = showSearchField)` оборачивает `CollapsibleSearchField`. При `visible = false` контент не участвует в layout-фазе.

#### Scenario: Scroll below visibility threshold does nothing

- **WHEN** в списке 4 записи (`itemsCount < MIN_ITEMS_FOR_SEARCH`) **AND** `searchQuery.isEmpty()` **AND** `SearchField` скрыто через `AnimatedVisibility`
- **THEN** скролл списка не влияет на `CollapsibleSearchField` (контент не отрисовывается)
- **AND** список скроллится нормально

### Requirement: Collapse State Persists Across Rotation

Состояние сворачивания `searchBarCollapsePx` **MUST** сохраняться при повороте устройства. После поворота высота `CollapsibleSearchField` **SHALL** соответствовать состоянию до поворота.

Реализация: `rememberSaveable(mutableFloatStateOf(0f))` для `searchBarCollapsePx` (`MainScreenScaffold.kt`). `searchFieldHeightPx` пересчитывается через `Modifier.onSizeChanged` естественным образом при новой layout-фазе после rotation.

#### Scenario: Rotation preserves collapse state

- **WHEN** `searchBarCollapsePx == 32f` (поле свёрнуто на 32px) **AND** устройство поворачивается (portrait → landscape)
- **THEN** после rotation `searchBarCollapsePx` остаётся `32f`
- **AND** `CollapsibleSearchField` отображается с соответствующей высотой

### Requirement: TopBar Layout Stays Single-Line

Заголовок «Events» в `TopAppBar` **MUST** всегда находиться в одной строке с `SortMenu` (в `navigationIcon`) и `PaletteFilter` (в `actions`). Заголовок **SHALL NOT** перемещаться во вторую строку ни при каких условиях (collapsed, expanded, scroll state).

Реализация: `TopAppBar` (small, 64dp), без `scrollBehavior` (`MainScreen.kt:MainScreenTopBar`). Возврат с `MediumTopAppBar` зафиксирован в Этапе 3.1.

#### Scenario: Title stays single-line during scroll

- **WHEN** пользователь скроллит список вверх **AND** `CollapsibleSearchField` сворачивается
- **THEN** заголовок «Events» остаётся в одной строке с `SortMenu` и `PaletteFilter`
- **AND** высота `TopAppBar` не меняется

### Requirement: CollapsibleSearchField Lives in ScreenBody

`CollapsibleSearchField` **MUST** быть дочерним элементом `ScreenBody` (внутри `Column` с `AnimatedVisibility`), а не частью `TopAppBar` или `MainScreenTopBar`. Размещение в `TopAppBar` через `title`-слот **SHALL NOT** использоваться.

Реализация: `Column { AnimatedVisibility(CollapsibleSearchField); MainScreenContentByState }` в `ScreenBody` (`MainScreen.kt`).

#### Scenario: CollapsibleSearchField is in ScreenBody

- **WHEN** `MainScreen` компонуется
- **THEN** `CollapsibleSearchField` находится в иерархии `ScreenBody` (не `MainScreenTopBar`)
- **AND** `TopAppBar` не содержит `SearchField` в своём `title`-слоте

### Requirement: Modifier.layout Without Constraints.Infinity

`CollapsibleSearchField` **SHALL** использовать `Modifier.layout { measurable, constraints -> layout(w, visibleHeight) { placeRelative(0, -collapsePx) } }` БЕЗ копии `constraints` с `Constraints.Infinity`. Копия `Constraints.Infinity` ломает M3 `SearchBar` (`SearchBar.kt:2206`: `minHeight = maxHeight = defaultStartHeight`).

Реализация: `measurable.measure(constraints)` напрямую, без `copy(maxHeight = Constraints.Infinity)` (`MainScreen.kt`).

#### Scenario: Modifier.layout preserves M3 SearchBar rendering

- **WHEN** `CollapsibleSearchField` компонуется
- **THEN** M3 `SearchBar` внутри `Modifier.layout` корректно измеряется и рисуется
- **AND** поле видимо (не «раздувается» до невидимости)

### Requirement: Height Measured via Modifier.onSizeChanged

Высота `SearchField` **MUST** измеряться через `Modifier.onSizeChanged { onSearchFieldHeightPxChange(it.height) }` на дочернем `SearchField`, а не через state-write колбэк внутри `Modifier.layout` (паттерн `TopAppBarLayout`). Side-effect в layout-фазе ломает exit-анимацию `AnimatedVisibility`.

Реализация: `Modifier.onSizeChanged` на дочернем `SearchField` (`MainScreen.kt`), не в layout-блоке.

#### Scenario: Height reported without breaking exit animation

- **WHEN** `CollapsibleSearchField` компонуется **AND** `SearchField` внутри него меняет высоту
- **THEN** `searchFieldHeightPx` обновляется через колбэк `onSizeChanged`
- **AND** exit-анимация `AnimatedVisibility` (при `showSearchField` из `true` в `false`) не ломается
- **AND** тест `when_user_clears_query_with_4_items_then_search_field_animates_out` проходит за <1000мс

### Requirement: Padding Scheme Preserves TopAppBar Space

`Column` в `ScreenBody` **MUST** принимать `top+start+end` от `paddingValues` (`Scaffold.paddingValues`), а `MainScreenContentByState` **SHALL** получать только `bottom` padding. Иначе `CollapsibleSearchField` рендерится ЗА TopAppBar.

Реализация: `Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding(), start = ..., end = ..., bottom = 0.dp))` (`MainScreen.kt`); `MainScreenContentByState` получает `PaddingValues(top = 0.dp, start = 0.dp, end = 0.dp, bottom = paddingValues.calculateBottomPadding())`.

#### Scenario: Column reserves TopAppBar space

- **WHEN** `ScreenBody` компонуется
- **THEN** `Column` имеет верхний padding равный высоте `TopAppBar` + status bar inset
- **AND** `CollapsibleSearchField` находится ниже `TopAppBar` (не перекрывается)
- **AND** список начинается сразу под `CollapsibleSearchField`

### Requirement: Scroll Formula Sign Correctness

Формула обновления `searchBarCollapsePx` в `onPreScroll` **MUST** быть `(searchBarCollapsePx - available.y)`, а не `(searchBarCollapsePx + available.y)`. Знаковая ошибка приводит к разворачиванию поля при swipe up вместо сворачивания.

Реализация: `(searchBarCollapsePx - available.y).coerceIn(0f, searchFieldHeightPx.toFloat())` (`MainScreenScaffold.kt`).

#### Scenario: Scroll up grows collapse

- **WHEN** пользователь выполняет swipe up по `LazyColumn` **AND** `available.y < 0` (отрицательный scroll-offset)
- **THEN** `searchBarCollapsePx = searchBarCollapsePx - available.y` (вычитание отрицательного = увеличение)
- **AND** `CollapsibleSearchField` сворачивается на величину `|available.y|`

#### Scenario: Scroll down shrinks collapse

- **WHEN** пользователь выполняет swipe down по `LazyColumn` **AND** `available.y > 0` (положительный scroll-offset)
- **THEN** `searchBarCollapsePx = searchBarCollapsePx - available.y` (вычитание положительного = уменьшение)
- **AND** `CollapsibleSearchField` разворачивается на величину `available.y` (clamped к `searchFieldHeightPx`)
