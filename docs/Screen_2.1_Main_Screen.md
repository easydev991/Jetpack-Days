# Экран 2.1: Main Screen (Главный экран)

## Обзор

Main Screen является основным экраном приложения для отображения списка всех записей о событиях. Экран предоставляет функциональность поиска, сортировки и управления записями.

## Назначение

Отображение списка всех записей о событиях с возможностью поиска, сортировки и навигации к деталям записи или созданию новой записи.

**Сохранение порядка сортировки:** Выбранный порядок сортировки (ASCENDING/DESCENDING) сохраняется в DataStore для сохранения между запусками приложения. При первом запуске используется значение по умолчанию DESCENDING.

## Текущее состояние

✅ **Весь функционал реализован (100% завершено)**:

- **Data Layer**: Entity, DAO, Database, Mapper, Repository — реализованы и протестированы
- **Domain Layer**: Repository interface, Domain model — реализованы и протестированы
- **Presentation Layer**: ViewModel, UI State, UI Components (ListItemView, EmptyState, TopAppBar, SearchField, Context Menu, Delete Dialog, LazyColumn), DataStore Integration для сортировки, навигация, factory methods

✅ **Все функции работают корректно**:

- Отображение, поиск, сортировка (сохраняется в DataStore), удаление, редактирование, просмотр записей, пустые состояния, выделение элементов

✅ **Все тесты проходят успешно**:

- Unit-тесты для ViewModel: 10 тестов
- Интеграционные тесты: DAO, Database, Repository
- Компонентные тесты UI: DaysCountText — 7 тестов
- ✅ ktlintCheck пройден успешно, сборка успешна

⚠️ **Осталось (опционально)**:

- Дополнительные UI-тесты для MainScreen
- Проверка detekt (статический анализ)

---

## Требования

### Функциональные требования

- [x] Отображение списка всех записей о событиях
- [x] Форматирование количества дней для каждой записи
- [x] Цветовая метка (индикатор цвета) для каждой записи
- [x] Фильтрация записей по поисковому запросу
- [x] Сортировка записей по дате (возрастание/убывание)
- [x] Сохранение порядка сортировки — выбор пользователя сохраняется в DataStore между запусками приложения
- [x] Удаление записей (с диалогом подтверждения)
- [x] Редактирование записей (через контекстное меню)
- [x] Переход к деталям записи (через клик или контекстное меню)
- [x] Переход к созданию новой записи
- [x] Отображение пустого состояния при отсутствии записей
- [x] Контекстное меню при длинном нажатии (Просмотр/Редактирование/Удаление)
- [x] Диалог подтверждения удаления записи
- [x] Выделение выбранного элемента списка
- [x] Проверка линтеров (ktlint) — ✅ пройдено
- [ ] UI-тесты для MainScreen (опционально)
- [ ] Проверка detekt (статический анализ) — опционально

### UI требования

- [x] Список записей в LazyColumn
- [x] Карточка записи с количеством дней и цветовой меткой
- [x] SearchField (OutlinedTextField) для поиска записей — отображается при наличии 5+ записей или при активном поиске
- [x] Кнопка сортировки (возрастание/убывание) — отображается при наличии 2+ записей
- [x] Кнопка добавления новой записи (+)
- [x] Контекстное меню при длинном нажатии (Просмотр/Редактирование/Удаление)
- [x] Диалог подтверждения удаления записи
- [x] Пустое состояние с иконкой, заголовком и описанием
- [x] Выделение выбранного элемента списка
- [x] Соответствие iOS-дизайну (аналогично iOS-версии)

### Архитектурные требования

- [x] MVVM архитектура: ViewModel, State, UI
- [x] Repository pattern для работы с данными
- [x] Flow для реактивного обновления UI
- [x] Dependency Injection через factory методы
- [x] Compose для UI

---

## Зависимости от других этапов

- ✅ **Этап 7**: Модель данных (Entity, Domain model, Room Database, DAO, Repository) — **ЗАВЕРШЕН**, готов к использованию
- ✅ **Этап 6**: Форматирование количества дней — **РЕАЛИЗОВАНО**, используется для отображения количества дней в списке
- ✅ **Экран 1.1**: Root Screen — **ВЫПОЛНЕН**, навигация настроена через RootScreen

---

## План реализации

### Шаг 1: Подготовка слоя данных (Data Layer)

✅ **ВЫПОЛНЕНО**

- **1.1** Entity — `app/src/main/java/com/dayscounter/data/database/entity/ItemEntity.kt`
- **1.2** DAO — `app/src/main/java/com/dayscounter/data/database/dao/ItemDao.kt`
- **1.3** Database — `app/src/main/java/com/dayscounter/data/database/DaysDatabase.kt`
- **1.4** Mapper — `app/src/main/java/com/dayscounter/data/database/mapper/ItemMapper.kt`
- **1.5** Repository — `app/src/main/java/com/dayscounter/data/repository/ItemRepositoryImpl.kt`

### Шаг 2: Подготовка слоя домена (Domain Layer)

✅ **ВЫПОЛНЕНО**

- **2.1** Domain Entity — `Item.kt`, `DisplayOption.kt`, `SortOrder.kt`, `ItemExtensions.kt`
- **2.2** Repository Interface — `ItemRepository.kt`
- **2.3** Repository Implementation — `ItemRepositoryImpl.kt`
- **2.4** SortOrder Enum — `SortOrder.kt`

### Шаг 3: Подготовка слоя представления (Presentation Layer)

✅ **ВЫПОЛНЕНО**

- **3.1** UI State — `MainScreenState` (Loading, Success, Error) в `MainScreenViewModel.kt`
- **3.2** ViewModel — `MainScreenViewModel.kt` с factory методом, DataStore для сортировки, поиск, фильтрация, удаление

### Шаг 4: Реализация UI компонентов

✅ **ВЫПОЛНЕНО**

- **4.1** ListItemView — карточка записи, 7 preview-вариантов
- **4.2** EmptyState — 4 варианта (empty, search, loading, error)
- **4.3** Toolbar — сортировка (возрастание/убывание), заголовок "События"
- **4.4** MainScreen — LazyColumn, SearchField (OutlinedTextField), SwipeToDismissBox, состояния
- **4.5** Context Menu — Просмотр/Редактирование/Удаление, длинное нажатие, выделение
- **4.6** Delete Dialog — AlertDialog с подтверждением
- **4.7** RootScreen Integration — `eventsScreenContent()`, NavHost

### Шаг 5: Интеграция с другими экранами

✅ **ВЫПОЛНЕНО**

- **5.1** DetailScreen — маршрут `Screen.ItemDetail`, навигация при клике, factory метод
- **5.2** CreateEditScreen (создание) — маршрут `Screen.CreateItem`, кнопка "+", factory метод
- **5.3** CreateEditScreen (редактирование) — маршрут `Screen.EditItem`, свайп влево, factory метод
- **5.4** Factory Methods — companion object в CreateEditScreenViewModel и DetailScreenViewModel
- **5.5** Локализация — план в `Localization_Plan.md`

---

## Тестирование

**ВАЖНО:** Подробные правила тестирования см. в `.cursor/rules/testing.mdc`

### Unit-тесты

✅ **ВЫПОЛНЕНО**

- Entity, Domain model, DAO (in-memory), Repository — протестированы
- ViewModel — `MainScreenViewModelTest.kt` (10 тестов с MockK): загрузка, поиск (название/детали, регистронезависимый), сортировка, удаление, пустое состояние

### Компонентные тесты (Compose Testing)

✅ **ВЫПОЛНЕНО**

- `daysCountText` — `DaysCountTextTest.kt` (7 тестов): отображение текста, "Сегодня", форматы, английский язык

### Интеграционные тесты

✅ **ВЫПОЛНЕНО** (только DAO и Repository, БЕЗ ViewModels)

- Repository с Room Database

### UI-тесты (Espresso)

⚠️ **Опционально можно добавить:**

- Тесты для ListItemView, контекстного меню, диалога удаления, SearchField, интеграционные UI-тесты MainScreen

---

## Критерии завершения этапа

✅ **ВЫПОЛНЕНО (100%)**

- Все компоненты, unit-тесты (10), компонентные тесты (7), интеграционные тесты (DAO, Repository)
- Код соответствует правилам, ktlint пройден
- Поиск, сортировка (сохраняется в DataStore), навигация, контекстное меню, диалог удаления работают корректно

**Примечание:** Интеграционные тесты ViewModels не создаются (см. `.cursor/rules/testing.mdc`). ViewModel тестируется через unit-тесты с MockK.

---

## Примечания

- Совместимость с iOS, производительность (LazyColumn), реактивность (Flow), тестирование компонентов
- ✅ Зависимости: Этап 7 (Модель данных), Этап 6 (Форматирование)
- ✅ Поиск: SearchField (OutlinedTextField), отображается при 5+ записях или активном поиске
- ✅ Сортировка: SortOrder enum, сохраняется в DataStore
- ✅ Свайп-действия: SwipeToDismissBox
- ✅ Factory методы: companion object в CreateEditScreenViewModel и DetailScreenViewModel

---

## Блокируемые этапы

✅ Экраны 3.1 (Item Screen) и 4.1 (Create/Edit Item Screen) реализованы и интегрированы с Main Screen

---

## Текущие проблемы

✅ **Исправлено:**

- Краш при открытии экранов CreateEdit и Detail — добавлены factory методы
- Переделка компонента поиска — SearchField (OutlinedTextField), устранен deprecated API
- Контекстное меню для записей — Просмотр/Редактирование/Удаление, диалог подтверждения, выделение элемента
- Качество кода — ktlintCheck пройден, все тесты и сборка успешны

⚠️ **Опционально:**

- Дополнительные UI-тесты для MainScreen (ListItemView, контекстное меню, диалог, SearchField)
- Проверка detekt (статический анализ)

---

## Обновленный план завершения Main Screen

✅ **Приоритет 0: Исправление краша при открытии экранов CreateEdit и Detail**

- Добавлены companion object с factory методами в ViewModels
- Обновлена навигация для использования фабрик
- Приложение не крашится при нажатии на "+" или открытии деталей

✅ **Приоритет 1: Исправление критических ошибок сборки**

- Исправлена версия Gradle (AGP 8.13.2 → Gradle 9.2.1)
- Добавлена зависимость `material-icons-extended`
- Реализована функция `itemsListContent()`

✅ **Приоритет 2: Реализация функциональности поиска**

- Реализован `SearchBar` → позже заменен на `SearchField` (OutlinedTextField)
- Фильтрация по названию и деталям (нечувствительно к регистру) через Combine Flow

✅ **Приоритет 3: Полная интеграция навигации**

- Интеграция с DetailScreen, CreateEditScreen (создание), CreateEditScreen (редактирование)
- Маршруты в Screen.kt, навигация в RootScreenComponents.kt, factory методы

✅ **Приоритет 4: Unit-тесты для ViewModel**

- `MainScreenViewModelTest.kt` с 10 тестами, FakeItemRepository

✅ **Приоритет 4.5: Реализация контекстного меню**

- Компоненты `contextMenu()`, `listItemWrapper()`, `deleteDialog()`
- Три опции меню, визуальное выделение, ViewModel для диалога удаления

✅ **Приоритет 5: Компонентные тесты для UI**

- `DaysCountTextTest.kt` с 7 тестами, Compose Testing API

✅ **Приоритет 6: Проверка качества кода**

- ✅ ktlintCheck пройден, все тесты и сборка успешны

✅ **Приоритет 7: Запуск линтеров**

- ✅ ktlintCheck пройден без ошибок
- ⚠️ detekt — опционально

---

## Обновленная документация

В план были внесены изменения: добавлены разделы "Текущие проблемы" и "Приоритет 0", обновлены разделы "Текущее состояние", "Шаг 1-5", критерии завершения и требования к UI, добавлена документация о переделке поиска и контекстном меню.

---

## Полезные ссылки

- [Этап 7: Модель данных](./Stage_7_Data_Model_Implementation_Plan.md)
- [Этап 6: Форматирование дней](./Stage_6_Days_Formatting_Implementation_Plan.md)
- [План локализации](./Localization_Plan.md)
- [Экран 1.1: Root Screen](./Screen_1.1_Root_Screen.md)
- [Экран 3.1: Item Screen](./Screen_3.1_Item_Screen.md)
- [Экран 4.1: Create/Edit Item Screen](./Screen_4.1_Create_Edit_Item_Screen.md)

---

## История изменений

- 2025-01-01: Первоначальный план создания Main Screen
- 2025-01-02: Исправление краша (factory методы), обновление навигации, актуализация документации (97%)
- 2026-01-01: Обновление документации — экран полностью реализован
- 2026-01-02: Переделка поиска (SearchField на OutlinedTextField, условное отображение), контекстное меню, диалог удаления, компонентные тесты (7), проверка качества (ktlint), завершение (100%)
- 2026-01-11: Актуализация — весь функционал реализован (100%)
- 2026-01-15: Реализовано сохранение порядка сортировки в DataStore (ASCENDING/DESCENDING)
- 2026-01-15: Сжатие описания выполненных пунктов (удаление дублирования)
