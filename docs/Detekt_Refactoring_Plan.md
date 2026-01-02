# План рефакторинга для устранения ошибок Detekt

## Обзор

Detekt обнаружил **24 ошибки** следующих типов:
- `TooManyFunctions`: 5 ошибок
- `LongParameterList`: 4 ошибки
- `LongMethod`: 7 ошибок
- `MaxLineLength`: 3 ошибки
- `MagicNumber`: 5 ошибок

**Цель рефакторинга:**
- Устранить все ошибки detekt
- Сохранить успешное прохождение всех тестов
- Не нарушить сборку проекта
- Улучшить читаемость и поддерживаемость кода
- Использовать ресурсы из `dimens.xml` для размеров и отступов

---

## Структура плана

Рефакторинг разбит на **4 этапа**, каждый из которых можно выполнить независимо и тестировать после завершения.

---

## Этап 1: Устранение магических чисел (5 ошибок)

### Ошибки

| Файл | Строка | Текущее значение | Тип | Рекомендация |
|------|--------|------------------|-----|--------------|
| `DetailScreenViewModel.kt` | 69 | `5000` | Время (мс) | Создать константу |
| `ListItemView.kt` | 99 | `12.dp` | Размер цветовой метки | `@dimen/color_tag_size_small` |
| `ListItemView.kt` | 103 | `12.dp` | Отступ (width) | `@dimen/spacing_extra_large` |
| `ListItemView.kt` | 122 | `4.dp` | Отступ (height) | `@dimen/spacing_small` |
| `ListItemView.kt` | 108 | `0.7f` | Вес левой колонки | Создать константу |
| `ListItemView.kt` | 135 | `0.3f` | Вес правой колонки | Создать константу |
| `CreateEditPreviewComponents.kt` | 159 | `minusDays(5)` | Дни (прошлое) | Создать константу |
| `CreateEditPreviewComponents.kt` | 179 | `plusDays(10)` | Дни (будущее) | Создать константу |
| `CreateEditPreviewComponents.kt` | 137,153,173 | `16.dp` | Отступ (padding) | `@dimen/spacing_extra_large` |
| `DaysFormatterImpl.kt` | 100 | `12` | Месяцев в году | Создать константу |

### План действий

**1.1. Использование ресурсов dimens.xml для размеров и отступов**

Для всех значений `X.dp` использовать существующие ресурсы из `dimens.xml`:
- `4.dp` → `@dimen/spacing_small`
- `8.dp` → `@dimen/spacing_medium`
- `12.dp` → `@dimen/spacing_extra_large`
- `16.dp` → `@dimen/spacing_extra_large`
- `12.dp` (размер цветовой метки) → `@dimen/color_tag_size_small`

**1.2. Создание именованных констант**

Для логических значений и временных констант создать companion object с константами.

**1.3. Изменения по файлам**

1. **`DetailScreenViewModel.kt`**
   - Строка 69: `SharingStarted.WhileSubscribed(5000)`
   - Создать companion object с константой:
     ```kotlin
     companion object {
         private const val STATE_SUBSCRIPTION_TIMEOUT_MS = 5000L
     }
     ```
   - Заменить: `SharingStarted.WhileSubscribed(STATE_SUBSCRIPTION_TIMEOUT_MS)`

2. **`ListItemView.kt`**
   - Строка 99: `size(12.dp)` → `size(dimensionResource(R.dimen.color_tag_size_small))`
   - Строка 103: `width(12.dp)` → `width(dimensionResource(R.dimen.spacing_extra_large))`
   - Строка 122: `height(4.dp)` → `height(dimensionResource(R.dimen.spacing_small))`
   - Строки 137, 153, 173: `padding(16.dp)` → `padding(dimensionResource(R.dimen.spacing_extra_large))`
   - Строки 108, 135: `weight(0.7f)` и `weight(0.3f)`
   - Создать companion object:
     ```kotlin
     companion object {
         private const val TITLE_WEIGHT = 0.7f
         private const val DAYS_WEIGHT = 0.3f
     }
     ```

3. **`CreateEditPreviewComponents.kt`**
   - Строки 137, 153, 173: `padding(16.dp)` → `padding(dimensionResource(R.dimen.spacing_extra_large))`
   - Строка 159: `minusDays(5)`
   - Строка 179: `plusDays(10)`
   - Создать companion object для превью:
     ```kotlin
     companion object {
         private const val PREVIEW_PAST_DAYS = 5
         private const val PREVIEW_FUTURE_DAYS = 10
     }
     ```
   - Заменить: `.minusDays(PREVIEW_PAST_DAYS)` и `.plusDays(PREVIEW_FUTURE_DAYS)`

4. **`DaysFormatterImpl.kt`**
   - Строка 100: `period.years * 12`
   - Добавить константу в существующий companion object:
     ```kotlin
     companion object {
         // ... существующие константы ...
         private const val MONTHS_IN_YEAR = 12
     }
     ```
   - Заменить: `period.years * MONTHS_IN_YEAR`

**1.4. Дополнительные улучшения для dimens.xml**

Проверить, достаточно ли существующих ресурсов. Если нет, добавить недостающие:
- Все основные размеры уже есть в файле
- Для весов (weights) создавать только константы в коде (ресурсы не применяются)

**1.5. Проверка**
- Запустить `./gradlew detekt` - должно исчезнуть 5 ошибок MagicNumber
- Запустить все тесты `./gradlew test` - должны проходить успешно
- Проверить визуально экраны (если есть UI)

---

## Этап 2: Исправление длинных строк (3 ошибки)

### Ошибки

| Файл | Строка | Описание | Текущее содержание |
|------|--------|----------|-------------------|
| `MainScreenViewModel.kt` | 104 | Длинная строка с println | ~120 символов |
| `CreateEditFormContent.kt` | 120 | Длинная строка с параметрами | ~150 символов |
| `DaysCalculatorViewModelTest.kt` | 216 | Длинная строка с аннотацией | ~120 символов |

### План действий

**2.1. Изменения по файлам**

1. **`MainScreenViewModel.kt` - строка 104**
   - Текущая длинная строка с println
   - Разбить на несколько строк для лучшей читаемости:
   ```kotlin
   println(
       "Фильтрация: элемент='${item.title}', " +
       "детали='${item.details}', " +
       "запрос='$query', " +
       "titleContains=$titleContains, " +
       "detailsContains=$detailsContains"
   )
   ```

2. **`CreateEditFormContent.kt` - строка 120**
   - Текущая длинная строка с вызовом `dateSection(...)`
   - Разбить параметры на несколько строк:
   ```kotlin
   dateSection(
       selectedDate = params.uiStates.selectedDate,
       showDatePicker = params.showDatePicker,
       onValueChange = onValueChange
   )
   ```

3. **`DaysCalculatorViewModelTest.kt` - строка 216**
   - Текущая длинная строка с аннотацией @Disabled и комментарием
   - Разбить на несколько строк:
   ```kotlin
   @Disabled(
       "TODO: Fix test - calculateDays with custom displayOption - " +
       "viewModelScope coroutines don't execute with test dispatcher"
   )
   ```

**2.2. Проверка**
- Запустить `./gradlew detekt` - должно исчезнуть 3 ошибки MaxLineLength
- Запустить все тесты `./gradlew test` - должны проходить успешно

---

## Этап 3: Разбивка длинных методов (7 ошибок)

### Ошибки

| Файл | Строка | Текущая длина | Превышение | Причина |
|------|--------|---------------|------------|---------|
| `ListItemView.kt` | 55 | 75 строк | +15 | Много UI-элементов |
| `RootScreenComponents.kt` | 90 | 69 строк | +9 | Много навигационных назначений |
| `CreateEditPreviewComponents.kt` | 36 | 73 строки | +13 | Много логики форматирования |
| `CreateEditFormContent.kt` | 80 | 65 строк | +5 | Много секций формы |
| `MainScreen.kt` | 366 | 92 строки | +32 | Много логики списка |
| `MainScreen.kt` | 129 | 76 строк | +16 | Много секций экрана |
| `MainScreen.kt` | 598 | 76 строк | +16 | Много элементов TopBar |

### План действий

**3.1. Принципы рефакторинга**

Для каждого длинного метода:
- Выделить логические блоки в отдельные приватные @Composable функции
- Сохранить читаемость и понятность
- Не изменять бизнес-логику
- Использовать ресурсов dimens.xml для размеров/отступов

**3.2. Изменения по файлам**

1. **`ListItemView.kt` - метод `listItemView` (75 строк)**
   - Выделить UI-элементы в отдельные компоненты:
     - `ItemColorTag()` - отображение цветовой метки
     - `ItemTitleAndDetails()` - левая колонка с названием и описанием
     - `ItemDaysBadge()` - правая колонка с количеством дней
   - Основной метод станет короче и читабельнее:
     ```kotlin
     @Composable
     fun listItemView(
         item: Item,
         formattedDaysText: String,
         modifier: Modifier = Modifier,
         onClick: (Item) -> Unit = {},
         onLongClick: ((Offset) -> Unit)? = null,
         isSelected: Boolean = false,
     ) {
         // ... анимация фона ...
         Row(
             modifier = modifier.padding(dimensionResource(R.dimen.spacing_extra_large)),
             // ...
         ) {
             ItemColorTag(item.colorTag)
             ItemTitleAndDetails(item)
             ItemDaysBadge(formattedDaysText)
         }
     }
     ```

2. **`RootScreenComponents.kt` - метод `navHostContent` (69 строк)**
   - Выделить навигационные назначения в отдельные функции:
     - `mainScreenDestination()` - назначение для главного экрана
     - `detailScreenDestination()` - назначение для экрана деталей
     - `createEditScreenDestination()` - назначение для создания/редактирования
     - `moreScreenDestination()` - назначение для экрана настроек
   - Основной метод станет:
     ```kotlin
     @Composable
     fun navHostContent(
         navController: NavController,
         // ... параметры ...
     ) {
         NavHost(navController = navController, startDestination = ...) {
             mainScreenDestination(...)
             detailScreenDestination(...)
             createEditScreenDestination(...)
             moreScreenDestination(...)
         }
     }
     ```

3. **`CreateEditPreviewComponents.kt` - метод `previewDaysContentInner` (73 строки)**
   - Выделить логические блоки:
     - `createPreviewUseCases()` - создание use cases для форматирования
     - `calculatePreviewDays()` - вычисление разницы дат
     - `createPreviewItem()` - создание временного Item
     - `formatPreviewDaysText()` - форматирование текста дней
   - Выделить UI-компоненты:
     - `PreviewHeader()` - заголовок превью
     - `PreviewDays()` - отображение дней
     - `PreviewFooter()` - описание прошедшее/оставшееся
   - Основной метод станет короче и понятнее

4. **`CreateEditFormContent.kt` - метод `createEditFormContent` (65 строк)**
   - Метод уже использует секции (`titleSection`, `detailsSection`, `dateSection` и т.д.)
   - Выделить логику группировки секций в отдельные компоненты:
     - `MainFormSections()` - основные секции формы (название, детали, дата)
     - `PreviewSection()` - секция предпросмотра
     - `ColorTagSection()` - секция выбора цвета
   - Уменьшить длину основного метода за счет группировки

5. **`MainScreen.kt` - три длинных метода**

   **`itemsListContent` (92 строки):**
   - Выделить `ListItemWrapper()` - обертка для элемента списка с весами
   - Выделить `EmptyStateView()` - компонент для пустого состояния
   - Выделить `SearchEmptyView()` - компонент для пустого поиска
   - Основной метод станет короче:
     ```kotlin
     @Composable
     fun itemsListContent(
         // ... параметры ...
     ) {
         if (items.isEmpty()) {
             EmptyStateView()
         } else {
             LazyColumn(...) {
                 items(items, key = { it.id }) { item ->
                     ListItemWrapper(item, ...)
                 }
             }
         }
     }
     ```

   **`mainScreenContent` (76 строк):**
   - Выделить `ScreenHeader()` - заголовок экрана
   - Выделить `ScreenBody()` - тело экрана со списком
   - Упростить условную логику

   **`mainScreenTopBar` (76 строк):**
   - Выделить `TopBarTitle()` - название приложения
   - Выделить `TopBarActions()` - действия в топбаре
   - Выделить логику отображения кнопок в отдельные функции

**3.3. Проверка**
- Запустить `./gradlew detekt` - должно исчезнуть 7 ошибок LongMethod
- Запустить все тесты `./gradlew test` - должны проходить успешно
- Проверить визуально экраны

---

## Этап 4: Устранение TooManyFunctions и LongParameterList (9 ошибок)

### 4.1. TooManyFunctions (5 ошибок)

| Файл | Текущее кол-во | Превышение | Решение |
|------|----------------|------------|---------|
| `MainScreenViewModel.kt` | 12 функций | +1 | Выделить вспомогательные функции в extension |
| `DetailContent.kt` | 13 функций | +2 | Выделить компоненты в отдельный файл |
| `MainScreen.kt` | 11 функций | +0 (на грани) | После этапа 3 уменьшится |
| `ItemDao.kt` | 11 функций | +0 (на грани) | Обосновать исключение для DAO |

#### План действий

**MainScreenViewModel (12 функций)**
- Проанализировать функции по связности
- Выделить вспомогательные функции поиска в extension-файл:
  - Создать `MainScreenViewModelSearchExtensions.kt`
  - Перенести функции фильтрации и поиска
- Основной класс сократится до ~8-9 функций

**DetailContent (13 функций)**
- Выделить UI-компоненты в отдельный файл
- Создать `DetailContentComponents.kt`
- Перенести вспомогательные @Composable функции:
  - `DetailTitle()`
  - `DetailDays()`
  - `DetailMetadata()`
  - `DetailActions()`
- В `DetailContent.kt` оставить только основные функции (~8)

**MainScreen (11 функций)**
- После разбивки длинных методов из этапа 3 количество функций уменьшится
- Создать `MainScreenComponents.kt` для вспомогательных компонентов
- Не требуется дополнительных действий

**ItemDao (11 функций)**
- DAO-интерфейс с множеством методов - это нормальная практика для Room
- Рассмотреть настройку исключения в `config/detekt/detekt.yml` для DAO-интерфейсов:
  ```yaml
  TooManyFunctions:
     active: true
     thresholdInClasses: 11
     thresholdInInterfaces: 11
     thresholdInObjects: 11
     thresholdInFiles: 11
     exclude: ['**/dao/**Dao.kt']
  ```
- Или обосновать, что DAO требует много методов по архитектуре

### 4.2. LongParameterList (4 ошибки)

| Файл | Функция | Кол-во параметров | Решение |
|------|---------|-------------------|---------|
| `ListItemView.kt` | `listItemView` | 6 параметров | Создать data-класс |
| `DetailScreen.kt` | `detailScreenContent` | 9 параметров | Создать data-класс |
| `MainScreen.kt` | `mainScreenContent` | 6 параметров | Создать data-класс |
| `MainScreen.kt` | `itemsListContent` | 7 параметров | Создать data-класс |

#### План действий

**Принцип рефакторинга:**
Сгруппировать связанные параметры в data-классы с именованными свойствами

**1. ListItemView.kt - функция `listItemView`**
- Создать data-класс `ListItemParams`:
  ```kotlin
  data class ListItemParams(
      val item: Item,
      val formattedDaysText: String,
      val onClick: (Item) -> Unit = {},
      val onLongClick: ((Offset) -> Unit)? = null,
      val isSelected: Boolean = false
  )
  ```
- Функция примет только `params: ListItemParams` и `modifier: Modifier`

**2. DetailScreen.kt - функция `detailScreenContent`**
- Создать data-класс `DetailScreenParams`:
  ```kotlin
  data class DetailScreenParams(
      val itemId: Long,
      val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
      val onBackClick: () -> Unit,
      val onEditClick: (Long) -> Unit,
      val onDeleteClick: () -> Unit,
      val showDeleteDialog: Boolean,
      val onConfirmDelete: () -> Unit,
      val onCancelDelete: () -> Unit
  )
  ```
- Функция примет `params: DetailScreenParams`, `modifier: Modifier`, `uiState: DetailScreenState`

**3. MainScreen.kt - функция `mainScreenContent`**
- Создать data-класс `MainScreenParams`:
  ```kotlin
  data class MainScreenParams(
      val viewModel: MainScreenViewModel,
      val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
      val onItemClick: (Long) -> Unit,
      val onEditClick: (Long) -> Unit,
      val onCreateClick: () -> Unit
  )
  ```
- Функция примет `params: MainScreenParams` и `modifier: Modifier`

**4. MainScreen.kt - функция `itemsListContent`**
- Создать data-класс `ItemsListParams`:
  ```kotlin
  data class ItemsListParams(
      val items: List<Item>,
      val listState: LazyListState,
      val getFormattedDaysForItemUseCase: GetFormattedDaysForItemUseCase,
      val onItemClick: (Long) -> Unit,
      val onEditClick: (Long) -> Unit,
      val viewModel: MainScreenViewModel,
      val paddingValues: PaddingValues
  )
  ```
- Функция примет только `params: ItemsListParams`

**4.3. Проверка**
- Запустить `./gradlew detekt` - должно исчезнуть 9 ошибок (5 TooManyFunctions + 4 LongParameterList)
- Запустить все тесты `./gradlew test` - должны проходить успешно
- Проверить сборку `./gradlew assembleDebug` - должна проходить без ошибок

---

## Порядок выполнения

### Рекомендуемая последовательность

1. **Этап 1**: Устранение магических чисел (самый безопасный этап)
   - Время: ~45 минут
   - Риск: низкий
   - Только замена на константы и ресурсы

2. **Этап 2**: Исправление длинных строк
   - Время: ~15 минут
   - Риск: низкий
   - Только форматирование

3. **Этап 3**: Разбивка длинных методов
   - Время: ~2-3 часа
   - Риск: средний (требует тщательного тестирования)
   - Рефакторинг структуры методов

4. **Этап 4**: Устранение TooManyFunctions и LongParameterList
   - Время: ~2 часа
   - Риск: средний (изменяет структуру кода)
   - Создание data-классов и разделение файлов

**Общее время: ~5-6 часов**

---

## Контрольные точки

После каждого этапа:
1. Запускать `./gradlew detekt` - проверять количество ошибок
2. Запускать `./gradlew test` - убедиться, что все тесты проходят
3. Запускать `./gradlew assembleDebug` - убедиться, что сборка работает
4. Проверять визуально UI-компоненты (если применимо)
5. Зафиксировать изменения в git с осмысленным комментарием

---

## Дополнительные рекомендации

### Не вносить изменения в конфиг detekt (кроме DAO)

Вместо того чтобы ослабить правила detekt, следует привести код в соответствие с ними.

**Исключение:** Можно добавить исключение для DAO-интерфейсов в `config/detekt/detekt.yml`:
```yaml
TooManyFunctions:
  active: true
  thresholdInInterfaces: 11
  exclude: ['**/dao/**Dao.kt']
```

Это обосновано тем, что DAO по своей природе требует множества методов для работы с БД.

### Использовать ресурсы dimens.xml

Для всех размеров и отступов использовать существующие ресурсы:
- `spacing_small`, `spacing_medium`, `spacing_extra_large` - для отступов
- `color_tag_size_small` - для размеров цветовых меток
- Не создавать магические числа типа `16.dp`, `8.dp` и т.д.

### Обновление документации

После завершения рефакторинга:
- Обновить документацию к затронутым файлам
- Обновить README, если изменилась архитектура
- Обновить комментарии в коде
- Актуализировать этот план (отметить выполненные этапы)

### Code Review

Перед слиянием изменений:
- Провести self-code review
- Проверить, что все тесты проходят
- Проверить, что detekt не выдает ошибок
- Проверить, что сборка проходит без ошибок

---

## Ожидаемый результат

После выполнения всех 4 этапов:
- `./gradlew detekt` будет выполняться **без ошибок**
- Все существующие тесты будут **проходить успешно**
- Сборка проекта будет работать **без ошибок**
- Код станет **более читаемым и поддерживаемым**
- Архитектура станет **более прозрачной**
- Размеры и отступы будут использовать **ресурсы dimens.xml**
- Магические числа будут заменены на **именованные константы**

---

## Приложение: Карта магических чисел

| Тип значения | Детектировано | Рекомендация |
|--------------|---------------|--------------|
| Отступы (spacing) | `4.dp`, `8.dp`, `12.dp`, `16.dp` | Использовать ресурсы dimens.xml |
| Размеры (size) | `12.dp` | Использовать ресурсы dimens.xml |
| Веса (weights) | `0.7f`, `0.3f` | Создать константы в companion object |
| Время (мс) | `5000` | Создать константу `TIMEOUT_MS` |
| Дни | `5`, `10` | Создать константы `PAST_DAYS`, `FUTURE_DAYS` |
| Коэффициенты | `12` (месяцев в году) | Создать константу `MONTHS_IN_YEAR` |
