# План локализации для Android

## Обзор

Данный документ описывает реализацию локализации для Android-приложения на основе строк из iOS-приложения. Все строки определены в iOS-проекте (`Localizable.xcstrings`) и перенесены в Android-проект с сохранением идентичности переводов.

**Статус:** ✅ Локализация реализована и интегрирована в код

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
- `rate_the_app` — "Rate the app" / "Оценить приложение"
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

---

## План реализации

### Шаг 1: Создание файлов ресурсов

#### Задачи:
1. Создать `app/src/main/res/values/strings.xml` (английские строки по умолчанию)
2. Создать `app/src/main/res/values-ru/strings.xml` (русские переводы)
3. Добавить все строки из списка выше в соответствующие файлы

#### Структура файла:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Общие строки -->
    <string name="events">Events</string>
    <string name="more">More</string>
    
    <!-- Кнопки действий -->
    <string name="add_item">Add Item</string>
    <string name="save">Save</string>
    <!-- ... остальные строки ... -->
</resources>
```

#### Критерии готовности:
- ✅ Все файлы созданы
- ✅ Все строки добавлены
- ✅ Переводы идентичны iOS-версии
- ✅ Форматирование строк корректно (параметры %1$s, %1$d)

---

### Шаг 2: Строки с параметрами

#### Задачи:
1. Реализовать строки с параметрами:
   - `app_version` — "App version: %1$s" / "Версия приложения: %1$s"
   - `variant` — "Variant %1$d" / "Вариант %1$d"
2. Использовать правильный формат Android для параметров (`%1$s`, `%1$d`)

#### Пример использования:
```kotlin
// В коде
val versionText = getString(R.string.app_version, BuildConfig.VERSION_NAME)
// Результат: "App version: 1.0.0" / "Версия приложения: 1.0.0"
```

#### Критерии готовности:
- ✅ Строки с параметрами определены корректно
- ✅ Использование в коде работает правильно

---

### Шаг 3: Проверка соответствия iOS

#### Задачи:
1. Сравнить все строки с iOS-версией (`Localizable.xcstrings`)
2. Убедиться, что переводы идентичны
3. Проверить, что все строки присутствуют в Android-версии

#### Критерии готовности:
- ✅ Все строки из iOS присутствуют в Android
- ✅ Переводы идентичны iOS-версии
- ✅ Нет пропущенных строк

---

### Шаг 4: Интеграция в код

#### Задачи:
1. Заменить все хардкод-строки на ресурсы из `strings.xml`
2. Использовать `getString()` или `stringResource()` в Compose
3. Убедиться, что локализация применяется корректно

#### Пример использования в Compose:
```kotlin
// Вместо хардкода
Text("Events")

// Использовать ресурс
Text(stringResource(R.string.events))
```

#### Пример использования в Screen sealed class:
```kotlin
sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val titleResId: Int, // ID строкового ресурса для локализации
) {
    object Events : Screen(
        route = "events",
        icon = Icons.Filled.List,
        titleResId = R.string.events,
    )
    
    object More : Screen(
        route = "more",
        icon = Icons.Filled.MoreVert,
        titleResId = R.string.more,
    )
}

// Использование в UI
Text(text = stringResource(id = screen.titleResId))
```

#### Критерии готовности:
- ✅ Все хардкод-строки заменены на ресурсы
- ✅ Screen использует titleResId вместо хардкод-строк
- ✅ Локализация работает корректно
- ✅ Переключение языка работает

---

### Шаг 5: Тестирование локализации

#### Задачи:
1. Протестировать приложение на русском языке
2. Протестировать приложение на английском языке
3. Проверить, что все строки отображаются корректно
4. Проверить строки с параметрами

#### Критерии готовности:
- ✅ Все экраны протестированы на обоих языках
- ✅ Все строки отображаются корректно
- ✅ Строки с параметрами работают правильно

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
| `.today` | `today` | "Сегодня" |
| `.daysOnly` | `days_only` | "Только дни" |
| `.monthsAndDays` | `months_and_days` | "Месяцы и дни" |
| `.yearsMonthsAndDays` | `years_months_and_days` | "Годы, месяцы и дни" |
| `.whatShouldWeRemember` | `what_should_we_remember` | "Что нужно запомнить?" |
| `.createYourFirstItem` | `create_your_first_item` | "Сделайте вашу первую запись" |
| `.oldFirst` | `old_first` | "Сначала старые" |
| `.newFirst` | `new_first` | "Сначала новые" |
| `.sort` | `sort` | "Сортировка" |
| `.sortOrder` | `sort_order` | "Порядок сортировки" |
| `.appVersion` | `app_version` | "Версия приложения: %@" |
| `.sendFeedback` | `send_feedback` | "Отправить обратную связь" |
| `.rateTheApp` | `rate_the_app` | "Оценить приложение" |
| `.shareTheApp` | `share_the_app` | "Поделиться приложением" |
| `.gitHubPage` | `github_page` | "Ссылка на GitHub" |
| `.appTheme` | `app_theme` | "Тема приложения" |
| `.appIcon` | `app_icon` | "Иконка приложения" |
| `.light` | `light` | "Светлая" |
| `.dark` | `dark` | "Тёмная" |
| `.system` | `system` | "Системная" |
| `.createABackup` | `create_a_backup` | "Создать резервную копию" |
| `.restoreFromBackup` | `restore_from_backup` | "Восстановить из резервной копии" |
| `.deleteAllData` | `delete_all_data` | "Удалить все данные" |
| `.backupDataSaved` | `backup_data_saved` | "Резервная копия данных сохранена" |
| `.dataRestoredFromBackup` | `data_restored_from_backup` | "Данные восстановлены из резервной копии" |
| `.allDataDeleted` | `all_data_deleted` | "Все данные удалены" |
| `.unableToRecoverDataFromTheSelectedFile` | `unable_to_recover_data_from_the_selected_file` | "Не удалось восстановить данные из выбранного файла" |
| `.error` | `error` | "Ошибка" |
| `.done` | `done` | "Готово" |
| `.ok` | `ok` | "Ок" |

---

## Критерии завершения этапа

Этап локализации считается завершенным, когда:

- ✅ Все файлы ресурсов созданы (`values/strings.xml`, `values-ru/strings.xml`)
- ✅ Все строки из iOS-версии перенесены в Android
- ✅ Переводы идентичны iOS-версии
- ✅ Строки с параметрами реализованы корректно
- ✅ Все хардкод-строки заменены на ресурсы
- ✅ Screen использует titleResId для локализации вместо хардкод-строк
- ✅ Локализация протестирована на обоих языках
- ✅ Переключение языка работает корректно

---

## Статус реализации

**✅ Локализация реализована и интегрирована в код**

Все этапы плана локализации выполнены:
- Файлы ресурсов созданы и содержат все необходимые строки
- Локализация интегрирована в код (RootScreen, Screen)
- Хардкод-строки заменены на строковые ресурсы
- Screen использует `titleResId` для локализации заголовков экранов

---

## Примечания

1. **Источник истины**: iOS-приложение (`Localizable.xcstrings`) является источником истины для всех локализованных строк.

2. **Идентичность переводов**: Все переводы должны быть идентичны iOS-версии для обеспечения единообразия пользовательского опыта.

3. **Форматирование параметров**: Android использует формат `%1$s`, `%1$d` для параметров, в то время как iOS использует `%@`, `%lld`. При переносе необходимо адаптировать формат.

4. **Именование ключей**: Android использует snake_case для ключей строк, в то время как iOS использует camelCase. Ключи должны быть адаптированы, но значения должны оставаться идентичными.

5. **Тестирование**: Необходимо протестировать локализацию на обоих языках на всех экранах приложения.

