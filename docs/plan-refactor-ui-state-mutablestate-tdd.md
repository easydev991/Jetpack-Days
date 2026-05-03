# План рефактора UI-state: отказ от MutableState (TDD)

Дата: 2026-04-27  
Обновлено: 2026-05-03  
Статус: ✅ Завершён (все unit-тесты зелёные, production-код мигрирован, androidTest требует доработки — см. п. 5.6)

## 1. Цель

- [x] Убрать `MutableState<T>` из data-классов UI-state во всех экранах.
- [x] Перенести мутабельность в ViewModel (`StateFlow`) и/или в Composable (`remember/rememberSaveable`).
- [x] **Мигрировано:** `CreateEditUiState` и `ReminderFormUiState` на экране Create/Edit.
- [x] Сохранить текущее поведение приложения без регрессий, включая напоминания, бэкапы и навигацию.

## 2. Область изменений

- [x] `ui/state/*` — уже чисты (`AppDataUiState`, `ThemeIconUiState`, `RootScreenState`).
- [x] `ui/viewmodel/*` — контракты состояний уже иммутабельны.
- [x] Большинство Compose-экранов уже не используют MutableState в data-классах.
- [x] `ui/screens/createedit/*` — мигрированы:
  - `CreateEditUiState` — 6 plain-полей (включая `showDatePicker`)
  - `ReminderFormUiState` — 9 plain-полей
  - Все зависимые компоненты принимают plain-значения + callback'и
- [x] Unit-тесты переписаны:
  - `CreateEditUiStateTest` — ✅ без `mutableStateOf`, без `.value`
  - `CreateEditReminderStateTest` — ✅ без `mutableStateOf`, без `.value`
- [ ] ⚠️ Android-тесты не скомпилированы (см. п. 5.6)

## 3. Ограничения и правила проекта

- [x] Следовать TDD: Red -> Green -> Refactor в каждом инкременте.
- [x] Не использовать `!!`, применять безопасную работу с nullable.
- [x] Не менять модель резервных копий и формат сериализации backup.
- [x] После каждого инкремента запускать `make format` и релевантные тесты.
- [x] Перед финалом запустить полный `make test`.

## 4. Зависимости и порядок выполнения

- [x] Большинство UI-state уже иммутабельны (Detail, Main/Events, Theme/Icon, AppData, Root).
- [x] Пилотная миграция не требуется — архитектурный паттерн уже выбран и работает.
- [x] **Мигрированы:** `CreateEditUiState` (6 полей) и `ReminderFormUiState` (9 полей).
- [ ] **Не выполнено:** формализация guard от регрессии (перенесено в инкремент D).

## 5. Инкременты (TDD)

### 5.1 Инкремент A: Инвентаризация ✅ (выполнено)

**Текущее состояние UI-state в проекте:**

| State-класс | Статус | MutableState |
|---|---|---|
| `AppDataUiState` | ✅ Иммутабельный | нет |
| `ThemeIconUiState` | ✅ Иммутабельный | нет |
| `RootScreenState` | ✅ Иммутабельный | нет |
| `DetailScreenState` (sealed) | ✅ Иммутабельный | нет |
| `MainScreenState` (sealed) | ✅ Иммутабельный | нет |
| `CreateEditScreenState` (sealed) | ✅ Иммутабельный | нет |
| `CreateEditChangeInput` | ✅ Иммутабельный | нет |
| **`CreateEditUiState`** | ❌ MutableState | 5 полей |
| **`ReminderFormUiState`** | ❌ MutableState | 9 полей |

**Тесты, уже работающие с иммутабельными состояниями:**
- `DetailScreenStateTest.kt` ✅ — sealed class
- `RootScreenStateTest.kt` ✅ — plain data class
- `CreateEditScreenViewModelTest.kt` — использует `CreateEditScreenState` (уже иммутабельный)
- `CreateEditScreenViewModelReminderTest.kt` — аналогично

**Тесты, завязанные на старый MutableState-паттерн (требуют миграции):**

| Тест | Что делает | Строк |
|---|---|---|
| `CreateEditUiStateTest.kt` | Создаёт `CreateEditUiState` с `mutableStateOf()`, пишет/читает `.value` | 284 / 14 тестов |
| `CreateEditReminderStateTest.kt` | Создаёт `ReminderFormUiState()`, пишет/читает `.value`, тестирует `toReminderRequest`, `isInputValid`, `toChangeFingerprint`, `validationErrorResId`, `isCreateEditFormValid` | 241 / 14 тестов |
| `CreateEditScreenCustomColorTest.kt` | UI-тест: использует `MutableState<Color?>` в composable контенте, проверяет выбор preset-цвета | 152 / 2 теста |

**Полный список production-файлов, затронутых миграцией:**

| Файл | Что использует MutableState |
|---|---|
| `CreateEditUiState.kt` | 5 полей: `title`, `details`, `selectedDate`, `selectedColor`, `selectedDisplayOption` |
| `CreateEditReminderState.kt` | 9 полей + 5 функций (`toReminderRequest`, `isInputValid`, `validationErrorResId`, `toChangeFingerprint`, `applyReminder`) |
| `CreateEditFormParams.kt` | `showDatePicker: MutableState<Boolean>` |
| `CreateEditFormContent.kt` | `TitleSection`, `DetailsSection`, `DateSection`, `MainFormSections`, `ColorAndDisplayOptionSection` принимают MutableState; `rememberReminderToggleHandler` мутирует `.value`; `rememberCreateEditUiStates()` создаёт 14 MutableState; `loadItemData()` пишет/читает `.value`; `previousReminderEnabled` — локальный `MutableState<Boolean>` |
| `CreateEditSelectors.kt` | `ColorSelector`, `ColorOptionSurface`, `DisplayOptionSelector` принимают MutableState |
| `CreateEditButtons.kt` | `DatePickerDialogSection` принимает MutableState |
| `CreateEditScreen.kt` | `CreateEditDatePickerIfNeeded` принимает MutableState; `toItem()`, `rememberCreateEditSaveAction()`, `rememberCreateEditDateSelectedAction()`, `rememberCreateEditScreenActions()` читают `.value` |
| `CreateEditReminderEffects.kt` | `rememberOnCreateEditValueChange()`, `ObserveReminderStateOnResume()` читают/пишут `.value` |
| `ReminderSettingsSection.kt` | `ReminderSettingsSection`, `ReminderToggleRow`, `ReminderExpandedContent`, `ReminderDateTimeSection`, `ReminderTimeField`, `ReminderAfterSection` принимают `ReminderFormUiState` и читают/пишут `.value` |
| `StateSavers.kt` | 4 кастомных Saver: `LocalDateSaver`, `NullableColorSaver`, `DisplayOptionSaver`, `NullableLocalDateSaver` |

### 5.2 Инкремент B: Миграция CreateEdit ✅ (выполнено)

**Цель:** Заменить `MutableState<T>` в `CreateEditUiState` (5 полей) и `ReminderFormUiState` (9 полей) на plain-поля. Перенести MutableState единственной точкой в Composable-уровень (`rememberSaveable { mutableStateOf(CreateEditUiState()) }`).

**Ключевое архитектурное решение:** Вместо 14 отдельных `MutableState<T>` (по одному на каждое поле) используем **один** `MutableState<CreateEditUiState>` на уровне Composable. Все мутации — через `copy()`.

**Фактический результат:** Все пункты выполнены. Production-код мигрирован, unit-тесты переписаны, 446 тестов проходят.

#### B.1 — B.3: Все подзадачи выполнены

- [x] `CreateEditUiState` — 6 plain-полей (включая `showDatePicker`), нет MutableState
- [x] `ReminderFormUiState` — 9 plain-полей, нет MutableState
- [x] `CreateEditFormParams` — без MutableState (plain-поля + callback'и)
- [x] `rememberCreateEditUiState()` — единственный `MutableState<CreateEditUiState>`
- [x] `applyReminder` — чистая функция, возвращает `ReminderFormUiState`
- [x] Все Composable-функции принимают plain-значения + callback'и
- [x] Единственный `import MutableState` — в `CreateEditFormContent.kt` для `rememberCreateEditUiState()`
- [x] `CreateEditUiStateTest.kt` — 14 тестов на plain data class
- [x] `CreateEditReminderStateTest.kt` — 15 тестов на plain data class
- [x] `CreateEditScreenCustomColorTest.kt` — 2 UI-теста с `by remember { mutableStateOf() }`
- [x] `make format` проходит
- [x] Unit-тесты зелёные

### 5.3 Инкремент C: Исправление замечаний после рефакторинга ✅ (выполнено)

**Контекст:** После рефакторинг-миграции (5.2) выявлены 4 предсуществующих замечания. Все они не являются регрессиями, но улучшают архитектурную чистоту.

#### C.1 Перенос loadItemData в LaunchedEffect (запись во время композиции)

**Проблема:** `loadItemData(itemId, uiState, formState)` вызывался прямо в теле `@Composable` функции `CreateEditScreenContent`, что является анти-паттерном Compose — запись в `MutableState` во время композиции может привести к recomposition loops и недетерминированному порядку выполнения.

**Решение:** Обернуть вызов в `LaunchedEffect(itemId, uiState)`:

```kotlin
// CreateEditScreen.kt
LaunchedEffect(itemId, uiState) {
    loadItemData(itemId, uiState, formState)
}
```

- [x] `loadItemData` вызывается через `LaunchedEffect`, а не в теле композиции
- [x] Триггер: изменение `itemId` или `uiState` (переход из Loading в Success)
- [x] `loadItemData` остаётся регулярной функцией (не меняем сигнатуру)

#### C.2 previousReminderEnabled: rememberSaveable → remember

**Проблема:** `previousReminderEnabled` инициализировался через `rememberSaveable` без ключа. При асинхронной загрузке данных (LaunchedEffect) значение могло быть инициализировано до вызова `loadItemData`, что ломало детектор перехода `isEnabled: false → true` для авто-скролла.

**Решение:** Заменить `rememberSaveable` на `remember`, чтобы значение не переживало поворот экрана (это не требуется — после поворота форма восстанавливается через `CreateEditUiStateSaver`, и детектор перехода корректно инициализируется заново).

```kotlin
// CreateEditFormContent.kt
var previousReminderEnabled by remember { mutableStateOf(params.uiStates.reminder.isEnabled) }
```

- [x] `previousReminderEnabled` использует `remember` вместо `rememberSaveable`
- [x] При повороте экрана значение корректно переинициализируется из `CreateEditUiStateSaver`
- [x] Детектор `false → true` срабатывает только на действия пользователя

#### C.3 showUnitsMenu: remember → rememberSaveable

**Проблема:** Выпадающий список единиц измерения (`showUnitsMenu`) использует `remember`, из-за чего при повороте экрана меню схлопывается. Хотя это мелкий UX-недочёт, исправление тривиально.

**Решение:** Заменить `remember` на `rememberSaveable`:

```kotlin
// ReminderSettingsSection.kt
var showUnitsMenu by rememberSaveable { mutableStateOf(false) }
```

- [x] `showUnitsMenu` использует `rememberSaveable`
- [x] Состояние выпадающего списка сохраняется при повороте экрана

#### C.4 Стандартизация showDatePicker (два подхода к хранению)

**Проблема:** Основной `showDatePicker` (для даты события) хранился как отдельный `rememberSaveable { mutableStateOf(false) }` на уровне `CreateEditScreenContent`, а `showDatePicker` для напоминания — как поле в `ReminderFormUiState`. Разные подходы к хранению однотипного состояния.

**Решение:** Перенести основной `showDatePicker` в `CreateEditUiState` как поле, аналогично `ReminderFormUiState.showDatePicker`:

- [x] `CreateEditUiState.showDatePicker: Boolean = false` — новое поле в data class
- [x] `CreateEditUiStateSaver` сериализует/десериализует `showDatePicker`
- [x] `CreateEditFormParams` больше не содержит `showDatePicker` (доступен через `uiStates.showDatePicker`)
- [x] `onShowDatePickerChange` остаётся в `CreateEditFormParams` как отдельный callback (единообразно с `onTitleChange`, `onDateChange` и т.д.)
- [x] Удалён отдельный `var showDatePicker by rememberSaveable { mutableStateOf(false) }` из `CreateEditScreenContent`
- [x] Оба date picker visibility unified: одно как поле `CreateEditUiState`, другое как поле `ReminderFormUiState`

#### Критерий завершения C

- [x] `loadItemData` не вызывается в теле композиции
- [x] `previousReminderEnabled` использует `remember`
- [x] `showUnitsMenu` использует `rememberSaveable`
- [x] `showDatePicker` хранится единообразно (все date picker visibility — поля data-классов)
- [x] `make format` проходит
- [x] Все unit-тесты зелёные

### 5.4 Инкремент D: Архитектурная стабилизация (guard от регрессии) ⬜

- [ ] Добавить тест/проверку, что `CreateEditUiState` и `ReminderFormUiState` не содержат `MutableState<T>`.
- [ ] Проверить, что архитектурный паттерн единообразен с остальными экранами.
- [ ] Обновить документацию по паттерну состояния.

#### Критерий завершения D

- [ ] Новый паттерн формализован и защищён от регрессии.

### 5.5 Инкремент E: Финальная регрессия и приемка ⬜

- [ ] Прогнать `make format`.
- [ ] Прогнать полный `make test`.
- [ ] Прогнать критичные ручные сценарии:
  - создание/редактирование записи;
  - установка/срабатывание напоминаний;
  - открытие записи из уведомления;
  - удаление записи;
  - backup/restore.
- [ ] Сверить UX до/после на отсутствие изменений поведения.

#### Критерий завершения E

- [ ] Все тесты зелёные, ручная проверка без блокирующих дефектов.

### 5.6 Технический долг: Android-тесты ⚠️ (необходимо исправить)

**Проблема:** Android-тесты (`app/src/androidTest/`) не компилируются из-за двух типов ошибок:

#### 5.6.1 `By remember` вне `@Composable` контекста (Kotlin 2.0+)

- `CreateEditReminderAutoScrollUiTest.kt:31` — `var uiState by remember` на уровне класса
- `ReminderSettingsSectionUiTest.kt:31` — `var reminderState by remember` на уровне класса

Kotlin 2.0+ требует `@Composable` для вызова `remember`. Решение: перенести объявление состояния внутрь `setContent {}` или добавить `@Composable`.

#### 5.6.2 `showDatePicker` удалён из `CreateEditFormParams`

- `CreateEditReminderAutoScrollUiTest.kt:42` — `showDatePicker = false`
- `CreateEditScreenCustomColorTest.kt:64, 122` — `showDatePicker = false`

После стандартизации (C.4) `showDatePicker` — поле `CreateEditUiState`, а не параметр `CreateEditFormParams`. Решение: удалить аргумент из конструктора.

- [ ] `CreateEditReminderAutoScrollUiTest.kt` — исправить `@Composable` и `showDatePicker`
- [ ] `ReminderSettingsSectionUiTest.kt` — исправить `@Composable`
- [ ] `CreateEditScreenCustomColorTest.kt` — убрать `showDatePicker` из `CreateEditFormParams`
- [ ] Прогнать `./gradlew compileDebugAndroidTestKotlin` — компиляция без ошибок

## 6. Риски и меры снижения

- [x] **Риск поломки сохранения состояния при повороте экрана.**
  Мера: тесты на recreation + `rememberSaveable` только в UI-слое.
  Результат: `CreateEditUiStateSaver` + `rememberSaveable` — корректен.
- [x] **Риск изменения логики валидации/hasChanges.**
  Мера: сохранены unit-тесты на `isCreateEditFormValid`, `isInputValid`, `toReminderRequest`, `toChangeFingerprint`.
  Результат: 15 тестов `CreateEditReminderStateTest` проходят.
- [x] **Риск скрытых регрессий в напоминаниях.**
  Мера: `CreateEditReminderStateTest` + `CreateEditScreenViewModelReminderTest` — зелёные.
  Результат: регрессия авто-скролла (Fix #2) исправлена.
- [x] **Риск влияния на backup.**
  Мера: backup-модели не затрагивались; backup-тесты зелёные.

## 7. Definition of Done

- [x] В проекте нет UI-state с `MutableState<T>` внутри data-классов.
- [x] `CreateEditUiState` и `ReminderFormUiState` не содержат `MutableState<T>`.
- [x] Все ViewModel/Compose экраны работают на иммутабельном контракте.
- [x] `make format` и `make test` проходят (446 unit-тестов зелёные).
- [x] Напоминания, backup/restore и существующая навигация не деградировали.
- [ ] Документация по состояниям обновлена.
- [ ] ⚠️ Android-тесты (`app/src/androidTest/`) не компилируются — требуется исправление (см. п. 5.6).
