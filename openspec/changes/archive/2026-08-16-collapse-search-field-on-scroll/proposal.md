# Proposal: Collapsible Search Field on Scroll

## Why

На главном экране `MainScreen` поле поиска `SearchField` занимает фиксированную высоту (~56dp) и всегда видно при наличии 5+ записей или активном поиске. При длинном списке это уменьшает полезную площадь `LazyColumn` — пользователь видит меньше записей до первого скролла.

В Этапах 1–2 была попытка перевести экран на стандартный Material 3 паттерн `MediumTopAppBar` + `enterAlwaysScrollBehavior`. UX-ревью отклонило этот вариант: expanded-layout `MediumTopAppBar` рисует `title`-слот во второй строке под `navigationIcon` + `actions`. Когда `SearchField` живёт в `title` (даже в виде `Column`), получается верх — пустой title-слот + SortMenu/PaletteFilter, низ — SearchField с заголовком под ним. Это воспринимается как «лишний воздух между topappbar и поиском».

Возвращаемся на `TopAppBar` (small, 64dp) — заголовок «Events» ВСЕГДА в одной строке с `SortMenu` (слева) и `PaletteFilter` (справа). Сворачивание поля реализуем через `NestedScrollConnection` + `Modifier.layout` в `ScreenBody` — `SearchField` живёт в `ScreenBody` как `CollapsibleSearchField`, сворачивается по высоте при скролле вверх (от `expandedHeight` до `0dp`), разворачивается при скролле вниз.

## What Changes

- **`MainScreenTopBar`:** `MediumTopAppBar` откачен → `TopAppBar` (small). Удалены `scrollBehavior`, `Modifier.nestedScroll`, `canScroll`-guard, `MainScreenTopBarTitle`-helper, прокси-`ScreenBody`. Восстановлен `title = Text("Events")` + `SortMenu` в `navigationIcon` + `PaletteFilter` в `actions`.
- **`ScreenBody`:** перестроен как `Column { AnimatedVisibility(CollapsibleSearchField); MainScreenContentByState }`. Верхний паддинг TopAppBar вынесен на родительский `Column`, `MainScreenContentByState` получает только `bottom` padding.
- **`CollapsibleSearchField` (новый):** `private fun CollapsibleSearchField(...)` в `MainScreen.kt`. `Box(testTag="collapsibleSearchField" + clipToBounds + Modifier.layout { placeRelative(0, -collapsePx) })` оборачивает `SearchField` с `Modifier.onSizeChanged`. Высота измеряется дочерним элементом (без side-effect в layout-фазе).
- **`MainScreenScaffold`:** добавлен `nestedScrollConnection` на сам `Scaffold` (родитель `LazyColumn` через `content`-слот). State поднят: `searchFieldHeightPx` (Int, через `onSizeChanged`), `searchBarCollapsePx` (Float, `rememberSaveable` для rotation). Логика `onPreScroll`: `(searchBarCollapsePx - available.y).coerceIn(0f, searchFieldHeightPx.toFloat())`. Блокировка при активном поиске: `if (state.searchQuery.isNotEmpty()) return Offset.Zero`.
- **Тесты:** добавлено 5 новых UI-тестов в `MainScreenSearchVisibilityUiTest.kt`: `when_scroll_up_in_long_list_then_search_field_collapses`, `when_scroll_down_after_collapse_then_search_field_expands`, `when_short_scroll_up_then_search_field_partially_collapsed`, `when_search_query_active_then_scroll_does_not_collapse_search_field`, `when_items_below_threshold_then_scroll_does_not_affect_search_field`. Helpers: `collapsibleSearchFieldHeight()` (`MainScreenSearchVisibilityUiTest.kt`), `swipeVertically(upward, fraction)` (`MainScreenSearchVisibilityUiTest.kt`). Итого 12/12 зелёных (7 visibility + 5 collapse) на эмуляторе.

Бизнес-логика и поведение не изменились: порог `MIN_ITEMS_FOR_SEARCH` (≥5 элементов), инвариант видимости (`searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH`), `applyFilters()` в ViewModel, локализация — без изменений.

## Capabilities

### New Capabilities

- `ui-events-screen-collapse-search-field`: поведение сворачивания/разворачивания поля поиска при скролле на главном экране `MainScreen`. Включает `CollapsibleSearchField` в `ScreenBody`, `NestedScrollConnection` в `MainScreenScaffold`, формулу `searchBarCollapsePx - available.y` в `onPreScroll`, блокировку сворачивания при активном поиске, persistence через `rememberSaveable`.

### Modified Capabilities

<!-- Пусто — требования к существующим capability не изменились, изменилась только реализация UI. -->

## Impact

- `app/src/main/java/com/dayscounter/ui/screens/events/MainScreen.kt` — `ScreenBody` перестроен, `CollapsibleSearchField` добавлен (`MainScreen.kt`); `MediumTopAppBar`/`MainScreenTopBarTitle`/прокси-`ScreenBody` удалены.
- `app/src/main/java/com/dayscounter/ui/screens/events/MainScreenScaffold.kt` — добавлены `nestedScrollConnection` (`Modifier.nestedScroll`), state-поля (`searchFieldHeightPx`, `searchBarCollapsePx`), `onPreScroll`-логика (`MainScreenScaffold.kt`).
- `app/src/androidTest/java/com/dayscounter/ui/screens/events/MainScreenSearchVisibilityUiTest.kt` — добавлены 5 collapse-тестов и 2 helper-метода. Удалены 4 теста на MediumTopAppBar-collapse (завязаны на `testTag("mainScreenTopBar")`). Итого 12 тестов.
- `docs/plan-main-screen-search-scroll-hide-medium-topbar.md`, `docs/Screen_2.1_Main_Screen.md` — синхронизированы с кодом.
- Совместимость с iOS-бэкапом: не затрагивается.
