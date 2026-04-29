# Экран 3.1: Item Screen (Просмотр записи)

Дата обновления: 2026-04-29  
Статус: Реализован и используется в навигации (`Screen.ItemDetail`)

## Назначение

Экран отображает полную информацию о записи и позволяет:

1. Просматривать все поля записи в read-only виде.
2. Перейти к редактированию.
3. Удалить запись с подтверждением.
4. Увидеть предстоящее напоминание для записи, если оно активно и еще не наступило.

## Текущее поведение

1. `TopAppBar`:
   - кнопка `Назад` возвращает на предыдущий экран;
   - кнопка `Редактировать` открывает `Create/Edit` для текущей записи;
   - кнопка `Удалить` открывает диалог подтверждения.
2. Контент:
   - название;
   - описание (если не пустое);
   - цветовая метка (если есть);
   - дата записи + текст анализа количества дней;
   - формат отображения (`DAY`, `MONTH_DAY`, `YEAR_MONTH_DAY`).
3. Секция напоминания:
   - показывается только при наличии активного будущего reminder;
   - заголовок — `R.string.reminder_settings`;
   - значение — локализованные дата и время напоминания.
4. Удаление:
   - при подтверждении запись удаляется;
   - связанное напоминание очищается через `ReminderManager.clearReminder(itemId)`.

## Состояние и логика

1. `DetailScreenViewModel` хранит состояние в `StateFlow<DetailScreenState>` (`Loading`, `Success`, `Error`).
2. Основные данные записи читаются из `ItemRepository.getItemFlow(itemId)`.
3. Напоминание подмешивается отдельно через `ReminderManager.getActiveReminder(itemId)`.
4. Просроченное напоминание не показывается: используется фильтр `targetEpochMillis > currentTimeMillis`.
5. Для актуализации данных напоминания при возврате на экран используется `refreshReminder()`.

## Навигация

1. Маршрут: `Screen.ItemDetail.createRoute(itemId)`.
2. Подключение экрана: `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`.
3. Переход на редактирование передает текущие данные записи и текущее reminder-состояние для быстрого prefill формы.

## Тестирование (фактическое покрытие)

1. Unit:
   - `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt`
   - `app/src/test/java/com/dayscounter/ui/state/DetailScreenStateTest.kt`
2. Покрыты ключевые сценарии:
   - успешная загрузка записи;
   - отображение/скрытие reminder (будущее/прошедшее);
   - удаление записи и очистка reminder;
   - обработка базовых переходов состояния.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreenParams.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailAppBar.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContent.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailStates.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/DetailScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
