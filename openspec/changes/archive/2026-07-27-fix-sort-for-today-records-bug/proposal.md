## Why

События, созданные с одинаковой датой (например, два события за 26 июля), получают одинаковый `Item.timestamp = LocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()`. SQL-методы `ItemDao.getAllItems*()` сортируют только по `ORDER BY timestamp {DIR}`, поэтому для одинаковой даты порядок строк не определён и переключение «старые→новые» / «новые→старые» не меняет видимый список. Это воспроизводится в сценарии `demo old` / `demo new`.

iOS работает корректно, потому что `Date.now()` инициализирует `EditItemScreen.timestamp`, и `DatePicker` с `displayedComponents: .date` сохраняет time-of-day в underlying `Date` — два события, созданные с разрывом в секунды, получают разные `timestamp`. Android явно нормализует к полуночи через `atStartOfDay(ZoneId.systemDefault())` (`CreateEditScreen.kt`, `StateSavers.kt`), поэтому time-of-day теряется и tie-breaker отсутствует.

## What Changes

- Обновить SQL-сортировку в `ItemDao` до двухключевого порядка `timestamp → id` для `getAllItems()`, `getAllItemsAsc()`, `getAllItemsDesc()` и `searchItems()`. `id` — autoincrement primary key, монотонный в рамках БД (SQLite `rowid`); более высокий `id` соответствует более поздней вставке.
- Синхронизировать 5 копий `FakeItemRepository` в unit-тестах: перейти от одноключевой сортировки к `timestamp, id`.
- Расширить тесты: добавить регрессионные сценарии в `ItemDaoTest` и `MainScreenViewModelTest`.

## Capabilities

### New Capabilities

- `event-sort-stability`: список событий сортируется по `timestamp` (доминирующий ключ — дата события), затем по `id` (tie-breaker — monotonic autoincrement); направление сортировки применяется согласованно к обоим ключам; покрывает все четыре потока: `getAllItems`, `getAllItemsAsc`, `getAllItemsDesc`, `searchItems`.

### Modified Capabilities

Нет. Существующие спецификации (`openspec/specs/`) отсутствуют — это первая инициализация capability-набора.

## Impact

- Код: `ItemDao.kt` (3 SQL-запроса + `searchItems`), 5 × `FakeItemRepository` в unit-тестах.
- Тесты: `ItemDaoTest` (androidTest), `MainScreenViewModelTest` (unit), 5 файлов с `FakeItemRepository`.
- Backup DTO: без изменений. `id` уже не входит в `BackupItem` (`BackupItem.kt:103-115`), `id = 0` при импорте (`BackupItem.kt:139`), Room назначает новый автоинкремент на вставке.
- Схема БД: без изменений. Версия остаётся 2, миграция не требуется.
- Конфигурация: без изменений. `room-testing`, `room.schemaLocation`, `ksp { arg(...) }` не нужны.
- UI и локализация: не затрагиваются.
- Поведение iOS-импорта: после импорта Android назначает `id` в порядке JSON-массива (`ImportBackupUseCase.kt:67-71`), что приблизительно соответствует порядку экспорта (`export` → `repository.getAllItems()` → `ORDER BY timestamp DESC` → newest first → id grows с newest). Уже существующие в БД записи сохраняют свои `id`, поэтому cross-cutting сортировка между existing и imported может незначительно сбиваться — это pre-existing, не входит в этот change.
- Производительность: `id` — primary key, индекс уже существует. Дополнительный индекс на `items.id` не требуется.
