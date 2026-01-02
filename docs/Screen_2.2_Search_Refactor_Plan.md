# План переделки поиска на Main Screen

## Обзор

Документ описывает план по переделке компонента поиска на главном экране приложения. 

**Проблема:**
- Используется deprecated API `SearchBar` (требует использования нового overload с параметром `inputField`)
- Кнопка поиска находится в TopAppBar, что не соответствует желаемому UX
- При активации поиска SearchBar перекрывает весь экран и отображает отдельный список результатов
- Поле поиска отображается даже при пустом списке или при небольшом количестве записей (нет смысла искать)

**Цель:**
- Устранить предупреждение о deprecated API
- Разместить поле поиска над списком записей (между TopAppBar и LazyColumn) только при наличии 5 и более записей
- Поле поиска фильтрует основной список записей в реальном времени
- Список корректно учитывает paddingValues и безопасные зоны
- При удалении записей до 4 и менее поле поиска скрывается, а при добавлении до 5 и более — отображается

---

## Анализ текущей реализации

### Текущая архитектура

**Файл:** `app/src/main/java/com/dayscounter/ui/screen/MainScreen.kt`

**Логика поиска (ViewModel):**
```kotlin
// MainScreenViewModel.kt (строки 89-116)
private fun observeItems() {
    viewModelScope.launch {
        combine(
            _sortOrder.flatMapLatest { sortOrder -> repository.getAllItems(sortOrder) },
            _searchQuery,
        ) { items, query ->
            if (query.isEmpty()) {
                items
            } else {
                items.filter { item ->
                    val titleContains = item.title.contains(query, ignoreCase = true)
                    val detailsContains = item.details.contains(query, ignoreCase = true)
                    titleContains || detailsContains
                }
            }
        }.collect { items ->
            _itemsCount.value = items.size
            _uiState.value = MainScreenState.Success(items)
        }
    }
}
```

**Управление состоянием:**
- `isSearchActive` — состояние видимости SearchBar (строка 144 в MainScreen.kt)
- `searchQuery` — поисковый запрос (StateFlow в ViewModel)
- Фильтрация уже работает на уровне ViewModel через `combine()`

**UI компоненты:**
- `mainScreenTopBar()` — TopAppBar с кнопкой поиска и сортировки (строки 596-677)
  - При `isSearchActive = true`: отображается SearchBar с результатами
  - При `isSearchActive = false`: отображается TopAppBar с кнопкой поиска
- `searchResultItem()` — отдельный элемент списка результатов поиска (строки 574-591)
- LazyColumn с основными записями (строки 380-384)

**Проблемы:**
1. SearchBar deprecated — требуется новый overload с `inputField`
2. Поиск показывается в отдельном режиме, перекрывающем экран
3. Дублируется логика отображения результатов (в SearchBar и в основном списке)

---

## План реализации

### Шаг 1: Удаление кнопки поиска из TopAppBar

**Цель:** Убрать кнопку поиска (иконка лупы) из actions TopAppBar.

**Изменения в `MainScreen.kt`:**

1. **Удалить состояние `isSearchActive`:**
   - Удалить переменную `var isSearchActive` (строка 144)
   - Удалить передачу `isSearchActive` в `MainScreenTopBarState` (строки 151-152)

2. **Упростить `mainScreenTopBar()`:**
   - Удалить логику `if (state.isSearchActive) { SearchBar(...) } else { TopAppBar(...) }`
   - Оставить только TopAppBar без кнопки поиска в actions
   - Удалить параметр `isSearchActive` из `MainScreenTopBarState`
   - Удалить параметр `onSearchActiveChange` из `MainScreenTopBarState`

3. **Удалить лишние компоненты:**
   - Удалить функцию `searchResultItem()` (строки 574-591) — больше не нужна
   - Удалить импорт `SearchBar` из `androidx.compose.material3.SearchBar`

**Результат:** TopAppBar отображается всегда, без кнопки поиска.

---

### Шаг 2: Создание компонента поля поиска

**Цель:** Создать компонент TextField для поиска, который будет постоянно отображаться над списком.

**Реализация в `MainScreen.kt`:**

```kotlin
/**
 * Поле поиска для фильтрации списка записей.
 */
@Composable
private fun searchField(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchQueryChange,
        placeholder = {
            Text(stringResource(R.string.search))
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = stringResource(R.string.search),
            )
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close),
                    )
                }
            }
        },
        singleLine = true,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        colors = TextFieldDefaults.outlinedTextFieldColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}
```

**Характеристики компонента:**
- `OutlinedTextField` — стандартный компонент Material 3 для ввода текста
- Иконка поиска (лупа) в `leadingIcon`
- Иконка закрытия (крестик) в `trailingIcon` (показывается только когда поле не пустое)
- Placeholder с текстом "Search"
- Однострочное поле (`singleLine = true`)
- Высота 56dp (стандартная для текстовых полей)
- Цвет фона совпадает с `surface` для визуальной интеграции

---

### Шаг 3: Интеграция поля поиска в структуру экрана

**Цель:** Разместить поле поиска между TopAppBar и LazyColumn, но только при наличии 5 и более записей.

**Изменения в `mainScreenContent()`:**

Текущая структура:
```kotlin
Scaffold(
    topBar = { mainScreenTopBar(...) },
    floatingActionButton = { ... },
) { paddingValues ->
    mainScreenContentByState(state, paddingValues)
}
```

Новая структура:
```kotlin
Scaffold(
    topBar = { mainScreenTopBar(...) },
    floatingActionButton = { ... },
) { paddingValues ->
    Column(modifier = Modifier.fillMaxSize()) {
        // Поле поиска над списком (отображается, если есть текст в поиске ИЛИ есть 5+ записей)
        val showSearchField = searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH
        if (showSearchField) {
            searchField(
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        // Список записей (с учетом высоты поля поиска если оно отображается)
        mainScreenContentByState(
            state = state,
            paddingValues = PaddingValues(
                top = if (showSearchField) 0.dp else paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding(),
            ),
        )
    }
}
```

**Добавить константу:**
```kotlin
// Минимальное количество записей для отображения поля поиска
private const val MIN_ITEMS_FOR_SEARCH = 5
```

**Важные моменты:**

1. **Условное отображение поля поиска:**
   - Поле отображается, если `searchQuery.isNotEmpty() || itemsCount >= MIN_ITEMS_FOR_SEARCH` (есть текст в поиске ИЛИ 5+ записей)
   - Это гарантирует, что поле поиска не исчезнет во время активного поиска, даже если найдено мало результатов
   - При пустом поиске и 4 или менее записях поле поиска скрывается
   - При добавлении записей до 5 и более поле поиска автоматически отображается

2. **Адаптивный padding для списка:**
   - Если поле поиска отображено: список не учитывает top padding (занимает всё место под полем)
   - Если поле поиска скрыто: список учитывает top padding от TopAppBar

3. **Padding для поля поиска:**
   - Горизонтальные отступы: 16dp (согласно Material Design)
   - Вертикальные отступы: 8dp (для разделения от TopAppBar)
   - `padding(paddingValues)` применяет только top padding от TopAppBar

4. **Безопасные зоны:**
   - TopAppBar уже учитывает safe area (через Scaffold)
   - Поле поиска применяет padding от TopAppBar
   - Список применяет bottom padding для области FloatingActionButton и safe area

5. **Почему 5 записей?**
   - При меньшем количестве записей поиск не имеет практической ценности
   - Пользователь может легко найти нужную запись визуально
   - Уменьшает визуальный шум на экране для новых пользователей

---

### Шаг 4: Обновление функции `mainScreenContentByState()`

**Цель:** Убедиться, что список корректно отображается под полем поиска.

**Изменения в `mainScreenContentByState()`:**

Текущая сигнатура:
```kotlin
private fun mainScreenContentByState(
    state: MainScreenContentState,
    paddingValues: PaddingValues,
)
```

Новая сигнатура (без изменений):
```kotlin
private fun mainScreenContentByState(
    state: MainScreenContentState,
    paddingValues: PaddingValues,
)
```

**Обновление `emptyContent()`, `emptySearchContent()`, `loadingContent()`, `errorContent()`:**

Все эти функции принимают `paddingValues` и применяют их через `Modifier.padding(paddingValues)`. После изменений они должны:
- Применять только bottom и left/right padding из `paddingValues`
- Не применять top padding (учитывается полем поиска)

Реализация:
```kotlin
private fun itemsListContent(
    items: List<Item>,
    listState: LazyListState,
    getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
    paddingValues: PaddingValues,
) {
    // ... остальной код ...
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(
            start = paddingValues.calculateStartPadding(LocalLayoutDirection.current),
            end = paddingValues.calculateEndPadding(LocalLayoutDirection.current),
            bottom = paddingValues.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        // ... items ...
    }
    
    // ... контекстное меню ...
}
```

Аналогично обновить `emptyContent()`, `emptySearchContent()`, `loadingContent()`, `errorContent()`.

---

### Шаг 5: Упрощение `MainScreenTopBarState`

**Цель:** Удалить ненужные параметры после переделки поиска.

**Текущее состояние:**
```kotlin
private data class MainScreenTopBarState(
    val isSearchActive: Boolean,         // УДАЛИТЬ
    val searchQuery: String,             // УДАЛИТЬ (не используется в TopAppBar)
    val itemsCount: Int,
    val sortOrder: SortOrder,
    val viewModel: MainScreenViewModel,
    val onSearchActiveChange: (Boolean) -> Unit,  // УДАЛИТЬ
    val onSortOrderChange: (SortOrder) -> Unit,
    val onItemClick: (Long) -> Unit,
)
```

**Новое состояние:**
```kotlin
private data class MainScreenTopBarState(
    val itemsCount: Int,
    val sortOrder: SortOrder,
    val onSortOrderChange: (SortOrder) -> Unit,
)
```

**Упрощение `mainScreenTopBar()`:**
```kotlin
@Composable
private fun mainScreenTopBar(state: MainScreenTopBarState) {
    TopAppBar(
        title = { Text(stringResource(R.string.events)) },
        navigationIcon = {
            if (state.itemsCount > 1) {
                sortMenu(
                    sortOrder = state.sortOrder,
                    onSortOrderChange = state.onSortOrderChange,
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}
```

**Обновление использования в `mainScreenContent()`:**
```kotlin
Scaffold(
    topBar = {
        mainScreenTopBar(
            state = MainScreenTopBarState(
                itemsCount = itemsCount,
                sortOrder = sortOrder,
                onSortOrderChange = { viewModel.updateSortOrder(it) },
            ),
        )
    },
    floatingActionButton = { ... },
) { paddingValues ->
    // ... остальной код ...
}
```

---

### Шаг 6: Обновление ресурсов строк (опционально)

**Текущие ресурсы:**
- `search` — текст для placeholder ("Search")
- `close` — текст для иконки закрытия

Эти ресурсы уже определены в `strings.xml` и не требуют изменений.

---

## Тестирование после переделки

### Unit-тесты (ViewModel)

**Текущие тесты уже покрывают логику поиска:**
- ✅ `whenSearchQueryChanged_thenFiltersItems` — проверка фильтрации по названию
- ✅ `whenSearchQueryEmpty_thenShowsAllItems` — проверка очистки поиска
- ✅ `whenSearchInDetails_thenFindsItem` — поиск в деталях
- ✅ `whenSearchCaseInsensitive_thenFindsItem` — нечувствительность к регистру
- ✅ `whenSearchWithNoResults_thenShowsEmptyList` — пустой результат поиска

**Статус:** Изменения UI не влияют на логику ViewModel, тесты не требуют обновлений.

### Интеграционные тесты

**Рекомендуемые сценарии для проверки:**

1. **Отображение поля поиска при 5+ записях:**
   - Создать 5 и более записей
   - Проверить, что поле поиска отображается над списком
   - Удалить записи до 4
   - Проверить, что поле поиска скрывается
   - Добавить записи до 5
   - Проверить, что поле поиска снова отображается

2. **Поиск фильтрует список:**
   - Ввести текст в поле поиска
   - Проверить, что список отображает только отфильтрованные элементы
   - Очистить поле поиска
   - Проверить, что отображаются все элементы

3. **Поле поиска остается в верхней части:**
   - Прокрутить список вниз
   - Проверить, что поле поиска остается в верхней части экрана

4. **Padding и безопасные зоны:**
   - Открыть экран на устройстве с notch (если доступно)
   - Проверить, что TopAppBar корректно учитывает safe area
   - Проверить, что поле поиска находится под TopAppBar (если отображается)
   - Проверить, что список не перекрывается полем поиска

5. **Кнопка очистки поиска:**
   - Ввести текст в поле поиска
   - Проверить, что иконка закрытия отображается
   - Нажать на иконку закрытия
   - Проверить, что поле поиска очищено, а список отображает все элементы

6. **Пустой результат поиска:**
   - Ввести запрос, который не дает результатов
   - Проверить, что отображается `emptySearchContent()`
   - Очистить поле поиска
   - Проверить, что отображаются все элементы

7. **Сортировка при активном поиске:**
   - Ввести текст поиска
   - Изменить порядок сортировки
   - Проверить, что отфильтрованные элементы корректно сортируются

8. **Взаимодействие с элементами списка:**
   - Ввести текст поиска
   - Кликнуть на отфильтрованный элемент
   - Проверить, что открывается экран деталей
   - Вернуться на главный экран
   - Проверить, что поисковый запрос сохранен

9. **Сохранение поискового запроса при удалении записей:**
   - Ввести текст поиска
   - Удалить записи до 4 (поле поиска скрывается)
   - Добавить записи до 5 (поле поиска отображается)
   - Проверить, что поисковый запрос сохранен

### UI-тесты (Espresso / Compose Testing)

**Рекомендуемые тесты:**

1. **Тест отображения поля поиска:**
```kotlin
@Test
fun `whenScreenOpened_thenSearchFieldIsDisplayed`() {
    // Проверить, что TextField с placeholder "Search" отображается
}
```

2. **Тест ввода текста в поле поиска:**
```kotlin
@Test
fun `whenTextTypedInSearchField_thenListIsFiltered`() {
    // Ввести текст
    // Проверить количество элементов в списке
}
```

3. **Тест очистки поля поиска:**
```kotlin
@Test
fun `whenCloseButtonClicked_thenSearchFieldIsCleared`() {
    // Ввести текст
    // Кликнуть на иконку закрытия
    // Проверить, что поле пустое, а список содержит все элементы
}
```

---

## Преимущества новой реализации

1. **Устранено предупреждение о deprecated API:**
   - Вместо `SearchBar` используется стандартный `OutlinedTextField`
   - Не требуется миграция на новый overload с `inputField`

2. **Улучшенный UX:**
   - Поле поиска всегда доступно, не нужно нажимать на кнопку
   - Поиск работает в реальном времени при вводе текста
   - Пользователь видит, что список фильтруется

3. **Упрощение кода:**
   - Удалена логика переключения `isSearchActive`
   - Удален дублирующийся код отображения результатов поиска
   - Удален отдельный компонент `searchResultItem()`

4. **Корректная работа с paddingValues:**
   - Поле поиска учитывает padding от TopAppBar
   - Список учитывает padding от поля поиска и safe area
   - FloatingActionButton не перекрывается списком

5. **Соответствие Material Design:**
   - `OutlinedTextField` — стандартный компонент Material 3
   - Иконки (лупа, крестик) — стандартные Material Icons

---

## Порядок выполнения

1. **Шаг 1:** Удаление кнопки поиска из TopAppBar
2. **Шаг 2:** Создание компонента поля поиска
3. **Шаг 3:** Интеграция поля поиска в структуру экрана
4. **Шаг 4:** Обновление `mainScreenContentByState()` и компонентов контента
5. **Шаг 5:** Упрощение `MainScreenTopBarState` и `mainScreenTopBar()`
6. **Тестирование:** Проверка всех сценариев использования
7. **Опционально:** Написание UI-тестов для покрытия новых сценариев

---

## Критерии завершения

Переделка считается завершенной, когда:

- ✅ Поле поиска отображается над списком записей только при наличии 5 и более записей
- ✅ При удалении записей до 4 и менее поле поиска скрывается
- ✅ При добавлении записей до 5 и более поле поиска отображается
- ✅ При вводе текста список фильтруется в реальном времени
- ✅ Кнопка очистки поиска отображается при непустом поле
- ✅ При нажатии на кнопку очистки поле очищается, список показывает все элементы
- ✅ Поисковый запрос сохраняется при скрытии/показе поля поиска
- ✅ TopAppBar не содержит кнопку поиска
- ✅ Пользовательские элементы (padding, safe area) корректно учитываются
- ✅ Список не перекрывается полем поиска
- ✅ FloatingActionButton не перекрывается списком
- ✅ Все unit-тесты проходят без изменений
- ✅ Интеграционные тесты подтверждают корректную работу
- ✅ Нет предупреждений о deprecated API в сборке

---

## Обратная совместимость

**Логика поиска (ViewModel):**
- Не изменяется — фильтрация через `combine()` продолжает работать
- Все unit-тесты остаются актуальными

**API навигации:**
- Не изменяется — параметры `onItemClick`, `onEditClick`, `onCreateClick` остаются прежними

**Ресурсы строк:**
- Не изменяются — используются существующие ресурсы

**Хранение данных:**
- Не затрагивается — переделка только на уровне UI

---

## Примечания

1. **Альтернативные варианты:**
   - Можно использовать `TextField` вместо `OutlinedTextField` для более компактного вида
   - Можно добавить анимацию появления/исчезновения поля поиска (если в будущем понадобится)

2. **Будущие улучшения:**
   - Можно добавить историю поиска (через SharedPreferences)
   - Можно добавить автозаполнение (через Material 3 SuggestionChip)
   - Можно добавить голосовой поиск (через Speech Recognition API)

3. **Совместимость с iOS:**
   - Аналогичная реализация в iOS (постоянное поле поиска над списком)
   - Единый UX на обеих платформах

---

## Связанные документы

- [Экран 2.1: Main Screen](./Screen_2.1_Main_Screen.md) — текущая реализация
- [Этап 6: Форматирование дней](./Stage_6_Days_Formatting_Implementation_Plan.md)
- [Этап 7: Модель данных](./Stage_7_Data_Model_Implementation_Plan.md)

---

## История изменений

- 2026-01-02: Создание плана переделки поиска
