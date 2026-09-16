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
- переход на страницу GitHub;
- ручная проверка обновлений.

## Текущее поведение

- Кнопка **"Тема и иконка"** ведет на `Screen.ThemeIcon`.
- Кнопка **"Данные приложения"** ведет на `Screen.AppData`.
- Кнопки **"Оценить приложение"** и **"Поделиться приложением"** показываются только на RuStore-сборке (`BuildConfig.RUSTORE_FEATURES`).
- Кнопка **"Проверить обновления"** показывается только на github-сборке (`!BuildConfig.RUSTORE_FEATURES`); результат проверки отображается в `UpdateCheckDialog`, состояние приходит из `MoreScreenViewModel` (`CheckForAppUpdateUseCase`).
- Внизу экрана показывается версия из `BuildConfig.VERSION_NAME`.
- Для внешних действий используются `Intent` с обработкой `ActivityNotFoundException`.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/more/MoreScreen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/more/UpdateCheckDialog.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/MoreScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/CheckForAppUpdateUseCase.kt`
- `app/src/main/java/com/dayscounter/navigation/Screen.kt`
- `app/src/main/java/com/dayscounter/ui/screens/common/RootScreenComponents.kt`
- `app/src/main/java/com/dayscounter/util/AppConstants.kt`

## Тестирование

- UI-покрытие экрана: `app/src/androidTest/java/com/dayscounter/ui/screens/more/MoreScreenTest.kt`.
- Unit-тесты: `app/src/test/java/com/dayscounter/ui/viewmodel/MoreScreenViewModelTest.kt`.
- Дополнительные тесты для внешних `Intent`-действий можно расширять отдельно.
