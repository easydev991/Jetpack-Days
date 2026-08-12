# Proposal: Redesign Search Bar

## Why

Поле поиска на главном экране (`MainScreen`) реализовано поверх `OutlinedTextField` — это нестандартный паттерн для приложения на Material 3, плохо интегрируется с темой и не имеет анимации появления/скрытия при переходе через порог `MIN_ITEMS_FOR_SEARCH`. Замена на `SearchBar` из material3 даёт единый стиль с Material-компонентами приложения, плавный показ/скрытие, исправляет два визуальных бага (крестик очистки и зона клика при повороте экрана), обнаруженных в ручном прогоне 2026-08-12.

## What Changes

- **UI-компонент:** `SearchField` в `MainScreenComponents.kt` переписан с `OutlinedTextField(...)` на `SearchBar(SearchBarDefaults.InputField(...), expanded = false, onExpandedChange = { })` из material3 1.4.0. `trailingIcon` (кнопка очистки) передаётся через стандартный слот `InputField.trailingIcon` как `IconButton(onClick = { onSearchQueryChange("") })`. Используется `windowInsets = WindowInsets(0, 0, 0, 0)` для подавления дефолтного top status-bar inset.
- **Анимация:** поле поиска теперь обёрнуто в `AnimatedVisibility` с `enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(tween(200))` и `exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(tween(200))` в `ScreenBody` (`MainScreen.kt`). Верхний паддинг TopAppBar вынесен на родительский `Column`, чтобы список плавно следовал за полем при показе/скрытии.
- **Альтернативный компонент:** сравнивались `OutlinedTextField`, `DockedSearchBar` (`SearchBar.kt:648`) и `SearchBar` (`SearchBar.kt:534`). Финальный выбор — `SearchBar` (`SearchBar.kt:534`) — стандартный компонент Material3 для поиска, не требующий overlay-обёрток и совместимый с целевой версией `composeBom`.
- **Тесты:** добавлен `MainScreenSearchVisibilityUiTest` (7 UI-тестов: 4 элемента не видно, 5 видно, удаление до 4 с анимацией выхода, активный ввод удерживает поле при 3 элементах, очистка через `Close` на 4 элементах, крестик скрыт при пустом запросе, SearchField остаётся видимым после rotation устройства). Существующие 23 теста `MainScreenViewModelTest` не затронуты.

Бизнес-логика и поведение не изменились: порог `MIN_ITEMS_FOR_SEARCH` (≥5 элементов), инвариант видимости (`searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH`), `applyFilters()` в ViewModel, локализация (`R.string.search`, `R.string.close`) — без изменений.

## Capabilities

### New Capabilities

- `search-bar-main-screen`: видимость, поведение и стиль поля поиска на главном экране (`MainScreen`). Включает анимацию показ/скрытия, инвариант видимости (`searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH`), стандартный trailing-крестик через `IconButton`, отсутствие overlay-обёрток вокруг компонента.

### Modified Capabilities

<!-- Пусто — требования к существующим capability не изменились, изменилась только реализация одного UI-компонента в одном экране. -->

## Impact

- `app/src/main/java/com/dayscounter/ui/screens/events/MainScreenComponents.kt` — переписана функция `SearchField`, заменены импорты.
- `app/src/main/java/com/dayscounter/ui/screens/events/MainScreen.kt` — `ScreenBody` получает `Column`-паддинг TopAppBar и `AnimatedVisibility` вокруг `SearchField`.
- `app/src/androidTest/java/com/dayscounter/ui/screens/events/MainScreenSearchVisibilityUiTest.kt` — новый файл, 7 тестов (218 строк, `wc -l`).
- `docs/plan-main-screen-search-v2.md`, `docs/Screen_2.1_Main_Screen.md` — синхронизированы с кодом.
- Ветка: `feature/search-redesign`.
- Material3 1.4.0 — без новых зависимостей; используется через Compose BOM `2026.06.01`.
- IME-кнопка клавиатуры: изменилась с «Done»/«Enter» на «Search» (потому что `SearchBarDefaults.InputField` жёстко ставит `keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)`, см. `SearchBar.kt:1377, 1560, 1694` — не баг, фиксируется как визуальная разница.
- Совместимость с iOS-бэкапом: не затрагивается.
