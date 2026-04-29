# Экран 4.1: Create/Edit Item Screen (Создание и редактирование записей)

Дата обновления: 2026-04-29  
Статус: Реализован и используется в навигации (`Screen.CreateItem`, `Screen.EditItem`)

## Доработка по TDD: доступность уведомлений

Статус: выполнено (2026-04-29).

1. Red:
   - добавлены unit-тесты для сценариев разрешения и отключенных уведомлений (app/channel).
2. Green:
   - при включении toggle на Android 13+ сначала обрабатывается `POST_NOTIFICATIONS`;
   - далее проверяется фактическая доступность уведомлений (глобально и по reminder-каналу);
   - если уведомления недоступны, toggle остается выключенным и показывается локализованный `Snackbar` с action для перехода в настройки уведомлений.
   - при возврате на экран (`ON_RESUME`) состояние toggle синхронизируется с системным статусом уведомлений.
3. Refactor:
   - выделены policy/helper-функции для единообразного решения по активации reminder;
   - добавлена синхронизация reminder-state при возврате из системных настроек;
   - убрано дублирование поведения для веток включения reminder.
4. Verify:
   - `make format`;
   - таргетные unit-тесты;
   - `make test`;
   - `make lint`.

## Назначение

Экран для создания новой записи или редактирования существующей с валидацией, сохранением, отслеживанием изменений и настройкой одноразового напоминания.

## Текущее поведение

1. `TopAppBar`:
   - для создания: заголовок "Новая запись";
   - для редактирования: заголовок "Редактировать";
   - кнопка назад вызывает `onBackClick`.
2. Поля формы:
   - название (обязательное);
   - описание (необязательное, `minLines = 3`);
   - дата (read-only поле + `DatePickerDialog`);
   - цветовая метка (6 preset-цветов, повторный тап снимает выбор);
   - формат отображения дней (`DAY`, `MONTH_DAY`, `YEAR_MONTH_DAY`).
3. Блок напоминаний внизу формы:
   - toggle `Добавить напоминание` (по умолчанию выключен);
   - режим `На дату` (дата + время);
   - режим `Через N` (число + единица `дней` / `недель` / `месяцев` / `лет`);
   - для малых экранов при первом включении toggle выполняется анимированный автоскролл к секции.
4. Кнопка "Сохранить" в `bottomBar`:
   - для создания: активна, если заполнены обязательные поля и валиден reminder-блок;
   - для редактирования: активна, если заполнены обязательные поля, валиден reminder-блок и есть изменения.
5. При сохранении используется единый `saveItem(...)`:
   - создается/обновляется `Item`;
   - при валидном `ReminderRequest` вызывается `ReminderManager.saveReminder(...)`;
   - при выключенном reminder вызывается `ReminderManager.clearReminder(...)`;
   - выполняется возврат назад.

## Напоминания: сценарии

1. Пользователь включает `Добавить напоминание`.
2. Доступны два режима:
   - `На дату` — выбор даты и времени;
   - `Через N` — число + единица интервала.
3. При сохранении:
   - при валидных данных напоминание сохраняется и ставится в системный scheduler;
   - при выключенном toggle активное напоминание для записи очищается.
4. При срабатывании уведомления по тапу открывается `Detail` именно этой записи.
5. После открытия из уведомления напоминание переводится в `CONSUMED`, чтобы пользователь мог поставить новое.

## Валидация и состояние

1. Базовая валидация записи: `title.isNotEmpty()` и `selectedDate != null`.
2. Дополнительная валидация reminder:
   - если toggle выключен, блок считается валидным;
   - в режиме `На дату` дата/время напоминания должны быть в будущем;
   - в режиме `Через N` разрешены только цифры и значение `> 0`.
3. Если reminder включен и невалиден, кнопка сохранения отключена, в UI показывается текст ошибки валидации.
4. По умолчанию дата напоминания: текущая дата + 1 день.
5. Для режима `Через N` сохраняется текущее время суток; в режиме `На дату` время выбирается отдельно.
6. При редактировании `CreateEditScreenViewModel.checkHasChanges()` учитывает также fingerprint напоминания.
7. Состояние экрана: `CreateEditScreenState` (`Loading`, `Success`, `Error`).
8. Для сохранения состояния формы используются savers в `StateSavers.kt` (`LocalDate`, `Color`, `DisplayOption`).

## Права и уведомления

1. На Android 13+ при включении toggle выполняется запрос `POST_NOTIFICATIONS`.
2. После запроса permission (и для Android <13 при прямом включении) дополнительно проверяется доступность уведомлений приложения и reminder-канала.
3. Если уведомления отключены, toggle не включается, показывается `Snackbar` с кнопкой перехода в системные настройки уведомлений приложения.
4. После возвращения из системных настроек на экран формы toggle автоматически выключается, если уведомления стали недоступны.
5. Отсутствие reminder-канала (до первого фактического показа уведомления) не считается ошибкой и не блокирует включение toggle.
6. Уведомление одноразовое.
7. Заголовок уведомления берется из `title` записи (fallback — дефолтный заголовок).
8. Подзаголовок уведомления: локализованный текст перехода к просмотру записи.

## Архитектура реализации напоминаний

1. Domain:
   - модели `Reminder*`, `ReminderRequest`;
   - `BuildReminderUseCase` для построения валидного reminder и расчета `targetEpochMillis`.
2. Data/DB:
   - `ReminderEntity`, `ReminderDao`, mapper, repository;
   - миграция БД `1 -> 2` с таблицей reminders и индексами.
3. Infrastructure:
   - `AlarmReminderScheduler` — постановка/снятие alarm;
   - `ReminderAlarmReceiver` — публикация уведомления при fire;
   - `ReminderBootReceiver` — восстановление schedule после перезагрузки;
   - `ReminderManager` — orchestration save / clear / consume / reschedule.
4. Presentation:
   - `Create/Edit` — UI, валидация, сохранение reminder;
   - `Detail` — показ предстоящей даты активного reminder;
   - `MainActivity`/`Root` — обработка открытия из reminder-intent.

## Навигация

1. Переход на создание: `Screen.CreateItem.route`.
2. Переход на редактирование: `Screen.EditItem.createRoute(itemId)`.
3. Оба маршрута подключены в `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`.
4. При открытии из уведомления используется `itemId` из `ReminderIntentContract` и переход на экран детали записи.

## Жизненный цикл напоминания

1. `CreateEditScreenViewModel.saveItem(...)` сохраняет запись и синхронизирует reminder.
2. `ReminderManager.saveReminder(...)`:
   - валидирует request через `BuildReminderUseCase`;
   - сохраняет reminder в репозиторий;
   - ставит alarm через scheduler.
3. `ReminderAlarmReceiver` показывает уведомление с `itemId`.
4. По тапу на уведомление:
   - выполняется навигация в `Detail(itemId)`;
   - reminder переводится в `CONSUMED`;
   - notification очищается.

## Тестирование (фактическое состояние)

1. Unit:
   - `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/CreateEditUiStateTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/CreateEditReminderStateTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/ReminderNotificationPermissionPolicyTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/LocalDateSaverTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/ColorSaverTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/DisplayOptionSaverTest.kt`
   - reminder-domain/data/manager тесты (`BuildReminderUseCase*`, mapper/repository/manager lifecycle).
2. UI/instrumentation:
   - `app/src/androidTest/java/com/dayscounter/ui/screens/createedit/CreateEditScreenCustomColorTest.kt`
   - `app/src/androidTest/java/com/dayscounter/ui/screens/createedit/ReminderSettingsSectionTest.kt`
   - receiver/scheduler сценарии в reminder instrumentation-тестах.
3. Проверки качества:
   - `make format`
   - `make test`
   - `make lint`
   - таргетные `:app:connectedDebugAndroidTest` для reminder UI/receiver сценариев.

## Совместимость и ограничения

1. Backup-совместимость не нарушена: backup-модель не изменялась.
2. Функционал напоминаний рассчитан на Android 8+ (minSdk 26).
3. При недоступности `SCHEDULE_EXACT_ALARM` используется безопасная деградация (без падений).
4. Для disabled-notification сценария уже есть `Snackbar` с переходом в системные настройки; при необходимости можно дополнить отдельным экраном/диалогом.
5. Отдельного `CreateEditScreenViewModelIntegrationTest.kt` в текущем проекте нет.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditFormContent.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditReminderEffects.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/ReminderSettingsSection.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditReminderState.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/ReminderNotificationPermissionPolicy.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/DetailScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContent.kt`
- `app/src/main/java/com/dayscounter/reminder/DefaultReminderManager.kt`
- `app/src/main/java/com/dayscounter/reminder/AlarmReminderScheduler.kt`
- `app/src/main/java/com/dayscounter/reminder/ReminderAlarmReceiver.kt`
- `app/src/main/java/com/dayscounter/reminder/ReminderBootReceiver.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
