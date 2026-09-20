# Fakes — Fake-репозитории на MutableStateFlow

Для `ItemRepository`, `ReminderRepository` и других интерфейсов,
отдающих `Flow`, в проекте принят паттерн **Fake** на `MutableStateFlow`.
Это надёжнее `mockk() returns flowOf(...)`, потому что эмиссия
детерминирована и не зависит от порядка `collect`.

## Почему Fake, а не Mock

| Подход | Когда использовать |
|---|---|
| `mockk() returns flowOf(...)` | Один-два вызова, не реактивно |
| **Fake на MutableStateFlow** | `ItemRepository`, `ReminderRepository` — эмиссии, реактивные комбинации, несколько подписчиков |
| `mockk(relaxed = true)` | `Logger`, `DataStore`, `ResourceProvider` — простые stateless интерфейсы |

Проблема `mockk + flowOf`: `flowOf(...)` — холодный. Если в
ViewModel несколько `combine` / `flatMapLatest`, эмиссия может
произойти до того, как подписчик `collect`-ит — тест будет flaky.

## Канонический `FakeItemRepository`

```kotlin
private class FakeItemRepository : ItemRepository {
    private val _items = MutableStateFlow<List<Item>>(emptyList())

    fun setItems(items: List<Item>) {
        _items.value = items
    }

    fun containsItem(id: Long): Boolean = _items.value.any { it.id == id }

    override fun getAllItems(): Flow<List<Item>> = _items

    override fun getAllItems(sortOrder: SortOrder): Flow<List<Item>> =
        _items.map { items ->
            when (sortOrder) {
                SortOrder.ASCENDING -> items.sortedWith(compareBy({ it.timestamp }, { it.id }))
                SortOrder.DESCENDING -> items.sortedWith(
                    compareByDescending<Item> { it.timestamp }.thenByDescending { it.id }
                )
            }
        }

    override suspend fun getItemById(id: Long): Item? =
        _items.value.find { it.id == id }

    override fun getItemFlow(id: Long): Flow<Item?> =
        _items.map { items -> items.find { it.id == id } }

    override fun searchItems(query: String): Flow<List<Item>> =
        _items.map { items ->
            items.filter { item ->
                item.title.contains(query, ignoreCase = true) ||
                    item.details.contains(query, ignoreCase = true)
            }
        }

    override suspend fun insertItem(item: Item): Long {
        val newId = (_items.value.maxOfOrNull { it.id } ?: 0) + 1
        val newItem = item.copy(id = newId)
        _items.value = _items.value + newItem
        return newId
    }

    override suspend fun updateItem(item: Item) {
        _items.value = _items.value.map { if (it.id == item.id) item else it }
    }

    override suspend fun deleteItem(item: Item) {
        _items.value = _items.value.filterNot { it.id == item.id }
    }

    override suspend fun deleteAllItems() {
        _items.value = emptyList()
    }

    override suspend fun getItemsCount(): Int = _items.value.size
}
```

Тонкости:
- `private class` — внутри тест-класса, не выноси в общий файл.
- Метод `setItems(items)` — публичный для теста, не часть интерфейса.
- Дополнительные геттеры (`containsItem`) — допустимы,
  если упрощают assertions.
- `_items.value = newList` — триггерит эмиссию во всех подписчиках.

## Канонический `FakeReminderDao`

Для data-слоя (тестирование `ReminderRepositoryImpl`):

```kotlin
private class FakeReminderDao : ReminderDao {
    private val storage = linkedMapOf<Long, ReminderEntity>()

    override suspend fun getReminderByItemId(itemId: Long): ReminderEntity? = storage[itemId]

    override suspend fun getFutureActiveReminders(nowEpochMillis: Long): List<ReminderEntity> =
        storage.values.filter {
            it.status == ReminderStatus.ACTIVE.name &&
                it.targetEpochMillis > nowEpochMillis
        }

    override suspend fun upsertReminder(reminder: ReminderEntity) {
        storage[reminder.itemId] = reminder
    }

    override suspend fun updateStatus(itemId: Long, status: String, updatedAt: Long) {
        val current = storage[itemId] ?: return
        storage[itemId] = current.copy(status = status, updatedAt = updatedAt)
    }

    override suspend fun deleteByItemId(itemId: Long) {
        storage.remove(itemId)
    }
}
```

## Другие Fakes

**`FakeReminderManager`** — для тестов `DetailScreenViewModel`:

```kotlin
private class FakeReminderManager : ReminderManager {
    var activeReminder: Reminder? = null
    val clearedItemIds = mutableListOf<Long>()

    override suspend fun saveReminder(request: ReminderRequest, itemTitle: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun clearReminder(itemId: Long) {
        clearedItemIds += itemId
        activeReminder = null
    }

    override suspend fun getActiveReminder(itemId: Long): Reminder? =
        activeReminder?.takeIf { it.itemId == itemId }

    override suspend fun consumeReminder(itemId: Long) = Unit
    override suspend fun rescheduleFutureReminders() = Unit
}
```

Использование в тесте:

```kotlin
reminderManager.activeReminder = futureReminder
// ...
assertEquals(listOf(testItemId), reminderManager.clearedItemIds)
```

**`FakeReminderScheduler`** — для `DefaultReminderManagerTest`:

```kotlin
private class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<ScheduledReminder>()
    var lastCancelledItemId: Long? = null

    override fun schedule(reminder: ScheduledReminder) {
        scheduled += reminder
    }

    override fun cancel(itemId: Long) {
        lastCancelledItemId = itemId
    }
}
```

## Где размещать Fakes

**Внутри тест-класса как `private class`** — основной путь.
Плюсы: видно полный контракт, нет утечек между тестами,
не загрязняет `app/src/test/java/`.

Выносить в общий файл (`test/fakes/`) — **только если** один и тот же
Fake используется в 3+ тест-классах. Порог формально пройден:
`FakeItemRepository` сейчас дублируется в 5 тест-классах:

- `MainScreenViewModelTest`
- `DetailScreenViewModelTest`
- `CreateEditScreenViewModelTest` (вариация `FakeItemRepositoryWithLoggingDisabled`)
- `CreateEditScreenViewModelReminderTest`
- `DefaultReminderManagerTest`

Но выносить не стали — каждая версия подогнана под свой набор
assertions (например, `containsItem` есть только в
`DetailScreenViewModelTest`, отключённый логгер — только в
`CreateEditScreenViewModelTest`). Решение по выносу принимать
по месту при появлении 6+ дублирований или при существенных
изменениях контракта `ItemRepository`.

## Именование Fake-классов

- `Fake<InterfaceName>` — стандартный префикс.
- `Fake<InterfaceName>With<Modifier>` — если есть вариант:
  `FakeItemRepositoryWithLoggingDisabled` (для
  `CreateEditScreenViewModelTest`, где нужно убедиться, что логгер
  не вызывается).

## Когда Fake НЕ нужен

- Repository не использует Flow (только suspend `Result<T>`) —
  хватит `mockk()` + `coEvery`.
- Repository возвращает данные из реального источника (DAO, сеть) —
  это integration test, не unit.
- Один метод, одна проверка — `mockk(relaxed = true)` быстрее
  написать и читать.
