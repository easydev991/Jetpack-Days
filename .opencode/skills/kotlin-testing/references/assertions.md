# Assertions

JUnit 5 assertions + Kotlin-идиомы для sealed-классов и data class.

## Импорт

```kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
```

`org.junit.Assert.*` — JUnit 4, не используй в unit-тестах.

## Базовые проверки

```kotlin
assertEquals(expected, actual, "Сообщение на русском")
assertEquals(expected, actual) { "Лямбда-сообщение — ленивое, для сложных случаев" }

assertTrue(condition, "Условие должно быть истинным")
assertFalse(condition, "Условие должно быть ложным")

assertNull(value, "Должно быть null")
assertNotNull(value, "Не должно быть null")
```

## Bool-проверки

```kotlin
// ПЛОХО — сравнение через ==
assertEquals(true, condition)
assertEquals(false, condition)

// ХОРОШО
assertTrue(condition)
assertFalse(condition)

// Для negated
assertTrue(!condition)         // или assertFalse(condition)
```

## Sealed class / интерфейс — `is` + доступ к полям

```kotlin
@Test
fun when_item_loaded_successfully_then_updates_to_success_state() = runTest {
    // ...
    val currentState = viewModel.uiState.value
    assertTrue(currentState is DetailScreenState.Success, "Состояние должно быть Success")
    val successState = currentState as DetailScreenState.Success
    assertEquals(testItem, successState.item, "Элемент должен совпадать")
    assertEquals(null, successState.reminder, "Активного напоминания быть не должно")
}
```

Преимущества над `assertEquals(Success(testItem), currentState)`:
- При провале сразу видно, **какой** подкласс ожидался
  (`Состояние должно быть Success`).
- Доступ к полям — без лишних cast'ов в data class equality.

## `assertTrue(x is Y)` vs `assertEquals(Y, x)`

| Стиль | Когда |
|---|---|
| `assertTrue(x is Y)` + cast | sealed/интерфейс, нужно достать поля для отдельных assert |
| `assertEquals(Y(expected), x)` | data class со всеми полями, equality достаточно |

`assertTrue(x is Y)` **обязательно** для проверки sealed перед
приведением, если ниже идёт `as Y` — иначе получишь
`ClassCastException` без понятного сообщения.

## Условный assert через Kotlin `assert`

Для проверок, которые не критичны для теста, но могут указать на
проблему:

```kotlin
assert(AppTheme.entries.contains(AppTheme.LIGHT)) { "Должно содержать LIGHT" }
assertEquals(3, AppTheme.entries.size, "Должны быть 3 значения темы")
```

`assert(...) { "..." }` — это Kotlin-stdlib `kotlin.assert`,
не JUnit. Используй только если assert JUnit 5 не подходит по
семантике (например, проверка в середине Given-блока).

## `assertThrows` для исключений

Лямбда `assertThrows` (`Executable`) не suspend — suspend-вызов
внутри неё не скомпилируется. Для suspend-функций внутри `runTest`
используй `runCatching` — она inline, suspend-вызов допустим:

```kotlin
@Test
fun invoke_whenJsonInvalid_thenThrowsSerializationException() = runTest {
    // Given
    val invalidJson = "{ invalid json }"
    val uri = mockk<Uri>()

    // When & Then
    val thrown = runCatching { useCase(uri, invalidJson) }.exceptionOrNull()
    assertTrue(thrown is SerializationException, "Должна быть SerializationException")
}
```

Для не-suspend кода — `assertThrows` с явным `::class.java`
(reified-форма `assertThrows<T> { }` — это другой импорт:
`org.junit.jupiter.api.assertThrows`). Он возвращает брошенное
исключение — можно проверить поля:

```kotlin
val ex = assertThrows(IllegalArgumentException::class.java) {
    AppTheme.valueOf("INVALID_THEME")
}
assertEquals("INVALID_THEME", ex.message)   // если нужно
```

## Групповые проверки

```kotlin
@Test
fun user_action_type_contains_all_expected_values() {
    assertEquals("create", UserActionType.CREATE.value)
    assertEquals("edit", UserActionType.EDIT.value)
    assertEquals("delete", UserActionType.DELETE.value)
    assertEquals("sort", UserActionType.SORT.value)
    // ...
}
```

Это серия независимых проверок в одном тесте — допустимо для
проверки enum-контракта, где одна ошибка enum-члена — это одна
логическая проблема.

## Сообщения на русском

Все assertion-сообщения — на русском, как в существующих тестах:

```kotlin
assertEquals(2, items.size, "Должно быть 2 элемента")
assertTrue(result is Success, "Состояние должно быть Success")
assertEquals("День рождения", item.title, "Название должно совпадать")
```

Русское сообщение полезнее всегда: пройденный тест не виден,
а проваленный читает разработчик — ему проще на русском.

## Что НЕ делать

- Не используй `assertEquals(true, x)` / `assertEquals(false, x)`.
- Не комментируй то, что уже говорит сообщение assertion:

  ```kotlin
  // ПЛОХО
  assertEquals(2, size)  // Должно быть 2
  // ХОРОШО
  assertEquals(2, size, "Должно быть 2 элемента")
  ```

- Не используй `assertTrue(x == null)` — `assertNull(x)` яснее.
- Не используй `org.junit.Assert.assertEquals` — это JUnit 4.
- Не глуши исключения в тесте: `try { ... } catch (e: Throwable) {}` —
  скрывает баги.
