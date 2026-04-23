# Экран 4.1: Create/Edit Item Screen (Создание и редактирование записей)

## Статус

**Реализован и используется в навигации приложения** (`Screen.CreateItem`, `Screen.EditItem`).

## Назначение

Экран для создания новой записи или редактирования существующей с валидацией, сохранением и отслеживанием изменений.

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
3. Кнопка "Сохранить" в `bottomBar`:
   - для создания: активна, если заполнены обязательные поля;
   - для редактирования: активна, если заполнены обязательные поля и есть изменения.
4. При сохранении:
   - создается `Item` из текущей формы;
   - вызывается `createItem()` или `updateItem()` во `ViewModel`;
   - выполняется возврат назад.

## Валидация и состояние

- Валидация на уровне UI: `title.isNotEmpty()` и `selectedDate != null`.
- При редактировании `CreateEditScreenViewModel.checkHasChanges()` сравнивает `title`, `details`, `timestamp`, `colorTag`, `displayOption`.
- Состояние экрана: `CreateEditScreenState` (`Loading`, `Success`, `Error`).
- Для сохранения состояния формы используются savers в `StateSavers.kt` (`LocalDate`, `Color`, `DisplayOption`).

## Навигация

- Переход на создание: `Screen.CreateItem.route`.
- Переход на редактирование: `Screen.EditItem.createRoute(itemId)`.
- Оба маршрута подключены в `ui/screens/common/RootScreenComponents.kt`.

## Тестирование (фактическое состояние)

- Unit:
  - `app/src/test/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelTest.kt`
  - `app/src/test/java/com/dayscounter/ui/screens/createedit/CreateEditUiStateTest.kt`
  - `app/src/test/java/com/dayscounter/ui/screens/createedit/LocalDateSaverTest.kt`
  - `app/src/test/java/com/dayscounter/ui/screens/createedit/ColorSaverTest.kt`
  - `app/src/test/java/com/dayscounter/ui/screens/createedit/DisplayOptionSaverTest.kt`
- UI/instrumentation:
  - `app/src/androidTest/java/com/dayscounter/ui/screens/createedit/CreateEditScreenCustomColorTest.kt`

Отдельного файла `CreateEditScreenViewModelIntegrationTest.kt` в текущем проекте нет.

## Ограничения и примечания

- Автофокус в поле названия не реализован.
- Ошибки из `CreateEditScreenState.Error` логируются и хранятся во ViewModel, но отдельного UI-представления ошибки (snackbar/dialog) на этом экране сейчас нет.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditFormContent.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/CreateEditSelectors.kt`
- `app/src/main/java/com/dayscounter/ui/screens/createedit/StateSavers.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
