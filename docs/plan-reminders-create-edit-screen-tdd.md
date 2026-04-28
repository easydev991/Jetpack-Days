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

1. [x] Реализованы domain/data/DB слои напоминаний, включая миграцию `1 -> 2`.
2. [x] Реализована инфраструктура уведомлений и одноразовый flow: schedule -> tap -> `Detail` -> consume/cancel.
3. [x] Интегрирован reminder в Create/Edit и Detail (состояние, сохранение, `hasChanges`, показ предстоящего напоминания).
4. [x] Добавлены локализации, превью и UX-улучшения секции reminder (title из записи, default date +1 день, auto-scroll).
5. [x] Реализован runtime-запрос `POST_NOTIFICATIONS` на Android 13+.
6. [x] Добавлено покрытие unit/integration/compose тестами для reminder-flow.

## 3. TDD-прогресс по фазам

### Фаза A. Доменная логика и валидация

1. [x] Unit-тесты на расчет `targetEpochMillis` для `На дату` и `Через N`.
2. [x] Unit-тесты валидации (невалидный `N`, прошедшая дата/время).

### Фаза B. Data слой

1. [x] Тест миграции БД (`DaysDatabaseMigrationTest`).
2. [x] Тесты entity/mapper/repository (`ReminderEntityTest`, `ReminderMapperTest`, `ReminderRepositoryImplTest`).
3. [x] Реализация Room + repository.

Примечание: DAO покрыт через repository/fake DAO и migration-тест, отдельного Android instrumentation DAO-теста пока нет.

### Фаза C. Scheduler/Notification

1. [x] Реализация scheduler/receiver/channel/boot-reschedule.
2. [x] Unit-тесты на orchestration в `DefaultReminderManagerTest`.
3. [~] Платформенные тесты scheduler/receiver:
   - [x] `ReminderAlarmReceiverInstrumentedTest` (валидный/невалидный payload + канал).
   - [~] `AlarmReminderSchedulerInstrumentedTest`: покрыт сценарий cancel/remove `PendingIntent`, проверка exact-fire всё ещё pending.

### Фаза D. Навигация из уведомления

1. [x] Реализована обработка intent + переход в `DetailScreen(itemId)`.
2. [x] Реализовано consume/removal эффекта после открытия из уведомления.
3. [x] Добавлены unit-тесты intent-flow обычного старта (`ReminderIntentParserTest`) и интеграция parser в `MainActivity`.

### Фаза E. ViewModel + форма Create/Edit

1. [x] `hasChanges` учитывает поля напоминания.
2. [x] Сохранение создает/очищает reminder через `ReminderManager`.
3. [x] Тесты на новый save-flow (`CreateEditScreenViewModelReminderTest`).
4. [x] Дефолты reminder-состояния в `CreateEditUiStateTest`.

### Фаза F. UI тесты Compose

1. [x] Toggle по умолчанию выключен (параметры скрыты).
2. [x] При включении показываются параметры.
3. [x] Режим `На дату`: отображаются поля даты/времени.
4. [~] Режим `Через N`: отображение interval/unit закрыто compose-тестом, фильтрация only-digits покрыта unit-тестами состояния.
5. [ ] Невалидные значения блокируют сохранение (нужен отдельный compose/integration сценарий кнопки Save).
6. [x] Для маленького экрана добавлен auto-scroll к настройкам reminder при включении (покрыт `CreateEditReminderAutoScrollUiTest`).

### Фаза G. Сквозные сценарии (integration)

1. [ ] E2E/интеграционный сценарий: create/edit + schedule/cancel reschedule.
2. [ ] Сценарий: тап по notification -> detail(itemId) -> reminder consumed.
3. [ ] Сценарий повторной постановки напоминания после consume.

### Фаза H. Runtime permission уведомлений

1. [x] Добавлен policy-слой для решения по toggle (`ENABLE` / `DISABLE` / `REQUEST_PERMISSION`) без UI-зависимостей.
2. [x] Интегрирован runtime-запрос `POST_NOTIFICATIONS` через `ActivityResultContracts.RequestPermission` при включении reminder.
3. [x] Добавлены unit-тесты на policy (`ReminderNotificationPermissionPolicyTest`).
4. [x] Прогнаны проверки качества после внедрения (`make format`, таргетные тесты, `make test`).

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
- Статус: [x] реализовано (добавлен runtime-запрос `POST_NOTIFICATIONS` при включении reminder на Android 13+).
- Остаток: [ ] при необходимости добавить отдельный UX-фидбек после отказа в разрешении.

5. Тестовое покрытие платформенной части:
- Статус: [~] расширено частично (receiver + MainActivity intent parser tests).
- Остаток: [ ] дополнить scheduler-тест проверкой exact-fire/schedule сценария на API 36.

## 6. Актуальные следующие шаги

1. Добавить оставшиеся тесты фазы C/F/G:
   - расширить `AlarmReminderScheduler`-тест проверкой exact-fire/schedule,
   - compose/integration тест блокировки Save при невалидном reminder,
   - минимум один сквозной integration-сценарий create/edit -> consume -> повторная постановка reminder.
2. Добавить UI-сообщение об ошибке валидации reminder (сейчас сохранение блокируется, но без явного текстового фидбека в форме).
3. После закрытия пункта 1 прогнать полный цикл проверок: `make format`, `make test`, `make lint`.

## 6.1. Исправления по ревью (новый инкремент)

1. [x] Устранено дублирование fingerprint-логики через единый `toChangeFingerprint()`.
2. [x] Удалены legacy `createItem`/`updateItem`; сохранение унифицировано через `saveItem(...)`.
3. [x] Добавлены прямые unit-тесты `ReminderFormUiState` (`toReminderRequest`, `isInputValid`, fingerprint).
4. [x] Добавлен показ предстоящего reminder в `DetailScreen` и расширены тесты `DetailScreenViewModel`.
5. [x] Выполнены регрессионные проверки (форматирование и тесты), внесены микро-улучшения state-flow.

## 6.2. Инкремент UX напоминаний (2026-04-29)

1. [x] Улучшен UX reminder: title уведомления из записи, default date +1 день, auto-scroll на малых экранах.
2. [x] Пройдены проверки качества: `make format`, `make test`, `make lint`, таргетные `connectedDebugAndroidTest`.

## 7. Definition of Done (обновленный)

1. [~] Все новые unit/integration/UI тесты зелёные.
Сейчас: unit + новые reminder compose/instrumentation тесты зелёные; остаётся scheduler-test и отдельный сценарий Save-disable в compose/integration.
2. [x] `make format` и `make test` проходят.
Сейчас: `make format` и полный `make test` выполнены после инкремента с исправлениями по ревью; `make lint` также проходит без новых предупреждений.
3. [x] На форме есть новый блок напоминаний внизу с требуемым поведением.
4. [x] Уведомление одноразовое и открывает детали нужной записи.
5. [x] После открытия из уведомления reminder помечается как `CONSUMED`; повторная постановка возможна.
6. [x] Нет `!!`, архитектурные паттерны проекта сохранены.
