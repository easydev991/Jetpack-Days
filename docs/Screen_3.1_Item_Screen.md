# Экран 3.1: Item Screen (Просмотр записи)

## Обзор

Item Screen предназначен для просмотра полной информации о событии и количестве прошедших дней. Экран отображает все детали записи в структурированном виде.

## Назначение

Просмотр полной информации о событии и количества прошедших дней с возможностью перехода к редактированию записи.

## Компоненты

- DetailScreen.kt — главный экран детализации (`ui/screens/detail/DetailScreen.kt`)
- DetailScreenParams.kt — параметры экрана для передачи зависимостей
- DetailAppBar.kt — TopAppBar с кнопками "Назад", "Редактировать", "Удалить"
- DetailContent.kt — секции экрана: цветовая метка, название, дата, количество дней, описание, опция отображения, запланированное напоминание (UpcomingReminderSection)
- DetailContentPreviews.kt — preview компонентов секций
- DetailStates.kt — состояния экрана: загрузка, ошибка
- DetailScreenViewModel.kt — ViewModel с factory методом для DI (`ui/viewmodel/DetailScreenViewModel.kt`)

## Статус выполнения

✅ **ВЕСЬ ФУНКЦИОНАЛ РЕАЛИЗОВАН** (100%)

### Выполнено

- ✅ Навигация, ViewModel, UI State, UI компоненты (DetailScreen, DetailAppBar, DetailContent, DetailStates), удаление с диалогом, локализация, Preview, unit-тесты, исправлено отображение отрицательного количества дней (showMinus)
- ✅ Отображение запланированного напоминания (UpcomingReminderSection): если для записи есть активное будущее уведомление, на экране последней секцией отображается локализованный заголовок «Напоминание» и плановая дата в читаемом формате

### Осталось (опционально, не требуется для завершения)

- ⚠️ UI-тесты для DetailScreen (не начаты)
- ⚠️ Проверка линтеров (ktlint, detekt) — опционально

---

## Зависимости от других этапов

- ✅ **Этап 7**: Модель данных (Entity, Domain model, Room Database, DAO) — **ЗАВЕРШЕН**, готов к использованию
- ✅ **Этап 6**: Форматирование количества дней — **РЕАЛИЗОВАНО**, используется для отображения количества дней
- ✅ **Экран 2.1**: Main Screen — **ВЫПОЛНЕН**, навигация к Item Screen настроена
- ✅ **Экран 4.1**: Create/Edit Item Screen — **ВЫПОЛНЕН**, напоминания создаются при редактировании; DetailScreen получает активное напоминание через `ReminderManager.getActiveReminder(itemId)`

---

## Подробный план реализации по TDD

### Предварительные требования

Перед началом реализации этого экрана должны быть выполнены:

1. ✅ **Этап 7**: Модель данных (Entity, Domain model, Room Database, DAO) — **ЗАВЕРШЕН**
2. ✅ **Этап 6**: Форматирование количества дней (GetFormattedDaysForItemUseCase) — **РЕАЛИЗОВАНО**
3. ✅ **Экран 2.1**: Main Screen — **ВЫПОЛНЕН**, навигация настроена

---

### Шаг 1-6: Подготовка и реализация ✅

**Выполнено:** Навигация, Domain (ItemRepository.getItemFlow), UI State, UI компоненты (DetailContent, DetailStates, DetailContentPreviews), DetailAppBar, DetailScreen, обработка ошибок, локализация

---

### Шаг 7: Финальное тестирование ✅

**Задачи:**

1. ✅ Проверить правильное отображение всех полей записи (unit тесты реализованы)
2. ✅ Проверить корректное вычисление и отображение количества дней (unit тесты реализованы)
3. ⚠️ Проверить отображение цветовой метки (UI тесты не начаты)
4. ⚠️ Проверить функциональность кнопки редактирования и удаления (UI тесты не начаты)
5. ⚠️ Проверить корректную навигацию назад (UI тесты не начаты)
6. ✅ Проверить обработку ошибок (запись не найдена) (unit тесты реализованы)
7. ⚠️ Проверить условное отображение секций (если поля пустые) (UI тесты не начаты)

**Критерии готовности:**

- ✅ Unit-тесты реализованы (DetailScreenViewModelTest, DetailScreenStateTest)
- ✅ Все функции работают корректно

---

## Реализация ✅

DetailScreen.kt с Scaffold и DetailScreenParams, DetailContent (все секции), DetailStates, DetailAppBar с кнопками, Material Design 3.

**Архитектура:** Clean Architecture, ItemRepository.getItemFlow, Flow для реактивности, SavedStateHandle, Factory метод для DI

### Отображение запланированного напоминания ✅

Если для записи существует активное напоминание с датой в будущем, на экране детализации последним элементом отображается секция с информацией о нём:

- **Заголовок:** локализованная строка `R.string.reminder_settings` («Напоминание» / «Reminder»)
- **Содержимое:** плановая дата и время в формате `FormatStyle.MEDIUM` / `FormatStyle.SHORT` с локалью пользователя
- **Условие отображения:** только если `reminder != null` и `targetEpochMillis > currentTimeMillis`
- **Компонент:** приватная `UpcomingReminderSection` в `DetailContent.kt`, использующая `ReadSectionView` в едином стиле с остальными секциями

**Поток данных:**

1. `DetailScreenViewModel.observeItem()` при каждом изменении записи вызывает `reminderManager.getActiveReminder(itemId)`
2. Активный reminder фильтруется: `takeIf { it.targetEpochMillis > currentTimeMillisProvider() }` — просроченные не показываются
3. Результат передаётся в `DetailScreenState.Success(item = item, reminder = upcomingReminder)`
4. `DetailContentInner` рендерит `UpcomingReminderSection` последней секцией перед `Spacer`

**Порядок секций в DetailContentInner:**
1. Название (ReadSectionView)
2. Описание (ReadSectionView, если не пустое)
3. Цветовая метка (Row, если задана)
4. Дата + анализ дней (DetailDatePicker)
5. Формат отображения (DetailDisplayOptionPicker)
6. **Напоминание (UpcomingReminderSection)** ← последний контент-элемент
7. Spacer

---

## Тестирование ✅

### Критически важное предупреждение

**Запрещено:** Писать интеграционные тесты с ViewModels

**Причина:**

- Конфликт между `runBlocking` и `viewModelScope.launch`
- Flow репозитория не активируется корректно в тестах
- Тесты зависают бесконечно или падают
- Это фундаментальная проблема архитектуры, которая требует глубокого анализа

**Рабочий подход:**

- ✅ Писать unit-тесты для ViewModels с MockK
- ✅ Писать интеграционные тесты только для DAO и Repository (без ViewModels)
- ✅ Писать UI-тесты для Compose компонентов (без ViewModels)

Подробнее см. в документе `Testing_Status_Plan.md`

### Unit-тесты

**Unit-тесты:** DetailScreenViewModel, DetailScreenState — **РЕАЛИЗОВАНО**

- `DetailScreenViewModelTest.kt` (8 тестов, включая сценарии с напоминаниями: будущее — показывается, прошедшее — скрывается) в `test/java/com/dayscounter/ui/viewmodel/`
- `DetailScreenStateTest.kt` (10 тестов) в `test/java/com/dayscounter/ui/state/`

### Интеграционные тесты

**Важно:** Только для DAO и Repository, БЕЗ ViewModels

**Интеграционные тесты:** ViewModel с Repository — **ПОКРЫТО В UNIT-ТЕСТАХ** (FakeRepository в DetailScreenViewModelTest)

**Примечание:** Интеграционные тесты ViewModels (DetailScreenViewModelIntegrationTest) НЕ создаются из-за фундаментальной архитектурной проблемы (см. выше).

### UI-тесты

**UI-тесты:** отображение полей, дней, цветовой метки, кнопок редактирования/удаления, навигации, ошибок, условное отображение секций — **НЕ НАЧАТЫ**

---

## Критерии завершения

✅ Компоненты созданы и работают
✅ Код соответствует правилам проекта
✅ Навигация работает корректно
✅ Обработка ошибок реализована
✅ Unit-тесты реализованы (с MockK)

**Примечание:** При написании новых тестов следуйте правилам из `.cursor/rules/tdd.mdc`

**Статус:** ✅ ВЫПОЛНЕНО (100%)

---

## Блокируемые этапы

✅ Экран 4.1: Create/Edit Item Screen уже реализован

---

## Примечания и файлы

1. **Динамическое обновление:** Количество дней вычисляется при отображении с помощью GetFormattedDaysForItemUseCase, использующего Flow для реактивности
2. **Условное отображение:** ✅ Секции с пустым содержимым (colorTag, details) не отображаются; секция напоминания — только при наличии активного будущего reminder
3. **Обработка ошибок:** ✅ Можно вернуться к списку через кнопку "Назад"
4. **Preview:** ✅ Preview реализованы для всех секций в DetailContentPreviews.kt и для TopAppBar в DetailAppBar.kt, ⚠️ но отсутствует preview для полного DetailScreen
5. **Тестирование:** ✅ Unit-тесты реализованы для ViewModel и UI State, включая сценарии с напоминаниями; ⚠️ UI-тесты не начаты
6. **Зависимости:** ✅ Все этапы завершены (Этап 7, Этап 6, Экран 2.1); напоминания — зависимость от Экран 4.1 и ReminderManager
7. **Напоминание:** ✅ Отображается последней секцией; формат даты — локализованный `FormatStyle.MEDIUM/SHORT`; прошедшие напоминания не показываются

**Текущие файлы:**

- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreenParams.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailAppBar.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContent.kt` (содержит `UpcomingReminderSection`)
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContentPreviews.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailStates.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/DetailScreenViewModel.kt`
- `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt` (8 тестов, включая reminder-сценарии)
- `app/src/test/java/com/dayscounter/ui/state/DetailScreenStateTest.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
- `app/src/main/java/com/dayscounter/domain/model/Reminder.kt` (доменная модель напоминания)
- `app/src/main/java/com/dayscounter/reminder/ReminderManager.kt` (интерфейс, метод `getActiveReminder`)
- `app/src/main/java/com/dayscounter/reminder/DefaultReminderManager.kt` (реализация)

---

## История изменений

- 2025-01-01: Первоначальный план создания Item Screen
- 2026-01-01: Полная реализация функционала
- 2026-01-11: Актуализация плана (100% готовности)
- 2026-01-15: Добавление краткого анализа дней под датой
- 2026-01-15: Исправлен баг с отображением отрицательного количества дней (параметр `showMinus`)
- 2026-01-16: Обнаружен баг: минус отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем
- 2026-01-16: Исправлен баг: минус не отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем
- 2026-04-29: Добавлено отображение запланированного напоминания (UpcomingReminderSection); секция напоминания перемещена в конец списка (после формата отображения); документация актуализирована

---

## Баг: Минус отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем ✅ ИСПРАВЛЕН

**Симптомы:** Минус отображался перед количеством дней на DetailScreen для `displayOption = MONTH_DAY` или `YEAR_MONTH_DAY` при дате в будущем, но не для `displayOption = DAY`.

**Корневая причина:** 1) Параметр `showMinus` не использовался в методах `formatMonthDay()` и `formatYearMonthDay()`. 2) В `FormatDaysTextUseCase` происходило преобразование `totalDays` в абсолютное значение.

**Исправление:** Обновлен `DaysFormatterImpl.formatComposite()` для передачи `showMinus` и `totalDays` в методы форматирования. Обновлен `FormatDaysTextUseCase` для передачи оригинального `totalDays`.

**Измененные файлы:** `DaysFormatterImpl.kt`, `FormatDaysTextUseCase.kt`, `FormatDaysTextUseCaseTest.kt`.

**Результат:** Все тесты проходят, ручное тестирование подтверждает исправление.

**Уроки:** Использовать оригинальное `totalDays` для определения будущего, все методы форматирования должны учитывать `showMinus`, тесты проверять комбинации параметров.

---
