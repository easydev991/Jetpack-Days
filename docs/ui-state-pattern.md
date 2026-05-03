# Паттерн управления UI-состоянием

**Цель:** Единый архитектурный подход к управлению UI-состоянием во всех экранах приложения.
Паттерн обеспечивает иммутабельность, предсказуемость и тестируемость.

## Основные принципы

1. **Data-классы без MutableState.** Все UI-state модели — обычные `data class` без полей
   типа `MutableState<T>`. Состояние иммутабельно и изменяется только через `copy()`.
2. **StateFlow в ViewModel.** ViewModel хранит состояние в `StateFlow<T>` и предоставляет
   его Compose-слою через `stateIn()`.
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
             детали записи)          ─ selectedDate
                                     ─ showUnitsMenu
                                     ─ form fields
```

Для сложной формы допускается один локальный state-holder на экран. Например,
`CreateEditScreen` хранит `MutableState<CreateEditUiState>`, а вложенный блок
напоминания находится внутри `CreateEditUiState.reminder`.

## Когда что использовать

| Уровень | Механизм | Примеры |
|---------|----------|---------|
| ViewModel → UI | `StateFlow<T>` + `stateIn()` | Список событий, детали записи, тема |
| Composable (сохраняемое) | `rememberSaveable` + Saver | `CreateEditUiState`, `selectedColor` |
| Composable (временное) | `remember` | `previousReminderEnabled`, `expandedSection` |
| UI-события | Callback'и (лямбды) | `onDeleteClick`, `onSave`, `onBack` |
| Snackbar/Toast | `SharedFlow<T>` + `LaunchedEffect` | Сообщения об ошибках, подтверждения |

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
    val showDatePicker: Boolean = false,
    val reminder: ReminderFormUiState = ReminderFormUiState()
)

data class ReminderFormUiState(
    val isEnabled: Boolean = false,
    val selectedDate: LocalDate? = defaultReminderDate(),
    val showDatePicker: Boolean = false
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

## Проверка при code review

- В data-классах UI-state нет `import androidx.compose.runtime.MutableState`
- Нет `import` `getValue`/`setValue` в state-файлах
- Все мутации через `copy()`
- Saver сериализует/десериализует plain-поля без MutableState
- Дочерние Composable принимают plain-значения и callback'и, а не `MutableState<T>`
- Dialog-state закрывается и на confirm, и на dismiss, включая сценарии без выбранного значения
- ViewModel использует `StateFlow`, не хранит `MutableState`

## Навигация

Параметры навигации (itemId) передаются через `SavedStateHandle` с `checkNotNull`:

```kotlin
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) {
    "itemId parameter is required"
}
```
