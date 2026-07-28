## Why

Android нормализует `timestamp` к `atStartOfDay(ZoneId.systemDefault())` при сохранении, поэтому два события, созданные в один календарный день, получают идентичный `timestamp`. Для сортировки same-date событий используется tie-breaker по `id` (порядок создания), но `id` — это не «время события», а «когда запись попала в БД». После `Delete all data` + restore SQLite переиспользует освобождённые rowid, и порядок same-date событий переворачивается.

iOS хранит полный `Date` (с time-of-day, миллисекундная точность) — tie-breaker там не нужен, естественный порядок вытекает из самого `timestamp`. Цель: перестать нормализовать `timestamp` в Android, чтобы tie-breaker стал излишен (или срабатывал только для legacy-данных, у которых `timestamp` всё ещё приведён к началу дня).

## What Changes

- **Убрать нормализацию `timestamp` в `CreateEditScreen.kt`** (3 места: строки 87, 158, 215):
  - Строка 215 (`toItem()` — формирование `Item` при сохранении): при создании нового события сохранять `selectedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()`; при редактировании существующего — извлекать `originalTimeOfDay` из `item.timestamp` и использовать `selectedDate.atTime(originalTimeOfDay)`.
  - Строка 87 (`onValueChange → checkHasChanges`) и строка 158 (`onDateSelected → checkHasChanges`): **критический баг** — текущая реализация сравнивает `original.timestamp` (с time-of-day) с `selectedDate.atStartOfDay(...)` (всегда 00:00:00), что даёт ложное `hasChanges = true` сразу при открытии экрана редактирования. Решение: сравнивать `LocalDate` (только date-компонент), а не millisecond-`timestamp`. `hasChanges` должен срабатывать на изменение даты, а не на time-of-day.
- **Убрать нормализацию `timestamp` в `StateSavers.kt`** (4 места: строки 20, 73, 102, 157): при сохранении `LocalDate` использовать `LocalTime.now()` в момент сериализации (time-of-day момента сохранения, не открытия экрана). При восстановлении — `Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()` (time-of-day отбрасывается, остаётся только date-компонент).
- **Не менять SQL `ORDER BY` в `ItemDao.kt`** — синтаксис `ORDER BY timestamp DESC, id DESC` (и ASC-аналог) уже введён в change `fix-sort-for-today-records-bug` (коммит `1f809ac`). SQL остаётся как есть. Меняется **только KDoc**: явно зафиксировать, что `timestamp` хранится с millisecond precision и является осмысленным критерием порядка; `id` — defensive tie-breaker для legacy-данных (с `timestamp` = `atStartOfDay`) и для редких коллизий миллисекунд. Логика: «сначала сортируем по времени события, при равном `timestamp` — по rowid (стабильный ключ)».
- **Добавить тесты**:
  - DAO-тест: два события с одинаковой `date` частью, но разным `timeOfDay` → порядок строго по `timestamp`.
  - DAO-тест: backup+restore (delete all + insert тех же `timestamp`) → порядок сохраняется.
  - ViewModel-тест: `MainScreenViewModelTest` — регрессионный сценарий same-date, разный time-of-day.
  - **Новый ViewModel-тест** для `CreateEditScreenViewModel.checkHasChanges`: убедиться, что `hasChanges = false` сразу после загрузки существующего `Item` (т.е. что сравнение `timestamp` корректно учитывает time-of-day).
- **Обновить существующий `MainScreenViewModelTest.items_when_same_timestamp_use_id_as_tiebreaker`**: переименовать в `items_when_same_timestamp_id_breaks_tie` (или аналогично), уточнить комментарий, что это поведение для legacy-данных.

**Не меняется:**
- `Item`, `ItemEntity`, `BackupItem`, `IosBackupItem`, `BackupWrapper`, `IosBackupWrapper` — схема и формат бэкапа не меняются (`timestamp` уже `Long`).
- `CalculateDaysDifferenceUseCase.kt` — расчёт «дней» остаётся day-precision, time-of-day не влияет.
- iOS-приложение — не требует изменений; наоборот, кросс-платформенная совместимость бэкапов улучшается (iOS уже хранит миллисекунды, Android теперь тоже будет).
- **Миграция БД не требуется** (схема таблицы `items` не меняется, только значения `timestamp` для новых записей).
- **Legacy-данные не мигрируются** (нельзя узнать исходный time-of-day). Для них tie-breaker по `id` остаётся актуальным.
- **`CreateEditButtons.kt:39`** — `atStartOfDay(ZoneOffset.UTC)` для `DatePicker.rememberDatePickerState(initialSelectedDateMillis = ...)`. DatePicker работает с date-компонентом, time-of-day ему не нужен. Остаётся без изменений.
- **`CreateEditPreviewComponents.kt:76`** — `atStartOfDay(...)` в `createPreviewItem()` (используется только в `@Preview` композаблах для Android Studio preview, не в production runtime). Можно оставить для минимального diff; формально не влияет на функциональность.
- **`BuildReminderUseCase.kt:44`** — `atStartOfDay(clock.zone)` для поля `Reminder.selectedDateEpochMillis` (идентификатор выбранной даты в напоминании, **не** `Item.timestamp`). Это отдельный домен — `Reminder` оперирует парами `targetMillis` (полный `LocalDateTime` для момента срабатывания) и `selectedDateEpochMillis` (start-of-day как ID даты). В scope данного change не входит, остаётся без изменений. **Реализатор: при `grep atStartOfDay app/src/main` ожидаемо 10 вхождений, из них 9 покрыты change'ом, это 10-е — НЕ трогать.**

## Capabilities

### New Capabilities

- `timestamp-precision`: хранение полного `timestamp` (epoch millis с time-of-day) в `ItemEntity` и при экспорте/импорте бэкапа. Сортировка в `ItemDao` первично по `timestamp`, вторично по `id` (defensive tie-breaker для legacy-данных). `CreateEditScreen.checkHasChanges` сравнивает только date-компонент, не time-of-day.

### Modified Capabilities

- Нет. Существующие capabilities (если появятся) не затрагиваются.

## Impact

**Затрагиваемые файлы (код, в этом change не пишется — только документируется):**
- `app/src/main/java/com/dayscounter/data/database/dao/ItemDao.kt` — обновление KDoc (семантика `ORDER BY`: `timestamp` — primary key с millisecond precision, `id` — defensive tie-breaker; **синтаксис SQL без изменений**)
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditScreen.kt` — убрать `atStartOfDay(...)` в 3 местах: строка 215 (`toItem`), строка 87 (`checkHasChanges` через `onValueChange`), строка 158 (`checkHasChanges` через `onDateSelected`); плюс — изменить логику `checkHasChanges` для сравнения `LocalDate` вместо millis-`timestamp`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/StateSavers.kt` — убрать `atStartOfDay(...)` в 4 местах (20, 73, 102, 157)
- `app/src/main/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModel.kt` — при необходимости пробросить `originalTimeOfDay` в state и/или пересмотреть контракт `checkHasChanges`/`CreateEditChangeInput` (либо сравнение целиком переносится в Compose, либо `CreateEditChangeInput` принимает `LocalDate?` вместо `Long`)
- `app/src/androidTest/java/com/dayscounter/data/database/dao/ItemDaoTest.kt` — новые DAO-тесты
- `app/src/test/java/com/dayscounter/ui/viewmodel/MainScreenViewModelTest.kt` — обновление регрессионного теста
- `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt` — новый тест на `checkHasChanges` корректность

**Затрагиваемые системы:**
- База данных: формат хранения `timestamp` (схема) не меняется, только семантика значений (новые записи имеют time-of-day, старые — нет).
- Бэкап-формат: не меняется (`timestamp: Long` в `BackupItem` уже поддерживает millisecond precision).
- iOS-приложение: не требует изменений; улучшается совместимость бэкапов.
- Пользовательский UI: date picker не меняется; пользователь по-прежнему выбирает только дату, но time-of-day фиксируется автоматически (при создании — текущий момент, при редактировании — сохранённый).

**Риски / ограничения:**
- Legacy-данные (события, созданные до этого change'а) имеют `timestamp` = `atStartOfDay(...)`. Для них поведение tie-breкер'а по `id` сохраняется. После `Delete all data` + restore такие события могут иметь инвертированный порядок относительно исходного — это known limitation, не лечится без потерь (нет способа восстановить time-of-day для старых записей).
- Пользователь не видит time-of-day в UI, но порядок same-date событий становится «от более позднего time-of-day к более раннему» (при DESC) — это может удивить тех, кто создал два события утром и вечером одного дня, ожидая их «упорядочить по дате без подразделения». На практике это совпадает с iOS-семантикой и логично: «новее по времени = позже создано».
- **DST edge case:** в час перевода часов (spring forward — не существует 02:00–03:00, fall back — дублируется 01:00–02:00) `LocalTime.now().atZone(...)` корректно разрешается JDK (возвращает offset, действующий в этот момент). Риск минимален, но не нулевой — пользователь, создающий событие в 3 часа ночи в день перевода, может получить неожиданный `timestamp`. Задокументировать как edge case, не чинить (это особенность `java.time`, не нашего кода).
- **`StateSavers` фиксирует time-of-day в момент сериализации state, а не в момент открытия экрана.** При повороте экрана или другом configuration change между открытием экрана и сохранением пройдёт несколько минут — `LocalTime.now()` в `save =` будет отражать момент сериализации, а не открытия. Разница не влияет на корректность сортировки (порядок стабилен в пределах нескольких минут) и незаметна пользователю. При следующем открытии экрана редактирования `originalTimeOfDay` будет взят из уже сохранённого `Item.timestamp` (с обновлённым time-of-day). Эту разницу нужно упомянуть в `Design Risks`, не как баг, а как характеристику поведения.
- **CheckHasChanges** в текущей форме использует `atStartOfDay(...)` при построении `changeInput.timestamp` (строки 87, 158 в `CreateEditScreen.kt`), что приводит к ложному `hasChanges = true` сразу после загрузки существующего `Item` (timestamp которого имеет time-of-day). Это **критический баг, который наш change создаёт**, и его **обязательно нужно исправить в рамках этого change** (см. `What Changes` выше).
