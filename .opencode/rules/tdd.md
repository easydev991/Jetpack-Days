# Правила TDD (Test-Driven Development)

## Порядок разработки

**Важно:** Строго соблюдать порядок:

**1.** Тесты → **2.** Логика → **3.** UI

**1.** Писать модульные тесты для бизнес-логики перед реализацией
**2.** Реализовывать функциональность слоя домена и данных перед UI
**3.** Проверять бизнес-логику тестами перед разработкой UI
**4.** Создавать Compose UI только после тестирования логики

## Пирамида тестирования

- Модульные тесты (70%)
- Интеграционные тесты (20%)
- UI тесты (10%)

## Именование тестов

```kotlin
@Test
fun functionName_whenCondition_thenExpectedResult() {
    // Given
    // When
    // Then
}
```

## Пример

```kotlin
class DaysCalculatorTest {
    @Test
    fun calculateDaysDifference_whenSameDay_thenReturnsZero() {
        // Given
        val date = LocalDate.now()

        // When
        val result = DaysCalculator.calculateDaysDifference(date, date)

        // Then
        assertEquals(0, result)
    }
}
```

## Цикл TDD

**1. Red**: Написать падающий тест
**2. Green**: Написать минимальный код для прохождения теста
**3. Refactor**: Улучшить код, сохраняя тесты зелеными

## Принципы

- Тесты пишутся первыми
- Один тест - одна проверка
- Тесты независимы друг от друга
- Тесты быстрые и читаемые

## Конвенции Room

- **Миграция в паре с MigrationTest:** каждая новая `Migration` в `data/database/` коммитится вместе с `MigrationTest` (androidTest, `MigrationTestHelper`) и новым `N.json` в `app/schemas/`. Миграция без теста не принимается.
- **Простые изменения схемы** (новая колонка / таблица / индекс без перестройки данных) — предпочтителен `@AutoMigration`, рукописный SQL — только для сложных случаев.
- **Мок-юниты на SQL миграций запрещены:** `verify { database.execSQL(...) }` с `relaxed = true` не ловит синтаксические ошибки и создаёт ложное покрытие. Только интеграционный тест через `MigrationTestHelper`.

**Важно:** Подробные правила тестирования, включая запрет на интеграционные тесты ViewModels и рабочий подход, см. в `.agents/skills/testing/SKILL.md`