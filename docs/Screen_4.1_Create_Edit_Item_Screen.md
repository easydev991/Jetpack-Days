# Экран 4.1: Create/Edit Item Screen (Создание и редактирование записей)

Дата обновления: 2026-04-29  
Статус: Реализован и используется в навигации (`Screen.CreateItem`, `Screen.EditItem`)

## Назначение

Экран предназначен для:

1. Создания новой записи.
2. Редактирования существующей записи.
3. Настройки одноразового напоминания для записи.
4. Валидации формы и сохранения только корректных данных.

## Текущее поведение

1. `TopAppBar`:
   - создание: заголовок "Новая запись";
   - редактирование: заголовок "Редактировать";
   - кнопка назад вызывает `onBackClick`.
2. Поля формы:
   - название (обязательное);
   - описание (необязательное);
   - дата записи (через `DatePickerDialog`);
   - цветовая метка;
   - формат отображения (`DAY`, `MONTH_DAY`, `YEAR_MONTH_DAY`).
3. Блок напоминания (внизу формы):
   - toggle `Добавить напоминание`;
   - режим `На дату` (дата + время);
   - режим `Через N` (целое число + единица `дней/недель/месяцев/лет`);
   - при включении на малом экране выполняется анимированный скролл к раскрытому блоку, если он не виден.
4. Кнопка `Сохранить`:
   - создание: активна только при валидной форме;
   - редактирование: активна только при валидной форме и наличии изменений.
5. Сохранение:
   - используется единый `saveItem(...)`;
   - запись создается/обновляется;
   - reminder сохраняется/очищается синхронно с записью;
   - после успеха выполняется возврат назад.

## Валидация

1. Базовая: `title.isNotBlank()` и выбранная дата записи.
2. Reminder-валидация:
   - toggle выключен: reminder валиден;
   - `На дату`: дата/время reminder должны быть в будущем;
   - `Через N`: только цифры и значение `> 0`.
3. Если reminder включен и невалиден, кнопка сохранения отключена и показывается локализованная ошибка.
4. Дефолт для режима `На дату`: текущая дата + 1 день.
5. В режиме `Через N` используется текущее время суток; в режиме `На дату` время задается отдельно.

## Разрешения и доступность уведомлений

1. Android 13+:
   - при включении toggle запрашивается `POST_NOTIFICATIONS`.
2. После шага permission дополнительно проверяется доступность уведомлений:
   - глобально для приложения;
   - для reminder-канала (если канал уже существует).
3. Если уведомления недоступны:
   - toggle не включается или выключается при возврате на экран;
   - показывается локализованный `Snackbar` с action для перехода в системные настройки уведомлений.
4. Отсутствие reminder-канала до первой публикации уведомления не блокирует включение toggle.

## Поведение reminder

1. Поддерживаются два способа планирования:
   - точная дата/время;
   - интервал (`N * unit`).
2. Reminder одноразовый.
3. По срабатыванию показывается системное уведомление.
4. Заголовок уведомления: фактический `title` записи (fallback — строка по умолчанию).
5. Текст уведомления: локализованный CTA на открытие записи.
6. По тапу на уведомление открывается `Detail` конкретной записи.
7. После открытия из уведомления reminder переводится в `CONSUMED`, чтобы можно было назначить новый.

## Архитектура

1. Presentation:
   - `CreateEditScreen`, `CreateEditFormContent`, `ReminderSettingsSection`, эффекты/политики reminder.
2. ViewModel:
   - `CreateEditScreenViewModel` хранит состояние, отслеживает `hasChanges`, выполняет `saveItem(...)`.
3. Domain:
   - `ReminderRequest`, `BuildReminderUseCase`, модели reminder.
4. Data:
   - `ReminderEntity`, `ReminderDao`, repository, мапперы.
5. Infra:
   - `ReminderManager`, `AlarmReminderScheduler`, `ReminderAlarmReceiver`, `ReminderBootReceiver`.

## Навигация

1. Создание: `Screen.CreateItem.route`.
2. Редактирование: `Screen.EditItem.createRoute(itemId)`.
3. Переход из `Detail` в `Edit` использует prefill, чтобы минимизировать визуальные артефакты загрузки.
4. Открытие из уведомления маршрутизируется через `ReminderIntentContract` с передачей `itemId`.

## Тестирование (фактическое покрытие)

1. Unit:
   - `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/CreateEditUiStateTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/CreateEditReminderStateTest.kt`
   - `app/src/test/java/com/dayscounter/ui/screens/createedit/ReminderNotificationPermissionPolicyTest.kt`
   - saver-тесты (`LocalDateSaverTest`, `ColorSaverTest`, `DisplayOptionSaverTest`)
   - domain/data/manager тесты для reminder-цепочки.
2. Instrumentation/UI:
   - `app/src/androidTest/java/com/dayscounter/ui/screens/createedit/CreateEditScreenCustomColorTest.kt`
   - `app/src/androidTest/java/com/dayscounter/ui/screens/createedit/ReminderSettingsSectionTest.kt`
   - reminder receiver/scheduler сценарии в reminder instrumentation-тестах.

## Совместимость и ограничения

1. `minSdk = 26`, reminder-функционал поддерживает Android 8+.
2. При ограничениях exact alarms используется безопасная деградация без падений.
3. Backup-совместимость не нарушается: существующий backup-поток не ломается.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditFormContent.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditReminderEffects.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/ReminderSettingsSection.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditReminderState.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/ReminderNotificationPermissionPolicy.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/reminder/DefaultReminderManager.kt`
- `app/src/main/java/com/dayscounter/reminder/AlarmReminderScheduler.kt`
- `app/src/main/java/com/dayscounter/reminder/ReminderAlarmReceiver.kt`
- `app/src/main/java/com/dayscounter/reminder/ReminderBootReceiver.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
