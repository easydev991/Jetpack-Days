# Экран 5.1: More Screen (Ещё)

## Статус

**Реализован и используется в приложении**.

## Назначение

Экран с дополнительными действиями и переходами в разделы настроек:

- переход в экран темы и иконки;
- переход в экран данных приложения;
- отправка отзыва;
- переход к оценке приложения;
- шаринг приложения;
- переход на страницу GitHub.

## Текущее поведение

- Кнопка **"Тема и иконка"** ведет на `Screen.ThemeIcon`.
- Кнопка **"Данные приложения"** ведет на `Screen.AppData`.
- Внизу экрана показывается версия из `BuildConfig.VERSION_NAME`.
- Для внешних действий используются `Intent` с обработкой `ActivityNotFoundException`.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/more/MoreScreen.kt`
- `app/src/main/java/com/dayscounter/navigation/Screen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
- `app/src/main/java/com/dayscounter/util/AppConstants.kt`

## Тестирование

- UI-покрытие экрана: `app/src/androidTest/java/com/dayscounter/ui/screens/more/MoreScreenTest.kt`.
- Дополнительные тесты для внешних `Intent`-действий можно расширять отдельно.
