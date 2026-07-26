## 1. DAO

- [x] 1.1 `app/src/main/java/com/dayscounter/data/database/dao/ItemDao.kt` — `getAllItems()`: добавить `, id DESC` в `ORDER BY`.
- [x] 1.2 Тот же файл — `getAllItemsAsc()`: добавить `, id ASC`.
- [x] 1.3 Тот же файл — `getAllItemsDesc()`: добавить `, id DESC`.
- [x] 1.4 Тот же файл — `searchItems()`: добавить `ORDER BY timestamp DESC, id DESC`.
- [x] 1.5 KDoc на DAO: зафиксировать, что `id` — tie-breaker для одинаковой `timestamp`.

## 2. Test doubles

Замечание: только `MainScreenViewModelTest` уже применяет `sortOrder` в `FakeItemRepository.getAllItems(sortOrder)`. Остальные 4 возвращают `items`/`flowOf(emptyList())` без сортировки. Для них добавление двухключевой сортировки одновременно добавляет и sortOrder-awareness. Это выравнивает fake с реальным репозиторием — желаемое поведение, но фактический диф больше, чем «только tiebreaker».

- [x] 2.1 `app/src/test/java/com/dayscounter/ui/viewmodel/MainScreenViewModelTest.kt` — `FakeItemRepository.getAllItems(sortOrder)` уже сортирует, заменить `sortedBy/sortedByDescending { it.timestamp }` на `sortedWith(compareBy(...))` с tiebreaker по `id`.
- [x] 2.2 `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt` — `FakeItemRepository.getAllItems()` и `getAllItems(sortOrder)` сейчас возвращают `items` без сортировки. Добавить `getAllItems(sortOrder) = items.map { sort(it, sortOrder) }` с двухключевой сортировкой; `getAllItems()` оставить `items` (pre-existing fallback для неподдерживаемого вьюмоделью вызова).
- [x] 2.3 `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelReminderTest.kt` — `getAllItems(sortOrder)` сейчас `flowOf(emptyList())`. Сортировка не нужна (тест не использует `getAllItems`); проверить, что изменения не требуются. Если оставлять как есть — задача тривиальна.
- [x] 2.4 `app/src/test/java/com/dayscounter/reminder/DefaultReminderManagerTest.kt` — `getAllItems(sortOrder)` сейчас `flowOf(emptyList())`. Аналогично 2.3: тест не использует сортировку, изменения не требуются.
- [x] 2.5 `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt` — `FakeItemRepositoryWithLoggingDisabled.getAllItems(sortOrder)` сейчас `flowOf(emptyList())`. Аналогично 2.3/2.4: тест не использует сортировку, изменения не требуются.

## 3. Tests

- [x] 3.1 `app/src/androidTest/java/com/dayscounter/data/database/dao/ItemDaoTest.kt`: расширить существующие проверки — добавить 2+ записи с одинаковой `timestamp` и разными `id`, проверить `getAllItems()`, `getAllItemsAsc()`, `getAllItemsDesc()` в обоих направлениях, проверить доминирование `timestamp` для разных дат.
- [x] 3.2 `app/src/test/java/com/dayscounter/ui/viewmodel/MainScreenViewModelTest.kt`: регрессионный тест с двумя `Item` на одну дату события и разными `id`, проверять по `title`, а не только по `id`.

## 4. Проверка

- [x] 4.1 `make test`.
- [x] 4.2 `make android-test` — выполнен на `emulator-5554` (BUILD SUCCESSFUL).
- [x] 4.3 `make format`.
- [x] 4.4 `make lint`.
- [x] 4.5 `make check`.

## 5. Ручной acceptance

- [x] 5.1 Создать `demo old` и `demo new` с одинаковой датой 26 июля; переключить «старые→новые» — порядок `demo old`, `demo new`; переключить «новые→старые» — порядок `demo new`, `demo old`.
- [x] 5.2 Отредактировать `demo old` — убедиться, что оно не стало новым.
- [ ] 5.3 **Заблокировано.** Backup → factory reset → restore выявил два pre-existing бага (НЕ от этого change):
  1. **Файл-коррупция** (`/sdcard/Download/Days backup.json`, 429 байт, последние 21 — мусор `"\n        }\n    ]\n}`). BackupWrapper-decode падает, парсер ошибочно валится в fallback `List<BackupItem>`, ошибка "Expected array, got object" вводит в заблуждение. Требует отдельного расследования `ExportBackupUseCase` (truncate mode? гонка write? ручная правка?).
  2. **Порядок после restore переворачивается**: id как tiebreaker + autoincrement-reset после factory reset = для одинаковой даты события видим `[old, new]` вместо исходного `[new, old]`. Корректное решение — добавить `createdAt` в `BackupItem`, что выходит за scope этого change. До фикса в `BackupItem` это ограничение задокументировано.
