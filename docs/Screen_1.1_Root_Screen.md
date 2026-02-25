# Экран 1.1: Root Screen (Корневой экран с TabBar)

**Статус: Полностью реализован** ✅

## Обзор

Root Screen является основным экраном приложения с нижней навигацией. Он обеспечивает переключение между основными разделами приложения через TabBar и управляет навигацией ко всем экранам приложения.

## Назначение

Основной экран приложения с нижней навигацией, обеспечивающий переключение между вкладками "События" и "Ещё", а также навигацию к детальным экранам, экранам создания/редактирования, теме и иконке приложения, и данным приложения.

## Компоненты

- **Bottom Navigation Bar** — нижняя навигация с двумя вкладками ✅
- **Контейнер для содержимого** — отображение содержимого выбранной вкладки ✅
- **Управление состоянием** — управление активной вкладкой через ViewModel ✅
- **Система маршрутизации** — NavHost с маршрутами для всех экранов приложения ✅

## Вкладки

- **Вкладка "События"** — главный список записей (Main Screen) ✅
- **Вкладка "Ещё"** — дополнительные функции и настройки (More Screen) ✅

## Доступные маршруты

- **Events** — главный экран событий ✅
- **More** — экран дополнительных функций и настроек ✅
- **ItemDetail** — детальный экран события (с параметром `itemId`) ✅
- **CreateItem** — экран создания нового события ✅
- **EditItem** — экран редактирования события (с параметром `itemId`) ✅
- **ThemeIcon** — экран темы и иконки приложения ✅
- **AppData** — экран данных приложения ✅

## Зависимости от других этапов

Нет зависимостей — это базовый экран навигации.

---

## Реализация

Этап полностью завершен ✅

### Реализованные компоненты

- `navigation/Screen.kt` — sealed class с маршрутами
- `ui/viewmodel/RootScreenViewModel.kt` — ViewModel для управления вкладками
- `ui/state/RootScreenState.kt` — UI State
- `ui/screens/RootScreen.kt` — RootScreen composable
- `ui/screens/root/RootScreenComponents.kt` — компоненты навигации

### Тесты

Все тесты реализованы и активны (9 тестов):

- `RootScreenViewModelTest.kt` — unit-тесты ViewModel (4 теста)
- `RootScreenStateTest.kt` — тесты UI State (3 теста)
- `ScreenTest.kt` — тесты sealed class Screen (2 теста)

---

## Блокируемые этапы

После завершения этого экрана можно приступать к:

- ✅ Экран 2.1: Главный экран (реализован)
- ✅ Экран 3.1: Item Screen / Детальный экран (реализован)
- ✅ Экран 4.1: Create/Edit Item Screen (реализован)
- ✅ Экран 5.1: More Screen (реализован)
- ✅ Экран темы и иконки приложения (реализован)
- ✅ Экран данных приложения (реализован)

---

## Примечания

- **Material Design 3**: Современные компоненты для навигации
- **Состояние**: Синхронизация вкладок с навигационным стеком через ViewModel
- **Тестирование**: Все тесты активны и покрывают все компоненты
- **Тема**: Поддержка светлого и темного режимов
- **Локализация**: Использованы ресурсы из `Localization_Plan.md` (`events`, `more`, `app_theme_and_icon`, `app_data`) через `titleResId`
- **Доп. маршруты**: `ItemDetail`, `CreateItem`, `EditItem`, `ThemeIcon`, `AppData` с параметрами `itemId` где необходимо
- **Видимость навигации**: Скрыта на вторичных экранах, отображается только на главных вкладках (Events, More)
- **Интеграция**: Все экраны приложения интегрированы через систему навигации Root Screen

---

## Текущее состояние реализации

### Выполнено (100%)

- ✅ Навигация, компоненты и логика Root Screen
- ✅ Интеграция всех экранов приложения
- ✅ Все тесты активны

### Не выполнено

Нет — весь функционал полностью реализован

---

## Файловая структура

```text
ui/screens/
├── RootScreen.kt                     # Главный экран с TabBar
└── root/
    └── RootScreenComponents.kt        # Компоненты навигации
        ├── navigationBarContent()     # Навигационная панель
        ├── navHostContent()           # Маршрутизация (NavHost)
        ├── mainScreenDestination()    # Маршрут для Events
        ├── detailScreenDestination()  # Маршрут для ItemDetail
        ├── createEditScreenDestination() # Маршруты для CreateItem и EditItem
        ├── moreScreenDestination()    # Маршрут для More
        ├── themeIconScreenDestination() # Маршрут для ThemeIcon
        ├── appDataScreenDestination() # Маршрут для AppData
        ├── eventsScreenContent()      # Контент экрана событий
        └── updateTabBasedOnRoute()    # Обновление вкладки на основе маршрута

navigation/
└── Screen.kt                         # Sealed class для навигации (включая Events, More, ThemeIcon, AppData, ItemDetail, CreateItem, EditItem)

ui/viewmodel/
├── RootScreenViewModel.kt             # ViewModel для управления вкладками
└── ui/state/
    └── RootScreenState.kt            # UI State

test/
└── ...
    ├── ui/viewmodel/RootScreenViewModelTest.kt     # Тесты ViewModel (4 теста)
    ├── ui/viewmodel/RootScreenViewModelIntegrationTest.kt  # Интеграционные тесты
    ├── ui/state/RootScreenStateTest.kt         # Тесты UI State (3 теста)
    └── navigation/
        └── ScreenTest.kt             # Тесты sealed class Screen (2 теста)
```

---

## История изменений

- 2025-01: Базовая реализация
- 2026-01: Добавление всех маршрутов и интеграция экранов
- 2026-01-11: Полная реализация (100%)
