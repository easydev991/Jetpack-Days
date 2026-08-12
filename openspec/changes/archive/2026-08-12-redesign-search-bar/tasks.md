# Tasks: Search Bar Redesign

> Все задачи уже выполнены и закоммичены в ветке `feature/search-redesign`. Чек-боксы `[x]` отражают фактическое состояние на 2026-08-12. Финальная история зафиксирована в коммитах `1910c5e` → `da05bfe` → `cee078c` → `3404411` → `057d469` (5 коммитов). Промежуточная история (`c8f15f1`/`e3332f1`/`b2297f2` — DockedSearchBar+overlay → SearchBar-финал) была сквошнута; восстановима по финальному коду в `MainScreenComponents.kt:222-263` (тело `SearchField`) и `MainScreen.kt:169-205` (`ScreenBody`).

## 1. Material 3 API и подготовительные решения

- [x] 1.1 Проверена версия material3 1.4.0 в `~/.gradle/caches/.../material3-android-1.4.0-sources.jar` (BOM `2026.06.01`)
- [x] 1.2 Выбран `SearchBar(SearchBar.kt:534)` с `expanded = false` (финальный компонент — после провала `DockedSearchBar`; промежуточные коммиты сквошнуты, см. `MainScreenComponents.kt:222-263`)
- [x] 1.3 Зафиксирован `windowInsets = WindowInsets(0, 0, 0, 0)` против дефолтного top status-bar inset
- [x] 1.4 Отвергнуты альтернативные API: deprecated-оверлоады старых сигнатур с `active`/`onActiveChange` — `SearchBar.kt:1949` (старая `SearchBar`) и `SearchBar.kt:2030` (deprecated `DockedSearchBar`); `SearchBarState`-based API (`SearchBar.kt:225, 426`); `Surface(shape) + BasicTextField`
- [x] 1.5 Спецификация анимации: `expandVertically(Alignment.Top) + fadeIn(tween(200))`, `shrinkVertically(Alignment.Top) + fadeOut(tween(200))`

## 2. Реализация SearchField в MainScreenComponents.kt

- [x] 2.1 Публичная сигнатура `SearchField(searchQuery, onSearchQueryChange, modifier)` сохранена (`MainScreenComponents.kt:222`)
- [x] 2.2 Тело заменено: `OutlinedTextField` → `SearchBar(SearchBarDefaults.InputField(...), expanded = false, onExpandedChange = { }, modifier, windowInsets = WindowInsets(0,0,0,0))` (`MainScreenComponents.kt:222-263`)
- [x] 2.3 `trailingIcon` передан через стандартный слот `InputField.trailingIcon` как `IconButton(onClick = { onSearchQueryChange("") }) { Icon(Close, ...) }`, показывается только при `searchQuery.isNotEmpty()`
- [x] 2.4 Импорты: добавлены `SearchBar`, `SearchBarDefaults`, `WindowInsets`; удалён `OutlinedTextField` + 8 неиспользуемых после рефакторинга (`zIndex`, `clickable`, `requiredSize`, `size`, `dp`, `Role`, `role`, `semantics`). Остальные (в т.ч. `Box`, `Alignment`, `contentDescription`) сохранены — они используются в `SortMenu`, `LoadingContent`, `ErrorContent`, `EmptyContent` и в новых `Icon.Search`/`Icon.Close`.
- [x] 2.5 `@OptIn(ExperimentalMaterial3Api::class)` на уровне `SearchField`

## 3. Реализация ScreenBody в MainScreen.kt

- [x] 3.1 `ScreenBody` переписан: паддинг TopAppBar вынесен на родительский `Column` (`MainScreen.kt:173-178`)
- [x] 3.2 `AnimatedVisibility` обёртывает `SearchField` (`MainScreen.kt:181-194`) с параметрами: `visible = showSearchField = searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH`
- [x] 3.3 `MainScreenContentByState` теперь получает `PaddingValues(top = 0.dp, start = 0.dp, end = 0.dp, bottom = paddingValues.calculateBottomPadding())` (`MainScreen.kt:198-203`)
- [x] 3.4 Modifier для `SearchField`: `Modifier.fillMaxWidth().padding(horizontal = dimensionResource(R.dimen.spacing_regular))` (`MainScreen.kt:189-193`); `vertical = 8.dp` убран
- [x] 3.5 Импорты добавлены: `AnimatedVisibility`, `tween`, `expandVertically`, `fadeIn`, `fadeOut`, `shrinkVertically`, `Alignment`

## 4. UI-тесты

- [x] 4.1 Создан `app/src/androidTest/java/com/dayscounter/ui/screens/events/MainScreenSearchVisibilityUiTest.kt` (218 строк, `wc -l`)
- [x] 4.2 Реализовано 7 тестов: `when_items_count_4_then_search_field_not_displayed`, `when_items_count_5_then_search_field_displayed`, `when_items_count_drops_from_5_to_4_then_search_field_animates_out`, `when_user_typed_query_with_3_items_then_search_field_stays_visible`, `when_user_clears_query_with_4_items_then_search_field_animates_out`, `when_search_query_empty_then_clear_button_not_displayed` (R3.2 — крестик скрыт при пустом запросе), `when_device_rotates_then_search_field_remains_displayed` (R5 — rotation не ломает SearchField)
- [x] 4.3 Тесты анимации через `composeTestRule.waitUntil(timeoutMillis = 1000)` + `onAllNodesWithContentDescription(searchDescription).fetchSemanticsNodes().isEmpty()`
- [x] 4.4 Удаление элементов в тестах: напрямую через DAO (`dao.getAllItems().first().firstOrNull()`), а не через long-press + контекстное меню

## 5. Bugfixes (БАГ #1A и БАГ #1B)

- [x] 5.1 **БАГ #1A** (крестик очистки): временный фикс через overlay-`Box(zIndex(2f))` отменён как костыль в финальной реализации (промежуточные коммиты сквошнуты; см. финальный `MainScreenComponents.kt:222-263`).
- [x] 5.2 **БАГ #1A** (финальное): `trailingIcon` через стандартный `IconButton` в `SearchBarDefaults.InputField.trailingIcon` — Material3 сам позиционирует
- [x] 5.3 **БАГ #1B** (зона клика при rotation): замена `DockedSearchBar` → `SearchBar` (`SearchBar.kt:534`)
- [x] 5.4 Отвергнутые гипотезы (не помогли для БАГ #1B): `key(LocalConfiguration.current.orientation)` и `LaunchedEffect { LocalFocusManager.current.clearFocus() }`

## 6. Верификация

- [x] 6.1 `make format` — успешно (ktlint + detekt)
- [x] 6.2 `make lint` — без ошибок (ktlintCheck + detekt + markdownlint)
- [x] 6.3 `./gradlew assembleDebug` — успешно, без `DockedSearchBar + Deprecated` warnings
- [x] 6.4 `./gradlew test` — 455/455 unit-тестов зелёных (включая 23 в `MainScreenViewModelTest` без изменений)
- [x] 6.5 `make android-test` — 7/7 UI-тестов `MainScreenSearchVisibilityUiTest` зелёных
- [x] 6.6 Визуальный прогон 12 сценариев в эмуляторе (`emulator-5554` AVD «RuStore-Screenshots(AVD) - 16») — пользователь подтвердил 2026-08-12
- [ ] 6.7 `./gradlew assembleRelease` — требует секретов `.secrets/keystore/dayscounter-release.keystore` (перед релизом)

## 7. Документация

- [x] 7.1 `docs/plan-main-screen-search-v2.md` синхронизирован с кодом (Этап 3.3 отмечен `[x]`, разделы 0.1, 1.1, 3.5, Критерии завершения обновлены)
- [x] 7.2 `docs/Screen_2.1_Main_Screen.md` обновлён (9 правок: `OutlinedTextField` → `SearchBar`, 10→23 теста, добавлен changelog про UI-тесты)

## 8. План

- [x] 8.1 `openspec/changes/redesign-search-bar/` создан со всеми артефактами: `proposal.md`, `design.md`, `specs/search-bar-main-screen/spec.md`, `tasks.md`

## 9. Опционально (out of scope, не блокирует)

- [ ] 9.1 Скриншоты Play Store — пользователь обновит сам
- [ ] 9.2 `Modifier.imePadding()` для `ScreenBody` — pre-existing проблема всего приложения
- [ ] 9.3 Сворачивание при скролле (`exitUntilCollapsedScrollBehavior`)
- [ ] 9.4 `expanded = true` режим с подсказками/историей
- [ ] 9.5 Миграция на `SearchBarState`-based API при будущей `@Deprecated` текущей сигнатуры
- [ ] 9.6 Визуальная ревизия на планшетах/фолдабелях
- [ ] 9.7 `TextFieldState` вместо `String searchQuery` в VM

## 10. Known gaps в покрытии спеки

- [ ] 10.1 **R6 «Color Filter Narrows List Below Threshold»** — `specs/search-bar-main-screen/spec.md:104-109`. Реализовано: `MainScreenViewModel.kt:155,158` (`applyFilters(...)` → `_itemsCount.value = items.size`); visibility-predicate в `MainScreen.kt:180` потребляет filtered `itemsCount` (`showSearchField = searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH`). Цепочка VM→UI на месте, теста нет. Требует DataStore-взаимодействия (per-user цветовой фильтр применяется к репозиторию через `MainScreenViewModel.applyColorFilter(...)`/preferences), не дёшево. Отдельная задача.
