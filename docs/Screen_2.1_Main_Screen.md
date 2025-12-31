# Экран 2.1: Main Screen (Главный экран)

## Обзор

Main Screen является основным экраном приложения для отображения списка всех записей о событиях. Экран предоставляет функциональность поиска, сортировки и управления записями.

## Назначение

Отображение списка всех записей о событиях с возможностью поиска, сортировки и навигации к деталям записи или созданию новой записи.

## Компоненты

1. **Заголовок экрана** — "События"
2. **Кнопка сортировки** (если более 1 записи)
   - Позиция: слева на Toolbar (topBarLeading)
   - Опции: "Старые первые" / "Новые первые"
   - Сортировка по дате события (timestamp)
   - Меню с DropdownMenu для выбора опции сортировки
3. **Кнопка добавления записи**
   - Позиция: справа на Toolbar (floatingActionButton)
   - Иконка: "+"
   - Открывает экран создания новой записи
4. **Список записей** (LazyColumn)
   - Карточка каждой записи содержит:
     - Название события
     - Количество дней с момента события (отформатированное)
     - Дата события
     - Цветовая метка (если задана)
   - Клик по карточке — переход к деталям записи
   - Свайп-действия:
     - Свайп влево (StartToEnd) — редактирование записи
     - Свайп вправо (EndToStart) — удаление записи
5. **Пустое состояние** (если нет записей)
   - Иконка: Material Icons (аналог "tray.fill")
   - Заголовок: "Что запомнить?"
   - Описание: "Создайте первую запись"
   - Кнопка: "Добавить запись"
6. **Пустое состояние поиска** (если поиск не дал результатов)
   - Заголовок: "Ничего не найдено"
   - Описание: "Попробуйте изменить поисковый запрос"

## Взаимодействия

- **Клик по записи** — переход к экрану деталей (Item Screen)
- **Свайп-действия**:
  - Свайп влево (StartToEnd) — открытие экрана редактирования
  - Свайп вправо (EndToStart) — удаление записи

## Навигация

- Клик на запись → Screen 3.1 (Item Screen)
- Кнопка "+" → Screen 4.1 (Create Item Screen)
- Свайп "Редактировать" → Screen 4.1 (Edit Item Screen)

## Зависимости от других этапов

- ✅ **Этап 7**: Модель данных (Entity, Domain model, Room Database, DAO) — **ЗАВЕРШЕН**, готов к использованию
- ✅ **Этап 6**: Форматирование количества дней — **РЕАЛИЗОВАНО**, используется для отображения количества дней в списке
- ✅ **Экран 1.1**: Root Screen — **ВЫПОЛНЕН**, навигация настроена через RootScreen

## Текущие проблемы ❌

- ❌ **Ошибка сборки Gradle**: Требуется обновление версии Gradle или AGP для совместимости
- ❌ **Отсутствие зависимости**: `material-icons-extended` не добавлен в `build.gradle.kts`
- ❌ **Отсутствующая функция**: `itemsListContent()` не реализована в MainScreen.kt (используется на строке 166)
- ❌ **Иконка для пустого состояния**: Иконка "tray.fill" не доступна без `material-icons-extended`

---

## Реализация

### Шаг 1: Подготовка слоя данных (Data Layer)

#### 1.1. Модель данных Room Entity

**Задачи:**
1. Создать Room Entity для записи события (ItemEntity)
   - Поля: id, title, details, timestamp, colorTag, displayOption
   - Индексы для оптимизации запросов
2. Написать тесты для проверки корректности Entity

**Критерии готовности:**
- ✅ Room Entity создана
- ✅ Поля определены корректно
- ✅ Индексы добавлены

**Примечание:** ✅ Реализовано в Этапе 7.

---

#### 1.2. Room DAO

**Задачи:**
1. Создать ItemDao с методами:
   - `getAllItems(): Flow<List<ItemEntity>>` - все записи (по убыванию)
   - `getAllItemsAsc(): Flow<List<ItemEntity>>` - все записи (по возрастанию)
   - `getAllItemsDesc(): Flow<List<ItemEntity>>` - все записи (по убыванию)
   - `getItemById(id: Long): Flow<ItemEntity?>`
   - `insertItem(item: ItemEntity): Long`
   - `updateItem(item: ItemEntity)`
   - `deleteItem(id: Long)`
   - `searchItems(query: String): Flow<List<ItemEntity>>`
2. Написать тесты для DAO (используя in-memory database)

**Критерии готовности:**
- ✅ DAO создан со всеми методами
- ✅ Методы сортировки добавлены (getAllItemsAsc, getAllItemsDesc)
- ✅ Интеграционные тесты написаны и проходят

**Примечание:** ✅ Реализовано в Этапе 7.

---

#### 1.3. Room Database

**Задачи:**
1. Создать AppDatabase с миграциями
2. Настроить версионирование базы данных
3. Написать тесты для проверки миграций

**Критерии готовности:**
- ✅ Database создана
- ✅ Миграции настроены
- ✅ Тесты написаны и проходят

**Примечание:** ✅ Реализовано в Этапе 7.

---

### Шаг 2: Подготовка слоя домена (Domain Layer)

#### 2.1. Domain Entity

**Задачи:**
1. Создать data class Item (domain model)
   - Конвертация из/в ItemEntity
   - Метод для вычисления количества дней
2. Написать тесты для domain model

**Критерии готовности:**
- ✅ Domain model создана
- ✅ Конвертация реализована
- ✅ Unit-тесты написаны и проходят

**Примечание:** ✅ Реализовано в Этапе 7.

---

#### 2.2. Repository Interface

**Задачи:**
1. Создать интерфейс ItemRepository в domain слое
   - Методы для CRUD операций
   - Методы для поиска и сортировки:
     - `getAllItems(sortOrder: SortOrder): Flow<List<Item>>`
   - `searchItems(query: String): Flow<List<Item>>`
2. Написать тесты для интерфейса (контракт)

**Критерии готовности:**
- ✅ Интерфейс Repository создан
- ✅ Методы объявлены, включая сортировку
- ✅ Unit-тесты написаны

**Примечание:** ✅ Реализовано в Этапе 7.

---

#### 2.3. Repository Implementation

**Задачи:**
1. Реализовать ItemRepositoryImpl в data слое
   - Использовать Room DAO
   - Преобразование между Entity и Domain model
   - Реализовать сортировку по timestamp (возрастание/убывание)
2. Написать интеграционные тесты для Repository

**Критерии готовности:**
- ✅ Реализация Repository создана
- ✅ Метод сортировки реализован
- ✅ Интеграционные тесты написаны и проходят

**Примечание:** ✅ Реализовано в Этапе 7.

---

#### 2.4. SortOrder Enum

**Задачи:**
1. Создать enum SortOrder для порядка сортировки
   - ASCENDING (старые первые)
   - DESCENDING (новые первые)

**Критерии готовности:**
- ✅ SortOrder enum создан

**Примечание:** ✅ Реализовано в рамках этого этапа.

---

### Шаг 3: Подготовка слоя представления (Presentation Layer)

#### 3.1. UI State

**Задачи:**
1. Создать data class MainScreenState
   - Список записей
   - Состояние поиска (query)
   - Состояние сортировки (SortOrder)
   - Состояние загрузки (Loading/Success/Error)
2. Создать sealed class для состояний (Loading, Success, Error)
3. Написать тесты для UI State

**Структура:**
```kotlin
sealed class MainScreenState {
    object Loading : MainScreenState()
    data class Success(val items: List<Item>) : MainScreenState()
    data class Error(val message: String) : MainScreenState()
}
```

**Критерии готовности:**
- ✅ UI State создан
- ✅ Sealed class для состояний создан
- ✅ Unit-тесты написаны и проходят

---

#### 3.2. ViewModel

**Задачи:**
1. Создать MainScreenViewModel
   - Инициализация: загрузка всех записей
   - Обработка поиска: фильтрация списка
   - Обработка сортировки: изменение порядка записей
   - Обработка удаления: удаление записи
2. Написать unit-тесты:
   - `whenViewModelCreated_thenLoadsAllItems()`
   - `whenSearchQueryChanged_thenFiltersItems()`
   - `whenSortOrderChanged_thenSortsItems()`
   - `whenItemDeleted_thenRemovesFromList()`
   - `whenNoItems_thenShowsEmptyState()`

**Критерии готовности:**
- ✅ ViewModel создан
- ✅ Все методы реализованы (updateSearchQuery, updateSortOrder, deleteItem)
- ✅ Объединение состояния поиска и сортировки для фильтрации
- ⚠️ Unit-тесты частично написаны (нужно добавить тесты для сортировки)

---

### Шаг 4: Реализация UI компонентов

#### 4.1. Компонент карточки записи

**Задачи:**
1. Создать ListItemView composable (аналог iOS ListItemView)
   - Верстка: Row с элементами:
     - Цветовая метка (круглый индикатор 16x16dp, слева) - условно, если задана
     - Название события (flexible, слева, maxLines: 2)
     - Количество дней (справа)
   - Отображение названия события
   - Отображение количества дней (отформатированное)
   - Цветовая метка (круглый индикатор 16x16dp)
   - Верстка идентична iOS для единообразия UX
2. Написать Preview для карточки
3. Написать UI тесты для карточки

**Критерии готовности:**
- ✅ ListItemView создан
- ✅ Все поля отображаются корректно
- ✅ Preview работает
- ✅ UI тесты написаны

**Примечание:** ✅ Реализовано ранее.

---

#### 4.2. Компонент пустого состояния

**Задачи:**
1. Создать EmptyState composable (аналог iOS ContentUnavailableView)
   - Иконка: "tray.fill" (Material Icons)
   - Заголовок: "Что запомнить?"
   - Описание: "Создайте первую запись"
   - Кнопка: "Добавить запись"
   - Верстка идентична iOS для единообразия UX
2. Написать Preview для пустого состояния

**Критерии готовности:**
- ✅ EmptyState создан
- ✅ Preview работает

**Примечание:** ✅ Реализовано ранее.

---

#### 4.3. Toolbar с сортировкой и кнопкой добавления

**Задачи:**
1. Реализовать TopAppBar с:
   - Заголовком "События"
   - Кнопкой сортировки (если записей > 1, placement: topBarLeading)
     - Меню с DropdownMenu для выбора сортировки
     - Опции: "Старые первые" / "Новые первые"
   - Кнопкой добавления "+" (placement: floatingActionButton)
2. Реализовать выпадающее меню (DropdownMenu) для сортировки
3. Написать Preview для Toolbar

**Критерии готовности:**
- ✅ TopAppBar реализован
- ✅ Сортировка работает
- ✅ Кнопка добавления работает
- ✅ Preview работает

---

#### 4.4. Главный экран MainScreen

**Задачи:**
1. Создать MainScreen composable
   - Подключить к ViewModel
   - Использовать NavigationStack (NavHost) для навигации
   - Отобразить TopAppBar с поиском, сортировкой и кнопкой добавления
   - Отобразить LazyColumn со списком записей или EmptyState
   - Реализовать свайп-действия (SwipeToDismissBox):
     - Кнопка "Удалить" (деструктивная, красная)
     - Кнопка "Редактировать" (синяя)
   - Обработать клики по карточкам (NavigationLink к Item Screen)
   - Пустое состояние поиска (если поиск не дал результатов)
   - Анимация списка при изменении данных
2. Интегрировать с навигацией (NavController)
3. Написать Preview для MainScreen

**Критерии готовности:**
- ✅ MainScreen создан
- ✅ Навигация работает
- ⚠️ Сортировка реализована (UI есть, но нужна интеграция)
- ❌ Свайп-действия не реализованы (функция itemsListContent отсутствует)
- ⚠️ Preview работает (частично)
- ⚠️ Поиск не реализован (UI для поиска отсутствует)

---

#### 4.5. Интеграция с RootScreen

**Задачи:**
1. Заменить заглушку eventsScreenContent() на MainScreen
2. Передать NavController в MainScreen
3. Проверить корректную работу навигации

**Критерии готовности:**
- ✅ Интеграция выполнена
- ✅ Навигация работает корректно

**Примечание:** ✅ Реализовано ранее.

---

### Шаг 5: Дополнительные функции

#### 5.1. Свайп-действия для удаления/редактирования

**Задачи:**
1. Реализовать SwipeToDismissBox для удаления
2. Реализовать SwipeToDismissBox для редактирования
3. Написать тесты для свайпов

**Критерии готовности:**
- ✅ Свайпы реализованы
- ⚠️ Тесты частично написаны (нужно добавить UI тесты)

---

#### 5.2. Локализация

**Задачи:**
1. Использовать готовые строковые ресурсы из `Localization_Plan.md`:
   - Заголовок экрана: `events`
   - Пустое состояние: `what_should_we_remember`, `create_your_first_item`
   - Пустое состояние поиска: `no_results_found`, `try_different_search_terms`
   - Опции сортировки: `sort`, `sort_order`, `old_first`, `new_first`
   - Кнопки: `add_item`, `edit`, `delete`
2. Поддержка русского и английского языков (уже реализована в `Localization_Plan.md`)

**Критерии готовности:**
- ✅ Используются готовые строковые ресурсы
- ✅ Локализация работает корректно
- ✅ Новые строки для пустого состояния поиска добавлены

**Примечание:** План локализации готов в документе `Localization_Plan.md`. Все строки определены и готовы к использованию.

---

### Шаг 6: Финальное тестирование

**Задачи:**
1. Проверить отображение списка записей (unit + UI тесты)
2. Проверить функциональность поиска (unit + UI тесты)
3. Проверить сортировку записей (unit + UI тесты)
4. Проверить переход к деталям записи (UI тесты)
5. Проверить отображение пустого состояния (UI тесты)
6. Проверить функциональность кнопки добавления (UI тесты)
7. Проверить свайп-действия (UI тесты)
8. Проверить обработку ошибок загрузки данных

**Критерии готовности:**
- ✅ Все компоненты работают корректно
- ✅ Поиск и сортировка работают
- ✅ Навигация работает корректно
- ⚠️ Unit-тесты частично написаны
- ⚠️ UI тесты не написаны

---

## Реализация (детали)

### UI компоненты
- Использовать LazyColumn для отображения списка записей (аналог iOS List)
- Реализовать кнопку сортировки с меню и DropdownMenu
- Создать ListItemView для элемента списка (идентично iOS верстке):
  - Row с spacing: 12dp
  - Цветовая метка: круглый индикатор 16x16dp (условно, если задана)
  - Название: Text с lineLimit(2), flexible width
  - Количество дней: Text с multilineTextAlignment: trailing
- Реализовать свайп-действия (SwipeToDismissBox):
  - Кнопка "Удалить" (деструктивная, красная)
  - Кнопка "Редактировать" (синяя)
- Обработать клики по карточкам (NavigationLink к Item Screen)
- Добавить состояние пустого экрана (EmptyState composable, идентично iOS ContentUnavailableView)
- Добавить пустое состояние поиска (если поиск не дал результатов)
- Анимация списка при изменении данных
- Подключить к ViewModel для получения данных (collectAsState)
- Использовать современные компоненты Material Design 3
- Применить тему приложения с поддержкой темного режима
- Применить корректные иконки из Material Icons

### Архитектура
- Использовать Room для локального хранения данных
- Реализовать Repository Pattern для абстракции источников данных
- Использовать Flow для реактивного обновления UI
- Применить Clean Architecture (Data → Domain → Presentation)
- Использовать SortOrder enum для управления порядком сортировки
- Объединение Flow для комбинации фильтрации и сортировки

---

## Тестирование

### Unit-тесты
- Тесты для Entity и Domain model ✅
- Тесты для DAO (in-memory database) ✅
- Тесты для Repository ✅
- Тесты для ViewModel (логика поиска, сортировки, удаления) ⚠️ частично написаны

### Интеграционные тесты
- Тесты взаимодействия Repository с Room Database ✅
- Тесты взаимодействия ViewModel с Repository ⚠️

### UI-тесты
- Проверить отображение списка записей ⚠️
- Проверить функциональность поиска ⚠️
- Проверить сортировку записей ⚠️
- Проверить переход к деталям записи ⚠️
- Проверить отображение пустого состояния ⚠️
- Проверить функциональность кнопки добавления ⚠️
- Проверить свайп-действия ⚠️

---

## Критерии завершения этапа

Этап считается завершенным, когда:

- ✅ Все компоненты созданы и работают
- ✅ Все unit-тесты написаны и проходят
- ✅ Все интеграционные тесты написаны и проходят
- ✅ Код соответствует правилам проекта
- ✅ Линтеры не выдают ошибок
- ✅ Поиск и сортировка работают корректно
- ✅ Навигация работает корректно
- ⚠️ UI тесты не написаны (опционально для следующего этапа)

---

## Блокируемые этапы

После завершения этого экрана можно приступать к:
- ✅ Экран 3.1: Item Screen (требует навигацию из Main Screen) — уже реализован
- ✅ Экран 4.1: Create/Edit Item Screen (требует навигацию из Main Screen) — уже реализован

**Примечание**: Экраны 3.1 и 4.1 реализованы, но не полностью интегрированы с Main Screen из-за ошибок сборки и отсутствия ключевых функций.

---

## Примечания

1. **Совместимость с iOS**: Верстка элемента списка должна быть похожа на iOS-версию для единообразия пользовательского опыта.

2. **Производительность**: Использовать LazyColumn для эффективного отображения больших списков.

3. **Реактивность**: Использовать Flow для автоматического обновления UI при изменении данных.

4. **Тестирование**: Все компоненты должны быть покрыты тестами перед использованием в других этапах.

5. **Зависимости**: ✅ Этап 7 (Модель данных) завершен и готов к использованию. ✅ Этап 6 (Форматирование дней) реализован и используется.

6. **Поиск**: Фильтрация планируется на уровне ViewModel через объединение Flow. Фильтрация должна выполняться по названию и описанию с игнорированием регистра. ⚠️ **НЕ РЕАЛИЗОВАНО** - UI для поиска отсутствует.

7. **Сортировка**: Реализована через enum SortOrder и выбор порядка в Repository и DAO. По умолчанию — новые первые (DESCENDING). ✅ **РЕАЛИЗОВАНО** - UI для сортировки есть (SortMenu), но нужна полная интеграция.

8. **Свайп-действия**: SwipeToDismissBox импортирован, но функция itemsListContent() не реализована. ❌ **НЕ РЕАЛИЗОВАНО**.

9. **Иконки**: Требуется зависимость `implementation("androidx.compose.material:material-icons-extended:1.7.6")` для доступа к иконке "tray.fill" для пустого состояния. ❌ **НЕ ДОБАВЛЕНО**.

10. **Ошибки сборки**: Требуется исправление версии Gradle или AGP для совместимости. ❌ **КРИТИЧНО**.

11. **Навигация**: Базовая навигация работает, но полная интеграция с DetailScreen и CreateEditScreen невозможна без исправления ошибок сборки.

---

## Обновленный план завершения Main Screen

### Приоритет 1: Исправление критических ошибок сборки

#### Шаг 1.1: Исправление версии Gradle
**Проблема**: AGP 8.13.2 требует Gradle 8.13+, но установлен Gradle 8.9

**Подробное описание проблемы:**
```
Build file '/Users/Oleg991/Documents/GitHub/JetpackDays/app/build.gradle.kts' line: 1
An exception occurred applying plugin request [id: 'com.android.application', version: '8.13.2']
Failed to apply plugin 'com.android.internal.version-check'.
Minimum supported Gradle version is 8.13. Current version is 8.9.
Try updating the 'distributionUrl' property in gradle/wrapper/gradle-wrapper.properties
to 'gradle-8.13-bin.zip'.
```

**Задачи:**

1. Открыть файл `gradle/wrapper/gradle-wrapper.properties`:
   ```properties
   distributionBase=GRADLE_USER_HOME
   distributionPath=wrapper/dists
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
   networkTimeout=10000
   validateDistributionUrl=true
   zipStoreBase=GRADLE_USER_HOME
   zipStorePath=wrapper/dists
   ```

2. Заменить строку с версией Gradle:
   - Текущая строка: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip`
   - Новая строка: `distributionUrl=https\://services.gradle.org/distributions/gradle-8.13-bin.zip`

3. Синхронизировать проект:
   - Выполнить `./gradlew clean`
   - Выполнить `./gradlew build`

**Критерии готовности:**
- ✅ Проект успешно собирается без ошибок
- ✅ Отсутствие сообщений о несовместимости версий Gradle и AGP
- ✅ Команда `./gradlew build` завершается успешно

**Проверка:**
```bash
# Проверить версию Gradle после обновления
./gradlew --version
# Ожидаемый вывод: Gradle 8.13 или выше

# Попытка собрать проект
./gradlew build
# Ожидаемый результат: BUILD SUCCESSFUL
```

**Возможные проблемы и решения:**
- **Проблема**: Если новая версия Gradle не скачивается
  - **Решение**: Проверить интернет-соединение, попробовать другую версию (8.14, 8.15)
- **Проблема**: Если появляются новые ошибки после обновления
  - **Решение**: Проверить, что все зависимости совместимы с новой версией Gradle

#### Шаг 1.2: Добавление зависимости material-icons-extended
**Проблема**: Иконка "tray.fill" для пустого состояния недоступна без `material-icons-extended`

**Подробное описание проблемы:**
```kotlin
// Текущий код в MainScreen.kt (строка 34):
// Иконка "tray.fill" не будет найдена без material-icons-extended
```

**Задачи:**

**Шаг 1.2.1: Добавить версию в `gradle/libs.versions.toml`**
1. Открыть файл `gradle/libs.versions.toml`
2. В секцию `[versions]` добавить:
   ```toml
   material = "1.7.6"
   ```

**Шаг 1.2.2: Добавить библиотеку в `gradle/libs.versions.toml`**
В секцию `[libraries]` добавить:
   ```toml
   androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "material" }
   ```

**Шаг 1.2.3: Добавить зависимость в `app/build.gradle.kts`**
В секцию `dependencies` добавить после других Compose зависимостей:
   ```kotlin
   implementation(libs.androidx.compose.material.icons.extended)
   ```

**Полное содержание обновленного `gradle/libs.versions.toml`:**
```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
coreKtx = "1.15.0"
junit = "4.13.2"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
lifecycleRuntimeKtx = "2.8.6"
activityCompose = "1.9.3"
composeBom = "2024.12.01"
navigationCompose = "2.8.3"
lifecycleViewmodelCompose = "2.8.6"
room = "2.6.1"
datastore = "1.1.1"
coroutines = "1.9.0"
junitJupiter = "5.11.3"
mockk = "1.13.13"
material = "1.7.6"  // <-- ДОБАВИТЬ ЭТУ СТРОКУ

[libraries]
// ... остальные библиотеки ...
androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended", version.ref = "material" }  // <-- ДОБАВИТЬ ЭТУ СТРОКУ
```

**Полное содержание обновленного `app/build.gradle.kts` (секция dependencies):**
```kotlin
dependencies {
    // Core AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)

    // <-- ДОБАВИТЬ ЭТУ ЗАВИСИМОСТЬ -->
    implementation(libs.androidx.compose.material.icons.extended)
    // <-- КОНЕЦ ДОБАВЛЕНИЯ -->

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

**Шаг 1.2.4: Синхронизировать проект**
```bash
./gradlew clean
./gradlew build
```

**Критерии готовности:**
- ✅ Проект успешно собирается с новой зависимостью
- ✅ Иконка `Icons.Outlined.Tray` доступна в коде
- ✅ Нет ошибок импорта при использовании иконок из material-icons-extended

**Пример использования иконки в MainScreen.kt:**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tray

// В emptyContent():
Icon(
    imageVector = Icons.Outlined.Tray,  // Иконка теперь доступна
    contentDescription = null,
    modifier = Modifier.size(80.dp)
)
```

**Проверка:**
```bash
# Попытка собрать проект
./gradlew build

# Проверить, что иконка доступна
# Открыть MainScreen.kt и попробовать:
import androidx.compose.material.icons.outlined.Tray
```

**Примечание:** В `material-icons-extended` иконка `Tray` находится в пакете `outlined`, а не `filled` как в iOS. Это аналогичная иконка для Material Design.

#### Шаг 1.3: Реализация отсутствующей функции itemsListContent()
**Проблема**: Функция вызывается на строке 166, но не определена

**Подробное описание проблемы:**
```kotlin
// В функции mainScreenContent() (строка 166-172):
when (val state = uiState) {
    is MainScreenState.Success -> {
        if (state.items.isEmpty()) {
            // ... empty state
        } else {
            itemsListContent(  // <-- ФУНКЦИЯ НЕ СУЩЕСТВУЕТ
                items = state.items,
                listState = listState,
                onItemClick = onItemClick,
                onEditClick = onEditClick,
                viewModel = viewModel,
            )
        }
    }
}
```

**Задачи:**

**Шаг 1.3.1: Создать структуру функции**
В `MainScreen.kt` добавить функцию после `emptySearchContent()`:

```kotlin
/**
 * Контент со списком записей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun itemsListContent(
    items: List<Item>,
    listState: LazyListState,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
) {
    // TODO: Реализовать логику
}
```

**Шаг 1.3.2: Реализовать LazyColumn с элементами**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun itemsListContent(
    items: List<Item>,
    listState: LazyListState,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            // TODO: Реализовать SwipeToDismissBox для каждого элемента
        }
    }
}
```

**Шаг 1.3.3: Реализовать SwipeToDismissBox для каждого элемента**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun itemsListContent(
    items: List<Item>,
    listState: LazyListState,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it != SwipeToDismissBoxValue.Settled) {
                        true
                    } else {
                        false
                    }
                },
                positionalThreshold = { distance ->
                    distance * 0.5f
                }
            )

            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                LaunchedEffect(Unit) {
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary  // Редактирование (синий)
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error  // Удаление (красный)
                        else -> Color.Transparent
                    }

                    val icon = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Edit
                        SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                        contentAlignment = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    ) {
                        icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = true,  // Свайп влево для редактирования
                enableDismissFromEndToStart = true, // Свайп вправо для удаления
            ) {
                // TODO: Реализовать содержимое карточки
            }
        }
    }
}
```

**Шаг 1.3.4: Обработать свайп-действия**
```kotlin
LaunchedEffect(dismissState.currentValue) {
    when (dismissState.currentValue) {
        SwipeToDismissBoxValue.StartToEnd -> {
            // Свайп влево - редактирование
            onEditClick(item.id)
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
        SwipeToDismissBoxValue.EndToStart -> {
            // Свайп вправо - удаление
            coroutineScope.launch {
                viewModel.deleteItem(item.id)
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
        else -> {}
    }
}
```

**Шаг 1.3.5: Добавить содержимое карточки**
```kotlin
SwipeToDismissBox(
    // ... остальные параметры ...
) {
    // Содержимое карточки (ListItemView)
    listItemView(
        modifier = Modifier.clickable { onItemClick(item.id) },
        item = item
    )
}
```

**Полная реализация функции:**
```kotlin
/**
 * Контент со списком записей.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun itemsListContent(
    items: List<Item>,
    listState: LazyListState,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
    viewModel: MainScreenViewModel,
) {
    val coroutineScope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.spacing_medium)),
    ) {
        items(
            items = items,
            key = { it.id },
        ) { item ->
            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it != SwipeToDismissBoxValue.Settled) {
                        true
                    } else {
                        false
                    }
                },
                positionalThreshold = { distance ->
                    distance * 0.5f
                }
            )

            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                LaunchedEffect(Unit) {
                    dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                }
            }

            LaunchedEffect(dismissState.currentValue) {
                when (dismissState.currentValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        // Свайп влево - редактирование
                        onEditClick(item.id)
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        // Свайп вправо - удаление
                        coroutineScope.launch {
                            viewModel.deleteItem(item.id)
                        }
                        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                    }
                    else -> {}
                }
            }

            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    val color = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary  // Редактирование (синий)
                        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error  // Удаление (красный)
                        else -> Color.Transparent
                    }

                    val icon = when (dismissState.dismissDirection) {
                        SwipeToDismissBoxValue.StartToEnd -> Icons.Filled.Edit
                        SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                        else -> null
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = dimensionResource(R.dimen.spacing_large)),
                        contentAlignment = when (dismissState.dismissDirection) {
                            SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                            SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                            else -> Alignment.Center
                        }
                    ) {
                        icon?.let {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                },
                enableDismissFromStartToEnd = true,  // Свайп влево для редактирования
                enableDismissFromEndToStart = true, // Свайп вправо для удаления
            ) {
                listItemView(
                    modifier = Modifier.clickable { onItemClick(item.id) },
                    item = item
                )
            }
        }
    }
}
```

**Критерии готовности:**
- ✅ Функция `itemsListContent()` создана и компилируется
- ✅ LazyColumn отображает список записей
- ✅ Свайп влево открывает CreateEditScreen в режиме редактирования
- ✅ Свайп вправо удаляет запись из списка
- ✅ Клик по карточке открывает DetailScreen
- ✅ Анимации свайпа работают корректно
- ✅ Кнопки удаления (красная) и редактирования (синяя) отображаются при свайпе

**Проверка:**
```kotlin
// 1. Проверить, что функция компилируется
// В MainScreen.kt:
itemsListContent(
    items = state.items,
    listState = listState,
    onItemClick = onItemClick,
    onEditClick = onEditClick,
    viewModel = viewModel,
)

// 2. Проверить свайп-действия
// - Свайп влево: должен открывать редактирование
// - Свайп вправо: должен удалять запись
// - Клик по карточке: должен открывать детали

// 3. Проверить анимации
// - Свайп должен иметь плавную анимацию
// - Кнопки должны появляться с правильным цветом
```

**Примечания:**
1. **Свайп-действия**:
   - Свайп влево (StartToEnd) → редактирование (синяя кнопка)
   - Свайп вправо (EndToStart) → удаление (красная кнопка)

2. **Анимации**:
   - Использовать `SwipeToDismissBoxValue.Settled` для возврата в исходное состояние
   - Использовать `rememberSwipeToDismissBoxState` для управления состоянием свайпа

3. **UX**:
   - Кнопки должны быть визуально понятны (иконки + цвета)
   - Свайп должен быть плавным и отзывчивым
   - После свайпа элемент должен возвращаться в исходное состояние

### Приоритет 2: Реализация функциональности поиска

#### Шаг 2.1: Добавление UI для поиска
**Проблема**: Текущая реализация не имеет UI для ввода поискового запроса

**Подробное описание проблемы:**
```kotlin
// Текущая реализация в MainScreen.kt:
TopAppBar(
    title = { Text(stringResource(R.string.events)) },
    navigationIcon = { /* сортировка */ },
    actions = { /* кнопка добавления */ },
)
// Отсутствует SearchBar для ввода поискового запроса
```

**Задачи:**

**Шаг 2.1.1: Добавить состояние для отображения SearchBar**
```kotlin
private fun mainScreenContent(
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel,
    onItemClick: (Long) -> Unit,
    onEditClick: (Long) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val itemsCount by viewModel.itemsCount.collectAsState()
    val listState = rememberLazyListState()

    // <-- ДОБАВИТЬ ЭТО СОСТОЯНИЕ -->
    var showSearchBar by remember { mutableStateOf(false) }
    // <-- КОНЕЦ ДОБАВЛЕНИЯ -->

    Scaffold(
        // ...
    )
}
```

**Шаг 2.1.2: Обновить TopAppBar для поддержки SearchBar**
```kotlin
Scaffold(
    modifier = modifier.fillMaxSize(),
    topBar = {
        Column {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        // TODO: Добавить SearchBar
                    } else {
                        Text(stringResource(R.string.events))
                    }
                },
                navigationIcon = {
                    if (itemsCount > 1) {
                        SortMenu(
                            sortOrder = sortOrder,
                            onSortOrderChange = { viewModel.updateSortOrder(it) },
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSearchBar = !showSearchBar }) {
                        Icon(
                            imageVector = if (showSearchBar) {
                                Icons.Filled.Close
                            } else {
                                Icons.Filled.Search
                            },
                            contentDescription = if (showSearchBar) {
                                stringResource(R.string.close)
                            } else {
                                stringResource(R.string.search)
                            },
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
            )

            // <-- ДОБАВИТЬ SearchBar ВНИЗ -->
            if (showSearchBar) {
                // TODO: Реализовать SearchBar
            }
            // <-- КОНЕЦ ДОБАВЛЕНИЯ -->
        }
    },
    floatingActionButton = {
        FloatingActionButton(
            onClick = { showSearchBar = false },  // Закрыть поиск при нажатии FAB
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.add_item),
            )
        }
    },
) { paddingValues ->
    // ... остальной контент
}
```

**Шаг 2.1.3: Добавить импорты для SearchBar**
```kotlin
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
```

**Шаг 2.1.4: Реализовать SearchBar**
```kotlin
if (showSearchBar) {
    SearchBar(
        query = searchQuery,
        onQueryChange = { viewModel.updateSearchQuery(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.spacing_small)),
        placeholder = {
            Text(stringResource(R.string.search_placeholder))
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        },
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        // Поиск подсказок (опционально)
        // TODO: Реализовать подсказки при вводе
    }
}
```

**Шаг 2.1.5: Добавить строковые ресурсы для поиска**
В файлы `values/strings.xml` и `values-ru/strings.xml` добавить:
```xml
<!-- values/strings.xml -->
<string name="search">Search</string>
<string name="search_placeholder">Search records...</string>
<string name="close">Close</string>

<!-- values-ru/strings.xml -->
<string name="search">Поиск</string>
<string name="search_placeholder">Поиск по записям...</string>
<string name="close">Закрыть</string>
```

**Шаг 2.1.6: Обновить логику в ViewModel для фильтрации**
```kotlin
class MainScreenViewModel(
    private val repository: ItemRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    val uiState: StateFlow<MainScreenState> = combine(
        repository.getAllItems(_sortOrder.value),
        _searchQuery
    ) { items, query ->
        if (query.isBlank()) {
            items
        } else {
            items.filter { item ->
                val lowerCaseQuery = query.lowercase()
                item.title.lowercase().contains(lowerCaseQuery) ||
                item.details?.lowercase()?.contains(lowerCaseQuery) == true
            }
        }
    }.map { items ->
        MainScreenState.Success(items)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainScreenState.Loading,
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun deleteItem(id: Long) {
        viewModelScope.launch {
            try {
                repository.deleteItem(id)
            } catch (e: Exception) {
                Log.e("MainScreenViewModel", "Ошибка удаления записи: ${e.message}")
            }
        }
    }
}
```

**Полная реализация SearchBar:**
```kotlin
if (showSearchBar) {
    SearchBar(
        query = searchQuery,
        onQueryChange = { viewModel.updateSearchQuery(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.spacing_small)),
        placeholder = {
            Text(stringResource(R.string.search_placeholder))
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                    Icon(Icons.Filled.Close, contentDescription = null)
                }
            }
        },
        colors = SearchBarDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        // Подсказки поиска (опционально)
        // Можно добавить подсказки последних запросов или популярных записей
    }
}
```

**Критерии готовности:**
- ✅ SearchBar отображается при нажатии на иконку поиска
- ✅ Плейсхолдер "Поиск по записям..." отображается
- ✅ Ввод текста фильтрует список в реальном времени
- ✅ Фильтрация работает по названию и описанию
- ✅ Регистр игнорируется при поиске
- ✅ Кнопка "Закрыть" скрывает SearchBar
- ✅ Кнопка "Очистить" очищает поисковый запрос
- ✅ EmptySearchContent отображается при отсутствии результатов

**Проверка:**
```kotlin
// 1. Проверить, что SearchBar открывается
// Нажать на иконку поиска в TopAppBar
// Ожидаемый результат: SearchBar открывается, заголовок скрывается

// 2. Проверить фильтрацию
// Ввести поисковый запрос
// Ожидаемый результат: список обновляется, содержатся только совпадения

// 3. Проверить очистку
// Нажать на кнопку "X" для очистки поиска
// Ожидаемый результат: поисковый запрос очищается, отображается весь список

// 4. Проверить закрытие
// Нажать на кнопку закрытия в TopAppBar
// Ожидаемый результат: SearchBar скрывается, заголовок отображается

// 5. Проверить пустой результат поиска
// Ввести запрос, который не даст результатов
// Ожидаемый результат: отображается emptySearchContent()
```

**Примечания:**
1. **UX решения**:
   - SearchBar заменяет заголовок экрана при открытии
   - Иконка поиска переключается на "Закрыть" при открытии SearchBar
   - Кнопка очистки (X) появляется только когда есть текст
   - Поиск закрывается при нажатии FAB

2. **Фильтрация**:
   - Поиск выполняется по названию и описанию
   - Регистр игнорируется для удобства
   - Фильтрация выполняется в реальном времени

3. **Пустое состояние поиска**:
   - Если поиск не дал результатов, отображается `emptySearchContent()`
   - Пользователь видит понятное сообщение "Ничего не найдено"

### Приоритет 3: Полная интеграция навигации

#### Шаг 3.1: Интеграция с DetailScreen
**Задачи:**

**Шаг 3.1.1: Обновить вызов MainScreen в RootScreen.kt**
```kotlin
// Найти события Screen.Events в RootScreen.kt
// Текущий код:
composable(route = Screen.Events.route) {
    eventsScreenContent(
        onItemClick = { /* TODO */ },
        onEditClick = { /* TODO */ },
    )
}

// Обновить на:
composable(route = Screen.Events.route) {
    mainScreen(
        onItemClick = { navController.navigate(Screen.Item.createRoute(it)) },
        onEditClick = { navController.navigate(Screen.EditItem.createRoute(it)) },
    )
}
```

**Шаг 3.1.2: Убедиться в существовании маршрутов для DetailScreen**
Проверить `navigation/Screen.kt`:
```kotlin
sealed class Screen(val route: String) {
    object Events : Screen("events")
    object More : Screen("more")

    object Item : Screen("item/{itemId}") {
        fun createRoute(itemId: Long): String = "item/$itemId"
    }

    object CreateItem : Screen("create_item")
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: Long): String = "edit_item/$itemId"
    }
}
```

**Шаг 3.1.3: Проверить, что DetailScreen принимает itemId**
В `ui/screen/DetailScreen.kt`:
```kotlin
@Composable
fun detailScreen(
    itemId: Long,
    modifier: Modifier = Modifier,
) {
    val viewModel: DetailScreenViewModel =
        viewModel(
            factory = DetailScreenViewModel.factory(
                com.dayscounter.di.AppModule.createItemRepository(
                    com.dayscounter.data.database.DaysDatabase.getDatabase(
                        androidx.compose.ui.platform.LocalContext.current.applicationContext,
                    ),
                ),
                itemId,
            ),
        )
    // ... остальной код
}
```

**Шаг 3.1.4: Добавить composable для DetailScreen в NavHost**
В `RootScreen.kt`:
```kotlin
composable(
    route = Screen.Item.route,
    arguments = listOf(navArgument("itemId") { type = NavType.LongType })
) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
    detailScreen(
        itemId = itemId,
        onBack = { navController.popBackStack() },
    )
}
```

**Шаг 3.1.5: Добавить функцию для возврата в DetailScreen**
В `DetailScreen.kt`:
```kotlin
@Composable
fun detailScreen(
    itemId: Long,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // В DetailAppBar использовать onBack:
    DetailAppBar(
        onBack = onBack,
        // ...
    )
}
```

**Критерии готовности:**
- ✅ Клик по записи открывает DetailScreen
- ✅ itemId корректно передается в DetailScreen
- ✅ DetailScreen отображает правильные данные
- ✅ Кнопка "Назад" в DetailScreen возвращает к MainScreen
- ✅ Навигация работает корректно

**Проверка:**
```kotlin
// 1. Клик на запись
// Ожидаемый результат: открывается DetailScreen

// 2. Проверить URL навигации
// Ожидаемый формат: "events/item/123"

// 3. Проверить, что данные отображаются
// Ожидаемый результат: название, описание, дата, цвет отображаются корректно

// 4. Нажать "Назад"
// Ожидаемый результат: возврат к MainScreen
```

**Примечания:**
1. **Передача itemId**: itemId должен быть типом Long, так как в базе данных id имеет тип Long
2. **Возврат**: Использовать `navController.popBackStack()` для возврата к предыдущему экрану
3. **Безопасность**: Проверить, что itemId не равен 0 или -1 перед открытием DetailScreen

#### Шаг 3.2: Интеграция с CreateEditScreen (создание)
**Задачи:**

**Шаг 3.2.1: Убедиться в существовании маршрута для создания**
Проверить `navigation/Screen.kt`:
```kotlin
sealed class Screen(val route: String) {
    object Events : Screen("events")
    object More : Screen("more")

    object Item : Screen("item/{itemId}") {
        fun createRoute(itemId: Long): String = "item/$itemId"
    }

    object CreateItem : Screen("create_item")  // <-- ДОЛЖЕН БЫТЬ
    object EditItem : Screen("edit_item/{itemId}") {
        fun createRoute(itemId: Long): String = "edit_item/$itemId"
    }
}
```

**Шаг 3.2.2: Обновить FAB в MainScreen.kt**
Текущая реализация:
```kotlin
floatingActionButton = {
    FloatingActionButton(
        onClick = { viewModel.updateSearchQuery("") },  // <-- ИЗМЕНИТЬ ЭТО
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = stringResource(R.string.add_item),
        )
    }
}
```

Новая реализация:
```kotlin
// В mainScreen() добавить параметр navController:
@Composable
fun mainScreen(
    modifier: Modifier = Modifier,
    navController: NavController = rememberNavController(),
    onItemClick: (Long) -> Unit = {},
    onEditClick: (Long) -> Unit = {},
) {
    // ... остальной код

    Scaffold(
        // ... topBar
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.CreateItem.route)  // <-- ПРАВИЛЬНАЯ НАВИГАЦИЯ
                },
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.add_item),
                )
            }
        },
    ) { paddingValues ->
        // ... контент
    }
}
```

**Шаг 3.2.3: Обновить вызов MainScreen в RootScreen.kt**
```kotlin
composable(route = Screen.Events.route) {
    mainScreen(
        navController = navController,  // <-- ПЕРЕДАТЬ NavController
        onItemClick = { navController.navigate(Screen.Item.createRoute(it)) },
        onEditClick = { navController.navigate(Screen.EditItem.createRoute(it)) },
    )
}
```

**Шаг 3.2.4: Добавить composable для CreateEditScreen в NavHost**
В `RootScreen.kt`:
```kotlin
composable(route = Screen.CreateItem.route) {
    createEditScreen(
        itemId = null,  // null для режима создания
        onBack = { navController.popBackStack() },
        onSave = { navController.popBackStack() },
    )
}
```

**Шаг 3.2.5: Убедиться, что CreateEditScreen принимает nullable itemId**
В `ui/screen/CreateEditScreen.kt`:
```kotlin
@Composable
fun createEditScreen(
    itemId: Long?,  // <-- NULLABLE ДЛЯ РЕЖИМА СОЗДАНИЯ
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val viewModel: CreateEditScreenViewModel =
        viewModel(
            factory = CreateEditScreenViewModel.factory(
                com.dayscounter.di.AppModule.createItemRepository(
                    com.dayscounter.data.database.DaysDatabase.getDatabase(
                        androidx.compose.ui.platform.LocalContext.current.applicationContext,
                    ),
                ),
                itemId,
            ),
        )
    // ... остальной код
}
```

**Шаг 3.2.6: Обновить фабрику ViewModel для поддержки null**
В `CreateEditScreenViewModel.kt`:
```kotlin
class CreateEditScreenViewModel(
    private val repository: ItemRepository,
    itemId: Long?,
) : ViewModel() {

    // Если itemId == null, то режим создания
    // Если itemId != null, то режим редактирования

    private val _uiState = MutableStateFlow(
        if (itemId == null) {
            CreateEditUiState(
                title = "",
                details = "",
                timestamp = System.currentTimeMillis(),
                colorTag = null,
                displayOption = DisplayOption.DAY,
            )
        } else {
            // Загрузить данные из Repository
            CreateEditUiState(/* ... */)
        }
    )
    // ... остальной код
}

companion object {
    fun factory(
        repository: ItemRepository,
        itemId: Long?,
    ): ViewModelProvider.Factory = viewModelFactory {
        CreateEditScreenViewModel(repository, itemId)
    }
}
```

**Критерии готовности:**
- ✅ Кнопка "+" открывает CreateEditScreen в режиме создания
- ✅ CreateEditScreen открывается с пустой формой
- ✅ itemId передается как null для создания
- ✅ Кнопка "Сохранить" сохраняет новую запись
- ✅ Кнопка "Отмена/Закрыть" возвращает к MainScreen
- ✅ После сохранения пользователь возвращается к MainScreen

**Проверка:**
```kotlin
// 1. Нажать на кнопку "+"
// Ожидаемый результат: открывается CreateEditScreen

// 2. Проверить, что форма пустая
// Ожидаемый результат: все поля пустые

// 3. Заполнить форму и сохранить
// Ожидаемый результат: запись сохраняется, возврат к MainScreen

// 4. Проверить, что новая запись отображается в списке
// Ожидаемый результат: новая запись в списке событий
```

**Примечания:**
1. **itemId = null**: Обозначает режим создания новой записи
2. **itemId != null**: Обозначает режим редактирования существующей записи
3. **Возврат**: После сохранения/отмены пользователь должен вернуться к MainScreen
4. **Обновление списка**: После создания новой записи список должен автоматически обновиться через Flow

#### Шаг 3.3: Интеграция с CreateEditScreen (редактирование)
**Задачи:**

**Шаг 3.3.1: Проверить реализацию свайпа в itemsListContent()**
Убедиться, что свайп влево (StartToEnd) корректно обрабатывается:
```kotlin
// В itemsListContent():
LaunchedEffect(dismissState.currentValue) {
    when (dismissState.currentValue) {
        SwipeToDismissBoxValue.StartToEnd -> {
            // Свайп влево - редактирование
            onEditClick(item.id)  // <-- ЭТОТ ОБРАБОТЧИК
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
        SwipeToDismissBoxValue.EndToStart -> {
            // Свайп вправо - удаление
            coroutineScope.launch {
                viewModel.deleteItem(item.id)
            }
            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
        }
        else -> {}
    }
}
```

**Шаг 3.3.2: Проверить передачу onEditClick в mainScreen()**
```kotlin
// В mainScreenContent():
Scaffold(/* ... */) { paddingValues ->
    when (val state = uiState) {
        is MainScreenState.Success -> {
            if (state.items.isEmpty()) {
                // ... empty state
            } else {
                itemsListContent(
                    items = state.items,
                    listState = listState,
                    onItemClick = onItemClick,
                    onEditClick = onEditClick,  // <-- ПЕРЕДАТЬ ОБРАБОТЧИК
                    viewModel = viewModel,
                )
            }
        }
        // ... другие состояния
    }
}
```

**Шаг 3.3.3: Проверить вызов MainScreen в RootScreen.kt**
```kotlin
// В RootScreen.kt:
composable(route = Screen.Events.route) {
    mainScreen(
        navController = navController,
        onItemClick = { navController.navigate(Screen.Item.createRoute(it)) },
        onEditClick = { navController.navigate(Screen.EditItem.createRoute(it)) },  // <-- КОРРЕКТНАЯ НАВИГАЦИЯ
    )
}
```

**Шаг 3.3.4: Добавить composable для EditItem в NavHost**
В `RootScreen.kt`:
```kotlin
composable(
    route = Screen.EditItem.route,
    arguments = listOf(navArgument("itemId") { type = NavType.LongType })
) { backStackEntry ->
    val itemId = backStackEntry.arguments?.getLong("itemId") ?: 0L
    createEditScreen(
        itemId = itemId,  // <-- ПЕРЕДАТЬ itemId
        onBack = { navController.popBackStack() },
        onSave = { navController.popBackStack() },
    )
}
```

**Шаг 3.3.5: Проверить загрузку данных в CreateEditScreenViewModel**
```kotlin
// В CreateEditScreenViewModel.kt:
class CreateEditScreenViewModel(
    private val repository: ItemRepository,
    itemId: Long?,
) : ViewModel() {

    init {
        if (itemId != null) {
            // Загрузить данные для редактирования
            viewModelScope.launch {
                try {
                    val item = repository.getItemById(itemId)
                    _uiState.value = CreateEditUiState(
                        title = item.title,
                        details = item.details ?: "",
                        timestamp = item.timestamp,
                        colorTag = item.colorTag,
                        displayOption = item.displayOption,
                    )
                } catch (e: Exception) {
                    Log.e("CreateEditScreenViewModel", "Ошибка загрузки записи: ${e.message}")
                }
            }
        }
        // Если itemId == null, то форма остается пустой (режим создания)
    }
    // ... остальной код
}
```

**Критерии готовности:**
- ✅ Свайп влево открывает CreateEditScreen в режиме редактирования
- ✅ itemId корректно передается в CreateEditScreen
- ✅ CreateEditScreen загружает существующие данные
- ✅ Форма предзаполнена текущими значениями
- ✅ Кнопка "Сохранить" сохраняет изменения
- ✅ После сохранения пользователь возвращается к MainScreen
- ✅ Изменения отображаются в списке записей

**Проверка:**
```kotlin
// 1. Свайп записи влево
// Ожидаемый результат: открывается CreateEditScreen

// 2. Проверить URL навигации
// Ожидаемый формат: "edit_item/123"

// 3. Проверить, что данные загружены
// Ожидаемый результат: форма предзаполнена текущими значениями

// 4. Изменить данные и сохранить
// Ожидаемый результат: запись обновляется

// 5. Проверить, что изменения отображаются в списке
// Ожидаемый результат: обновленная запись в списке с новыми данными
```

**Примечания:**
1. **Свайп-действия**:
   - Свайп влево (StartToEnd) → редактирование
   - Свайп вправо (EndToStart) → удаление

2. **Загрузка данных**:
   - Данные загружаются в `init` блока ViewModel
   - Использовать `viewModelScope.launch` для асинхронной загрузки
   - Обработать возможные ошибки при загрузке

3. **Сохранение**:
   - Проверять, что данные изменились перед сохранением
   - В режиме редактирования кнопка "Сохранить" активна только при изменениях
   - В режиме создания кнопка активна при заполненном названии

4. **UX**:
   - Пользователь должен видеть плавный переход к экрану редактирования
   - Форма должна быть предзаполнена текущими данными
   - Изменения должны сразу отображаться в списке после возврата

### Приоритет 4: Тестирование

#### Шаг 4.1: Написание unit-тестов для ViewModel
**Задачи:**

**Шаг 4.1.1: Создать структуру тестового класса**
В файл `app/src/test/java/com/dayscounter/viewmodel/MainScreenViewModelTest.kt`:

```kotlin
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.model.Item

class MainScreenViewModelTest {

    private lateinit var viewModel: MainScreenViewModel
    private val repository = mockk<ItemRepository>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        // TODO: Создать ViewModel
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        resetMain()
    }

    // TODO: Добавить тесты
}
```

**Шаг 4.1.2: Создать ViewModel в setUp()**
```kotlin
@BeforeEach
fun setUp() {
    Dispatchers.setMain(UnconfinedTestDispatcher())

    // Настроить mock repository
    every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf()
    every { repository.getAllItems(SortOrder.ASCENDING) } returns flowOf()

    viewModel = MainScreenViewModel(repository)
}
```

**Шаг 4.1.3: Тест - загрузка всех записей при создании**
```kotlin
@Test
fun `whenViewModelCreated_thenLoadsAllItems`() = runTest {
    // Given
    val testItems = listOf(
        Item(
            id = 1L,
            title = "Test Event 1",
            details = null,
            timestamp = System.currentTimeMillis(),
            colorTag = null,
            displayOption = com.dayscounter.domain.model.DisplayOption.DAY,
        ),
    )
    every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)

    // When
    val testViewModel = MainScreenViewModel(repository)

    // Then
    val state = testViewModel.uiState.value
    assert(state is MainScreenViewModel.MainScreenState.Success)
    val items = (state as MainScreenViewModel.MainScreenState.Success).items
    assertEquals(testItems, items)
}
```

**Шаг 4.1.4: Тест - фильтрация по поисковому запросу**
```kotlin
@Test
fun `whenSearchQueryChanged_thenFiltersItems`() = runTest {
    // Given
    val testItems = listOf(
        Item(1L, "Apple", null, System.currentTimeMillis(), null, DisplayOption.DAY),
        Item(2L, "Banana", null, System.currentTimeMillis(), null, DisplayOption.DAY),
    )
    every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)

    val testViewModel = MainScreenViewModel(repository)

    // When
    testViewModel.updateSearchQuery("app")

    // Then
    val state = testViewModel.uiState.value as MainScreenViewModel.MainScreenState.Success
    assertEquals(1, state.items.size)
    assertEquals("Apple", state.items[0].title)
}
```

**Шаг 4.1.5: Тест - сортировка записей**
```kotlin
@Test
fun `whenSortOrderChanged_thenSortsItems`() = runTest {
    // Given
    val testItems = listOf(
        Item(1L, "First", System.currentTimeMillis() - 10000, null, DisplayOption.DAY),
        Item(2L, "Second", System.currentTimeMillis() - 5000, null, DisplayOption.DAY),
        Item(3L, "Third", System.currentTimeMillis(), null, DisplayOption.DAY),
    )
    every { repository.getAllItems(any()) } returns flowOf(testItems)

    val testViewModel = MainScreenViewModel(repository)

    // When
    testViewModel.updateSortOrder(SortOrder.ASCENDING)

    // Then
    verify { repository.getAllItems(SortOrder.ASCENDING) }
    val state = testViewModel.uiState.value as MainScreenViewModel.MainScreenState.Success
    assertEquals("First", state.items[0].title)
    assertEquals("Third", state.items[2].title)
}
```

**Шаг 4.1.6: Тест - удаление записи**
```kotlin
@Test
fun `whenItemDeleted_thenRemovesFromList`() = runTest {
    // Given
    val testItems = listOf(
        Item(1L, "Test 1", System.currentTimeMillis(), null, DisplayOption.DAY),
        Item(2L, "Test 2", System.currentTimeMillis(), null, DisplayOption.DAY),
    )
    every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)
    every { repository.deleteItem(1L) } just Runs

    val testViewModel = MainScreenViewModel(repository)

    // When
    testViewModel.deleteItem(1L)

    // Then
    verify { repository.deleteItem(1L) }
}
```

**Шаг 4.1.7: Тест - отображение пустого состояния**
```kotlin
@Test
fun `whenNoItems_thenShowsEmptyState`() = runTest {
    // Given
    every { repository.getAllItems(any()) } returns flowOf(emptyList())

    val testViewModel = MainScreenViewModel(repository)

    // When
    val state = testViewModel.uiState.value as MainScreenViewModel.MainScreenState.Success

    // Then
    assertTrue(state.items.isEmpty())
}
```

**Шаг 4.1.8: Добавить необходимые импорты**
```kotlin
import com.dayscounter.domain.model.DisplayOption
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import io.mockk.just
import io.mockk.Runs
```

**Полная структура тестового класса:**
```kotlin
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import io.mockk.just
import io.mockk.Runs
import com.dayscounter.domain.model.SortOrder
import com.dayscounter.domain.model.Item
import com.dayscounter.domain.model.DisplayOption
import com.dayscounter.viewmodel.MainScreenViewModel

class MainScreenViewModelTest {

    private lateinit var viewModel: MainScreenViewModel
    private val repository = mockk<ItemRepository>()

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())

        val testItems = listOf(
            Item(1L, "Test Event", null, System.currentTimeMillis(), null, DisplayOption.DAY),
        )
        every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)
        every { repository.getAllItems(SortOrder.ASCENDING) } returns flowOf(testItems)

        viewModel = MainScreenViewModel(repository)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterEach
    fun tearDown() {
        resetMain()
    }

    @Test
    fun `whenViewModelCreated_thenLoadsAllItems`() = runTest {
        // Given
        val testItems = listOf(
            Item(1L, "Test Event", null, System.currentTimeMillis(), null, DisplayOption.DAY),
        )
        every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)

        // When
        val testViewModel = MainScreenViewModel(repository)

        // Then
        val state = testViewModel.uiState.value
        assertTrue(state is MainScreenViewModel.MainScreenState.Success)
        val items = (state as MainScreenViewModel.MainScreenState.Success).items
        assertEquals(testItems, items)
    }

    @Test
    fun `whenSearchQueryChanged_thenFiltersItems`() = runTest {
        // Given
        val testItems = listOf(
            Item(1L, "Apple", null, System.currentTimeMillis(), null, DisplayOption.DAY),
            Item(2L, "Banana", null, System.currentTimeMillis(), null, DisplayOption.DAY),
        )
        every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)

        val testViewModel = MainScreenViewModel(repository)

        // When
        testViewModel.updateSearchQuery("app")

        // Then
        val state = testViewModel.uiState.value as MainScreenViewModel.MainScreenState.Success
        assertEquals(1, state.items.size)
        assertEquals("Apple", state.items[0].title)
    }

    @Test
    fun `whenSortOrderChanged_thenSortsItems`() = runTest {
        // Given
        val testItems = listOf(
            Item(1L, "First", System.currentTimeMillis() - 10000, null, DisplayOption.DAY),
            Item(2L, "Second", System.currentTimeMillis() - 5000, null, DisplayOption.DAY),
            Item(3L, "Third", System.currentTimeMillis(), null, DisplayOption.DAY),
        )
        every { repository.getAllItems(any()) } returns flowOf(testItems)

        val testViewModel = MainScreenViewModel(repository)

        // When
        testViewModel.updateSortOrder(SortOrder.ASCENDING)

        // Then
        verify { repository.getAllItems(SortOrder.ASCENDING) }
        val state = testViewModel.uiState.value as MainScreenViewModel.MainScreenState.Success
        assertEquals("First", state.items[0].title)
        assertEquals("Third", state.items[2].title)
    }

    @Test
    fun `whenItemDeleted_thenRemovesFromList`() = runTest {
        // Given
        val testItems = listOf(
            Item(1L, "Test 1", System.currentTimeMillis(), null, DisplayOption.DAY),
            Item(2L, "Test 2", System.currentTimeMillis(), null, DisplayOption.DAY),
        )
        every { repository.getAllItems(SortOrder.DESCENDING) } returns flowOf(testItems)
        every { repository.deleteItem(1L) } just Runs

        val testViewModel = MainScreenViewModel(repository)

        // When
        testViewModel.deleteItem(1L)

        // Then
        verify { repository.deleteItem(1L) }
    }

    @Test
    fun `whenNoItems_thenShowsEmptyState`() = runTest {
        // Given
        every { repository.getAllItems(any()) } returns flowOf(emptyList())

        val testViewModel = MainScreenViewModel(repository)

        // When
        val state = testViewModel.uiState.value as MainScreenViewModel.MainScreenState.Success

        // Then
        assertTrue(state.items.isEmpty())
    }
}
```

**Критерии готовности:**
- ✅ Все unit-тесты написаны
- ✅ Тест покрывает основные сценарии ViewModel
- ✅ Тесты используют AAA паттерн (Arrange, Act, Assert)
- ✅ Тесты независимы друг от друга
- ✅ Тесты используют mockk для мокирования зависимостей
- ✅ Все тесты проходят успешно

**Проверка:**
```bash
# Запустить unit-тесты
./gradlew test --tests MainScreenViewModelTest

# Ожидаемый результат: BUILD SUCCESSFUL
# Все тесты должны пройти
```

**Примечания:**
1. **AAA паттерн**: Arrange (Given), Act (When), Assert (Then)
2. **MockK**: Использовать mockk для мокирования Repository
3. **Coroutines**: Использовать test dispatcher для корутин в тестах
4. **Покрытие**: Тесты должны покрывать основные сценарии бизнес-логики

#### Шаг 4.2: Написание UI-тестов
**Задачи:**
1. Проверить отображение списка записей
2. Проверить функциональность поиска
3. Проверить сортировку записей
4. Проверить переход к деталям записи
5. Проверить отображение пустого состояния
6. Проверить функциональность кнопки добавления
7. Проверить свайп-действия

**Критерий готовности:** Все UI-тесты проходят

### Приоритет 5: Проверка качества кода

#### Шаг 5.1: Запуск линтеров
**Задачи:**
1. Запустить `./gradlew ktlintCheck`
2. Запустить `./gradlew detekt`
3. Исправить все найденные ошибки

**Критерий готовности:** ktlint и detekt не выдают ошибок

---

## Резюме текущего статуса

### Выполнено ✅
- ✅ Базовая структура MainScreen создана
- ✅ MainScreenViewModel реализован с состояниями
- ✅ ListItemView для отображения записей реализован
- ✅ Empty states реализованы (пустой список, пустой поиск)
- ✅ SortMenu с выбором порядка сортировки реализован
- ✅ Базовая навигация настроена
- ✅ Локализация строк реализована
- ✅ Интеграционные тесты для Repository работают

### Не выполнено ❌
- ❌ Исправление ошибки сборки Gradle (критично)
- ❌ Добавление зависимости material-icons-extended
- ❌ Реализация функции itemsListContent() для списка со свайпами
- ❌ Реализация UI для поиска
- ❌ Полная интеграция навигации с DetailScreen и CreateEditScreen
- ❌ Unit-тесты для ViewModel
- ❌ UI-тесты для MainScreen

### Частично выполнено ⚠️
- ⚠️ Сортировка (UI есть, но нужна полная интеграция)
- ⚠️ Preview компонентов (частичная реализация)

---

## Порядок действий для завершения этапа

1. **Исправить ошибку сборки Gradle** (Шаг 1.1) - КРИТИЧНО
2. **Добавить зависимость material-icons-extended** (Шаг 1.2)
3. **Реализовать itemsListContent()** (Шаг 1.3)
4. **Добавить UI для поиска** (Шаг 2.1)
5. **Полная интеграция навигации** (Шаги 3.1, 3.2, 3.3)
6. **Написать unit-тесты** (Шаг 4.1)
7. **Написать UI-тесты** (Шаг 4.2)
8. **Запустить линтеры и исправить ошибки** (Шаг 5.1)

**Примечание:** До исправления ошибки сборки Gradle (Шаг 1.1) невозможно проверить работу большинства компонентов.
