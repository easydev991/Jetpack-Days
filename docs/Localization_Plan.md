# План локализации для Android

Данный документ описывает реализацию локализации для Android-приложения на основе строк из iOS-приложения. Все строки определены в iOS-проекте (`Localizable.xcstrings`) и перенесены в Android-проект с сохранением идентичности переводов.

**Статус:** ✅ Локализация реализована и протестирована

## Источник истины

**iOS-приложение** (`SwiftUI-Days/SupportingFiles/Localizable.xcstrings`) является источником истины для всех локализованных строк. Android-версия должна использовать идентичные переводы.

## Поддерживаемые языки

- **Русский (ru)** — основной язык
- **Английский (en)** — дополнительный язык

## Структура локализации в Android

### Файлы ресурсов
- `app/src/main/res/values/strings.xml` — строки по умолчанию (английский)
- `app/src/main/res/values-ru/strings.xml` — русские переводы

### Формат строк

Все строки должны быть определены в формате Android strings.xml:

```xml
<resources>
    <string name="string_key">String value</string>
</resources>
```

---

## Список локализованных строк

### Общие строки

#### Навигация и вкладки
- `events` — "Events" / "События"
- `more` — "More" / "Ещё"

#### Кнопки действий
- `add_item` — "Add Item" / "Добавить запись"
- `save` — "Save" / "Сохранить"
- `cancel` — "Cancel" / "Отмена"
- `close` — "Close" / "Закрыть"
- `edit` — "Edit" / "Изменить"
- `delete` — "Delete" / "Удалить"
- `ok` — "Ok" / "Ок"
- `done` — "Done" / "Готово"

#### Заголовки экранов
- `events` — "Events" / "События"
- `more` — "More" / "Ещё"
- `new_item` — "New Item" / "Новая запись"
- `edit_item` — "Edit Item" / "Изменить запись"
- `app_data` — "App data" / "Данные приложения"
- `app_theme_and_icon` — "App theme and Icon" / "Тема и иконка приложения"

---

### Экран Main Screen

#### Заголовки и описания
- `what_should_we_remember` — "What should we remember?" / "Что нужно запомнить?"
- `create_your_first_item` — "Create your first item" / "Сделайте вашу первую запись"

#### Сортировка
- `sort` — "Sort" / "Сортировка"
- `sort_order` — "Sort Order" / "Порядок сортировки"
- `old_first` — "Old first" / "Сначала старые"
- `new_first` — "New first" / "Сначала новые"

---

### Экран Item Screen (Просмотр записи)

#### Заголовки секций
- `title` — "Title" / "Название"
- `details` — "Details" / "Детали"
- `date` — "Date" / "Дата"
- `color_tag` — "Color tag" / "Цветовой тег"
- `display_format` — "Display format" / "Формат отображения"

#### Специальные значения
- `today` — "Today" / "Сегодня"

---

### Экран Create/Edit Item Screen

#### Заголовки полей
- `title` — "Title" / "Название"
- `details` — "Details" / "Детали"
- `title_for_the_item` — "Title for the Item" / "Название для записи"
- `details_for_the_item` — "Details for the Item" / "Подробное описание"

#### Цветовая метка
- `add_color_tag` — "Add color tag" / "Добавить цветовой тег"
- `color_tag` — "Color tag" / "Цветовой тег"

#### Опции отображения дней
- `display_format` — "Display format" / "Формат отображения"
- `days_only` — "Days only" / "Только дни"
- `months_and_days` — "Months and days" / "Месяцы и дни"
- `years_months_and_days` — "Years, months and days" / "Годы, месяцы и дни"

---

### Экран More Screen

#### Кнопки
- `send_feedback` — "Send feedback" / "Отправить обратную связь"
- `rate_the_app` — "Rate app" / "Оценить приложение"
- `share_the_app` — "Share the app" / "Поделиться приложением"
- `github_page` — "GitHub page" / "Ссылка на GitHub"

#### Версия приложения
- `app_version` — "App version: %1$s" / "Версия приложения: %1$s"
  - Формат: строка с параметром для подстановки версии

---

### Экран Theme and Icon Screen

#### Заголовки секций
- `app_theme` — "App theme" / "Тема приложения"
- `app_icon` — "App Icon" / "Иконка приложения"

#### Опции темы
- `light` — "Light" / "Светлая"
- `dark` — "Dark" / "Тёмная"
- `system` — "System" / "Системная"

#### Иконки
- `primary_icon` — "Primary icon" / "Основная иконка"
- `variant` — "Variant %1$d" / "Вариант %1$d"
  - Формат: строка с параметром для номера варианта
- `selected` — "Selected" / "Выбрано"
- `not_selected` — "Not selected" / "Не выбрано"

---

### Экран App Data Screen

#### Кнопки действий
- `create_a_backup` — "Create a backup" / "Создать резервную копию"
- `restore_from_backup` — "Restore from backup" / "Восстановить из резервной копии"
- `delete_all_data` — "Delete all data" / "Удалить все данные"

#### Диалоги подтверждения
- `do_you_want_to_delete_all_data_permanently` — "Do you want to delete all data permanently?" / "Вы хотите навсегда удалить все данные?"

#### Сообщения результатов операций
- `backup_data_saved` — "Backup data saved" / "Резервная копия данных сохранена"
- `data_restored_from_backup` — "Data restored from backup" / "Данные восстановлены из резервной копии"
- `all_data_deleted` — "All data deleted" / "Все данные удалены"
- `unable_to_recover_data_from_the_selected_file` — "Unable to recover data from the selected file" / "Не удалось восстановить данные из выбранного файла"
- `error` — "Error" / "Ошибка"
- `event_deleted` — "Event deleted" / "Событие удалено"

---

### UI состояния и сообщения

##### Загрузка и навигация
- `loading` — "Loading" / "Загрузка..."
- `back` — "Back" / "Назад"

##### Поля формы
- `select_date` — "Select date" / "Выбрать дату"

##### Предпросмотр
- `preview` — "Preview" / "Предпросмотр"
- `elapsed` — "elapsed" / "прошло"
- `remaining` — "remaining" / "осталось"

##### Сообщения об ошибках
- `event_not_found` — "Event not found" / "Событие не найдено"
- `error_loading_event` — "Error loading event: %1$s" / "Ошибка загрузки события: %1$s"
- `error_creating_event` — "Error creating event: %1$s" / "Ошибка создания события: %1$s"
- `error_updating_event` — "Error updating event: %1$s" / "Ошибка обновления события: %1$s"
- `error_formatting` — "Formatting error" / "Ошибка форматирования"
- `error_calculating` — "Calculation error: %1$s" / "Ошибка при вычислении: %1$s"
- `error_formatting_details` — "Formatting error: %1$s" / "Ошибка форматирования: %1$s"

---

## План реализации

### Шаг 1: Создание файлов ресурсов

**Статус:** ✅ Выполнен

---

### Шаг 2: Строки с параметрами

**Статус:** ✅ Выполнен

---

### Шаг 3: Проверка соответствия iOS

**Статус:** ✅ Выполнен

---

### Шаг 4: Интеграция в код

**Статус:** ✅ Выполнен

---

### Шаг 5: Замена хардкод-строк на строковые ресурсы

**Статус:** ✅ Выполнен

---

### Шаг 6: Рефакторинг StubResourceProvider по best practice

**Статус:** ✅ Выполнен

---

### Шаг 7: Локализация форматирования дней, дат и числительных

**Статус:** ✅ Выполнен

**Выполнено:**
- ✅ Удалена неиспользуемая функция `applyAbbreviation()`
- ✅ Удалён неиспользуемый параметр `resourceProvider` из `formatComponents()`
- ✅ Удалён неиспользуемый тест с противоречащей логикой
- ✅ Реализовано поведение: всегда скрывать нулевые компоненты

---

### Шаг 8: Тестирование локализации

**Статус:** ✅ Выполнен

#### Подготовка окружения для тестирования

##### 8.1 Установка симулятора с разными локалями

Для тестирования локализации необходимо:
1. Создать два AVD (Android Virtual Device) с разными языками:
   - AVD #1: English (United States) - locale=en_US
   - AVD #2: Russian (Russia) - locale=ru_RU

2. Или использовать один AVD и менять локаль:
   ```bash
   # Настройка локали через adb
   adb shell "setprop persist.sys.locale en-US"
   adb shell "setprop ctl.restart zygote"
   ```

##### 8.2 Инструменты для проверки

Для автоматизированной проверки локализации можно использовать:
- **Android Lint** - проверка отсутствующих переводов
- **MissingTranslation** - lint-правило для обнаружения отсутствующих переводов
- **PluralsCandidate** - lint-правило для проверки plurals

---

## Критерии завершения этапа

Этап локализации считается завершенным, когда:

### Базовая локализация (шаги 1-6)
- ✅ Файлы ресурсов созданы, все строки из iOS перенесены
- ✅ Хардкод-строки заменены на ресурсы
- ✅ Локализация в ViewModel'ах реализована через DI с `ResourceProvider`
- ✅ `StubResourceProvider` используется только в тестах

### Локализация форматирования (шаг 7)
- ✅ Plurals и сокращения определены корректно для обоих языков
- ✅ Форматирование дат использует локаль устройства
- ✅ Тексты статусов локализованы через stringResource
- ✅ `GetFormattedDaysForItemUseCase` использует `ResourceProvider`
- ✅ `NumberFormattingUtils` удалён

### Тестирование (шаг 8)
- ✅ Локализация протестирована на английском языке (en-US)
- ✅ Локализация протестирована на русском языке (ru-RU)
- ✅ Все экраны протестированы на обоих языках
- ✅ Все строки отображаются корректно
- ✅ Строки с параметрами работают правильно
- ✅ Сообщения об ошибках локализованы и отображаются корректно
- ✅ Количество дней корректно локализовано (plurals работают правильно)
- ✅ Даты корректно локализованы с учетом локали устройства
- ✅ Тексты статусов корректно локализованы
- ✅ Форматирование составных периодов корректно на обоих языках
- ✅ Сокращения (дн./d, мес./mo, г./y) корректны на обоих языках
- ✅ Переключение языка работает корректно без рестарта приложения
- ✅ Нет хардкод-строк в UI (включая Compose компоненты)
- ✅ Unit-тесты для локализованного кода успешно проходят

---

## Маппинг ключей iOS → Android

| iOS ключ | Android ключ | Описание |
|----------|--------------|----------|
| `.events` | `events` | Название вкладки "События" |
| `.more` | `more` | Название вкладки "Ещё" |
| `.addItem` | `add_item` | Кнопка "Добавить запись" |
| `.save` | `save` | Кнопка "Сохранить" |
| `.cancel` | `cancel` | Кнопка "Отмена" |
| `.close` | `close` | Кнопка "Закрыть" |
| `.edit` | `edit` | Кнопка "Изменить" |
| `.delete` | `delete` | Кнопка "Удалить" |
| `.title` | `title` | Заголовок "Название" |
| `.details` | `details` | Заголовок "Детали" |
| `.date` | `date` | Заголовок "Дата" |
| `.colorTag` | `color_tag` | Заголовок "Цветовой тег" |
| `.displayFormat` | `display_format` | Заголовок "Формат отображения" |
| `.today` | `today` | Сегодня" |
| `.daysOnly` | `days_only` | Только дни" |
| `.monthsAndDays` | `months_and_days` | Месяцы и дни" |
| `.yearsMonthsAndDays` | `years_months_and_days` | Годы, месяцы и дни" |
| `.whatShouldWeRemember` | `what_should_we_remember` | Что нужно запомнить?" |
| `.createYourFirstItem` | `create_your_first_item` | Сделайте вашу первую запись" |
| `.oldFirst` | `old_first` | Сначала старые" |
| `.newFirst` | `new_first` | Сначала новые" |
| `.sort` | `sort` | Сортировка" |
| `.sortOrder` | `sort_order` | Порядок сортировки" |
| `.appVersion` | `app_version` | Версия приложения: %@" |
| `.sendFeedback` | `send_feedback` | Отправить обратную связь" |
| `.rateTheApp` | `rate_the_app` | Оценить приложение" |
| `.shareTheApp` | `share_the_app` | Поделиться приложением" |
| `.gitHubPage` | `github_page` | Ссылка на GitHub" |
| `.appTheme` | `app_theme` | Тема приложения" |
| `.appIcon` | `app_icon` | Иконка приложения" |
| `.light` | `light` | Светлая" |
| `.dark` | `dark` | Тёмная" |
| `.system` | `system` | Системная" |
| `.createABackup` | `create_a_backup` | Создать резервную копию" |
| `.restoreFromBackup` | `restore_from_backup` | Восстановить из резервной копии" |
| `.deleteAllData` | `delete_all_data` | Удалить все данные" |
| `.backupDataSaved` | `backup_data_saved` | Резервная копия данных сохранена" |
| `.dataRestoredFromBackup` | `data_restored_from_backup` | Данные восстановлены из резервной копии" |
| `.allDataDeleted` | `all_data_deleted` | Все данные удалены" |
| `.unableToRecoverDataFromTheSelectedFile` | `unable_to_recover_data_from_the_selected_file` | Не удалось восстановить данные из выбранного файла" |
| `.error` | `error` | Ошибка" |
| `.done` | `done` | Готово" |
| `.ok` | `ok` | Ок" |
| `.loading` | `loading` | Загрузка..." |
| `.back` | `back` | Назад" |
| `.event_deleted` | `event_deleted` | Событие удалено" |
| `.select_date` | `select_date` | Выбрать дату" |
| `.preview` | `preview` | Предпросмотр" |
| — | `elapsed` | прошло" / "elapsed" |
| — | `remaining` | осталось" / "remaining" |
| — | `event_not_found` | Событие не найдено" |
| — | `error_loading_event` | Ошибка загрузки события: %1$s" |
| — | `error_creating_event` | Ошибка создания события: %1$s" |
| — | `error_updating_event` | Ошибка обновления события: %1$s" |
| — | `error_formatting` | Ошибка форматирования" |
| — | `error_calculating` | Ошибка при вычислении: %1$s" |
| — | `error_formatting_details` | Ошибка форматирования: %1$s" |
| — | `days_abbreviated` | дн." / "d" |
| — | `months_abbreviated` | мес." / "mo" |
| — | `years_abbreviated` | г." / "y" |
| — | `days_count` (plurals) | Множественное число дней: "день"/"дня"/"дней" (en: "day"/"days") |
| — | `months_count` (plurals) | Множественное число месяцев: "месяц"/"месяца"/"месяцев" (en: "month"/"months") |
| — | `years_count` (plurals) | Множественное число лет: "год"/"года"/"лет" (en: "year"/"years") |
