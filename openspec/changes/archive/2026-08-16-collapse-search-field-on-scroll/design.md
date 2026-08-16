# Design: Collapsible Search Field on Scroll

## Context

Поле поиска `SearchField` на `MainScreen` всегда занимает фиксированную высоту ~56dp, когда видно (порог `MIN_ITEMS_FOR_SEARCH = 5` или активный поиск). При длинном списке это уменьшает полезную площадь `LazyColumn` — пользователь видит меньше записей до первого скролла. Цель — сворачивать `SearchField` по высоте при скролле вверх (от `expandedHeight` до `0dp`) и разворачивать при скролле вниз.

В Этапах 1–2 была попытка перевести экран на стандартный Material 3 паттерн `MediumTopAppBar` + `enterAlwaysScrollBehavior`. UX-ревью отклонило `MediumTopAppBar`: его expanded-layout рисует `title`-слот во второй строке (под `navigationIcon` + `actions`), создавая «лишний воздух между topappbar и поиском».

Возвращаемся на `TopAppBar` (small, 64dp) — заголовок «Events» всегда в одной строке с `SortMenu` и `PaletteFilter`. `SearchField` переезжает в `ScreenBody` как `CollapsibleSearchField`, сворачивание делаем через `NestedScrollConnection` + `Modifier.layout`.

## Goals / Non-Goals

**Goals:**

- Вернуть `TopAppBar` (small) с `title = Text("Events")`, `SortMenu` в `navigationIcon`, `PaletteFilter` в `actions` (Этап 3.1).
- Снять `MediumTopAppBar` / `scrollBehavior` / `Modifier.nestedScroll` / `canScroll`-guard / `MainScreenTopBarTitle`-helper / прокси-`ScreenBody` (Этап 3.1).
- Реализовать `CollapsibleSearchField` в `ScreenBody`: `Box(testTag + clipToBounds + Modifier.layout { placeRelative(0, -collapsePx) })` оборачивает `SearchField` с `Modifier.onSizeChanged` (Этап 3.2).
- Поднять state в `MainScreenScaffold`: `searchFieldHeightPx` (Int, через `onSizeChanged`), `searchBarCollapsePx` (Float, `rememberSaveable` для rotation). `nestedScrollConnection` живёт на самом `Scaffold` — родитель `LazyColumn` через `content`-слот, pre-scroll доходит без прокидки `Modifier` через `ItemsListContent` (Этап 3.2).
- Блокировать сворачивание при активном поиске: `if (state.searchQuery.isNotEmpty()) return Offset.Zero` в `onPreScroll` (Этап 3.2).
- Исправить 3 наслоённые причины регрессии (см. раздел «Bugs fixed»): padding-схема Column, `Constraints.Infinity` ломает M3 SearchBar, state-write в layout-блоке ломает exit-анимацию `AnimatedVisibility` (Этап 3.2.1).
- Исправить знаковую ошибку в формуле плана A: `(searchBarCollapsePx + available.y)` → `(searchBarCollapsePx - available.y)` (`MainScreenScaffold.kt`).
- Сохранить публичную сигнатуру `SearchField(searchQuery, onSearchQueryChange, modifier)` — caller в `ScreenBody` (`MainScreen.kt`) остаётся без изменений по форме вызова.
- Добавить 5 collapse-UI-тестов + 2 helper-метода в `MainScreenSearchVisibilityUiTest.kt` (Этап 3.3).
- Удалить 4 устаревших теста на MediumTopAppBar-collapse (завязаны на `testTag("mainScreenTopBar")`).

**Non-Goals:**

- Snap к границам при отпускании скролла (плавная анимация expand/collapse в зависимости от offset) — отдельная задача, см. план `docs/plan-main-screen-search-scroll-hide-medium-topbar.md` раздел «Не в скоупе».
- `imePadding()` / `consumeWindowInsets()` для `MainScreen` — pre-existing проблема, отдельная задача.
- `pinnedScrollBehavior()` вместо `NestedScrollConnection` — отдельная задача.
- Миграция на `SearchBarState`-based API — отдельная задача.
- Кастомизация `keyboardOptions` для `SearchField` (дефолт Material3 — `imeAction = Search`).
- Параллакс / fade-out вместо slide для collapse-effect.

## Decisions

### D1. `TopAppBar` (small) — финальный вариант topbar

**Принято:** `TopAppBar(title = { Text("Events") }, navigationIcon = { SortMenu(...) }, actions = { PaletteFilter(...) })`. Без `scrollBehavior`.

**Альтернативы, рассмотренные и отвергнутые:**

- `MediumTopAppBar` (Этап 2, откачен в Этапе 3.1) — UX-ревью отклонило: title «Events» прыгает во вторую строку в expanded, создавая «лишний воздух».
- `LargeTopAppBar` (152dp expanded) — отдельная задача, не подходит для требования «title всегда в одной строке».
- `TopAppBar` + `pinnedScrollBehavior` — отдельная задача, если в будущем понадобится pinned-collapse без ручного `Modifier.layout`.

**Обоснование:** заголовок всегда в одной строке с `SortMenu` и `PaletteFilter` — UX-критерий плана. Подтверждено коммитом `26f4cbb`.

### D2. `CollapsibleSearchField` живёт в `ScreenBody`, не в topbar

**Принято:** `Column { AnimatedVisibility(CollapsibleSearchField); MainScreenContentByState }` в `ScreenBody`.

**Альтернатива (отвергнутая):** `SearchField` внутри `MediumTopAppBar.title` через `Column { AnimatedVisibility(...); Text("Events") }` (Этап 2) — UX-ревью отклонило.

**Обоснование:** `ScreenBody` — единственное место, где `SearchField` может корректно анимироваться по высоте без конфликтов с `TopAppBar`-layout.

### D3. `NestedScrollConnection` на самом `Scaffold` (не на `Column`)

**Принято:** `Modifier.nestedScroll(nestedScrollConnection)` на `Scaffold` в `MainScreenScaffold.kt`.

**Альтернатива (отвергнутая):** `Modifier.nestedScroll` на `Column` в `ScreenBody` (план A `docs/plan-main-screen-search-scroll-hide.md`) — требовала явной прокидки `Modifier` через `ItemsListContent`, лишний coupling.

**Обоснование:** `Scaffold` гарантированно ancestor `LazyColumn` (через `content`-слот). `pre-scroll` от списка доходит до колбэка без прокидки.

### D4. Формула `onPreScroll`: `(searchBarCollapsePx - available.y)`

**Принято:** `(searchBarCollapsePx - available.y).coerceIn(0f, searchFieldHeightPx.toFloat())` (`MainScreenScaffold.kt`).

**Альтернатива (отвергнутая, план A):** `(searchBarCollapsePx + available.y)` — знаковая ошибка: при swipe up `available.y < 0` → offset уменьшается → поле разворачивается вместо сворачивания.

**Обоснование:** для swipe up (`available.y < 0`) нужно УВЕЛИЧИТЬ `searchBarCollapsePx`, чтобы `Modifier.layout` сдвинул контент вверх на бóльшую величину (clipToBounds скрывает). Исправлено в коммите `26f4cbb`.

### D5. Блокировка при активном поиске: `if (state.searchQuery.isNotEmpty()) return Offset.Zero`

**Принято:** ранний `return Offset.Zero` в `onPreScroll` если `searchQuery.isNotEmpty()` (`MainScreenScaffold.kt`).

**Альтернативы (отвергнутые):**

- `canScroll = { state.searchQuery.isEmpty() }` через `enterAlwaysScrollBehavior` (Этап 2) — откачено вместе с `MediumTopAppBar`.
- `LaunchedEffect(searchQuery) { heightOffset = 0f }` (Этап 2, Вариант B) — откачено.
- `SearchField` виден только при `scrollBehavior.isExpanded || searchQuery.isNotEmpty()` (Этап 2, Вариант C) — откачено.

**Обоснование:** идиоматичный паттерн плана A, проверен в `MainScreenSearchVisibilityUiTest.kt` (`when_search_query_active_then_scroll_does_not_collapse_search_field`).

### D6. `Modifier.layout` без `Constraints.Infinity` + `clipToBounds()` снаружи

**Принято:** `Modifier.layout { measurable, constraints -> layout(w, visibleHeight) { placeRelative(0, -collapsePx) } }` + `clipToBounds()` на родительском `Box`.

**Альтернатива (отвергнутая, план A):** копия `constraints.copy(maxHeight = Constraints.Infinity)` + `graphicsLayer { translationY = -delta }` — ломает M3 `SearchBar` (см. Bugfix #2 в разделе «Bugs fixed»).

**Обоснование:** `placeRelative` сам клипоет контент в пределах `layout(w, visibleHeight)`; `clipToBounds()` нужен снаружи `Modifier.layout` для клиппинга за пределами видимой высоты.

### D7. Высота измеряется через `Modifier.onSizeChanged`, не через колбэк в layout-блоке

**Принято:** `Modifier.onSizeChanged { onSearchFieldHeightPxChange(it.height) }` на дочернем `SearchField` (`MainScreen.kt`).

**Альтернатива (отвергнутая, план A):** state-write через колбэк внутри `Modifier.layout` (паттерн `TopAppBarLayout`) — ломает exit-анимацию `AnimatedVisibility` (см. Bugfix #3 в разделе «Bugs fixed»).

**Обоснование:** side-effect в layout-фазе ломает `AnimatedVisibility` тест `when_user_clears_query_with_4_items_then_…_animates_out` падает на 1000мс. Вынос в `onSizeChanged` — стандартный паттерн измерения высоты Compose-компонента.

### D8. Padding-схема: `Column` принимает `top+start+end`, list — только `bottom`

**Принято:** `Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding(), start = ..., end = ..., bottom = 0.dp))` (`MainScreen.kt`); `MainScreenContentByState` получает `PaddingValues(top = 0.dp, start = 0.dp, end = 0.dp, bottom = paddingValues.calculateBottomPadding())`.

**Альтернатива (отвергнутая, Этап 3.1 первый вариант):** весь `paddingValues` уходил в `contentPadding` `LazyColumn` → `CollapsibleSearchField` рендерился ЗА TopAppBar (см. Bugfix #1 в разделе «Bugs fixed»).

**Обоснование:** паддинг TopAppBar должен влиять на положение `Column` (родитель `CollapsibleSearchField`), а не на contentPadding списка — иначе поле рендерится в верхней области экрана, перекрытой TopAppBar.

### D9. `rememberSaveable(mutableFloatStateOf(0f))` для `searchBarCollapsePx`

**Принято:** `rememberSaveable(mutableFloatStateOf(0f))` для persistence через rotation.

**Обоснование:** без Saver `searchBarCollapsePx` сбрасывается в 0 при rotation → теряется состояние сворачивания. Подтверждено визуальным прогоном на эмуляторе.

### D10. Тесты: расширение `MainScreenSearchVisibilityUiTest`, не новый файл

**Принято:** добавить 5 collapse-тестов + 2 helper в существующий `MainScreenSearchVisibilityUiTest.kt`.

**Альтернатива (отвергнутая):** новый файл `MainScreenSearchCollapseUiTest.kt` — дробление одного тематического файла.

## Bugs fixed

Этап 3.2.1 (`docs/plan-main-screen-search-scroll-hide-medium-topbar.md`) зафиксировал 3 наслоённые причины регрессии, обнаруженные при попытке реализовать план A (`docs/plan-main-screen-search-scroll-hide.md`):

### Bugfix #1 — `CollapsibleSearchField` рендерился ЗА TopAppBar

**Симптом:** `assertIsDisplayed()` проходит (семантика честная), но визуально поле не видно — перекрыто TopAppBar. uiautomator показывал `EditText [0,24][1260,192]`.

**Причина:** revert Этапа 3.1 потерял padding-схему `143f0ec`. `Column` нёс `top+horizontal` сам, list получал `contentPadding(top=0, bottom=…)`. После revert весь `paddingValues` уходил в `contentPadding` `LazyColumn` → `CollapsibleSearchField` на `y=24..192`, topbar на `y=0..264`.

**Фикс:** `Column` принимает `top+start+end`, list — только `bottom` (`MainScreen.kt`). uiautomator: стало `[48,288][1212,456]`.

### Bugfix #2 — `Constraints.Infinity` ломает M3 `SearchBar`

**Симптом:** `SearchBar` не рисуется, хотя `placeable.height` корректный — сбивает с толку. Гипотезы про `graphicsLayer.compositingStrategy`/`clip` опровергнуты чтением исходника.

**Причина:** M3 `SearchBar` — кастомный `SearchBarLayout` (`SearchBar.kt:2206`) с `minHeight = maxHeight = defaultStartHeight`. Без клампа `Infinity` измерение SearchBar раздувается → не рисуется.

**Фикс:** `measurable.measure(constraints)` без копии с `Infinity`. Сворачивание: `layout(w, visibleHeight) { placeRelative(0, -collapsePx) }` + `clipToBounds()` снаружи `Modifier.layout`. `graphicsLayer` не нужен (`MainScreen.kt`).

### Bugfix #3 — state-write в measure-блоке `Modifier.layout` ломает exit-анимацию

**Симптом:** тест `when_user_clears_query_with_4_items_then_…_animates_out` падает на 1000мс.

**Причина:** план A репортил высоту через колбэк внутри `Modifier.layout` (паттерн `TopAppBarLayout`). Side-effect в layout-фазе ломает `AnimatedVisibility`.

**Фикс:** `Modifier.onSizeChanged { onSearchFieldHeightPxChange(it.height) }` на дочернем `SearchField`, не в layout-блоке (`MainScreen.kt`).

### Bugfix #4 — знаковая ошибка в формуле плана A

**Симптом:** при swipe up поле РАЗВОРАЧИВАЕТСЯ вместо сворачивания.

**Причина:** план A: `newOffset = (searchBarCollapsePx + available.y)`. `available.y<0` для swipe up → offset уменьшается → поле разворачивается вместо сворачивания.

**Фикс:** `(searchBarCollapsePx - available.y)` (`MainScreenScaffold.kt`).

## Risks / Trade-offs

- **R1. Анимация пропорциональная, без snap.** В текущей реализации (`Modifier.layout` + ручной `searchBarCollapsePx`) анимация сворачивания пропорциональна скорости скролла, без snap к границам при отпускании. Если UX-тест покажет, что snap нужен — отдельный тикет (`pinnedScrollBehavior` или кастомный `Animatable`).

- **R2. `imePadding` отсутствует в `ScreenBody`.** Pre-existing проблема всего приложения. Усугубляется при фильтрации через `SearchField` — последние элементы списка и FAB остаются под клавиатурой. Не блокирует collapse, отдельная задача.

- **R3. Совместимость с iOS-бэкапом.** Не затрагивается — collapse-search локальный, в бэкап не попадает.

- **R4. Re-render при изменении `searchFieldHeightPx`.** `Modifier.onSizeChanged` дёргает колбэк при каждом изменении высоты (например, при rotation). Это редкое событие, performance impact минимален.

- **R5. detekt: `LongMethod` 67/60 в `MainScreenScaffold.kt` + `TooManyFunctions` 11/11 в `MainScreen.kt`.** Техдолг из Этапа 3, не блокирует. Рефакторинг через `rememberNestedScrollForSearch(...)` хелпер — отдельная задача.
