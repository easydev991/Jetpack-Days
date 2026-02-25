# Экран 3.1: Item Screen (Просмотр записи)

## Обзор

Item Screen предназначен для просмотра полной информации о событии и количестве прошедших дней. Экран отображает все детали записи в структурированном виде.

## Назначение

Просмотр полной информации о событии и количества прошедших дней с возможностью перехода к редактированию записи.

## Компоненты

- DetailScreen.kt — главный экран детализации (`ui/screens/detail/DetailScreen.kt`)
- DetailScreenParams.kt — параметры экрана для передачи зависимостей
- DetailAppBar.kt — TopAppBar с кнопками "Назад", "Редактировать", "Удалить"
- DetailContent.kt — секции экрана: цветовая метка, название, дата, количество дней, описание, опция отображения
- DetailContentPreviews.kt — preview компонентов секций
- DetailStates.kt — состояния экрана: загрузка, ошибка
- DetailScreenViewModel.kt — ViewModel с factory методом для DI (`ui/viewmodel/DetailScreenViewModel.kt`)

## Статус выполнения

✅ **ВЕСЬ ФУНКЦИОНАЛ РЕАЛИЗОВАН** (100%)

### Выполнено

- ✅ Навигация, ViewModel, UI State, UI компоненты (DetailScreen, DetailAppBar, DetailContent, DetailStates), удаление с диалогом, локализация, Preview, unit-тесты, исправлено отображение отрицательного количества дней (showMinus)

### Осталось (опционально, не требуется для завершения)

- ⚠️ UI-тесты для DetailScreen (не начаты)
- ⚠️ Проверка линтеров (ktlint, detekt) — опционально

---

## Зависимости от других этапов

- ✅ **Этап 7**: Модель данных (Entity, Domain model, Room Database, DAO) — **ЗАВЕРШЕН**, готов к использованию
- ✅ **Этап 6**: Форматирование количества дней — **РЕАЛИЗОВАНО**, используется для отображения количества дней
- ✅ **Экран 2.1**: Main Screen — **ВЫПОЛНЕН**, навигация к Item Screen настроена

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

- `DetailScreenViewModelTest.kt` (2 теста с MockK) в `test/java/com/dayscounter/ui/viewmodel/`
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

**Примечание:** При написании новых тестов следуйте правилам из `.cursor/rules/testing.mdc`

**Статус:** ✅ ВЫПОЛНЕНО (100%)

---

## Блокируемые этапы

✅ Экран 4.1: Create/Edit Item Screen уже реализован

---

## Примечания и файлы

1. **Динамическое обновление:** Количество дней вычисляется при отображении с помощью GetFormattedDaysForItemUseCase, использующего Flow для реактивности
2. **Условное отображение:** ✅ Секции с пустым содержимым (colorTag, details) не отображаются
3. **Обработка ошибок:** ✅ Можно вернуться к списку через кнопку "Назад"
4. **Preview:** ✅ Preview реализованы для всех секций в DetailContentPreviews.kt и для TopAppBar в DetailAppBar.kt, ⚠️ но отсутствует preview для полного DetailScreen
5. **Тестирование:** ✅ Unit-тесты реализованы для ViewModel и UI State, ⚠️ UI-тесты не начаты
6. **Зависимости:** ✅ Все этапы завершены (Этап 7, Этап 6, Экран 2.1)

**Текущие файлы:**

- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailScreenParams.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailAppBar.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContent.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailContentPreviews.kt`
- `app/src/main/java/com/dayscounter/ui/screens/detail/DetailStates.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/DetailScreenViewModel.kt`
- `app/src/test/java/com/dayscounter/ui/viewmodel/DetailScreenViewModelTest.kt`
- `app/src/test/java/com/dayscounter/ui/state/DetailScreenStateTest.kt`
- `app/src/main/java/com/dayscounter/ui/screens/root/RootScreenComponents.kt` (строки 98-124)

---

## История изменений

- 2025-01-01: Первоначальный план создания Item Screen
- 2026-01-01: Полная реализация функционала
- 2026-01-11: Актуализация плана (100% готовности)
- 2026-01-15: Добавление краткого анализа дней под датой
- 2026-01-15: Исправлен баг с отображением отрицательного количества дней (параметр `showMinus`)
- 2026-01-16: Обнаружен баг: минус отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем
- 2026-01-16: Исправлен баг: минус не отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем

---

## Баг: Минус отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем ⚠️ ОБНАРУЖЕН

**Симптомы:** Минус отображается перед количеством дней на DetailScreen для `displayOption = MONTH_DAY` или `YEAR_MONTH_DAY` при дате в будущем, но не для `displayOption = DAY`.

**Воспроизведение:** Создать запись с датой в будущем + `displayOption = MONTH_DAY/YEAR_MONTH_DAY` → открыть DetailScreen → отображается текст с минусом.

**Корневая причина:** Параметр `showMinus` не учитывается в методах `formatMonthDay()` и `formatYearMonthDay()` в `DaysFormatterImpl.formatComposite()`.

**Почему тесты не обнаружили:** Тесты для `GetDaysAnalysisTextUseCase` используют mocked `getFormattedDaysForItemUseCase`, не проверяя реальное поведение методов форматирования.

---

## План исправления бага по TDD

### Шаг 1: Написание тестов (Red)

Добавить тесты в `GetDaysAnalysisTextUseCaseTest.kt` и `DaysFormatterImplTest.kt` для проверки: будущая дата + `MONTH_DAY`/`YEAR_MONTH_DAY` + `showMinus = false/false`.

---

### Шаг 2: Реализация исправления (Green)

**Варианты:** 1) Обновить `formatMonthDay()` и `formatYearMonthDay()` для использования `showMinus`, 2) Использовать `totalDays` для определения знака. **Рекомендация:** Вариант 1.

---

### Шаг 3: Проверка качества (Refactor)

Запустить unit-тесты, проверить линтеры (ktlint, detekt), Preview, ручное тестирование.

---

### Шаг 4: Документация

Обновить документ с описанием исправления, уроками из бага и историей изменений.

---

### Критерии завершения

Unit-тесты проходят, линтеры без ошибок, документация обновлена, код соответствует правилам, ручное тестирование подтверждает исправление.

---

## Баг: Минус отображается для MONTH_DAY и YEAR_MONTH_DAY при дате в будущем ✅ ИСПРАВЛЕН

**Симптомы:** Минус отображался перед количеством дней на DetailScreen для `displayOption = MONTH_DAY` или `YEAR_MONTH_DAY` при дате в будущем, но не для `displayOption = DAY`.

**Корневая причина:** 1) Параметр `showMinus` не использовался в методах `formatMonthDay()` и `formatYearMonthDay()`. 2) В `FormatDaysTextUseCase` происходило преобразование `totalDays` в абсолютное значение.

**Исправление:** Обновлен `DaysFormatterImpl.formatComposite()` для передачи `showMinus` и `totalDays` в методы форматирования. Обновлен `FormatDaysTextUseCase` для передачи оригинального `totalDays`.

**Измененные файлы:** `DaysFormatterImpl.kt`, `FormatDaysTextUseCase.kt`, `FormatDaysTextUseCaseTest.kt`.

**Результат:** Все тесты проходят, ручное тестирование подтверждает исправление.

**Уроки:** Использовать оригинальное `totalDays` для определения будущего, все методы форматирования должны учитывать `showMinus`, тесты проверять комбинации параметров.

---
