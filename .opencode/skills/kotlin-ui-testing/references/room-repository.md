# Room and Repository — интеграционные тесты данных

Тесты слоя данных в `app/src/androidTest/java/com/dayscounter/data/`:
DAO, база, репозиторий. Используют реальную Room in-memory базу —
быстро, без устройства хранения, с полным SQL-движком.

Примеры:
- `data/database/dao/ItemDaoTest.kt` — полный набор CRUD/сортировки/поиска DAO
- `data/database/DaysDatabaseTest.kt` — создание базы
- `data/database/MigrationTest.kt` — миграции схемы
- `data/repository/ItemRepositoryIntegrationTest.kt` — CRUD + маппинг

## Скелет DAO-теста

Полный код — `references/EXAMPLE.md` (раздел «DAO-тест (Room in-memory)»).
Ключевое:

- `Room.inMemoryDatabaseBuilder(context, DaysDatabase::class.java)`
  `.allowMainThreadQueries().build()` — свежая БД на каждый тест
- `.allowMainThreadQueries()` — обязателен: запросы идут из `runBlocking`
  на main thread
- `@After` — `database.close()`, пересоздание в `@Before` каждого теста
  (в DAO-тестах `clearAllTables()` не нужен — база и так свежая)
- Flow-запросы тестируются через `.first()`:

```kotlin
val all = itemDao.getAllItems().first()
assertEquals(expectedCount, all.size)
```

## Репозиторий-интеграция

`ItemRepositoryIntegrationTest` — реальный `ItemRepositoryImpl(itemDao)`:

```kotlin
@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(...).allowMainThreadQueries().build()
    itemDao = database.itemDao()
    repository = ItemRepositoryImpl(itemDao)
}

@Test
fun fullCycle_createReadUpdateDelete_thenWorksCorrectly() =
    runBlocking {
        // Given
        val item = Item(title = "Тестовое событие", ...)

        // When
        val insertedId = repository.insertItem(item)
        val retrieved = repository.getItemById(insertedId)
        repository.updateItem(retrieved?.copy(title = "Обновлённое событие") ?: item)
        val updated = repository.getItemById(insertedId)

        // Then
        assertNotNull(retrieved)
        assertEquals("Тестовое событие", retrieved?.title)
        assertEquals("Обновлённое событие", updated?.title)

        // When
        repository.deleteItem(updated ?: item)

        // Then
        assertNull(repository.getItemById(insertedId))
    }
```

Тестируется:
- CRUD-цикл (create → read → update → delete)
- Поиск по title (`searchItems("День").first()`)
- Сортировка DESC, tiebreaker id DESC
- Маппинг Entity ↔ Domain (проверяются доменные `Item`, не `ItemEntity`)
- null-safe поля (`conversionWithNullColorTag_preservesNull`)

Правила:
- Репозиторий создаётся вручную: `ItemRepositoryImpl(itemDao)` — без DI
- Доменные модели в assertions — `Item`, а не `ItemEntity`
- Turbine не используется — обычные `assertEquals` / `assertNotNull`
- `runBlocking` для синхронных проверок
- Сортировка: `getAllItems_whenSameTimestamp_thenSortedByIdDesc` —
  сначала по timestamp DESC, при равенстве — по id DESC
