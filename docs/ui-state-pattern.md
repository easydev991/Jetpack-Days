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

## Когда что использовать

| Уровень | Механизм | Примеры |
|---------|----------|---------|
| ViewModel → UI | `StateFlow<T>` + `stateIn()` | Список событий, детали записи, тема |
| Composable (сохраняемое) | `rememberSaveable` + Saver | `CreateEditUiState`, `selectedColor` |
| Composable (временное) | `remember` | `showUnitsMenu`, `expandedSection` |
| UI-события | Callback'и (лямбды) | `onDeleteClick`, `onSave`, `onBack` |
| Snackbar/Toast | `SharedFlow<T>` + `LaunchedEffect` | Сообщения об ошибках, подтверждения |

## Где находится MutableState

Ровно одно место в композиции на экран:

```kotlin
var uiState by remember { mutableStateOf(defaultState) }
// или
var uiState by rememberSaveable(stateSaver = ...) { mutableStateOf(defaultState) }
```

Все мутации — через `uiState = uiState.copy(...)`.

## Пример (Create/Edit экран)

```kotlin
// 1. Data class — чистый, без MutableState
data class CreateEditUiState(
    val toolbarTitleResId: Int = R.string.create_event,
    val selectedDate: LocalDate? = null,
    val showDatePicker: Boolean = false,
    ...
)

// 2. Единственный MutableState на уровне экрана
@Composable
fun CreateEditScreen(...) {
    var formState by rememberSaveable(stateSaver = CreateEditUiStateSaver) {
        mutableStateOf(CreateEditUiState())
    }
    // ...
}

// 3. Мутация через copy()
formState = formState.copy(selectedDate = date, showDatePicker = false)
```

## Проверка при code review

- В data-классах UI-state нет `import androidx.compose.runtime.MutableState`
- Нет `import` `getValue`/`setValue` в state-файлах
- Все мутации через `copy()`
- Saver сериализует/десериализует plain-поля без MutableState
- ViewModel использует `StateFlow`, не хранит `MutableState`

## Навигация

Параметры навигации (itemId) передаются через `SavedStateHandle` с `checkNotNull`:

```kotlin
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) {
    "itemId parameter is required"
}
```
