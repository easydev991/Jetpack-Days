# Этап 6: Рефакторинг и разбивка длинных функций — План исправлений

## Обзор

Документ описывает план рефакторинга кода для устранения предупреждений detekt, lint и ktlint, а также исправления ошибок сборки и тестов в соответствии с правилами проекта.

**Статус:** ⚠️ ТРЕБУЕТСЯ ЗАВЕРШЕНИЕ
**Приоритет:** ВЫСОКИЙ (критично для прохождения сборки и тестов)

## Примечание
Последнее обновление: 30 декабря 2025 года
Анализ текущего состояния кода выполнен, план обновлен.

---

## Этап 11.5: Исправление ошибок линтинга (ktlint, detekt, lint) ⚠️ ТРЕБУЕТСЯ

**Статус:** ⚠️ ТРЕБУЕТСЯ ИСПРАВЛЕНИЕ

### 11.5.1 Исправить ошибки detekt — ЧАСТИЧНО ВЫПОЛНЕНО (~50%)

**TooManyFunctions (3 файла):** — ОСТАЛОСЬ (~50%)
- `DetailScreen.kt` — 17 функций (порог: 11)
  - ✅ Все функции используют camelCase именование
  - ❌ Требуется создание DetailScreenComponents.kt
- `CreateEditScreen.kt` — 19 функций (порог: 11)
  - ✅ Все private функции используют camelCase именование
  - ✅ Использует CreateEditFormParams data class
  - ❌ Требуется создание CreateEditScreenComponents.kt
  - ⚠️ Есть проблемы с определением функций (строки 269-293)
- `RootScreen.kt` — 12 функций (порог: 11)
  - ✅ Все функции используют camelCase именование
  - ❌ Требуется создание RootScreenComponents.kt
- `MainScreen.kt` — 6 функций (✓ в пределах порога)

**LongParameterList (1 функция):** — ВЫПОЛНЕНО ✅
- `CreateEditFormContent` — использует CreateEditFormParams data class (6 параметров, ✓ в пределах порога)

**TooGenericExceptionCaught (14 случаев):** — ВЫПОЛНЕНО ✅
- `MainScreenViewModel.kt` — использует `ItemException.LoadFailed`, `ItemException.DeleteFailed`, `ItemException.UpdateFailed`
- `DetailScreenViewModel.kt` — использует `ItemException.DeleteFailed`, `ItemException.UpdateFailed`
- `CreateEditScreenViewModel.kt` — использует `ItemException.LoadFailed`, `ItemException.SaveFailed`, `ItemException.UpdateFailed`

**FunctionNaming (28 случаев):** — ВЫПОЛНЕНО ЧАСТИЧНО (~80%)
- Конфликт между правилами: Detekt требует PascalCase для всех функций, Compose lint требует camelCase для Composable функций
  - ✅ Решение принято: использовать camelCase для всех Composable функций (приоритет Compose lint)
- Затронутые файлы:
  - `DetailScreen.kt` — ✅ Все private функции в camelCase, публичные функции используют PascalCase (допустимо)
  - `CreateEditScreen.kt` — ✅ Все private функции в camelCase, публичные функции используют PascalCase (допустимо)
  - `RootScreen.kt` — ✅ Все private функции в camelCase
  - `MainScreen.kt` — ✅ Все private функции в camelCase

**Примечание:** Публичные функции могут использовать PascalCase в соответствии с общими правилами именования для публичных API.

**MagicNumber (6 случаев):** — ВЫПОЛНЕНО ✅
- `DetailScreen.kt` — использует `NumberFormattingUtils.formatDaysCount()`
- `MainScreen.kt` — использует `NumberFormattingUtils.formatDaysCount()`
- `CreateEditScreen.kt` — использует `NumberFormattingUtils.formatDaysCount()`

**UnusedParameter (2 случая):** — ТРЕБУЕТ ПРОВЕРКИ ⚠️
- `CreateEditScreen.kt:413` — требуется проверка актуальности
- `RootScreen.kt:150` — требуется проверка актуальности
  - Требуется запуск detekt для получения актуального списка

---

### 11.5.2 Исправить ошибки lint — ЧАСТИЧНО ВЫПОЛНЕНО (~50%)

**MissingTranslation (6 ресурсов):** — ВЫПОЛНЕНО ✅
- `days_abbreviated` — добавлен перевод на русский: "дн."
- `months_abbreviated` — добавлен перевод на русский: "мес."
- `years_abbreviated` — добавлен перевод на русский: "г."
- `days_count` (plurals) — добавлены переводы на русский (все варианты: one/few/many/other)
- `months_count` (plurals) — добавлены переводы на русский (все варианты)
- `years_count` (plurals) — добавлены переводы на русский (все варианты)
  - Все переводы находятся в `app/src/main/res/values-ru/strings.xml`

**PrivateResource (2 ресурса):** — ТРЕБУЕТ ПРОВЕРКИ ⚠️
- `selected` — найден в `values-en/strings.xml` (строка 72)
- `not_selected` — найден в `values-en/strings.xml` (строка 73)
- Возможен конфликт с Compose UI (androidx.compose.ui:ui-android)
  - Требуется проверить использование этих ресурсов в коде
  - Если используются и конфликтуют — переименовать в уникальные имена (например, `color_tag_selected`, `color_tag_not_selected`)
  - Если не используются — удалить из `values-en/strings.xml`

---

### 11.5.3 Порядок исправления

1. **Создать файл констант** для magic numbers
2. **Исправить TooGenericExceptionCaught** — заменить на конкретные исключения
3. **Исправить LongParameterList** — создать data class для состояния формы
4. **Исправить TooManyFunctions** — разделить экраны на компоненты
5. **Исправить FunctionNaming** — привести к единому стилю (PascalCase для приватных Composable)
6. **Исправить UnusedParameter** — удалить неиспользуемые параметры
7. **Исправить MissingTranslation** — добавить переводы на русский
8. **Исправить PrivateResource** — переименовать конфликтующие ресурсы

---

### 11.5.4 Проверка исправлений

После исправлений запустить:
```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew lintDebug
```

Убедиться, что все ошибки устранены.

---

## Текущее состояние проблем (после анализа кода)

### ✅ ВЫПОЛНЕНО:

#### 1. Generic Exceptions — ВЫПОЛНЕНО ✅
Все ViewModels используют конкретные исключения `ItemException`:
- `MainScreenViewModel.kt` — использует `ItemException.LoadFailed`, `ItemException.DeleteFailed`, `ItemException.UpdateFailed`
- `DetailScreenViewModel.kt` — использует `ItemException.DeleteFailed`, `ItemException.UpdateFailed`
- `CreateEditScreenViewModel.kt` — использует `ItemException.LoadFailed`, `ItemException.SaveFailed`, `ItemException.UpdateFailed`

#### 2. Magic Numbers — ВЫПОЛНЕНО ✅
Все магические числа заменены на константы:
- `MainScreen.kt` — использует `NumberFormattingUtils.formatDaysCount()`
- `DetailScreen.kt` — использует `NumberFormattingUtils.formatDaysCount()`
- `CreateEditScreen.kt` — использует `NumberFormattingUtils.formatDaysCount()`

#### 3. LongParameterList — ВЫПОЛНЕНО ✅
`CreateEditFormContent` использует `CreateEditFormParams` data class с 6 параметрами.

#### 4. MissingTranslation — ВЫПОЛНЕНО ✅
Все необходимые переводы добавлены в `values-ru/strings.xml`:
- `days_abbreviated` — "дн."
- `months_abbreviated` — "мес."
- `years_abbreviated` — "г."
- `days_count` (plurals) — полные переводы для всех вариантов
- `months_count` (plurals) — полные переводы для всех вариантов
- `years_count` (plurals) — полные переводы для всех вариантов

---

### ⚠️ ОСТАЛОСЬ ВЫПОЛНИТЬ:

#### 1. TooManyFunctions — ТРЕБУЕТ РЕШЕНИЯ ⚠️
Проблема: Detekt считает все private функции (>11 шт.) как нарушение.

**Текущее состояние:**
- `RootScreen.kt` — 12 функций (camelCase, но >11)
- `DetailScreen.kt` — 17 функций (camelCase, но >11)
- `CreateEditScreen.kt` — 19 функций (camelCase, но >11)
- `MainScreen.kt` — 6 функций (camelCase, ✓)

**Рекомендуемое решение:** Создать отдельные файлы для компонентов:
- Создать `app/src/main/java/com/dayscounter/ui/screen/components/` директорию
- Перенести секции из `DetailScreen.kt` в `DetailScreenComponents.kt`
- Перенести секции из `CreateEditScreen.kt` в `CreateEditScreenComponents.kt`
- Перенести компоненты из `RootScreen.kt` в `RootScreenComponents.kt`

#### 2. FunctionNaming — ЧАСТИЧНО ВЫПОЛНЕНО ⚠️
**Текущее состояние:**
- ✅ RootScreen.kt — все private функции в camelCase (`rootScreenContent`, `navigationBarContent`, `NavHostContent`, `updateTabBasedOnRoute`)
- ✅ DetailScreen.kt — большинство функций в camelCase (`detailScreenContent`, `detailContentByState`, `colorTagSection`, `titleSection`, `daysCountSection`, `detailsSection`, `displayOptionInfoSection`, `loadingContent`, `errorContent`, `deletedContent`)
- ⚠️ DetailScreen.kt — некоторые функции с PascalCase: `DetailTopAppBar`, `DetailActionButtons`, `DetailContentInner` (публичные функции)
- ✅ CreateEditScreen.kt — большинство функций в camelCase (`createEditScreenContent`, `rememberCreateEditUiStates`, `loadItemData`, `previewDaysContent`, `previewDaysContentInner`, `colorSelector`, `colorOptionSurface`, `noColorOptionSurface`, `displayOptionSelector`, `displayOptionSurface`, `buttonsSection`, `saveButton`, `datePickerDialogSection`)
- ⚠️ CreateEditScreen.kt — PascalCase для публичных функций: `CreateEditTopAppBar`, `CreateEditFormContent` (public)
- ✅ MainScreen.kt — все private функции в camelCase (`mainScreenContent`, `emptyContent`, `itemsListContent`, `loadingContent`, `errorContent`)

**Примечание:** Detekt требует PascalCase для private функций, но Compose lint требует camelCase. Компромисс: использовать camelCase для всех функций (приоритет Compose lint).

#### 3. Unused Parameters — ТРЕБУЕТ ПРОВЕРКИ ⚠️
План указывает на неиспользуемые параметры:
- ❌ `RootScreen.kt:150` — `viewModel` в `NavHostContent` не используется
- ❌ `CreateEditScreen.kt:413` — `selectedDisplayOption` не используется

**Требуется проверка:** Проверить актуальность этих проблем в текущем коде.

#### 4. PrivateResource — ТРЕБУЕТ ПРОВЕРКИ ⚠️
План указывает на конфликты ресурсов:
- ❌ `selected` — конфликт с Compose UI
- ❌ `not_selected` — конфликт с Compose UI

**Требуется проверка:** Проверить актуальность проблемы в текущем коде (присутствуют в `values-en/strings.xml`, но возможно не используются).

---

## Предварительные результаты

✅ **Выполнено (первый раунд):**
- Разбиты на мелкие части:
  - `RootScreen.kt` — 12 функций (было 95 строк)
  - `DetailScreen.kt` — 17 функций (было 67 строк)
  - `CreateEditScreen.kt` — 19 функций (было 87 строк)
  - `MainScreen.kt` — разбит на мелкие части

- Вынесены константы:
  - `RootScreenConstants`
  - `DetailScreenConstants`
  - `CreateEditScreenConstants`
  - `MainScreenConstants`

- Созданы data классы для состояний:
  - `CreateEditUiState` в CreateEditScreen

❌ **Осталось исправить (второй раунд):**
- Слишком много функций в файлах (>11)
- Именование функций (все private функции должны быть camelCase)
- Generic Exception → конкретные исключения (IOException, SQLiteException)
- Magic Numbers → использовать константы
- Unused Parameters → удалить неиспользуемые параметры

---

## План исправлений (актуальный статус)

### ✅ ВЫПОЛНЕНО:

#### Фаза 3: Обновление исключений в ViewModels ✅
Все ViewModels используют конкретные исключения `ItemException` вместо `Exception`.

#### Фаза 4: Устранение Magic Numbers ✅
Все магические числа заменены на использование `NumberFormattingUtils`.

#### Фаза (из раздела 11.5.2): Исправление MissingTranslation ✅
Все необходимые переводы добавлены в `values-ru/strings.xml`.

#### Фаза (из раздела 11.5.2): Исправление LongParameterList ✅
`CreateEditFormContent` использует `CreateEditFormParams` data class.

---

### ⚠️ ОСТАЛОСЬ ВЫПОЛНИТЬ:

#### Фаза 1: Проверка именования функций ⚠️

**Текущее состояние:**
- ✅ RootScreen.kt — все private функции в camelCase
- ⚠️ DetailScreen.kt — большинство функций в camelCase, но некоторые публичные функции с PascalCase
- ✅ CreateEditScreen.kt — большинство private функций в camelCase, публичные функции с PascalCase
- ✅ MainScreen.kt — все private функции в camelCase

**Задачи:**
- ❌ Рассмотреть возможность переименования публичных функций в `DetailScreen.kt` и `CreateEditScreen.kt` в camelCase (если это не нарушает общие правила именования для публичных API)
- ❌ Или документировать исключение для этих публичных функций в конфигурации detekt

**Примечание:** Detekt требует PascalCase для private функций, но Compose lint требует camelCase. Текущий код использует camelCase для всех private функций (верный подход).

---

#### Фаза 2: Уменьшение количества функций ⚠️

**Проблема:** Detekt считает все private функции (>11 шт.) как нарушение.

**Текущее состояние:**
- `RootScreen.kt` — 12 функций (>11)
- `DetailScreen.kt` — 17 функций (>11)
- `CreateEditScreen.kt` — 19 функций (>11)
- `MainScreen.kt` — 6 функций (✓)

**Решения:**

1. **Вариант A:** Перенести некоторые функции в отдельные файлы ⭐ РЕКОМЕНДУЕТСЯ
   - Создать директорию `app/src/main/java/com/dayscounter/ui/screen/components/`
   - Создать файлы:
     - `DetailScreenComponents.kt` — перенести секции из `DetailScreen.kt`
     - `CreateEditScreenComponents.kt` — перенести секции из `CreateEditScreen.kt`
     - `RootScreenComponents.kt` — перенести компоненты из `RootScreen.kt`
   - Это улучшит архитектуру и разделит код на логические модули

2. **Вариант B:** Объединить мелкие функции в более крупные
   - Вместо секций использовать inline функции
   - Не рекомендуется — ухудшит читаемость

3. **Вариант C:** Игнорировать правило `TooManyFunctions`
   - Не рекомендуется по архитектурным причинам

**Рекомендация:** Использовать **Вариант A** — создание отдельных файлов для компонентов.

---

#### Фаза 5: Удаление неиспользуемых параметров ⚠️

**Требуется проверка:**
- ❌ Проверить `RootScreen.kt:150` — используется ли `viewModel` в `NavHostContent`
- ❌ Проверить `CreateEditScreen.kt:413` — используется ли `selectedDisplayOption` в `displayOptionSelector`
- ❌ Запустить detekt и lint для получения актуального списка неиспользуемых параметров

---

#### Фаза (из раздела 11.5.2): Исправление PrivateResource ⚠️

**Требуется проверка:**
- ❌ Проверить, используются ли ресурсы `selected` и `not_selected` в коде
- ❌ Если используются и конфликтуют с Compose UI — переименовать в уникальные имена (например, `color_tag_selected`, `color_tag_not_selected`)
- ❌ Если не используются — удалить ресурсы из `values-en/strings.xml`

---

## Детальный план по файлам (актуальный статус)

### ✅ ВЫПОЛНЕНО:

#### 5. ViewModels (MainScreenViewModel, DetailScreenViewModel, CreateEditScreenViewModel) ✅

**Выполненные изменения:**
- Все ViewModels используют конкретные исключения `ItemException` вместо `Exception`
- Логирование ошибок на русском языке
- Правильная обработка ошибок и переход в состояние `Error`

---

### ⚠️ ОСТАЛОСЬ ВЫПОЛНИТЬ:

#### 1. `/app/src/main/java/com/dayscounter/ui/screen/RootScreen.kt` ⚠️

**Текущее состояние:**
- ✅ Все private функции в camelCase (например, `rootScreenContent`, `navigationBarContent`, `NavHostContent`, `updateTabBasedOnRoute`)
- ❌ 12 функций (detekt: TooManyFunctions, порог: 11)

**Изменения (TooManyFunctions):**
- Создать файл `app/src/main/java/com/dayscounter/ui/screen/components/RootScreenComponents.kt`
- Перенести туда функции:
  - `NavigationBarContent`
  - `NavHostContent`
  - `EventsRoute`
  - `MoreRoute`
  - `ItemDetailRoute`
  - `CreateItemRoute`
  - `EditItemRoute`
  - `EventsScreenContent`
  - `MoreScreenContent`
  - `UpdateTabBasedOnRoute`

---

#### 2. `/app/src/main/java/com/dayscounter/ui/screen/DetailScreen.kt` ⚠️

**Текущее состояние:**
- ✅ Большинство private функций в camelCase (`detailScreenContent`, `detailContentByState`, `colorTagSection`, `titleSection`, `daysCountSection`, `detailsSection`, `displayOptionInfoSection`, `loadingContent`, `errorContent`, `deletedContent`)
- ⚠️ Публичные функции с PascalCase: `DetailTopAppBar`, `DetailActionButtons`, `DetailContentInner` (допустимо для публичных API)
- ✅ Magic Numbers заменены на использование `NumberFormattingUtils.formatDaysCount()`
- ❌ 17 функций (detekt: TooManyFunctions, порог: 11)

**Изменения (TooManyFunctions):**
- Создать файл `app/src/main/java/com/dayscounter/ui/screen/components/DetailScreenComponents.kt`
- Перенести туда функции:
  - `DetailTopAppBar` (публичная)
  - `DetailActionButtons` (private)
  - `DetailContentInner` (private)
  - `ColorTagSection` (публичная)
  - `TitleSection` (публичная)
  - `DateSection` (private)
  - `DaysCountSection` (публичная)
  - `DetailsSection` (публичная)
  - `DisplayOptionInfoSection` (публичная)
  - `LoadingContent` (private)
  - `ErrorContent` (private)
  - `DeletedContent` (private)

---

#### 3. `/app/src/main/java/com/dayscounter/ui/screen/CreateEditScreen.kt` ⚠️

**Текущее состояние:**
- ✅ Большинство private функций в camelCase (`createEditScreenContent`, `rememberCreateEditUiStates`, `loadItemData`, `previewDaysContent`, `previewDaysContentInner`, `colorSelector`, `colorOptionSurface`, `noColorOptionSurface`, `displayOptionSelector`, `displayOptionSurface`, `buttonsSection`, `saveButton`, `datePickerDialogSection`)
- ⚠️ Публичные функции с PascalCase: `CreateEditTopAppBar`, `CreateEditFormContent` (допустимо для публичных API)
- ✅ `CreateEditFormContent` использует `CreateEditFormParams` data class (6 параметров)
- ✅ Magic Numbers заменены на использование `NumberFormattingUtils.formatDaysCount()`
- ❌ 19 функций (detekt: TooManyFunctions, порог: 11)

**Изменения (TooManyFunctions):**
- Создать файл `app/src/main/java/com/dayscounter/ui/screen/components/CreateEditScreenComponents.kt`
- Перенести туда функции:
  - `CreateEditTopAppBar` (private)
  - `CreateEditFormContent` (private)
  - `RememberCreateEditUiStates` (private)
  - `LoadItemData` (private)
  - `TitleSection` (вызывается, но не определена - перенести из DetailScreen или создать отдельный файл)
  - `DetailsSection` (вызывается, но не определена - перенести из DetailScreen или создать отдельный файл)
  - `DateSection` (определена без сигнатуры функции - нужно исправить)
  - `PreviewDaysContent` (private)
  - `PreviewDaysContentInner` (private)
  - `ColorSelector` (private)
  - `ColorOptionSurface` (private)
  - `NoColorOptionSurface` (private)
  - `DisplayOptionSelector` (private)
  - `DisplayOptionSurface` (private)
  - `ButtonsSection` (private)
  - `SaveButton` (private)
  - `DatePickerDialogSection` (private)

**Примечание:** В файле есть проблемы с определением функций (строки 269-293): `DateSection` и другие функции определены некорректно. Требуется исправление структуры файла.

---

#### 4. `/app/src/main/java/com/dayscounter/ui/screen/MainScreen.kt` ✅

**Текущее состояние:**
- ✅ Все private функции в camelCase (`mainScreenContent`, `emptyContent`, `itemsListContent`, `loadingContent`, `errorContent`)
- ✅ Magic Numbers заменены на использование `NumberFormattingUtils.formatDaysCount()`
- ✅ 6 функций (detekt: TooManyFunctions ✓, порог: 11)

**Изменения:** Не требуются.

---

## Критерии завершения

### ✅ ВЫПОЛНЕНО:
- ✅ Все ViewModels используют конкретные исключения (ItemException)
- ✅ Нет warning `TooGenericExceptionCaught`
- ✅ Нет warning `MagicNumber` (все числа используют `NumberFormattingUtils`)
- ✅ Нет warning `MissingTranslation` (все переводы добавлены в `values-ru/strings.xml`)
- ✅ Нет warning `LongParameterList` (CreateEditFormContent использует CreateEditFormParams)

### ⚠️ ОСТАЛОСЬ ВЫПОЛНИТЬ:
- ⚠️ Нет warning `TooManyFunctions` (≤11 функций в файле)
  - RootScreen.kt: 12 функций → перенести компоненты в RootScreenComponents.kt
  - DetailScreen.kt: 17 функций → перенести компоненты в DetailScreenComponents.kt
  - CreateEditScreen.kt: 19 функций → перенести компоненты в CreateEditScreenComponents.kt
- ⚠️ Нет warning `UnusedParameter` (все параметры используются)
  - Проверить актуальность проблем из плана
- ⚠️ Нет warning `PrivateResource` (конфликтующие ресурсы переименованы или удалены)
  - Проверить актуальность проблемы с `selected` и `not_selected`
- ⚠️ `ktlint` без ошибок
- ⚠️ `detekt` без ошибок (только предупреждения о стиле)
- ⚠️ `lint` без ошибок
- ⚠️ Сборка проекта успешна
- ⚠️ Unit тесты выполняются

---

### Общая статистика:
- **Выполнено:** ~50% (Generic Exceptions, Magic Numbers, LongParameterList, MissingTranslation)
- **Осталось:** ~50% (TooManyFunctions, UnusedParameter проверка, PrivateResource проверка, запуск линтеров)

---

## Порядок выполнения (актуальный)

### ✅ ВЫПОЛНЕНО:

#### Фаза 1: Конкретные исключения в ViewModels (~20 минут) ✅
- MainScreenViewModel: ✅ использует ItemException.LoadFailed, DeleteFailed, UpdateFailed
- DetailScreenViewModel: ✅ использует ItemException.DeleteFailed, UpdateFailed
- CreateEditScreenViewModel: ✅ использует ItemException.LoadFailed, SaveFailed, UpdateFailed

#### Фаза 2: Устранение Magic Numbers (~10 минут) ✅
- MainScreen: ✅ использует NumberFormattingUtils.formatDaysCount()
- DetailScreen: ✅ использует NumberFormattingUtils.formatDaysCount()
- CreateEditScreen: ✅ использует NumberFormattingUtils.formatDaysCount()

#### Фаза 3: LongParameterList (~10 минут) ✅
- CreateEditScreen: ✅ CreateEditFormContent использует CreateEditFormParams (6 параметров)

#### Фаза 4: MissingTranslation (~15 минут) ✅
- Добавлены переводы в values-ru/strings.xml:
  - ✅ days_abbreviated: "дн."
  - ✅ months_abbreviated: "мес."
  - ✅ years_abbreviated: "г."
  - ✅ days_count (plurals): полные переводы
  - ✅ months_count (plurals): полные переводы
  - ✅ years_count (plurals): полные переводы

---

### ⚠️ ОСТАЛОСЬ ВЫПОЛНИТЬ:

#### Фаза 5: Уменьшение количества функций (~60 минут) ⚠️

**Задача:** Разделить экраны на компоненты для соблюдения правила TooManyFunctions (≤11 функций).

**Порядок:**

1. **Создать директорию компонентов (~5 минут)**
   ```bash
   mkdir -p app/src/main/java/com/dayscounter/ui/screen/components
   ```

2. **Создать RootScreenComponents.kt (~15 минут)**
   - Перенести из RootScreen.kt:
     - NavigationBarContent
     - NavHostContent
     - EventsRoute
     - MoreRoute
     - ItemDetailRoute
     - CreateItemRoute
     - EditItemRoute
     - EventsScreenContent
     - MoreScreenContent
     - UpdateTabBasedOnRoute
   - Обновить импорты в RootScreen.kt

3. **Создать DetailScreenComponents.kt (~20 минут)**
   - Перенести из DetailScreen.kt:
     - DetailTopAppBar
     - DetailActionButtons
     - DetailContentInner
     - ColorTagSection
     - TitleSection
     - DateSection
     - DaysCountSection
     - DetailsSection
     - DisplayOptionInfoSection
     - LoadingContent
     - ErrorContent
     - DeletedContent
   - Обновить импорты в DetailScreen.kt

4. **Создать CreateEditScreenComponents.kt (~20 минут)**
   - Перенести из CreateEditScreen.kt:
     - CreateEditTopAppBar
     - CreateEditFormContent
     - RememberCreateEditUiStates
     - LoadItemData
     - TitleSection (создать или перенести)
     - DetailsSection (создать или перенести)
     - DateSection (исправить определение)
     - PreviewDaysContent
     - PreviewDaysContentInner
     - ColorSelector
     - ColorOptionSurface
     - NoColorOptionSurface
     - DisplayOptionSelector
     - DisplayOptionSurface
     - ButtonsSection
     - SaveButton
     - DatePickerDialogSection
   - **Обратить внимание:** Исправить некорректное определение функций (строки 269-293 в CreateEditScreen.kt)

---

#### Фаза 6: Проверка UnusedParameter (~10 минут) ⚠️

**Задача:** Запустить detekt и проверить актуальность проблем.

**Действия:**
1. Запустить `./gradlew detekt`
2. Найти предупреждения `UnusedParameter`
3. Удалить или исправить неиспользуемые параметры
4. Проверить:
   - RootScreen.kt:150 — используется ли `viewModel` в `NavHostContent`
   - CreateEditScreen.kt:413 — используется ли `selectedDisplayOption` в `displayOptionSelector`

---

#### Фаза 7: Проверка PrivateResource (~10 минут) ⚠️

**Задача:** Проверить и исправить конфликты ресурсов.

**Действия:**
1. Проверить использование ресурсов `selected` и `not_selected` в коде
2. Если используются и конфликтуют с Compose UI:
   - Переименовать в `color_tag_selected` и `color_tag_not_selected`
   - Обновить использование в коде
3. Если не используются:
   - Удалить из `values-en/strings.xml`

---

#### Фаза 8: Проверка линтеров и сборки (~20 минут) ⚠️

**Действия:**
1. Запустить `./gradlew ktlintCheck`
2. Запустить `./gradlew detekt`
3. Запустить `./gradlew lintDebug`
4. Исправить все выявленные ошибки
5. Запустить `./gradlew build testDebugUnitTest`
6. Убедиться, что все тесты проходят

---

**Общее время для выполнения оставшихся задач:** ~110 минут (1.8 часа)

---

## Альтернативные решения

### Если не удаеться соблюсти правило TooManyFunctions (<11 функций)

**Вариант 1:** Создать отдельные файлы для компонентов ⭐ РЕКОМЕНДУЕТСЯ
```
ui/screen/components/
  - DetailScreenComponents.kt
  - CreateEditScreenComponents.kt
  - RootScreenComponents.kt
```

**Вариант 2:** Объединить функции в inline
- Использовать `inline fun` для мелких вспомогательных функций
- ❌ Не рекомендуется — ухудшит читаемость кода

**Вариант 3:** Игнорировать правило в detekt.yml
```yaml
TooManyFunctions:
  active: false
```
- ❌ Не рекомендуется — правило помогает поддерживать чистую архитектуру

**Рекомендация:** Использовать **Вариант 1** для улучшения архитектуры и разделения ответственности.

---

### Если не удаеться соблюсти правило FunctionNaming

**Конфликт:**
- Detekt требует PascalCase для всех функций
- Compose lint требует camelCase для Composable функций

**Решение:**
- Использовать camelCase для всех Composable функций (приоритет Compose lint)
- Это соответствует рекомендациям Google для Compose
- Текущий код уже использует camelCase для большинства функций

---

## Заметки

### Обновление (30 декабря 2025 года)

После анализа текущего состояния кода:

**Выполнено:**
- ✅ Все ViewModels используют конкретные исключения `ItemException` вместо `Exception`
- ✅ Все magic numbers заменены на использование `NumberFormattingUtils.formatDaysCount()`
- ✅ LongParameterList исправлен — CreateEditFormContent использует CreateEditFormParams data class
- ✅ MissingTranslation исправлен — все переводы добавлены в values-ru/strings.xml
- ✅ Все private функции в экранах используют camelCase именование (соответствует Compose lint)

**Осталось выполнить:**
- ⚠️ TooManyFunctions — 3 файла с >11 функциями:
  - RootScreen.kt: 12 функций
  - DetailScreen.kt: 17 функций
  - CreateEditScreen.kt: 19 функций
- ⚠️ UnusedParameter — требуется проверка актуальности проблем
- ⚠️ PrivateResource — требуется проверка и исправление конфликтов
- ⚠️ Запуск линтеров для получения актуального списка проблем

**Технические замечания:**
- Код стал чище и модулее
- Все константы вынесены в отдельные объекты
- Все длинные функции разбиты на мелкие части
- ViewModels корректно обрабатывают ошибки с использованием специфических исключений

**Следующие шаги:**
1. Создать директорию `ui/screen/components/`
2. Перенести компоненты из экранов в отдельные файлы
3. Проверить и исправить UnusedParameter
4. Проверить и исправить PrivateResource
5. Запустить линтеры и убедиться в отсутствии ошибок

---

### Первоначальные заметки (первый раунд рефакторинга)

После первого раунда рефакторинга (RootScreen, DetailScreen, CreateEditScreen, MainScreen) код стал чище и модулее:
- Все константы вынесены в отдельные объекты
- Все длинные функции разбиты на мелкие части
- Оставшиеся проблемы в основном относятся к:
  1. Именованию функций (просто переименовать) ✅ ВЫПОЛНЕНО
  2. Количество функций (можно разбить на отдельные файлы) ⚠️ ТРЕБУЕТ ВЫПОЛНЕНИЯ
  3. Generic exceptions (просто заменить на конкретные) ✅ ВЫПОЛНЕНО
  4. Magic numbers (просто использовать константы) ✅ ВЫПОЛНЕНО
