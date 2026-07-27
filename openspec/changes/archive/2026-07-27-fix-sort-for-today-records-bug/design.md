## Context

В Android-приложении `JetpackDays` два события, созданные с одинаковой `LocalDate` (например, оба за 26 июля), получают одинаковый `Item.timestamp = LocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()`. SQL-методы `ItemDao.getAllItems*()` сортируют только по `ORDER BY timestamp {DIR}`, поэтому для одинаковой даты порядок строк не определён и переключение «старые→новые» / «новые→старые» не меняет видимый список.

Поиск первопричин (от UI до SQL):
- `CreateEditScreen.kt` сохраняет `LocalDate.atStartOfDay(...)` → `timestamp` (явная нормализация).
- `MainScreenViewModel` пробрасывает `SortOrder` в репозиторий; собственной сортировки нет.
- `ItemRepositoryImpl` доверяет SQL из `ItemDao`.
- `ItemDao.getAllItemsAsc/getAllItemsDesc` сортирует по одному ключу; `getAllItems()` (DESC по умолчанию) и `searchItems()` без `ORDER BY` вторичного ключа.

Контрактные ограничения:
- `BackupItem` НЕ должен получать новых полей (iOS-совместимость, явное требование).
- Проект не использует внешний DI (Hilt), manual DI в `FormatterModule` / `AppModule`.
- KSP (не KAPT), `room = 2.8.4`, `kotlin = 2.4.10`.
- База данных: `version = 2`, `exportSchema = false`, миграции зарегистрированы в `DaysDatabase.kt`.

## Goals / Non-Goals

**Goals:**
- Детерминированный порядок списка событий при одинаковой дате для ASC и DESC сортировки.
- Сохранить первый ключ — дата события (`timestamp`); `id` — единственный tie-breaker.
- Минимальный диф: только SQL-запросы в `ItemDao` и 5 синхронизаций `FakeItemRepository`.

**Non-Goals:**
- Введение новых полей (`createdAt`, `createdAtTimestamp`, …).
- Подъём версии БД или написание `Migration` / `MigrationTestHelper`.
- Изменение `BackupItem`, `BackupWrapper`, `Item.toBackupItem`, `BackupItem.toItem`.
- Изменение `DaysDifference` / способа нормализации `timestamp`.
- Изменение UI / локализации / формата отображения дат.
- Перенос сортировки в ViewModel или Compose.
- Объединение 5 копий `FakeItemRepository` в общий test util — зафиксировать, отложить.
- Удаление `getAllItemsDesc()` как дубликата `getAllItems()` — отметить как refactor-кандидатом, но не делать в этом изменении (минимальный диф).

## Decisions

### D1. SQL-сортировка — двухключевая `timestamp → id`

`ItemDao.getAllItems()`, `getAllItemsAsc()`, `getAllItemsDesc()`, `searchItems()` обновляются до `ORDER BY timestamp {DIR}, id {DIR}`. `id` — autoincrement primary key, SQLite `rowid` монотонный (после удаления `id=2` следующая вставка получает `id=max+1`, не `id=3`). Более высокий `id` соответствует более поздней вставке.

Альтернативы:
- Новое поле `createdAt: Long` через миграцию 2→3. Отклонено: меняет backup DTO (или создаёт sentinel `0L` после импорта, что не устраняет баг для импортированных записей); добавляет 4 build-setup шага, миграционный тест, схемы. Не окупается для багфикса.
- Сортировка на уровне репозитория через `kotlinx-collections`. Отклонено: SQL остаётся источником истины, чтобы `LazyColumn` с `key = { it.id }` получал упорядоченный поток без post-sort в `ViewModel`.
- Сортировка только по `timestamp` (как iOS-логика). Отклонено: iOS работает только потому, что `Date.now()` сохраняет time-of-day в `DatePicker` с `displayedComponents: .date`. Android явно нормализует к полуночи — без time-of-day одно поле `timestamp` не даёт детерминированного порядка.

### D2. Без новых полей и без миграции

`Item`, `ItemEntity`, `ItemMapper`, `ItemRepositoryImpl`, `DaysDatabase`, `BackupItem` остаются без изменений. `id` уже существует как primary key и в коде, и в схеме.

Альтернатива — добавить `createdAt` как отдельное поле. Отклонено: см. D1.

### D3. `BackupItem` без изменений

`id` уже не входит в `BackupItem` (только `title`, `details`, `timestamp`, `colorTag`, `displayOption` — `BackupItem.kt:28-34`). При импорте `id = 0` (`BackupItem.kt:139`), Room назначает новый autoincrement на вставке. После импорта Android порядок приблизительно соответствует порядку экспорта (id растут в порядке JSON-массива).

Альтернатива — добавить `createdAt` в `BackupItem` для точного сохранения порядка через backup. Отклонено: явное требование — backup DTO не меняется.

### D4. Тест-двойники: синхронизировать все 5 копий `FakeItemRepository`

`FakeItemRepository` в 5 файлах оперирует `MutableList<Item>` без SQL. Текущая сортировка `sortBy { it.timestamp }` не учитывает tie-breaker. Заменить на `sortedWith(compareBy({ it.timestamp }, { it.id }))` (для ASC) и зеркально для DESC.

Файлы:
- `app/src/test/java/com/dayscounter/ui/viewmodel/MainScreenViewModelTest.kt`
- `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt`
- `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelReminderTest.kt`
- `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt` (вариант `FakeItemRepositoryWithLoggingDisabled`)
- `app/src/test/java/com/dayscounter/reminder/DefaultReminderManagerTest.kt`

Объединение в общий test util — отдельный refactor-кандидат, не входит в этот change.

## Risks / Trade-offs

- **5 копий `FakeItemRepository`** → рассинхронизация сортировки → один источник забыли обновить. → Все 5 файлов перечислены в `tasks.md §2.1-2.5`; каждое синхронизируется в одном коммите.
- **Кросс-device порядок после backup+import может незначительно отличаться.** Импортированные записи получают `id` в порядке JSON-массива (`ImportBackupUseCase.kt:67-71`), но existing записи в БД сохраняют свои `id`. В рамках одной даты события порядок приблизительно соблюдается; точное совпадение не гарантируется. → Pre-existing ограничение backup-модели, не входит в этот change.
- **`id` как tie-breaker — семантический code smell.** `id` концептуально «внутренний идентификатор строки», а не «время создания». Использование PK как временного маркера — лёгкий smell. → Документируется KDoc'ом на DAO. Альтернатива (`createdAt`) отклонена по D1.

## Migration Plan

Не требуется. Схема БД не меняется, `version` остаётся 2, `exportSchema` остаётся `false`. Rollback: удалить `id {DIR}` из SQL — тривиальный revert.

## Open Questions

Нет. Все вопросы решены в Decisions и Risks.
