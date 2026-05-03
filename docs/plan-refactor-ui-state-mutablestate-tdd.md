# План рефактора UI-state: отказ от MutableState (TDD)

Дата: 2026-04-27  
Обновлено: 2026-05-03  
Статус: In Progress (выполнен инкремент C — исправление замечаний)

## 1. Цель

- [x] Убрать `MutableState<T>` из data-классов UI-state во всех экранах.
- [x] Перенести мутабельность в ViewModel (`StateFlow`) и/или в Composable (`remember/rememberSaveable`).
- [ ] **Осталось:** мигрировать `CreateEditUiState` и `ReminderFormUiState` на экране Create/Edit.
- [x] Сохранить текущее поведение приложения без регрессий, включая напоминания, бэкапы и навигацию.

## 2. Область изменений

- [x] `ui/state/*` — уже чисты (`AppDataUiState`, `ThemeIconUiState`, `RootScreenState`).
- [x] `ui/viewmodel/*` — контракты состояний уже иммутабельны.
- [x] Большинство Compose-экранов уже не используют MutableState в data-классах.
- [ ] **Осталось:** только `ui/screens/createedit/*`:
  - `CreateEditUiState` (5 MutableState полей)
  - `ReminderFormUiState` (9 MutableState полей)
  - Зависимые компоненты: `CreateEditFormContent`, `CreateEditFormParams`, `CreateEditSelectors`, `CreateEditButtons`, `CreateEditScreen`
- [ ] **Осталось:** Unit/UI-тесты, завязанные на старый mutable-паттерн:
  - `CreateEditUiStateTest`, `CreateEditReminderStateTest`, `CreateEditScreenCustomColorTest`

## 3. Ограничения и правила проекта

- [ ] Следовать TDD: Red -> Green -> Refactor в каждом инкременте.
- [ ] Не использовать `!!`, применять безопасную работу с nullable.
- [ ] Не менять модель резервных копий и формат сериализации backup.
- [ ] После каждого инкремента запускать `make format` и релевантные тесты.
- [ ] Перед финалом запустить полный `make test`.

## 4. Зависимости и порядок выполнения

- [x] Большинство UI-state уже иммутабельны (Detail, Main/Events, Theme/Icon, AppData, Root).
- [x] Пилотная миграция не требуется — архитектурный паттерн уже выбран и работает.
- [ ] **Остаётся:** мигрировать только `CreateEditUiState` (5 полей) и `ReminderFormUiState` (9 полей).
- [ ] После стабилизации CreateEdit — формализовать guard от регрессии.

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

### 5.2 Инкремент B: Миграция CreateEdit (единственный оставшийся)

**Цель:** Заменить `MutableState<T>` в `CreateEditUiState` (5 полей) и `ReminderFormUiState` (9 полей) на plain-поля. Перенести MutableState единственной точкой в Composable-уровень (`rememberSaveable { mutableStateOf(CreateEditUiState()) }`).

**Ключевое архитектурное решение:** Вместо 14 отдельных `MutableState<T>` (по одному на каждое поле) используем **один** `MutableState<CreateEditUiState>` на уровне Composable. Все мутации — через `copy()`.

#### B.1 RED: Тесты под новый контракт

##### B.1.1 Переписать `CreateEditUiStateTest.kt`

Файл: `app/src/test/.../createedit/CreateEditUiStateTest.kt`
Текущее: 284 строки, 14 тестов, все используют `mutableStateOf()` и `.value`.

Новый контракт тестов:
- [ ] Убрать `import androidx.compose.runtime.mutableStateOf` (не нужен в unit-тесте)
- [ ] Все тесты создают `CreateEditUiState` через data class конструктор с plain-значениями:

  ```kotlin
  val state = CreateEditUiState(
      title = "Test",
      details = "",
      selectedDate = null,
      selectedColor = null,
      selectedDisplayOption = DisplayOption.DAY
  )
  ```

- [ ] Проверка значений: `assertEquals("Test", state.title)` — без `.value`
- [ ] Мутация через `copy()`: `val newState = state.copy(title = "New")`
- [ ] Тесты на:
  - инициализация с дефолтными значениями (`CreateEditUiState()`)
  - инициализация с кастомными значениями через конструктор
  - копирование с изменением одного поля (`copy(title = "new")`)
  - копирование с изменением всех полей
  - иммутабельность — копия не меняет оригинал
  - `reminder` создаётся как `ReminderFormUiState()` (тоже plain, без MutableState)

##### B.1.2 Переписать `CreateEditReminderStateTest.kt`

Файл: `app/src/test/.../createedit/CreateEditReminderStateTest.kt`
Текущее: 241 строка, 14 тестов.

Новый контракт тестов:
- [ ] Убрать `import androidx.compose.runtime.mutableStateOf` (не нужен)
- [ ] `ReminderFormUiState` создаётся с plain-значениями без MutableState
- [ ] `toReminderRequest` — принимает `itemId: Long`, возвращает `ReminderRequest?` на основе plain-полей

  ```kotlin
  val state = ReminderFormUiState(isEnabled = true, mode = ReminderMode.AT_DATE, ...)
  val request = state.toReminderRequest(itemId = 42L)
  ```

- [ ] `isInputValid` — принимает `currentDateTime`, использует plain-поля
- [ ] `validationErrorResId` — принимает `currentDateTime`, возвращает `Int?`
- [ ] `toChangeFingerprint` — возвращает `String?` на основе plain-полей
- [ ] `applyReminder` — **чистая функция**: `fun ReminderFormUiState.applyReminder(reminder: Reminder?): ReminderFormUiState`
  - возвращает новое состояние вместо мутации `this`
  - тест: `assertTrue(result.isInitializedFromSource)`
- [ ] `defaultReminderDate` — при явной передаче `today` (как в тестах) детерминирована, проверка не меняется
- [ ] `isCreateEditFormValid` — уже принимает `reminderUiState: ReminderFormUiState`, проверка не меняется
- [ ] `toChangeFingerprint` для `Reminder?` — уже extension на domain-модели, не меняется

##### B.1.3 Переписать `CreateEditScreenCustomColorTest.kt`

Файл: `app/src/androidTest/.../createedit/CreateEditScreenCustomColorTest.kt`
Текущее: 152 строки, 2 теста, использует `MutableState<Color?>` и создаёт `CreateEditUiState` с MutableState-полями.

Новый контракт тестов:
- [ ] Вместо `var selectedColorStateHolder: MutableState<Color?>?`
  → `var selectedColor: Color? = null` с отдельным `onColorChange` callback
- [ ] Создание `CreateEditUiState` с plain-значениями:

  ```kotlin
  var selectedColor by remember { mutableStateOf<Color?>(customColor) }
  val uiState = CreateEditUiState(
      title = "...",
      details = "",
      selectedDate = LocalDate.now(),
      selectedColor = selectedColor, // plain Color?
      selectedDisplayOption = DisplayOption.DAY
  )
  ```

- [ ] Компоненты принимают callback'и вместо MutableState:

  ```kotlin
  ColorSelector(
      selectedColor = selectedColor,
      onColorSelected = { newColor -> selectedColor = newColor }
  )
  ```

- [ ] `CreateEditFormParams` принимает `showDatePicker: Boolean` + `onShowDatePickerChange: (Boolean) -> Unit`
- [ ] `selectedColorStateHolder` → `var latestSelectedColor: Color?` и обновляется в callback

#### B.2 GREEN: Реализация production-кода

##### B.2.1 `CreateEditUiState.kt` — замена MutableState на plain-поля

- [ ] Убрать `import androidx.compose.runtime.MutableState`
- [ ] Заменить 5 полей `MutableState<T>` на `T`:

  ```kotlin
  data class CreateEditUiState(
      val title: String = "",
      val details: String = "",
      val selectedDate: LocalDate? = null,
      val selectedColor: Color? = null,
      val selectedDisplayOption: DisplayOption = DisplayOption.DAY,
      val reminder: ReminderFormUiState = ReminderFormUiState()
  )
  ```

- [ ] **Добавить** `import java.time.LocalDate` (сейчас используется полный путь `java.time.LocalDate?`)

##### B.2.2 `CreateEditReminderState.kt` — замена MutableState на plain-поля

- [ ] Убрать `import androidx.compose.runtime.MutableState`
- [ ] Убрать `import androidx.compose.runtime.mutableStateOf`
- [ ] Заменить 9 полей на plain:

  ```kotlin
  data class ReminderFormUiState(
      val isEnabled: Boolean = false,
      val mode: ReminderMode = ReminderMode.AT_DATE,
      val selectedDate: LocalDate? = defaultReminderDate(),
      val showDatePicker: Boolean = false,
      val hour: Int = LocalTime.now().hour,
      val minute: Int = LocalTime.now().minute,
      val intervalValue: String = "",
      val intervalUnit: ReminderIntervalUnit = ReminderIntervalUnit.DAY,
      val isInitializedFromSource: Boolean = false
  )
  ```

- [ ] **Функции-расширения переписать с plain-полями:**
  - `toReminderRequest()`: `if (!isEnabled) return null` (без `.value`)
  - `isInputValid()`: `if (!isEnabled) return true` (без `.value`)
  - `validationErrorResId()`: `if (!isEnabled) return null` (без `.value`)
  - `toChangeFingerprint()`: `if (!isEnabled) return null` (без `.value`)
  - **`applyReminder()`**: из `Unit` → `ReminderFormUiState`, возвращает новое состояние через `copy()`:

    ```kotlin
    fun ReminderFormUiState.applyReminder(reminder: Reminder?): ReminderFormUiState =
        if (reminder == null) {
            copy(isInitializedFromSource = true, isEnabled = false, mode = ReminderMode.AT_DATE)
        } else {
            copy(
                isInitializedFromSource = true,
                isEnabled = true,
                mode = reminder.mode,
                ...
            )
        }
    ```

##### B.2.3 `CreateEditFormParams.kt` — замена MutableState

- [ ] Убрать `import androidx.compose.runtime.MutableState`
- [ ] Заменить `showDatePicker: MutableState<Boolean>` на два поля:

  ```kotlin
  data class CreateEditFormParams(
      val itemId: Long?,
      val paddingValues: PaddingValues,
      val uiState: CreateEditUiState,          // переименовать uiStates → uiState
      val showDatePicker: Boolean,              // plain
      val onShowDatePickerChange: (Boolean) -> Unit,  // callback
      val onTitleChange: (String) -> Unit,      // новые callback'и
      val onDetailsChange: (String) -> Unit,
      val onDateChange: (LocalDate?) -> Unit,
      val onColorChange: (Color?) -> Unit,
      val onDisplayOptionChange: (DisplayOption) -> Unit,
      val onReminderChange: (ReminderFormUiState) -> Unit,
      val viewModel: CreateEditScreenViewModel,
      val onBackClick: () -> Unit,
      val onReminderNotificationsUnavailable: () -> Unit
  )
  ```

  **ИЛИ** альтернативно: один общий callback:

  ```kotlin
  data class CreateEditFormParams(
      ...
      val uiState: CreateEditUiState,
      val onUiStateChange: (CreateEditUiState) -> Unit,  // общий callback
      ...
  )
  ```

##### B.2.4 `CreateEditFormContent.kt` — адаптация

- [ ] Убрать `import androidx.compose.runtime.MutableState`
- [ ] Убрать `import androidx.compose.runtime.mutableStateOf`
- [ ] `rememberCreateEditUiStates()`: заменить 14 отдельных MutableState на один:

  ```kotlin
  @Composable
  internal fun rememberCreateEditUiState(): MutableState<CreateEditUiState> =
      rememberSaveable(stateSaver = CreateEditUiStateSaver) {
          mutableStateOf(CreateEditUiState())
      }
  ```

  Где `CreateEditUiStateSaver` — кастомный Saver для всего `CreateEditUiState` (или Saver для каждого поля, если используем `Saver` композицию).
- [ ] `MainFormSections` — принимать plain-поля + callback'и:

  ```kotlin
  private fun MainFormSections(
      title: String,
      details: String,
      selectedDate: LocalDate?,
      showDatePicker: Boolean,
      onTitleChange: (String) -> Unit,
      onDetailsChange: (String) -> Unit,
      onShowDatePickerChange: (Boolean) -> Unit,
  )
  ```

- [ ] `TitleSection(title: String, onValueChange: (String) -> Unit)` — убрать MutableState
- [ ] `DetailsSection(details: String, onValueChange: (String) -> Unit)` — убрать MutableState
- [ ] `DateSection(selectedDate: LocalDate?, showDatePicker: Boolean, onShowDatePickerChange: (Boolean) -> Unit)` — убрать MutableState
- [ ] `ColorAndDisplayOptionSection` — принимать `selectedColor: Color?`, `selectedDisplayOption: DisplayOption` (plain) + `onColorSelected: (Color?) -> Unit`, `onDisplayOptionSelected: (DisplayOption) -> Unit` вместо MutableState
- [ ] `CreateEditFormContent` — адаптировать вызов `rememberOnCreateEditValueChange`: передавать `uiStateMutable` вместо `params`; вызов `ObserveReminderStateOnResume`: передавать `isReminderEnabled: Boolean` + `onReminderDisabled: () -> Unit` вместо `params` (см. B.2.8)
- [ ] `previousReminderEnabled` — адаптировать: локальный `remember { mutableStateOf(uiState.value.reminder.isEnabled) }` вместо `rememberSaveable { mutableStateOf(params.uiStates.reminder.isEnabled.value) }`
- [ ] `rememberReminderToggleHandler` — переписать на работу с `onReminderChange: (ReminderFormUiState) -> Unit` вместо прямого доступа к `.value`:

  ```kotlin
  // Было: params.uiStates.reminder.isEnabled.value = true
  // Стало: onReminderChange(reminder.copy(isEnabled = true))
  ```

- [ ] `loadItemData` — переписать на работу с `MutableState<CreateEditUiState>.value = value.copy(...)`:

  ```kotlin
  fun loadItemData(
      itemId: Long?,
      uiState: CreateEditScreenState,
      uiStateMutable: MutableState<CreateEditUiState>
  ) {
      val successState = uiState as? CreateEditScreenState.Success ?: return
      if (itemId != null && uiStateMutable.value.title.isEmpty()) {
          uiStateMutable.value = uiStateMutable.value.copy(
              title = successState.item.title,
              details = successState.item.details,
              ...
          )
      }
      if (itemId != null && !uiStateMutable.value.reminder.isInitializedFromSource) {
          uiStateMutable.value = uiStateMutable.value.copy(
              reminder = uiStateMutable.value.reminder.applyReminder(successState.reminder)
          )
      }
  }
  ```

##### B.2.5 `CreateEditSelectors.kt` — адаптация

- [ ] Убрать `import androidx.compose.runtime.MutableState`
- [ ] `ColorSelector(selectedColor: Color?, onColorSelected: (Color?) -> Unit)` — убрать MutableState
- [ ] `ColorOptionSurface(color: Color, selectedColor: Color?, onColorSelected: (Color?) -> Unit)` — убрать MutableState

  ```kotlin
  onClick = {
      if (isSelected) {
          onColorSelected(null)
      } else {
          onColorSelected(color)
      }
      onValueChange()
  }
  ```

- [ ] `DisplayOptionSelector(selectedDisplayOption: DisplayOption, onDisplayOptionSelected: (DisplayOption) -> Unit)` — убрать MutableState
- [ ] `isCustomColor()` — проверить, не зависит ли от MutableState (должна быть чистой функцией)
- [ ] Обновить превью: `val selectedColor = remember { mutableStateOf<Color?>(...) }` → `var selectedColor by remember { mutableStateOf(...) }` и передача plain-значения + callback

##### B.2.6 `CreateEditButtons.kt` — адаптация

- [ ] Убрать `import androidx.compose.runtime.MutableState`
- [ ] `DatePickerDialogSection(selectedDate: LocalDate?, showDatePicker: Boolean, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit)` — убрать MutableState

  ```kotlin
  onClick = {
      datePickerState.selectedDateMillis?.let { millis ->
          onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate())
      }
      onDismiss()
  }
  ```

##### B.2.7 `CreateEditScreen.kt` — адаптация

- [ ] Убрать `import androidx.compose.runtime.mutableStateOf` (может остаться для `remember { mutableStateOf(...) }` на уровне Screen)
- [ ] `CreateEditScreenContent`:
  - `val uiStateMutable = rememberCreateEditUiState()` (один MutableState)
  - `val showDatePicker = rememberSaveable { mutableStateOf(false) }` → `var showDatePicker by rememberSaveable { mutableStateOf(false) }` (plain Boolean через делегат)
  - Передавать в `CreateEditFormParams` plain-значения + callback'и
- [ ] `CreateEditDatePickerIfNeeded(shouldShowDatePicker: Boolean, selectedDate: LocalDate?, onDateSelected: (LocalDate) -> Unit, onDismiss: () -> Unit)` — убрать MutableState
- [ ] `CreateEditUiState.toItem()` — читать plain-поля:

  ```kotlin
  private fun CreateEditUiState.toItem(itemId: Long?): Item {
      val timestamp = selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
          ?: System.currentTimeMillis()
      return Item(
          id = itemId ?: 0L,
          title = title,
          details = details,
          timestamp = timestamp,
          colorTag = selectedColor?.toArgb(),
          displayOption = selectedDisplayOption
      )
  }
  ```

- [ ] `rememberCreateEditDateSelectedAction` — читать plain-поля
- [ ] `rememberCreateEditSaveAction` — читать plain-поля
- [ ] `rememberCreateEditScreenActions` — читать plain-поля из `uiStateMutable.value`

##### B.2.8 `CreateEditReminderEffects.kt` — адаптация

- [ ] `rememberOnCreateEditValueChange` — переписать на работу с `MutableState<CreateEditUiState>`:

  ```kotlin
  @Composable
  internal fun rememberOnCreateEditValueChange(
      itemId: Long?,
      state: MutableState<CreateEditUiState>,
      viewModel: CreateEditScreenViewModel
  ): () -> Unit = {
      if (itemId != null) {
          val s = state.value
          val timestamp = s.selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L
          viewModel.checkHasChanges(
              CreateEditChangeInput(
                  title = s.title,
                  details = s.details,
                  timestamp = timestamp,
                  colorTag = s.selectedColor?.toArgb(),
                  displayOption = s.selectedDisplayOption,
                  reminderFingerprint = s.reminder.toChangeFingerprint()
              )
          )
      }
  }
  ```

- [ ] `ObserveReminderStateOnResume` — переписать:

  ```kotlin
  @Composable
  internal fun ObserveReminderStateOnResume(
      isReminderEnabled: Boolean,
      onReminderDisabled: () -> Unit,
      onReminderNotificationsUnavailable: () -> Unit
  ) { ... }
  ```

  Вместо `params.uiStates.reminder.isEnabled.value` используем `isReminderEnabled`.
  Вместо `params.uiStates.reminder.isEnabled.value = false` вызываем `onReminderDisabled()`.

##### B.2.9 `ReminderSettingsSection.kt` — адаптация

- [ ] Убрать `import androidx.compose.runtime.mutableStateOf` (может остаться для `showUnitsMenu`)
- [ ] `ReminderSettingsSection` — принимать `reminder: ReminderFormUiState` (plain) + `onReminderChange: (ReminderFormUiState) -> Unit`

  ```kotlin
  internal fun ReminderSettingsSection(
      reminder: ReminderFormUiState,
      onReminderChange: (ReminderFormUiState) -> Unit,
      onValueChange: () -> Unit,
      ...
  )
  ```

- [ ] Все внутренние функции (`ReminderToggleRow`, `ReminderExpandedContent`, `ReminderDateTimeSection`, `ReminderTimeField`, `ReminderAfterSection`):
  - Принимать plain-значения + callback'и
  - Вместо `reminderUiState.isEnabled.value = false` → `onReminderChange(reminder.copy(isEnabled = false))`
  - Вместо `reminderUiState.hour.value = hour` → `onReminderChange(reminder.copy(hour = hour))`
- [ ] Обновить превью — использовать `var reminder by remember { mutableStateOf(...) }` с делегатом

##### B.2.10 `StateSavers.kt` — адаптация

Возможные варианты:
- **Вариант A:** Создать `CreateEditUiStateSaver` и `ReminderFormUiStateSaver`, композиция из существующих Saver'ов
- **Вариант B:** Если `rememberSaveable` может сохранить data class через `Parcelable`/`Serializable` (или kotlinx.serialization) — упростить до минимума
- **Вариант C:** Если data class состоит только из примитивов + `LocalDate?`/`Color?`/`DisplayOption`, сохранить Saver'ы только для этих типов, а для контейнера использовать `listSaver` или `mapSaver`

**Рекомендуемый подход:** Создать `CreateEditUiStateSaver` через композицию существующих Saver'ов + примитивные Saver'ы для String/Boolean/Int. Файл `StateSavers.kt` переименовать или расширить.

#### B.3 REFACTOR

- [ ] Удалить неиспользуемые Saver'ы (если после миграции остались без референсов)
- [ ] Проверить, что `import androidx.compose.runtime.mutableStateOf` остался только в Composable-файлах (там, где MutableState действительно нужен на верхнем уровне)
- [ ] Проверить отсутствие `import androidx.compose.runtime.MutableState` в файлах, где он не нужен
- [ ] Упростить валидацию — функции над plain-значениями без `.value`
- [ ] Обновить превью во всех затронутых файлах

#### Критерий завершения B

- [ ] `CreateEditUiState` — data class без MutableState (5 plain-полей)
- [ ] `ReminderFormUiState` — data class без MutableState (9 plain-полей)
- [ ] `CreateEditFormParams` — без MutableState (plain-поля + callback'и)
- [ ] Все Composable-функции в `CreateEditFormContent`, `CreateEditSelectors`, `CreateEditButtons`, `CreateEditScreen`, `CreateEditReminderEffects`, `ReminderSettingsSection` — принимают plain-значения + callback'и вместо MutableState
- [ ] `applyReminder` стала чистой функцией, возвращает `ReminderFormUiState`
- [ ] `rememberCreateEditUiStates()` → `rememberCreateEditUiState()` возвращает один `MutableState<CreateEditUiState>` (единственный MutableState во всей форме)
- [ ] `CreateEditUiStateTest.kt` — 14 тестов переписаны на plain data class, без `mutableStateOf`, без `.value`
- [ ] `CreateEditReminderStateTest.kt` — 14 тестов переписаны на plain data class, без `mutableStateOf`, без `.value`
- [ ] `CreateEditScreenCustomColorTest.kt` — 2 UI-теста работают без `MutableState<Color?>` в test-контенте
- [ ] `make format` проходит
- [ ] Таргетные тесты CreateEdit зелёные

### 5.3 Инкремент C: Исправление замечаний после рефакторинга ⬜ (выполняется)

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

### 5.4 Инкремент D: Архитектурная стабилизация (guard от регрессии)

- [ ] Добавить тест/проверку, что `CreateEditUiState` и `ReminderFormUiState` не содержат `MutableState<T>`.
- [ ] Проверить, что архитектурный паттерн единообразен с остальными экранами.
- [ ] Обновить документацию по паттерну состояния.

#### Критерий завершения D

- [ ] Новый паттерн формализован и защищён от регрессии.

### 5.5 Инкремент E: Финальная регрессия и приемка

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

## 6. Риски и меры снижения

- [x] **Риск поломки сохранения состояния при повороте экрана.**
  Мера: тесты на recreation + `rememberSaveable` только в UI-слое.
  Статус: остальные экраны уже используют этот паттерн, риск минимален.
- [ ] **Риск изменения логики валидации/hasChanges.**
  Мера: сохранить unit-тесты на `isCreateEditFormValid`, `isInputValid`, `toReminderRequest`, `toChangeFingerprint` при переходе на plain-поля.
- [ ] **Риск скрытых регрессий в напоминаниях.**
  Мера: `CreateEditReminderStateTest` — критичный набор, должен быть зелёным после миграции.
- [x] **Риск влияния на backup.**
  Мера: backup-модели не затрагиваются; обязательный прогон backup-тестов на финальном этапе.

## 7. Definition of Done

- [x] В проекте нет UI-state с `MutableState<T>` внутри data-классов, **кроме CreateEdit**.
- [x] После миграции: `CreateEditUiState` и `ReminderFormUiState` не содержат `MutableState<T>`.
- [ ] Все затронутые ViewModel/Compose экраны работают на иммутабельном контракте.
- [ ] `make format` и `make test` проходят.
- [ ] Напоминания, backup/restore и существующая навигация не деградировали.
- [ ] Документация по состояниям обновлена.
