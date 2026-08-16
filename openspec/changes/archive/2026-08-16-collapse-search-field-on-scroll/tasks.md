# Tasks: Collapsible Search Field on Scroll

> Все задачи Этапа 3.1+3.2+3.2.1+3.3 выполнены и закоммичены в коммите `26f4cbb`. Финальная история зафиксирована: `26f4cbb` Этап 3 — CollapsibleSearchField через NestedScroll. Промежуточные коммиты (`01570d3` / `7e757c8` — MediumTopAppBar) сохранены в истории, откачены в Этапе 3.1 частичным revert.

## 1. Этап 3.1 — Откат MediumTopAppBar → TopAppBar (small)

- [x] 1.1 Удалён `MediumTopAppBar` (заменён на `TopAppBar` small) в `MainScreen.kt:MainScreenTopBar`
- [x] 1.2 Удалены `scrollBehavior`, `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` в `MainScreenScaffold.kt`
- [x] 1.3 Удалена `canScroll = { state.searchQuery.isEmpty() }` лямбда (не применимо к `TopAppBar` без `scrollBehavior`)
- [x] 1.4 Удалён helper `MainScreenTopBarTitle` (вынесен для соблюдения `LongMethod` в Этапе 2, не нужен после revert)
- [x] 1.5 Удалён прокси-`ScreenBody` (`MainScreen.kt`); восстановлен `ScreenBody` с `Column { AnimatedVisibility(...); MainScreenContentByState }`
- [x] 1.6 Восстановлены `title = Text("Events")`, `SortMenu` в `navigationIcon`, `PaletteFilter` в `actions`
- [x] 1.7 Удалены 4 UI-теста на MediumTopAppBar-collapse (завязаны на `testTag("mainScreenTopBar")`)
- [x] 1.8 `MainScreenTopBarState.searchQuery`/`onSearchQueryChange` сохранены (для передачи в `NestedScrollConnection`-блокировку)
- [x] 1.9 `make format` / `make test` / `make build` — успех (7 ранее существовавших тестов зелёные без регрессий)

## 2. Этап 3.2 — CollapsibleSearchField через NestedScrollConnection

- [x] 2.1 Реализована `private fun CollapsibleSearchField(...)` в `MainScreen.kt`: `Box(testTag="collapsibleSearchField" + clipToBounds + Modifier.layout { placeRelative(0, -collapsePx) })` оборачивает `SearchField` с `Modifier.onSizeChanged`
- [x] 2.2 State поднят в `MainScreenScaffold`: `searchFieldHeightPx` (Int, через `onSizeChanged`), `searchBarCollapsePx` (Float, `rememberSaveable(mutableFloatStateOf(0f))`)
- [x] 2.3 `nestedScrollConnection` живёт на самом `Scaffold` (`Modifier.nestedScroll(nestedScrollConnection)`) — `Scaffold` гарантированно ancestor `LazyColumn` через `content`-слот
- [x] 2.4 `onPreScroll`-логика: `(searchBarCollapsePx - available.y).coerceIn(0f, searchFieldHeightPx.toFloat())` (`MainScreenScaffold.kt`)
- [x] 2.5 Блокировка collapse при активном поиске: `if (state.searchQuery.isNotEmpty()) return Offset.Zero` (`MainScreenScaffold.kt`)
- [x] 2.6 Импорты добавлены: `Modifier.layout`, `nestedScroll`, `rememberSaveable`, `NestedScrollConnection`, `LocalLayoutDirection`, `Modifier.onSizeChanged`, `clipToBounds`

## 3. Этап 3.2.1 — Bugfixes (3 наслоённые причины + знаковая ошибка)

- [x] 3.1 **Bugfix #1** — Padding-схема: `Column` принимает `top+start+end`, list — только `bottom` (`MainScreen.kt`). uiautomator: `EditText [0,24][1260,192]` → `[48,288][1212,456]`.
- [x] 3.2 **Bugfix #2** — `Constraints.Infinity` ломает M3 `SearchBar`: `measurable.measure(constraints)` без копии с `Infinity`; сворачивание через `layout(w, visibleHeight) { placeRelative(0, -collapsePx) }` + `clipToBounds()` снаружи (`MainScreen.kt`). `graphicsLayer` не нужен.
- [x] 3.3 **Bugfix #3** — state-write в measure-блоке ломает exit-анимацию: высота через `Modifier.onSizeChanged { onSearchFieldHeightPxChange(it.height) }` на дочернем `SearchField`, не в layout-блоке (`MainScreen.kt`).
- [x] 3.4 **Bugfix #4** — знаковая ошибка в формуле плана A: `(searchBarCollapsePx + available.y)` → `(searchBarCollapsePx - available.y)` (`MainScreenScaffold.kt`).
- [x] 3.5 Гипотезы про `graphicsLayer.compositingStrategy`/`clip` опровергнуты чтением исходника `SearchBar.kt:2206` — зафиксировано в плане

## 4. Этап 3.3 — UI-тесты (5 новых collapse + helpers)

- [x] 4.1 5 новых UI-тестов добавлены в `MainScreenSearchVisibilityUiTest.kt` (30 items каждый, кроме threshold):
  - `when_scroll_up_in_long_list_then_search_field_collapses`
  - `when_scroll_down_after_collapse_then_search_field_expands`
  - `when_short_scroll_up_then_search_field_partially_collapsed` (fraction 0.2f)
  - `when_search_query_active_then_scroll_does_not_collapse_search_field`
  - `when_items_below_threshold_then_scroll_does_not_affect_search_field` (4 items)
- [x] 4.2 Селектор `Modifier.testTag("collapsibleSearchField")` на `Box` (`MainScreen.kt`)
- [x] 4.3 Helpers: `collapsibleSearchFieldHeight()` — читает `boundsInRoot.height` через `onNodeWithTag("collapsibleSearchField")`; `swipeVertically(upward, fraction)` — явные координаты, стандартный `swipeUp()` попадал на topbar
- [x] 4.4 **12/12 зелёные** (7 visibility + 5 collapse) на `emulator-5554`, Android 16. `make lint` / `make test` — успех.

## 5. Этап 3.4 — Документация

- [x] 5.1 `docs/Screen_2.1_Main_Screen.md` обновлён:
  - счётчик UI-тестов «5 тестов» → «12 тестов» (7 visibility + 5 collapse)
  - уточнено — SearchField живёт в `ScreenBody` как `CollapsibleSearchField` через `NestedScrollConnection` + `Modifier.layout`, **не в topbar**; сворачивается по высоте при скролле вверх (от `expandedHeight` до `0dp`), разворачивается при скролле вниз
  - changelog: добавлена запись «2026-08-16: Возврат с MediumTopAppBar на TopAppBar (small) + CollapsibleSearchField в ScreenBody через NestedScrollConnection + Modifier.layout. 5 новых UI-тестов (12/12 зелёных). Заголовок Events теперь всегда в одной строке с SortMenu и PaletteFilter»
- [x] 5.2 Создана запись `openspec/changes/archive/2026-08-16-collapse-search-field-on-scroll/` со всеми артефактами:
  - `proposal.md` — почему вернулись с MediumTopAppBar; new capability `ui-events-screen-collapse-search-field`
  - `design.md` — 10 решений (D1–D10) + 4 bugfix'а (3 наслоённые причины + знаковая ошибка)
  - `tasks.md` — выполненные подэтапы Этапа 3 (3.1+3.2+3.2.1+3.3) + 3.4 документация
  - `specs/ui-events-screen-collapse-search-field/spec.md` — дельта-спека на новую capability

## 6. Верификация

- [x] 6.1 `make format` — успешно (ktlint + detekt)
- [x] 6.2 `make lint` — без ошибок (ktlintCheck + detekt)
- [x] 6.3 `./gradlew assembleDebug` — успешно
- [x] 6.4 `./gradlew test` — 457/457 unit-тестов зелёных
- [x] 6.5 `make android-test` — 12/12 UI-тестов `MainScreenSearchVisibilityUiTest` зелёных
- [x] 6.6 Визуальный прогон 12 сценариев в эмуляторе (`emulator-5554`) — подтверждено в коммите `26f4cbb`
- [ ] 6.7 `./gradlew assembleRelease` — требует секретов `.secrets/keystore/dayscounter-release.keystore` (перед релизом)

## 7. Опционально (out of scope, не блокирует)

- [ ] 7.1 Snap к границам при отпускании скролла (плавная анимация expand/collapse в зависимости от offset) — отдельная задача
- [ ] 7.2 `imePadding()` / `consumeWindowInsets()` для `MainScreen` — pre-existing проблема
- [ ] 7.3 `pinnedScrollBehavior()` вместо `NestedScrollConnection` — если в будущем понадобится pinned-collapse без ручного `Modifier.layout`
- [ ] 7.4 Миграция на `SearchBarState`-based API при будущей `@Deprecated` текущей сигнатуры
- [ ] 7.5 Кастомизация `keyboardOptions` (дефолт Material3 — `imeAction = Search`)
- [ ] 7.6 `LongMethod` refactor в `MainScreenScaffold.kt` через `rememberNestedScrollForSearch(...)` хелпер — отдельная задача

## 8. Known gaps в покрытии

- [ ] 8.1 **R6 «Color Filter Narrows List Below Threshold»** — pre-existing, см. `openspec/changes/archive/2026-08-12-redesign-search-bar/tasks.md:10.1`. Не блокирует.
