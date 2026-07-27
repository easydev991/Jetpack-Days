## 1. StateSavers — убрать нормализацию timestamp

- [ ] 1.1 `app/src/main/java/com/dayscounter/ui/screens/createedit/StateSavers.kt:20` — заменить `atStartOfDay(ZoneId.systemDefault())` на `atTime(LocalTime.now()).atZone(ZoneId.systemDefault())` (или эквивалент, сохраняющий time-of-day).
- [ ] 1.2 `StateSavers.kt:73` — то же.
- [ ] 1.3 `StateSavers.kt:102` — то же.
- [ ] 1.4 `StateSavers.kt:157` — то же.

## 2. CreateEditScreen — убрать нормализацию timestamp и починить checkHasChanges

**Критический баг:** строки 87 и 158 в `CreateEditScreen.kt` строят `changeInput.timestamp` через `atStartOfDay(...)`, что при сравнении с `original.timestamp` (с time-of-day) даёт ложное `hasChanges = true` сразу при открытии экрана редактирования. **Обязательно исправить в рамках этого change.**

- [ ] 2.1 `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditScreen.kt:215` (`toItem()`) — при создании нового события сохранять `selectedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()` вместо `atStartOfDay(...)`.
- [ ] 2.2 Тот же файл, `toItem()` — при редактировании существующего события извлекать `originalTimeOfDay = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()` и использовать `selectedDate.atTime(originalTimeOfDay)` при сохранении.
- [ ] 2.3 `CreateEditScreen.kt:87` (`onValueChange → checkHasChanges`) — изменить логику построения `changeInput.timestamp`: **сравнивать только `LocalDate` (date-компонент), а не millis-`timestamp`**. Конкретный подход: заменить `s.selectedDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli() ?: 0L` на передачу `s.selectedDate` напрямую (например, как `LocalDate?` через расширение `CreateEditChangeInput` или отдельный флаг). Альтернатива: вынести сравнение `hasChanges` в `ViewModel` (через `originalItem.value?.timestamp`), убрав timestamp из `CreateEditChangeInput` целиком. **Выбрано:** передача `LocalDate?` в `CreateEditChangeInput` (минимальное изменение контракта: оставляет вызов `checkHasChanges` из Compose без изменений, меняется только тип передаваемого параметра; само сравнение по-прежнему выполняется в `CreateEditScreenViewModel.checkHasChanges` — см. задачу 2.5).
- [ ] 2.4 `CreateEditScreen.kt:158` (`onDateSelected → checkHasChanges`) — то же, что и 2.3 (использует ту же `changeInput.timestamp` логику).
- [ ] 2.5 `app/src/main/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModel.kt:98-108` (`checkHasChanges`) — обновить реализацию, если `CreateEditChangeInput` теперь принимает `LocalDate?` вместо `Long` для timestamp. Сравнение: `changeInput.selectedDate != originalLocalDate` (где `originalLocalDate = Instant.ofEpochMilli(original.timestamp).atZone(ZoneId.systemDefault()).toLocalDate()`).

## 3. ItemDao — обновить KDoc

- [ ] 3.1 `app/src/main/java/com/dayscounter/data/database/dao/ItemDao.kt` — обновить header KDoc: явно зафиксировать, что `timestamp` хранится с millisecond precision; `id` — defensive tie-breaker для legacy-данных и коллизий миллисекунд, не отражает «новизну» события.
- [ ] 3.2 Тот же файл — KDoc на `getAllItems()` / `getAllItemsAsc()` / `getAllItemsDesc()`: уточнить роль `id` как tie-breaker, ссылка на спецификацию `timestamp-precision`.
- [ ] 3.3 Тот же файл — KDoc на `searchItems()`: аналогично.

**Примечание:** SQL `ORDER BY timestamp DESC, id DESC` остаётся без изменений (уже введён в `fix-sort-for-today-records-bug`, change `1f809ac`). Смысл `id` в нём меняется с «порядок создания» на «стабильный ключ», но синтаксис не меняется.

## 4. Тесты

- [ ] 4.1 `app/src/androidTest/java/com/dayscounter/data/database/dao/ItemDaoTest.kt` — новый тест `getAllItems_getAllItemsDesc_getAllItemsAsc_whenSameDateDifferentTimeOfDay` (или аналогичное имя): вставить два `ItemEntity` с `timestamp` в один календарный день, но с разным time-of-day (например, `09:00:00.000` и `18:00:00.000`). Проверить, что DESC возвращает 18:00 → 09:00, ASC — наоборот.
- [ ] 4.2 Тот же файл — `searchItems_whenSameDateDifferentTimeOfDay_returnsByTimeOfDay` (или аналогичное имя): два события с одинаковой date, разным time-of-day, поиск по общему слову → результат по timestamp DESC.
- [ ] 4.3 Тот же файл — `deleteAllItems_then_reinsert_preservesTimestampOrder` (или аналогичное имя): вставить два события с разным time-of-day, `deleteAllItems()`, вставить заново с теми же `timestamp` → порядок сохраняется (это доказывает, что restore работает корректно для новых данных).
- [ ] 4.4 `app/src/test/java/com/dayscounter/ui/viewmodel/MainScreenViewModelTest.kt` — обновить существующий регрессионный тест `items_when_same_timestamp_use_id_as_tiebreaker`: переименовать в `items_when_same_timestamp_id_breaks_tie` (или аналогично), уточнить комментарий, что это поведение для legacy-данных / коллизий миллисекунд.
- [ ] 4.5 Тот же файл — добавить новый регрессионный тест `items_when_same_date_different_time_of_day_sorts_by_timestamp` (или аналогично): два `Item` с одинаковой `date`, разным `timeOfDay` → порядок по `timeOfDay` (для FakeItemRepository, если он имитирует SQL-сортировку с millisecond precision).
- [ ] 4.6 `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt` — **новый регрессионный тест** на исправление критического бага `checkHasChanges`: после `loadItem()` с существующим `Item` (с time-of-day в `timestamp`) `hasChanges` должен быть `false` без касания формы. Воспроизведение бага: загрузить Item с `timestamp = 2026-01-15 14:30:00.000`, не вносить изменений, вызвать `checkHasChanges(...)` с `changeInput.timestamp = 2026-01-15 00:00:00.000` (через `atStartOfDay`) → до фикса: `hasChanges = true` (ошибочно — реальных изменений нет), после фикса (сравнение `LocalDate`): `hasChanges = false`.

## 5. Verify

- [ ] 5.1 `make test` — все unit-тесты проходят.
- [ ] 5.2 `make android-test` — все instrumentation-тесты проходят (на эмуляторе).
- [ ] 5.3 `make format` — форматирование применено.
- [ ] 5.4 `make lint` — ktlint + detekt без ошибок.
- [ ] 5.5 `make check` — полная проверка (build + test + lint).

## 6. Manual acceptance

- [ ] 6.1 Создать `demo old` (например, 26 июля в 09:00) и `demo new` (26 июля в 18:00, позже). Переключить «старые→новые» — порядок `demo old`, `demo new`. Переключить «новые→старые» — порядок `demo new`, `demo old`. Подтвердить, что порядок определяется **time-of-day**, а не порядком создания.
- [ ] 6.2 Отредактировать `demo old`, изменить дату на другой день, потом вернуть обратно — убедиться, что `demo old` остался первым при «старые→новые» (time-of-day сохранился).
- [ ] 6.3 Backup → Delete all data → restore → проверить, что порядок same-date событий с разным time-of-day сохранился. Это ключевой сценарий, ради которого change делается.
- [ ] 6.4 **(Опционально, при наличии тестового iOS-бэкапа)** Проверить, что в iOS-сгенерированном бэкапе same-date события с разным time-of-day импортируются и сортируются корректно. Без файла — пропустить, отметить как N/A в release notes.
