---
name: kotlin-testing
description: >
  Экспертное руководство по unit-тестированию в Android-проекте JetpackDays:
  JUnit 5, MockK, Fake-репозитории на MutableStateFlow, kotlinx-coroutines-test
  для ViewModel и Use Case, Given-When-Then, проектные соглашения
  (snake_case, без !!, русские сообщения, NoOpLogger, StubResourceProvider),
  а также ограничения (запрет новых интеграционных тестов ViewModel).
  Использовать при написании новых unit-тестов, рефакторинге существующих,
  отладке flaky-тестов и улучшении качества покрытия в проекте.
---

# Kotlin Testing — kotlin-testing skill

## Overview

Unit-тесты Kotlin-кода в JetpackDays. Стек: **JUnit 5**
(`org.junit.jupiter.api.*`), **MockK** (`io.mockk.*`),
**kotlinx-coroutines-test** (`runTest`, `StandardTestDispatcher`,
`Dispatchers.setMain` / `resetMain`) и **Fake-репозитории** на
`MutableStateFlow` для Flow-эмиссий. Цель — читаемые, быстрые,
независимые тесты (70% тестовой пирамиды).

> Интеграционные и UI тесты — `app/src/androidTest/` (JUnit 4,
> Compose Testing, Room in-memory, Turbine). Этот skill про `app/src/test/`.

## Agent behavior contract

1. **JUnit 5** — импорты только из `org.junit.jupiter.api.*`
   (детали — `references/fundamentals.md`).
2. **Given / When / Then в каждом тесте** — три маркера-комментария
   (`references/fundamentals.md`).
3. **snake_case в именах тестов, без обратных кавычек** — формат и
   допустимые вариации — `references/fundamentals.md`.
4. **Никакого `!!`** — `?.`, `?:`, `let`, `checkNotNull`. Требование
   AGENTS.md (примеры — `references/project-conventions.md`).
5. **MockK — для stateless-интерфейсов** (`Logger`, `DataStore`,
   `ResourceProvider`); **Fake на `MutableStateFlow` — для
   `ItemRepository`** и любых Flow-эмиссий (`references/fakes.md`);
   `ReminderRepositoryImpl` тестируется через `FakeReminderDao`
   (`references/data-layer.md`).
6. **`coEvery` / `coVerify` — для suspend-методов; `every` / `verify` —
   для не-suspend, включая Flow-возвращающие** (`references/mocking-mockk.md`).
7. **ViewModel — только unit-тест.** Новые интеграционные тесты
   ViewModel запрещены (`references/project-conventions.md`).
8. **`StandardTestDispatcher` + `Dispatchers.setMain` / `resetMain` +
   `@OptIn(ExperimentalCoroutinesApi::class)`** в каждом
   ViewModel-тесте (`references/viewmodel-testing.md`).
9. **`advanceUntilIdle()` после каждого действия ViewModel** — иначе
   корутины не отработают (`references/viewmodel-testing.md`).
10. **Use Case возвращают `Result<T>`** — assert-паттерны
    (`references/use-cases.md`).
11. **`Logger` в тестах ViewModel — `NoOpLogger` или
    `mockk(relaxed = true)`, не смешивать**
    (`references/project-conventions.md`).

## Запуск тестов

- `make test` — прогон с человекочитаемым отчётом
  (`scripts/test_report.py`); `make check` — build + test + lint.
- Одиночные классы/методы, фильтрация, где Gradle кладёт результаты —
  `references/running-tests.md`.

## First 60 seconds (triage template)

Прежде чем писать код, собери факты:

- **Цель**: новые unit-тесты, миграция с другого подхода, flaky failures,
  новый ViewModel, новый Use Case, рефакторинг?
- **Факты**:
  - Слой: domain model / Use Case / ViewModel / repository / provider / mapper?
  - Flow / StateFlow / suspend? Это меняет диспетчер и выбор Fake vs Mock.
  - I/O: DAO, ContentResolver, Clock, system time? Это меняет Fake vs Mock vs Stub.
  - TDD или покрытие существующего кода?
  - Room / DataStore / Android Context напрямую? Это уже integration, не unit.

## Routing map (читай нужный reference быстро)

- Анатомия теста, именование, Given/When/Then →
  `references/fundamentals.md`
- MockK: `every`, `coEvery`, `verify`, `coVerify`, `relaxed`, `slot`,
  `capture`, `answers`, `throws` → `references/mocking-mockk.md`
- Fake-репозитории на `MutableStateFlow` →
  `references/fakes.md`
- ViewModel: диспетчер, `setMain` / `resetMain`,
  `advanceUntilIdle`, `SavedStateHandle` → `references/viewmodel-testing.md`
- Use Case: `Result<T>`, `Clock.fixed`, `currentDate` →
  `references/use-cases.md`
- Data layer: mapper, converter, provider, JSON-ресурсы →
  `references/data-layer.md`
- Assertions JUnit 5 + sealed-class проверки →
  `references/assertions.md`
- Проектные конвенции JetpackDays →
  `references/project-conventions.md`
- Запуск `make test`, одиночные тесты, отчёт →
  `references/running-tests.md`
- Полные канонические примеры → `references/EXAMPLE.md`

## Hot pitfalls → next best move

| Проблема | Решение |
|---|---|
| Тест ViewModel зависает | Не `runBlocking` с `viewModelScope.launch` — `runTest` + `StandardTestDispatcher` + `advanceUntilIdle()` (`references/viewmodel-testing.md`) |
| `flowOf(...)` не доезжает до UI state | Заменить `mockk()` для `ItemRepository` на Fake на `MutableStateFlow` (`references/fakes.md`) |
| `coEvery { repo.getAllItems() }` не компилируется | Метод не suspend — нужен `every` (правило 6) |
| `StateFlow` с `WhileSubscribed` отдаёт initialValue | Подпишись до `advanceUntilIdle()`: `backgroundScope.launch { flow.collect { } }` — см. `references/viewmodel-testing.md`, раздел «StateFlow с `WhileSubscribed`» |
| Mockito в импортах | Только MockK (`references/project-conventions.md`) |

Типичные ошибки по слоям: `references/viewmodel-testing.md` и
`references/use-cases.md` (таблицы), `references/mocking-mockk.md`
(раздел «Типичные ошибки»), `references/assertions.md` («Что НЕ делать»).

## Verification checklist

- [ ] Импорты из `org.junit.jupiter.api.*`; имя теста в snake_case без кавычек
- [ ] `// Given`, `// When`, `// Then` в каждом тесте
- [ ] Нет `!!`; сообщения assert на русском
- [ ] ViewModel: Fake-репозиторий, `setMain`/`resetMain`, `advanceUntilIdle()`
- [ ] suspend → `coEvery`/`coVerify`; Flow-методы → `every`/`verify`
- [ ] Время — `Clock.fixed(...)` или явный `currentTimeMillisProvider`/`currentDate`
- [ ] Файл в `app/src/test/` зеркалит `app/src/main/`; класс оканчивается на `Test`
- [ ] Нет новых интеграционных тестов ViewModel

## References

- `references/fundamentals.md`
- `references/mocking-mockk.md`
- `references/fakes.md`
- `references/viewmodel-testing.md`
- `references/use-cases.md`
- `references/data-layer.md`
- `references/assertions.md`
- `references/project-conventions.md`
- `references/running-tests.md`
- `references/EXAMPLE.md`
