## Context

В Android-приложении `timestamp` события нормализуется к `atStartOfDay(ZoneId.systemDefault())` в 7 production-местах: 4 в `StateSavers.kt` (строки 20, 73, 102, 157) и 3 в `CreateEditScreen.kt` (строки 87, 158, 215). Это означает, что time-of-day всегда обнуляется: 2026-01-15 14:30 пользовательского ввода превращается в 2026-01-15 00:00:00.000. Два события, созданные в один календарный день, получают идентичный `timestamp`.

В `ItemDao.kt` после change `fix-sort-for-today-records-bug` (см. `git log 1f809ac`) ORDER BY включает `, id DESC` / `, id ASC` как tie-breaker. `id` — это rowid SQLite (autoincrement), не имеющий семантической связи со временем события. Он выражает «когда запись попала в БД», а не «когда произойдёт событие». После `Delete all data` + restore SQLite переиспользует освобождённые rowid, и порядок same-date событий переворачивается.

iOS-приложение хранит полный `Date` (epoch millis с time-of-day), tie-breaker не нужен, естественный порядок вытекает из самого `timestamp`. Цель change'а — привести Android к iOS-семантике, убрав нормализацию. Tie-breaker по `id` остаётся как defensive мера для legacy-данных и коллизий миллисекунд.

## Goals / Non-Goals

**Goals:**
- Хранить `timestamp` с millisecond precision (полный `LocalDateTime` → epoch millis), без `atStartOfDay(...)`.
- При создании нового события: `timestamp = selectedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()` (выбранная пользователем дата + текущее время). **Не** `Instant.now()` — это проигнорирует выбранную дату.
- При редактировании существующего: сохранять time-of-day из исходного `timestamp`, подменять только date-компонент на выбранный пользователем.
- Сделать `timestamp` единственным осмысленным критерием сортировки same-date событий.
- Сохранить `id` как defensive tie-breaker (стабильный ключ для edge-case коллизий).
- Улучшить кросс-платформенную совместимость бэкапов (iOS уже хранит millis, Android теперь тоже будет).

**Non-Goals:**
- Не менять `CalculateDaysDifferenceUseCase.kt` — расчёт «дней» остаётся day-precision, time-of-day не влияет.
- Не менять схему БД / `ItemEntity` / `BackupItem` — формат хранения не меняется, только семантика значений для новых записей.
- Не мигрировать legacy-данные (невозможно узнать исходный time-of-day).
- Не менять UI date picker — пользователь по-прежнему выбирает только дату.
- Не менять iOS-приложение (ему это и не нужно — оно уже хранит полный timestamp).
- Не менять `ExportBackupUseCase` / `ImportBackupUseCase` (формат бэкапа совместим).

## Decisions

### D1. Убрать `atStartOfDay(...)` в `CreateEditScreen.kt` и `StateSavers.kt`

**Решение:** В местах, где timestamp формируется из выбранной пользователем даты, использовать `selectedDate.atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()` вместо `selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()`.

**Альтернатива:** Использовать `Instant.now().toEpochMilli()` напрямую. **Отклонено:** не учитывает выбранную пользователем дату; требует ручного сдвига date-компонента.

**Альтернатива:** Использовать фиксированное время (например, `LocalTime.NOON = 12:00`). **Отклонено:** не соответствует iOS-семантике, противоречит интуиции пользователя («событие в 12:00? Я же не выбирал время»).

**Обоснование:** `LocalTime.now()` фиксируется в момент открытия экрана (или, в `StateSavers`, в момент сериализации) и комбинируется с `selectedDate` через `atTime(...)`. Это даёт выбранную пользователем дату + актуальное время — что соответствует iOS-семантике. Не путать с `LocalDateTime.now()` (текущая дата + время, проигнорирует выбор пользователя) и с `Instant.now()` (момент без даты, тоже проигнорирует выбор).

### D2. При редактировании: сохранять time-of-day из исходного `timestamp`

**Решение:** В `CreateEditScreenViewModel` при инициализации из существующего `Item` хранить `originalTimeOfDay: LocalTime = Instant.ofEpochMilli(item.timestamp).atZone(ZoneId.systemDefault()).toLocalTime()`. При сохранении использовать `selectedDate.atTime(originalTimeOfDay)` вместо `atStartOfDay()`.

**Альтернатива:** Всегда `Instant.now()` при сохранении. **Отклонено:** сдвигает time-of-day при каждом редактировании, недетерминированно.

**Обоснование:** Сохранение time-of-day из исходного `timestamp` означает, что повторное редактирование даты не сдвигает запись в сортировке. Это iOS-паттерн (`EditItemScreen.swift:24` использует `oldItem?.timestamp ?? .now`).

### D3. ORDER BY в `ItemDao`: первично `timestamp`, вторично `id`

**Решение:** Оставить `ORDER BY timestamp DESC, id DESC` (и ASC аналогично). Tie-breaker по `id` остаётся, но его роль меняется с «порядок создания» на «стабильный ключ для edge-case коллизий».

**Альтернатива:** Убрать tie-breaker совсем: `ORDER BY timestamp DESC`. **Отклонено:** для legacy-данных (с `timestamp = atStartOfDay`) это вернёт недетерминированный порядок (откат к поведению до change `fix-sort-for-today-records-bug`); для коллизий миллисекунд тоже теряем стабильность.

**Обоснование:** `id` — стабильный, монотонный по rowid ключ, не требует дополнительных данных, защищает от edge-case'ов. Стоимость минимальна (один столбец в ORDER BY).

### D4. Не мигрировать legacy-данные

**Решение:** Записи, созданные до этого change'а, остаются с `timestamp = atStartOfDay(...)`. Tie-breaker по `id` для них срабатывает в обычном режиме (как сейчас). Delete+restore таких записей по-прежнему может инвертировать их порядок — это known limitation, не лечится без потерь.

**Альтернатива:** Сделать миграцию БД v3, заполнив time-of-day для legacy-записей. **Отклонено:** нельзя узнать исходный time-of-day, любое значение (NOW, NOON, MIDNIGHT) — фикция, которая может ухудшить ситуацию.

**Обоснование:** Миграция невозможна без потерь; честнее оставить как есть и зафиксировать ограничение в KDoc / release notes.

### D5. KDoc фиксирует новую семантику

**Решение:** Обновить KDoc `ItemDao` явно:
- «`timestamp` хранится с millisecond precision; это единственный осмысленный критерий порядка same-date событий».
- «`id` — defensive tie-breaker, не отражает «новизну» события; используется для стабильности при коллизиях миллисекунд и для legacy-данных без time-of-day».

**Альтернатива:** Не обновлять KDoc. **Отклонено:** новый разработчик не поймёт, почему tie-breaker остался, и может решить «упростить» SQL, вернув недетерминированный порядок.

## Risks / Trade-offs

**[Risk] Legacy-данные не защищены от delete+restore инверсии порядка** → задокументировать в KDoc и в release notes; пользователям с важными same-date записями рекомендовать создать «тестовую» запись после обновления, чтобы запустить новую логику.

**[Risk] Пользователь не видит time-of-day в UI, но сортировка его учитывает** → совпадает с iOS-поведением; задокументировать в release notes как «улучшение: same-date события теперь сортируются по времени создания, как в iOS».

**[Risk] Sub-day перестановка для событий, созданных утром и вечером одного дня** → ожидаемое поведение, соответствует iOS. Не регрессия.

**[Risk] Расхождение legacy и новых данных в одной БД** → гетерогенная сортировка: для новых — естественный порядок по времени, для старых — tie-breaker по `id`. Пользователь увидит «правильный» порядок для новых и «условно правильный» для старых. Не баг, ожидаемое поведение при миграции без данных.

**[Trade-off] `atStartOfDay(...)` использовался в `StateSavers.kt:20, 73, 102, 157` (4 места) И в `CreateEditScreen.kt:87, 158, 215` (3 места) — всего 7 production-мест, относящихся к `Item.timestamp`.** Нужно поправить все 7, чтобы избежать рассинхрона. `CreateEditButtons.kt:39` (для `DatePicker.rememberDatePickerState`) остаётся без изменений — DatePicker оперирует только date-компонентом. `CreateEditPreviewComponents.kt:76` (используется только в `@Preview`) — можно оставить, не влияет на runtime. Покрывается тестами.

**[Сквозной grep-конфликт] `grep atStartOfDay app/src/main` даёт 10 совпадений, из них 9 покрыты change'ом.** 10-е — `BuildReminderUseCase.kt:44`, относится к `Reminder.selectedDateEpochMillis` (идентификатор выбранной даты в напоминании), а не к `Item.timestamp`. Это разные домены: `Reminder` хранит пару `(targetMillis, selectedDateEpochMillis)` для расписания уведомлений, `Item` хранит `timestamp` события. **Реализатор: не трогать** `BuildReminderUseCase.kt`, пусть grep'ом проверит, что 9 покрытых мест исправлены и 1 осталась в покое.

**[Trade-off] `StateSavers` фиксирует time-of-day в момент сериализации state, а не в момент открытия экрана.** Compose `Saver.save` вызывается на configuration change (например, поворот экрана). Если пользователь открыл экран в 14:00, заполнил форму 5 минут, потом повернул устройство — `LocalTime.now()` в `save =` будет 14:05, а не 14:00. После `restore =` время отбрасывается (`toLocalDate()`), а `toItem()` (строка 215) использует `LocalTime.now()` уже в момент сохранения, не из state. **Net effect:** time-of-day при сохранении берётся из `LocalTime.now()` в `toItem()`, а не из `StateSavers`. Это означает, что `StateSavers` фактически хранит только `LocalDate` (time-of-day не переживает configuration change в production-пути). Документируем как характеристику, не как баг. Если потребуется сохранять time-of-day между configuration change'ами — отдельная задача.

**[Rationale] Зачем менять `StateSavers`, если net effect = 0?** Правки 1.1–1.4 нужны по трём причинам: (a) **Единообразие кодовой базы** — убираем все 7 `atStartOfDay(...)` в production-коде, чтобы не было двух разных способов формировать timestamp из `LocalDate`. (b) **Гигиена для будущих разработчиков** — новый человек не увидит `atStartOfDay(...)` в StateSavers и не скопирует этот паттерн в новый Saver, думая, что это «правильно». (c) **Defensive coding** — если кто-то позже изменит `Saver.restore` на сохранение time-of-day (например, для восстановления полного `LocalDateTime`), `save` уже будет корректным и не потребует синхронной правки. Без этого обоснования задачи 1.1–1.4 выглядят как ненужная работа; с ним — как инвестиция в поддерживаемость.

**[Trade-off] Кросс-платформенная совместимость** — улучшается (iOS уже хранит millis, Android теперь тоже). Старые Android-бэкапы с `atStartOfDay` будут импортированы без time-of-day, попадут в категорию «legacy-данных».

**[Risk] DST edge case** — `LocalTime.now().atZone(ZoneId.systemDefault())` в час перевода часов («spring forward» — 02:00–03:00 не существует, «fall back» — 01:00–02:00 дублируется) разрешается JDK корректно через offset. Риск минимален: пользователь вряд ли создаёт события в 3 часа ночи в день перевода часов. Не чиним (особенность `java.time`), только упоминаем.

**[Risk] `checkHasChanges` ломается при сравнении time-of-day** — текущая реализация в `CreateEditScreen.kt:87, 158` строит `changeInput.timestamp` через `atStartOfDay(...)`, сравнивая с `original.timestamp`, который после нашего change'а имеет time-of-day. → ложное `hasChanges = true` сразу при открытии экрана редактирования. **Критический баг, который мы создаём своим change'ом.** Решение: сравнивать `LocalDate` (только date-компонент), а не millis-`timestamp`. Альтернатива: передавать `originalTimeOfDay` через `CreateEditChangeInput` и строить `changeInput.timestamp` через `selectedDate.atTime(originalTimeOfDay)` — но это сложнее и дублирует логику из `toItem()`. **Выбрано:** сравнение `LocalDate`.

## Migration Plan

**Не требуется** (ни схемы БД, ни формата бэкапа). Деплой сводится к публикации новой версии APK. Пользователи, обновившиеся, продолжат работать со старыми записями как раньше (tie-breaker по `id`); новые записи будут иметь полный `timestamp`. Поведение деградирует мягко: нет резкого изменения порядка, только постепенное «оздоровление» по мере создания новых записей.

**Rollback:** Если потребуется откатить change (например, обнаружится непредвиденный баг с time-of-day), достаточно вернуть `atStartOfDay(...)` в 7 production-местах (4 в `StateSavers.kt` + 3 в `CreateEditScreen.kt`); плюс вернуть сравнение `Long` (вместо `LocalDate`) в `checkHasChanges`. Старые `timestamp`-значения останутся валидными (с time-of-day), пользователь увидит «более точное» время события, но `id` снова станет tie-breкером. Потеря данных невозможна.

## Open Questions

1. **Что делать, если пользователь хочет видеть time-of-day в UI?** — вне scope этого change. Может быть отдельной задачей (например, добавить опциональное отображение времени в `ItemDetailScreen`).
2. **Нужно ли экспортировать/импортировать поле `createdAt` в `BackupItem` для оффлайн-миграции legacy-данных?** — вне scope. Для новых данных time-of-day в `timestamp` достаточно. Legacy-данные остаются с `id`-tie-breaker.
3. **Применять ли change автоматически (force-migrate при первом запуске новой версии) или постепенно (только новые записи)?** — выбрано «постепенно» (D4). Альтернатива: в `onCreate` `DaysDatabase` запустить однократный SQL `UPDATE items SET timestamp = timestamp + X` где X — фиксированный сдвиг (например, текущее время минус `atStartOfDay(timestamp)`). **Отклонено:** фиктивный time-of-day, не отражает реальный момент создания.
