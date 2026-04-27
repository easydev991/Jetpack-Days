# План доработки: напоминания для Create/Edit экрана (TDD)

Дата: 2026-04-27  
Статус: In Progress

## 1. Цель

Добавить на экран создания/редактирования записи новый блок внизу формы:

1. Тоггл `Добавить напоминание` (по умолчанию выключен).
2. При включении тоггла:
1. Режим `На дату`: дата из календаря + отдельная настройка времени.
2. Режим `Через N`: целое число + единица периода (`дней`, `недель`, `месяцев`, `лет`).

После сохранения записи создается одноразовое уведомление для конкретной записи.  
По тапу на уведомление приложение открывает экран этой записи и помечает напоминание как использованное (`CONSUMED`).

## 2. Фактически реализовано

1. [x] Домен напоминаний: модели `Reminder`, `ReminderMode`, `ReminderIntervalUnit`, `ReminderStatus`.
2. [x] Доменная валидация/расчет времени: `BuildReminderUseCase` + `ReminderRequest`.
3. [x] Data слой: `ReminderEntity`, `ReminderDao`, `ReminderMapper`, `ReminderRepository`, `ReminderRepositoryImpl`.
4. [x] Миграция БД `1 -> 2`: таблица `reminders`, индекс, подключение миграции в `DaysDatabase`.
5. [x] Инфраструктура уведомлений: `AlarmReminderScheduler`, `ReminderAlarmReceiver`, `ReminderBootReceiver`, `ReminderManager`/`DefaultReminderManager`.
6. [x] Навигация по уведомлению: обработка intent в `MainActivity`, переход на `Detail`, consume + cancel notification.
7. [x] Интеграция в Create/Edit:
1. вынесена секция `ReminderSettingsSection`,
2. состояние в `CreateEditUiState.reminder`,
3. `saveItem(...)` во ViewModel с сохранением/очисткой reminder,
4. `hasChanges` учитывает fingerprint напоминания.
8. [x] Локализация новых строк (`values`/`values-ru`) для UI и notification channel/content.
9. [x] Превью секции напоминаний (enabled/disabled).
10. [x] На `DetailScreen` показывается предстоящая дата/время активного reminder для записи.

## 3. TDD-прогресс по фазам

### Фаза A. Доменная логика и валидация

1. [x] Unit-тесты на расчет `targetEpochMillis` для `На дату` и `Через N`.
2. [x] Unit-тесты валидации (невалидный `N`, прошедшая дата/время).

Реализация/тесты:
- `BuildReminderUseCaseTest`
- `BuildReminderUseCase`

### Фаза B. Data слой

1. [x] Тест миграции БД (`DaysDatabaseMigrationTest`).
2. [x] Тесты entity/mapper/repository (`ReminderEntityTest`, `ReminderMapperTest`, `ReminderRepositoryImplTest`).
3. [x] Реализация Room + repository.

Примечание: DAO покрыт через repository/fake DAO и migration-тест, отдельного Android instrumentation DAO-теста пока нет.

### Фаза C. Scheduler/Notification

1. [x] Реализация scheduler/receiver/channel/boot-reschedule.
2. [x] Unit-тесты на orchestration в `DefaultReminderManagerTest`.
3. [ ] Unit/robolectric тесты непосредственно для `AlarmReminderScheduler` и `ReminderAlarmReceiver` (PendingIntent/extras/канал).

### Фаза D. Навигация из уведомления

1. [x] Реализована обработка intent + переход в `DetailScreen(itemId)`.
2. [x] Реализовано consume/removal эффекта после открытия из уведомления.
3. [ ] Добавить unit/integration тесты для `MainActivity` intent-flow и регресса обычного старта.

### Фаза E. ViewModel + форма Create/Edit

1. [x] `hasChanges` учитывает поля напоминания.
2. [x] Сохранение создает/очищает reminder через `ReminderManager`.
3. [x] Тесты на новый save-flow (`CreateEditScreenViewModelReminderTest`).
4. [x] Дефолты reminder-состояния в `CreateEditUiStateTest`.

### Фаза F. UI тесты Compose

1. [ ] Toggle по умолчанию выключен.
2. [ ] При включении показываются параметры.
3. [ ] Режим `На дату`: доступны date/time picker.
4. [ ] Режим `Через N`: только цифры + переключение единиц.
5. [ ] Невалидные значения блокируют сохранение.

### Фаза G. Сквозные сценарии (integration)

1. [ ] E2E/интеграционный сценарий: create/edit + schedule/cancel reschedule.
2. [ ] Сценарий: тап по notification -> detail(itemId) -> reminder consumed.
3. [ ] Сценарий повторной постановки напоминания после consume.

## 4. Изменяемые зоны кода (актуализировано)

1. [x] UI: `ui/screens/createedit/*`.
2. [x] ViewModel: `ui/viewmodel/CreateEditScreenViewModel.kt`, `ui/viewmodel/DetailScreenViewModel.kt`.
3. [x] Навигация: `ui/screens/common/RootScreenComponents.kt`, `ui/screens/root/RootScreen.kt`.
4. [x] Activity entrypoint: `MainActivity.kt`.
5. [x] Data/DB/domain: `data/database/*`, `data/repository/*`, `domain/*`, `domain/usecase/*`.
6. [x] Android infrastructure: `AndroidManifest.xml`, `reminder/*`.
7. [x] Ресурсы: `res/values/strings.xml`, `res/values-ru/strings.xml`.

## 5. Риски и технический долг

1. Backup-совместимость:
- Статус: [x] учтено (модель backup не менялась).

2. Timezone/летнее время:
- Статус: [x] базово покрыто в domain-тестах.
- Остаток: [ ] добавить edge-case тесты переходов DST для нескольких зон.

3. Дубли alarm:
- Статус: [x] реализован `cancel-before-schedule` в scheduler.

4. Android 13+ permission на уведомления:
- Статус: [~] частично (permissions добавлены в manifest; UI-запрос runtime permission не реализован в этом этапе).
- Остаток: [ ] добавить UX-поток запроса/обработки `POST_NOTIFICATIONS`.

5. Тестовое покрытие платформенной части:
- Статус: [ ] нужно расширить (receiver/scheduler/MainActivity intent tests).

## 6. Актуальные следующие шаги

1. Добавить тесты фазы C/D/F/G (приоритет: C -> D -> F -> G).
2. Добавить runtime-permission UX для `POST_NOTIFICATIONS`.
3. Добавить UI-сообщения об ошибке валидации reminder (сейчас сохранение блокируется, но без явного текстового фидбека в форме).
4. После расширения покрытия прогнать полный `make test` и зафиксировать финальный DoD.

## 6.1. Исправления по ревью (новый инкремент)

1. [x] Убрать дублирование fingerprint-логики:
   `CreateEditScreenViewModel` должен использовать `toChangeFingerprint()` из `CreateEditReminderState.kt` как единый источник.
2. [x] Исключить риск обхода reminder-flow:
   удалить legacy-методы `createItem`/`updateItem` из `CreateEditScreenViewModel` и перевести тесты на `saveItem(...)`.
3. [x] Добавить прямые unit-тесты для `ReminderFormUiState`:
   `toReminderRequest`, `isInputValid`, fingerprint-конвертация.
4. [x] Показать в `DetailScreen` предстоящую дату/время активного reminder для текущей записи.
5. [x] Расширить `DetailScreenViewModel` тестами для reminder-поля в `Success` и проверкой совместимости удаления записи с очисткой reminder.
6. [x] Проверить регрессии по бэкапам и существующим сценариям:
   обязательный прогон `make test` после форматирования.
7. [x] Статус инкремента: выполнено, тесты зелёные (`make format`, таргетные reminder/detail-тесты, `make test`).

## 7. Definition of Done (обновленный)

1. [~] Все новые unit/integration/UI тесты зелёные.
Сейчас: unit/часть integration закрыты, compose/integration сценарии в плане.
2. [x] `make format` и `make test` проходят.
Сейчас: `make format` и полный `make test` выполнены после инкремента с исправлениями по ревью.
3. [x] На форме есть новый блок напоминаний внизу с требуемым поведением.
4. [x] Уведомление одноразовое и открывает детали нужной записи.
5. [x] После открытия из уведомления reminder помечается как `CONSUMED`; повторная постановка возможна.
6. [x] Нет `!!`, архитектурные паттерны проекта сохранены.
