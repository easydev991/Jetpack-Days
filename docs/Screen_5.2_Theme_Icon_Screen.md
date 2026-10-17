# Экран 5.2: Theme and Icon Screen (Тема и иконка)

## Статус

**Реализован и доступен из More Screen**.

## Назначение

Экран позволяет пользователю:

- выбрать тему приложения (`LIGHT`, `DARK`, `SYSTEM`);
- включить/выключить динамические цвета (если поддерживаются устройством);
- выбрать иконку приложения.

Все настройки сохраняются и восстанавливаются при следующем запуске.

## Текущее поведение

- Навигация назад ведет в `More Screen`.
- Состояние экрана приходит из `ThemeIconViewModel`.
- Выбор темы и иконки применяется через ViewModel и сохраняется в DataStore/менеджере иконок.
- Секция динамических цветов показывается только на поддерживаемых версиях Android.
- Сетка иконок адаптивна: колонки считаются от ширины окна (`iconGridCells`), остаток ширины распределяется по ячейкам, размер превью иконки вынесен в ресурс `icon_size_app_preview` (64dp). Раньше при фиксированных 64dp + 32dp на 360dp экране влезало 3 колонки и справа оставалось пустое место.

## Зависимости

- `More Screen` уже реализован, переход на этот экран активен.
- Маршрут `Screen.ThemeIcon` зарегистрирован в навигации.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/themeicon/ThemeIconScreen.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/ThemeIconViewModel.kt`
- `app/src/main/java/com/dayscounter/navigation/Screen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`

## Тестирование

- UI-тесты: `app/src/androidTest/java/com/dayscounter/ui/screens/themeicon/ThemeIconScreenTest.kt`
- Unit-тесты: `app/src/test/java/com/dayscounter/ui/viewmodel/ThemeIconViewModelTest.kt`, `app/src/test/java/com/dayscounter/ui/screens/themeicon/IconGridCellsTest.kt` (расчёт сетки, включая инвариант «ряд не переполняет ширину»)
