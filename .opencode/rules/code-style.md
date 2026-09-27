# Стиль кода

## Kotlin

- Data классы для моделей; sealed классы для состояний/результатов
- Extension functions для читаемости
- **Никогда `!!`**: `?`, `?:`, `let`, `checkNotNull` — нарушение ловит detekt (`UnsafeCallOnNullableType`)

```kotlin
private val itemId: Long = checkNotNull(savedStateHandle["itemId"]) { "ItemId parameter is required" }
repository.getItemById(itemId)?.let { item -> /* ... */ }
val icon = screen.icon ?: defaultIcon
```

## Compose

- `State`/`MutableState` для UI состояния, однонаправленный поток данных
  (состояние течёт вниз, события — вверх)
- `ViewModel` для состояния UI; `CompositionLocal` только для темы/глобальной конфигурации

```kotlin
@Preview
@Composable
fun ComponentPreview() {
    jetpackDaysTheme {
        Component()
    }
}
```

## Обработка ошибок

- Use Cases — стандартный `Result<T>`, исключения мапятся в доменные
  (`BackupException`, `ItemException`):

```kotlin
suspend operator fun invoke(uri: Uri): Result<Int> =
    try {
        Result.success(items.size)
    } catch (e: IOException) {
        Result.failure(BackupException("Не удалось экспортировать данные: ${e.message}", e))
    }
```

- UI-состояния — sealed классы (`Loading` / `Success(item)` / `Error(message)`);
  простое состояние — data class (`RootScreenState`)

## Навигация

Маршруты — в `navigation/Screen.kt`: sealed class с `route` и опциональными
`icon`/`titleResId`, хелперы `createRoute(...)` для параметризованных маршрутов
(живой пример — `Screen.ItemDetail`).

## Комментарии

- KDoc для публичных API; объяснять «почему», не «что»
- Логи на русском; сообщения пользователю — только через `ResourceProvider`
