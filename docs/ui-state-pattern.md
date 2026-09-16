# Паттерн управления UI-состоянием

**Цель:** Единый архитектурный подход к управлению UI-состоянием во всех экранах приложения.
Паттерн обеспечивает иммутабельность, предсказуемость и тестируемость.

## Основные принципы

1. **Data-классы без MutableState.** Все UI-state модели — обычные `data class` без полей
   типа `MutableState<T>`. Состояние иммутабельно и изменяется только через `copy()`.
2. **StateFlow в ViewModel.** ViewModel хранит состояние в `StateFlow<T>`. Два фактических
   варианта:
   - sealed-состояния экрана (`Loading`/`Success`/`Error`) — через
     `MutableStateFlow<T>` + `asStateFlow()` (`DetailScreenViewModel`, `MainScreenViewModel`,
     `CreateEditScreenViewModel`);
   - флоу из DataStore/репозиториев — через `stateIn(viewModelScope, WhileSubscribed(5000), ...)`
     (`MainActivityViewModel`, `AppDataScreenViewModel`, `ThemeIconViewModel`).
3. **remember/rememberSaveable в Compose.** Для локального состояния экрана (не управляемого
   ViewModel) используется `remember` или `rememberSaveable` с кастомным Saver.
4. **Unidirectional data flow.** Состояние течёт вниз (в Composable через параметры),
   события — вверх (через callback'и).

## Иерархия состояний

```
ViewModel (StateFlow) ──► Compose Screen
                                │
                     ┌──────────┴──────────┐
                     │                     │
             ViewModel-управляемые    Локальные состояния
             состояния               (remember/rememberSaveable)
             (список событий,         ─ showDatePicker
              детали записи,          ─ menuVisible / menuOffset
              тема, hasItems)         ─ form fields
```

Для сложной формы допускается один локальный state-holder на экран. Например,
`CreateEditScreen` хранит `MutableState<CreateEditUiState>` (через
`rememberCreateEditUiState()`), а вложенный блок напоминания находится внутри
`CreateEditUiState.reminder`.

## Где живут UI-state модели

| Место | Модели |
|-------|--------|
| `ui/state/` | `RootScreenState`, `AppDataUiState`, `ThemeIconUiState`, `MoreScreenUiState` |
| файл ViewModel | sealed-состояния экрана: `MainScreenState`, `DetailScreenState`, `CreateEditScreenState` |
| пакет экрана | `CreateEditUiState`, `ReminderFormUiState` (`ui/screens/createedit/`) |

## Когда что использовать

| Уровень | Механизм | Примеры |
|---------|----------|---------|
| ViewModel → UI (экранное состояние) | `MutableStateFlow<T>` + `asStateFlow()` | `MainScreenState`, `DetailScreenState` |
| ViewModel → UI (флоу настроек/БД) | `stateIn()` + `WhileSubscribed(5000)` | тема, `sortOrder`, `hasItems` |
| Composable (сохраняемое) | `rememberSaveable` + Saver | `CreateEditUiState`, `selectedColor` |
| Composable (временное) | `remember` | `menuVisible`, `menuOffset` (`DetailContent`) |
| UI-события | Callback'и (лямбды) | `onDeleteClick`, `onSave`, `onBack` |
| Toast | Поле `resultMessage` в UI state + `LaunchedEffect` | Сообщения об ошибках/успехе на `AppData` |

## Где находится MutableState

Ровно одно место в композиции на экран:

```kotlin
val uiState = rememberSaveable(stateSaver = SomeUiStateSaver) {
    mutableStateOf(defaultState)
}
```

Все мутации — через `uiState.value = uiState.value.copy(...)`.

## Пример (Create/Edit экран)

```kotlin
// 1. Data class — чистый, без MutableState
data class CreateEditUiState(
    val title: String = "",
    val details: String = "",
    val selectedDate: LocalDate? = null,
    val selectedColor: Color? = null,
    val selectedDisplayOption: DisplayOption = DisplayOption.DAY,
    val showDatePicker: Boolean = false,
    val reminder: ReminderFormUiState = ReminderFormUiState()
)

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

// 2. Единственный MutableState на уровне экрана
@Composable
fun CreateEditScreen(...) {
    val formState = rememberCreateEditUiState()
    // ...
}

// 3. Мутация через copy()
formState.value = formState.value.copy(selectedDate = date, showDatePicker = false)
formState.value = formState.value.copy(
    reminder = formState.value.reminder.copy(isEnabled = true)
)
```

Saver формы — `CreateEditUiStateSaver` (`ui/screens/createedit/StateSavers.kt`),
сериализует plain-поля без `MutableState`.

## Пример (sealed-состояние экрана)

Экранные состояния Loading/Success/Error — sealed-классы, объявлены рядом с ViewModel:

```kotlin
// DetailScreenViewModel.kt
sealed class DetailScreenState {
    data object Loading : DetailScreenState()

    data class Success(
        val item: Item,
        val reminder: Reminder? = null
    ) : DetailScreenState()

    data class Error(
        val message: String
    ) : DetailScreenState()
}

// ViewModel
private val _uiState = MutableStateFlow<DetailScreenState>(DetailScreenState.Loading)
val uiState: StateFlow<DetailScreenState> = _uiState.asStateFlow()
```

Для вариантов без общего полиморфного контракта используется sealed-класс в `ui/state/`:

```kotlin
// MoreScreenUiState.kt — состояние проверки обновлений
sealed class MoreScreenUiState {
    data object Idle : MoreScreenUiState()
    data object Checking : MoreScreenUiState()
    data object UpToDate : MoreScreenUiState()
    data class UpdateAvailable(val info: UpdateInfo) : MoreScreenUiState()
    data object Error : MoreScreenUiState()
}
```

## Проверка при code review

- В data-классах UI-state нет `import androidx.compose.runtime.MutableState`
- Нет `import` `getValue`/`setValue` в state-файлах
- Все мутации через `copy()`
- Saver сериализует/десериализует plain-поля без MutableState
- Дочерние Composable принимают plain-значения и callback'и, а не `MutableState<T>`
- Dialog-state закрывается и на confirm, и на dismiss, включая сценарии без выбранного значения
- ViewModel использует `StateFlow`, не хранит `MutableState` (кроме приватного
  `MutableStateFlow`-источника, наружу отдаётся только `asStateFlow()`)

## Навигация

Параметры навигации (itemId) передаются через `SavedStateHandle` с `checkNotNull`:

```kotlin
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) {
    "itemId parameter is required"
}
```
