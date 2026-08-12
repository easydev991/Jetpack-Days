# Design: Search Bar Redesign

## Context

Поле поиска `SearchField` на `MainScreen` реализовано поверх `OutlinedTextField` (Jetpack Compose Material3 1.4.0 через Compose BOM `2026.06.01`). Цель редизайна — заменить его на специализированный компонент из Material3 и добавить плавную анимацию показ/скрытия при пересечении порога `MIN_ITEMS_FOR_SEARCH`.

Предпосылки:

- `OutlinedTextField` — общий компонент Material3, а не специализированный для поиска. В Material3 есть стандартный `SearchBar` для этой задачи.
- Поле показывается/скрывается мгновенно при пересечении порога `MIN_ITEMS_FOR_SEARCH` (≥5 элементов) — выглядит резковато при переходе из 4 в 5 и обратно.

Работа выполняется в ветке `feature/search-redesign`.

## Goals / Non-Goals

**Goals:**

- Заменить `OutlinedTextField` на `SearchBar` из Material3 в `MainScreenComponents.kt:SearchField`.
- Использовать `SearchBar(SearchBarDefaults.InputField(...), expanded = false, onExpandedChange = { })` (сигнатура `SearchBar.kt:534` из material3 1.4.0).
- Передать `trailingIcon` через стандартный слот `InputField.trailingIcon` как `IconButton(onClick = { onSearchQueryChange("") }) { Icon(Close, ...) }`, показывающийся только при `searchQuery.isNotEmpty()`.
- Установить `windowInsets = WindowInsets(0, 0, 0, 0)` для подавления дефолтного top status-bar inset (`SearchBar.kt:1051-1056`).
- Обернуть `SearchField` в `AnimatedVisibility` с `expandVertically(Alignment.Top) + fadeIn(tween(200))` на вход и `shrinkVertically(Alignment.Top) + fadeOut(tween(200))` на выход.
- Перенести `paddingValues` от TopAppBar на родительский `Column` (один раз), чтобы при показе/скрытии SearchField список плавно следовал за анимацией, а не «дёргался».
- Сохранить сигнатуру `SearchField(searchQuery, onSearchQueryChange, modifier)` — единственный caller (`MainScreen.kt:186`) остаётся без изменений по форме вызова.

**Non-Goals:**

- Изменение логики видимости (порог `MIN_ITEMS_FOR_SEARCH`, `searchQuery.isNotEmpty() || itemsCount >= …`).
- Изменение `MainScreenViewModel`, `applyFilters()`, `searchQuery`/`itemsCount`/`selectedColorTag` state.
- Изменение `SortMenu`, `PaletteFilter`, `ColorTagFilterDialog`, FAB.
- Сворачивание при скролле (`exitUntilCollapsedScrollBehavior`) — отдельная задача (out of scope, см. `docs/plan-main-screen-search-v2.md`, Этап 5).
- `expanded = true` режим с подсказками/историей — не требуется (out of scope).
- Перенос ViewModel на `TextFieldState` — отдельная задача (out of scope).
- Кастомизация `keyboardOptions` (дефолт Material3 для `SearchBarDefaults.InputField` — `imeAction = Search`).
- `imePadding()` для `ScreenBody` — pre-existing проблема, не специфична для поиска (out of scope).

## Decisions

### D1. `SearchBar` — финальный компонент

**Принято:** `SearchBar(SearchBar.kt:534)` с `expanded = false`.

**Альтернативы, рассмотренные и отвергнутые:**

- `DockedSearchBar` (`SearchBar.kt:648`) — компактный вариант Material3; не используем, т.к. стандартный `SearchBar` лучше согласован с full-screen-вариантом Material3 и не требует кастомного позиционирования `trailingIcon`.
- Собственный `Surface(dockedShape) + BasicTextField` — тяжёлый путь, избыточен.
- `SearchBarState`-based API (`SearchBar.kt:225`) — избыточен для in-memory фильтрации без suggestions/истории.
- Deprecated-оверлоады старых сигнатур с `active`/`onActiveChange` — `SearchBar.kt:1949` (старая `SearchBar`) и `SearchBar.kt:2030` (deprecated `DockedSearchBar`) — не используем.

**Обоснование:** стандартный компонент Material3 для поиска; минимальное количество внешних обёрток; стандартный `trailingIcon`-слот Material3 сам позиционирует крестик корректно.

### D2. `windowInsets = WindowInsets(0, 0, 0, 0)`

**Принято:** явно передаём пустые `WindowInsets` в `SearchBar`.

**Причина:** дефолт `SearchBarDefaults.windowInsets = WindowInsets.systemBarsForVisualComponents.only(Horizontal + Top)` (`SearchBar.kt:1051-1056`) добавляет ~28dp к высоте, пересекаясь с `paddingValues` от `Scaffold`. В нашем `ScreenBody` верхний отступ уже учтён родительским `Column`, дублирование нежелательно.

### D3. `trailingIcon` через стандартный `IconButton` в слоте `InputField`

**Принято:** `trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(...) { Icon(Close, ...) } }`.

**Причина:** Material3 сам оборачивает `trailingIcon` в `Box(Modifier.offset(x = -SearchBarIconOffsetX))` (`SearchBar.kt:1711-1714`) и позиционирует внутри inputField корректно, без absolute positioning.

**Альтернатива (отвергнутая):** overlay-`Box(Modifier.align(Alignment.CenterEnd))` поверх `DockedSearchBar.Surface` для борьбы с zIndex — костыль, удалён в финальной реализации (промежуточные коммиты сквошнуты; см. финальный `MainScreenComponents.kt:222-263`).

### D4. `AnimatedVisibility` с `expandFrom = Alignment.Top` + `tween(200)`

**Принято:** стандартная короткая анимация 200мс, выравнивание по верху.

**Обоснование:** список должен «отъезжать» вниз вслед за расширением поля сверху, а не подпрыгивать. Короткая длительность — чтобы не раздражать при частой смене порога.

### D5. Padding TopAppBar вынесен на родительский `Column`

**Принято:** `Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding(), start = ..., end = ..., bottom = 0.dp))` с одним общим паддингом, а `AnimatedVisibility` и `MainScreenContentByState` — дочерние элементы без собственных `top` паддингов.

**Причина:** раньше условная логика `if (showSearchField) 0.dp else paddingValues...` в `MainScreenContentByState` ломала согласованность анимации — список «дёргался». Вынос паддинга на `Column` делает анимацию бесшовной в обоих состояниях.

### D6. Modifier для SearchField: только боковой padding через `dimensionResource`

**Принято:** `Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.spacing_regular))`.

**Причина:** design-token вместо хардкода `16.dp`. Вертикальный `padding(vertical = 8.dp)` убран — `windowInsets(0,0,0,0)` контролирует top inset, дублирование не нужно.

### D7. Файлы тестов: новый `MainScreenSearchVisibilityUiTest.kt` без расширения `MainScreenSortByTimeOfDayUiTest`

**Принято:** отдельный файл `app/src/androidTest/java/com/dayscounter/ui/screens/events/MainScreenSearchVisibilityUiTest.kt` (218 строк, `wc -l`, 7 тестов).

**Альтернатива (отвергнутая):** расширение `MainScreenSortByTimeOfDayUiTest.kt` — файл становился бы смесью разных сценариев.

## Risks / Trade-offs

- **R1. IME-кнопка клавиатуры: «Search» вместо «Done».** `SearchBarDefaults.InputField` жёстко ставит `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)` во всех трёх оверлоадах (`SearchBar.kt:1377, 1560, 1694`), параметра `keyboardOptions` нет, переопределить нельзя. Нажатие вызывает `onSearch = { }` (no-op), фильтрация работает live через `onQueryChange`. Не баг, но пользовательская разница. Митигация: зафиксировано в плане V2 как заметка для Этапа 3.3. Этап 5 может добавить кастомный `BasicTextField` под `SearchBar`, если будет жалоба.

- **R2. `imePadding` отсутствует в `ScreenBody`.** Pre-existing проблема всего приложения (`imePadding`/`consumeWindowInsets` нигде в `app/src/main` не используются, `navigationBarsPadding` только в `CreateEditButtons.kt:103`). Усугубляется при фильтрации через `SearchBar` — последние элементы списка и FAB остаются под клавиатурой. Не блокирует редизайн поиска, отдельная задача (Этап 5).

- **R3. Поведение SearchBar при будущих обновлениях material3.** В версии 1.4.0 не-deprecated сигнатура `SearchBar(inputField, expanded, onExpandedChange)` (`SearchBar.kt:534`) может стать `@Deprecated`, как это уже произошло со старой `SearchBar` (`SearchBar.kt:1949`) и с `DockedSearchBar` (`SearchBar.kt:2030`). Митигация: Этап 5 фиксирует миграцию на `SearchBarState`-based API (`SearchBar.kt:225`) или `ExpandedDockedSearchBar(state, inputField)` (`SearchBar.kt:426`) при необходимости.

- **R4. Совместимость с iOS-бэкапом.** Не затрагивается — поиск локальный, в бэкап не попадает.
