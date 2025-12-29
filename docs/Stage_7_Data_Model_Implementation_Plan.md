# План реализации Этапа 7: Модель данных

## Обзор

Этап 7 является критическим блокером для остальных фич приложения. Реализация модели данных включает создание доменной модели, Room Entity, Database, DAO и необходимых конвертеров. Все компоненты должны быть реализованы с использованием подхода TDD (Test-Driven Development).

## Цель

Создать полноценную модель данных для работы с записями событий (Item), включая:
- Доменную модель (Domain layer)
- Room Entity и конвертеры (Data layer)
- Room Database и DAO (Data layer)
- Unit-тесты для всех компонентов

## Архитектурные принципы

- **Clean Architecture**: Разделение на слои Domain и Data
- **TDD**: Сначала тесты, затем реализация
- **Room**: Использование Room для локального хранения данных
- **Type Safety**: Использование enum для DisplayOption, безопасная работа с опционалами

---

## Шаг 1: Подготовка структуры папок и зависимостей

### Задачи:
1. ✅ Проверить наличие зависимостей Room в `gradle/libs.versions.toml`
2. ✅ Проверить наличие KSP плагина для компиляции Room
3. Создать структуру папок:
   - `app/src/main/java/com/dayscounter/domain/model/`
   - `app/src/main/java/com/dayscounter/domain/repository/`
   - `app/src/main/java/com/dayscounter/data/database/`
   - `app/src/main/java/com/dayscounter/data/repository/`
   - `app/src/test/java/com/dayscounter/domain/model/`
   - `app/src/test/java/com/dayscounter/data/database/`

### Результат:
- Структура папок создана
- Зависимости Room настроены (уже есть в проекте)

---

## Шаг 2: Создание enum DisplayOption (Domain layer)

### Задачи:
1. Создать enum `DisplayOption` в `domain/model/DisplayOption.kt`
   - Значения: `DAY`, `MONTH_DAY`, `YEAR_MONTH_DAY`
   - Значение по умолчанию: `DAY`
   - Метод для получения строкового представления (для сериализации в JSON)

2. Написать unit-тесты в `test/domain/model/DisplayOptionTest.kt`
   - Тест значений enum
   - Тест значения по умолчанию
   - Тест строкового представления

### Структура:
```kotlin
enum class DisplayOption {
    DAY,           // Соответствует "day" в JSON (iOS)
    MONTH_DAY,     // Соответствует "monthDay" в JSON (iOS)
    YEAR_MONTH_DAY; // Соответствует "yearMonthDay" в JSON (iOS)
    
    companion object {
        fun fromString(value: String): DisplayOption {
            // Поддержка camelCase формата iOS: "day", "monthDay", "yearMonthDay"
            return when (value.lowercase()) {
                "day" -> DAY
                "monthday", "month_day" -> MONTH_DAY
                "yearmonthday", "year_month_day" -> YEAR_MONTH_DAY
                else -> DEFAULT
            }
        }
        
        fun toJsonString(): String {
            // Возвращает camelCase формат для совместимости с iOS
            return when (this) {
                DAY -> "day"
                MONTH_DAY -> "monthDay"
                YEAR_MONTH_DAY -> "yearMonthDay"
            }
        }
        
        val DEFAULT = DAY
    }
}
```

### Критерии готовности:
- ✅ Enum создан с тремя значениями
- ✅ Метод `fromString` реализован
- ✅ Unit-тесты написаны и проходят
- ✅ Код соответствует стилю проекта (без `!!`, безопасное разворачивание)

---

## Шаг 3: Создание доменной модели Item (Domain layer)

### Задачи:
1. Создать data class `Item` в `domain/model/Item.kt`
   - Поля:
     - `id: Long` — уникальный идентификатор
     - `title: String` — название события (обязательное)
     - `details: String` — описание события (необязательное, по умолчанию "")
     - `timestamp: Long` — дата события в миллисекундах
     - `colorTag: Int?` — ARGB-цвет для цветовой метки (необязательное)
     - `displayOption: DisplayOption` — опция отображения дней (по умолчанию `DAY`)
   - Метод `makeDaysCount(currentDate: Long): String` — вычисляет и форматирует количество дней
     - **Примечание**: Реализация форматирования будет в Этапе 6, здесь только заглушка

2. Написать unit-тесты в `test/domain/model/ItemTest.kt`
   - Тест создания Item с обязательными полями
   - Тест создания Item со всеми полями
   - Тест значений по умолчанию
   - Тест метода `makeDaysCount` (заглушка, полная реализация в Этапе 6)

### Структура:
```kotlin
data class Item(
    val id: Long = 0L,
    val title: String,
    val details: String = "",
    val timestamp: Long,
    val colorTag: Int? = null,
    val displayOption: DisplayOption = DisplayOption.DEFAULT
) {
    fun makeDaysCount(currentDate: Long): String {
        // Заглушка, полная реализация в Этапе 6
        return "0 дней"
    }
}
```

### Критерии готовности:
- ✅ Data class создан со всеми полями
- ✅ Значения по умолчанию установлены
- ✅ Метод `makeDaysCount` объявлен (заглушка)
- ✅ Unit-тесты написаны и проходят
- ✅ Код соответствует стилю проекта

---

## Шаг 4: Создание Room Entity (Data layer)

### Задачи:
1. Создать Room Entity `ItemEntity` в `data/database/entity/ItemEntity.kt`
   - Аннотация `@Entity` с таблицей "items"
   - Первичный ключ: `id` с `autoGenerate = true`
   - Поля, соответствующие доменной модели
   - Конвертеры для `DisplayOption` и `Int?` (colorTag)

2. Создать TypeConverter для `DisplayOption` в `data/database/converters/DisplayOptionConverter.kt`
   - Метод `fromDisplayOption(DisplayOption): String`
   - Метод `toDisplayOption(String): DisplayOption`

3. Создать TypeConverter для `Int?` (colorTag) в `data/database/converters/ColorTagConverter.kt`
   - Метод `fromColorTag(Int?): Int?` (может быть null)
   - Метод `toColorTag(Int?): Int?` (может быть null)
   - Обработка null значений

4. Написать unit-тесты:
   - `test/data/database/converters/DisplayOptionConverterTest.kt`
   - `test/data/database/converters/ColorTagConverterTest.kt`
   - `test/data/database/entity/ItemEntityTest.kt`

### Структура:
```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val details: String = "",
    val timestamp: Long,
    @TypeConverters(ColorTagConverter::class)
    val colorTag: Int? = null,
    @TypeConverters(DisplayOptionConverter::class)
    val displayOption: String = DisplayOption.DAY.name
)

class DisplayOptionConverter {
    @TypeConverter
    fun fromDisplayOption(value: DisplayOption): String
    
    @TypeConverter
    fun toDisplayOption(value: String): DisplayOption
}

class ColorTagConverter {
    @TypeConverter
    fun fromColorTag(value: Int?): Int?
    
    @TypeConverter
    fun toColorTag(value: Int?): Int?
}
```

### Критерии готовности:
- ✅ Room Entity создана с правильными аннотациями
- ✅ TypeConverter для DisplayOption реализован
- ✅ TypeConverter для colorTag реализован
- ✅ Unit-тесты написаны и проходят
- ✅ Обработка null значений корректна

---

## Шаг 5: Создание маппера между Entity и Domain Model

### Задачи:
1. Создать extension функции для конвертации:
   - `ItemEntity.toDomain(): Item` в `data/database/mapper/ItemMapper.kt`
   - `Item.toEntity(): ItemEntity` в `data/database/mapper/ItemMapper.kt`

2. Написать unit-тесты в `test/data/database/mapper/ItemMapperTest.kt`
   - Тест конвертации Entity → Domain
   - Тест конвертации Domain → Entity
   - Тест конвертации с null значениями
   - Тест конвертации с разными DisplayOption

### Структура:
```kotlin
fun ItemEntity.toDomain(): Item {
    return Item(
        id = id,
        title = title,
        details = details,
        timestamp = timestamp,
        colorTag = colorTag,
        displayOption = DisplayOptionConverter().toDisplayOption(displayOption)
    )
}

fun Item.toEntity(): ItemEntity {
    return ItemEntity(
        id = id,
        title = title,
        details = details,
        timestamp = timestamp,
        colorTag = colorTag,
        displayOption = DisplayOptionConverter().fromDisplayOption(displayOption)
    )
}
```

### Критерии готовности:
- ✅ Мапперы созданы
- ✅ Корректная конвертация в обе стороны
- ✅ Обработка null значений
- ✅ Unit-тесты написаны и проходят

---

## Шаг 6: Создание DAO (Data Access Object)

### Задачи:
1. Создать интерфейс `ItemDao` в `data/database/dao/ItemDao.kt`
   - Методы:
     - `@Query("SELECT * FROM items ORDER BY timestamp DESC") fun getAllItems(): Flow<List<ItemEntity>>`
     - `@Query("SELECT * FROM items WHERE id = :id") suspend fun getItemById(id: Long): ItemEntity?`
     - `@Query("SELECT * FROM items WHERE title LIKE '%' || :searchQuery || '%' OR details LIKE '%' || :searchQuery || '%'") fun searchItems(searchQuery: String): Flow<List<ItemEntity>>`
     - `@Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertItem(item: ItemEntity): Long`
     - `@Update suspend fun updateItem(item: ItemEntity)`
     - `@Delete suspend fun deleteItem(item: ItemEntity)`
     - `@Query("DELETE FROM items") suspend fun deleteAllItems()`
     - `@Query("SELECT COUNT(*) FROM items") suspend fun getItemsCount(): Int`

2. Написать интеграционные тесты в `androidTest/data/database/dao/ItemDaoTest.kt`
   - Тест вставки записи
   - Тест получения всех записей
   - Тест получения записи по ID
   - Тест поиска записей
   - Тест обновления записи
   - Тест удаления записи
   - Тест удаления всех записей
   - Тест подсчета записей

### Структура:
```kotlin
@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY timestamp DESC")
    fun getAllItems(): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemById(id: Long): ItemEntity?
    
    @Query("SELECT * FROM items WHERE title LIKE '%' || :searchQuery || '%' OR details LIKE '%' || :searchQuery || '%'")
    fun searchItems(searchQuery: String): Flow<List<ItemEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ItemEntity): Long
    
    @Update
    suspend fun updateItem(item: ItemEntity)
    
    @Delete
    suspend fun deleteItem(item: ItemEntity)
    
    @Query("DELETE FROM items")
    suspend fun deleteAllItems()
    
    @Query("SELECT COUNT(*) FROM items")
    suspend fun getItemsCount(): Int
}
```

### Критерии готовности:
- ✅ DAO интерфейс создан со всеми методами
- ✅ Использованы правильные аннотации Room
- ✅ Интеграционные тесты написаны и проходят
- ✅ Тесты используют реальную in-memory базу данных

---

## Шаг 7: Создание Room Database

### Задачи:
1. Создать абстрактный класс `DaysDatabase` в `data/database/DaysDatabase.kt`
   - Аннотация `@Database` с entities и версией
   - Версия базы данных: 1
   - Абстрактный метод для получения DAO
   - Singleton паттерн для получения экземпляра базы данных

2. Создать DatabaseModule для DI (если используется DI) или фабричный метод

3. Написать интеграционные тесты в `androidTest/data/database/DaysDatabaseTest.kt`
   - Тест создания базы данных
   - Тест получения DAO
   - Тест миграций (если будут добавлены в будущем)

### Структура:
```kotlin
@Database(
    entities = [ItemEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DaysDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    
    companion object {
        @Volatile
        private var INSTANCE: DaysDatabase? = null
        
        fun getDatabase(context: Context): DaysDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DaysDatabase::class.java,
                    "days_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

### Критерии готовности:
- ✅ Database класс создан с правильными аннотациями
- ✅ Singleton паттерн реализован
- ✅ Метод получения DAO работает
- ✅ Интеграционные тесты написаны и проходят

---

## Шаг 8: Создание интерфейса Repository (Domain layer)

### Задачи:
1. Создать интерфейс `ItemRepository` в `domain/repository/ItemRepository.kt`
   - Методы:
     - `fun getAllItems(): Flow<List<Item>>`
     - `suspend fun getItemById(id: Long): Item?`
     - `fun searchItems(query: String): Flow<List<Item>>`
     - `suspend fun insertItem(item: Item): Long`
     - `suspend fun updateItem(item: Item)`
     - `suspend fun deleteItem(item: Item)`
     - `suspend fun deleteAllItems()`
     - `suspend fun getItemsCount(): Int`

2. Написать unit-тесты с моками в `test/domain/repository/ItemRepositoryTest.kt`
   - Тесты проверяют только интерфейс (контракт)
   - Использование MockK для мокирования

### Структура:
```kotlin
interface ItemRepository {
    fun getAllItems(): Flow<List<Item>>
    suspend fun getItemById(id: Long): Item?
    fun searchItems(query: String): Flow<List<Item>>
    suspend fun insertItem(item: Item): Long
    suspend fun updateItem(item: Item)
    suspend fun deleteItem(item: Item)
    suspend fun deleteAllItems()
    suspend fun getItemsCount(): Int
}
```

### Критерии готовности:
- ✅ Интерфейс Repository создан
- ✅ Все методы объявлены
- ✅ Unit-тесты написаны (проверка контракта)

---

## Шаг 9: Реализация Repository (Data layer)

### Задачи:
1. Создать реализацию `ItemRepositoryImpl` в `data/repository/ItemRepositoryImpl.kt`
   - Реализация интерфейса `ItemRepository`
   - Использование `ItemDao` для доступа к данным
   - Конвертация между Entity и Domain моделями
   - Обработка ошибок

2. Написать unit-тесты в `test/data/repository/ItemRepositoryImplTest.kt`
   - Мокирование `ItemDao`
   - Тесты всех методов репозитория
   - Тесты конвертации моделей
   - Тесты обработки ошибок

### Структура:
```kotlin
class ItemRepositoryImpl(
    private val itemDao: ItemDao
) : ItemRepository {
    override fun getAllItems(): Flow<List<Item>> {
        return itemDao.getAllItems()
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun getItemById(id: Long): Item? {
        return itemDao.getItemById(id)?.toDomain()
    }
    
    override fun searchItems(query: String): Flow<List<Item>> {
        return itemDao.searchItems(query)
            .map { entities -> entities.map { it.toDomain() } }
    }
    
    override suspend fun insertItem(item: Item): Long {
        return itemDao.insertItem(item.toEntity())
    }
    
    override suspend fun updateItem(item: Item) {
        itemDao.updateItem(item.toEntity())
    }
    
    override suspend fun deleteItem(item: Item) {
        itemDao.deleteItem(item.toEntity())
    }
    
    override suspend fun deleteAllItems() {
        itemDao.deleteAllItems()
    }
    
    override suspend fun getItemsCount(): Int {
        return itemDao.getItemsCount()
    }
}
```

### Критерии готовности:
- ✅ Реализация Repository создана
- ✅ Все методы реализованы
- ✅ Конвертация между слоями работает
- ✅ Unit-тесты написаны и проходят
- ✅ Обработка ошибок реализована

---

## Шаг 10: Интеграционное тестирование

### Задачи:
1. Создать интеграционные тесты в `androidTest/data/repository/ItemRepositoryIntegrationTest.kt`
   - Тест полного цикла: создание → чтение → обновление → удаление
   - Тест поиска записей
   - Тест получения всех записей
   - Тест обработки пустой базы данных

2. Проверить работу с реальной in-memory базой данных

### Критерии готовности:
- ✅ Интеграционные тесты написаны
- ✅ Все тесты проходят
- ✅ Покрытие кода достаточное

---

## Шаг 11: Документация и финализация

### Задачи:
1. Добавить KDoc комментарии к публичным API:
   - Классы и интерфейсы
   - Методы Repository
   - Методы DAO

2. Проверить соответствие кода правилам проекта:
   - Нет использования `!!`
   - Безопасное разворачивание опционалов
   - Логи на русском языке
   - Соответствие стилю кода

3. Запустить линтеры:
   - `./gradlew ktlintCheck`
   - `./gradlew detekt`

4. Исправить все замечания линтеров

### Критерии готовности:
- ✅ KDoc комментарии добавлены
- ✅ Код соответствует правилам проекта
- ✅ Линтеры не выдают ошибок
- ✅ Все тесты проходят

---

## Порядок выполнения (TDD подход)

**ВАЖНО:** Строго соблюдать порядок TDD:
1. **Red**: Написать падающий тест
2. **Green**: Написать минимальный код для прохождения теста
3. **Refactor**: Улучшить код, сохраняя тесты зелеными

### Последовательность реализации:
1. Шаг 2: DisplayOption enum + тесты
2. Шаг 3: Domain Item model + тесты
3. Шаг 4: Room Entity + конвертеры + тесты
4. Шаг 5: Мапперы + тесты
5. Шаг 6: DAO + тесты
6. Шаг 7: Database + тесты
7. Шаг 8: Repository интерфейс + тесты
8. Шаг 9: Repository реализация + тесты
9. Шаг 10: Интеграционные тесты
10. Шаг 11: Документация и финализация

---

## Критерии завершения этапа

Этап считается завершенным, когда:

- ✅ Все компоненты созданы и работают
- ✅ Все unit-тесты написаны и проходят
- ✅ Все интеграционные тесты написаны и проходят
- ✅ Код соответствует правилам проекта
- ✅ Линтеры не выдают ошибок
- ✅ KDoc комментарии добавлены
- ✅ Модель данных готова к использованию в других этапах

---

## Зависимости от других этапов

- **Этап 6 (Форматирование дней)**: Метод `makeDaysCount` в доменной модели Item будет полностью реализован в Этапе 6. На данном этапе создается только заглушка.

## Блокируемые этапы

После завершения Этапа 7 можно приступать к:
- Этап 2: Главный экран (требует модель данных)
- Этап 3: Детали записи (требует модель данных)
- Этап 4: Создание и редактирование записей (требует модель данных)
- Этап 8: Резервное копирование (требует модель данных)

---

## Примечания

1. **Совместимость с iOS**: Структура модели должна быть совместима с iOS-приложением для резервного копирования (Этап 8).
   - **DisplayOption**: Использовать camelCase в JSON ("day", "monthDay", "yearMonthDay") для совместимости с iOS
   - **ColorTag**: В iOS хранится как Data (NSKeyedArchiver), в Android нужно использовать Base64-строку для JSON
   - **Timestamp**: Использовать миллисекунды с 1970-01-01 (как Date в iOS)

2. **Безопасность типов**: Использовать enum для DisplayOption вместо строковых констант.

3. **Обработка null**: Все опциональные поля должны корректно обрабатываться при конвертации между слоями.

4. **Производительность**: Использовать Flow для реактивного обновления UI при изменении данных.

5. **Тестирование**: Все компоненты должны быть покрыты тестами перед использованием в других этапах.

6. **Формат резервной копии**: При реализации экспорта/импорта (Этап 8) необходимо обеспечить полную совместимость с iOS-форматом:
   - `colorTag` как Base64-строка (сериализованный UIColor через NSKeyedArchiver)
   - `displayOption` в camelCase формате
   - Поддержка обратной совместимости со старыми форматами (если были)

