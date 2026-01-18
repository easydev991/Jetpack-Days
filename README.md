# Счётчик дней

[<img alt="Kotlin Version" src="https://img.shields.io/badge/Kotlin_Version-2.3.0-purple">](https://kotlinlang.org/)
[<img alt="Android SDK" src="https://img.shields.io/badge/Android_SDK-35-green">](https://developer.android.com/)
[<img alt="Min SDK" src="https://img.shields.io/badge/Min_SDK-26-informational">](https://developer.android.com/)
[<img alt="Gradle" src="https://img.shields.io/badge/Gradle-9.2.1-blue">](https://gradle.org/)
[<img alt="AGP" src="https://img.shields.io/badge/AGP-9.0.0-green">](https://developer.android.com/tools/releases/gradle-plugin)
[![GitMCP](https://img.shields.io/endpoint?url=https://gitmcp.io/badge/easydev991/Jetpack-Days)](https://gitmcp.io/easydev991/Jetpack-Days)

- Идея приложения в том, чтобы было удобно запоминать события в указанные даты, а потом легко проверить, сколько прошло дней с момента события
- Это Android-версия моего пет-проекта "Счётчик дней", которая повторяет функциональность [iOS-версии](https://github.com/easydev991/SwiftUI-Days) для обеспечения единообразия пользовательского опыта
- Приложение работает полностью офлайн без сетевых функций
- Приложение автоматически адаптирует размер шрифтов к системным настройкам

## Что можно делать в приложении

1. Создавать/изменять/сортировать/удалять записи о событиях
2. Посмотреть, сколько дней назад произошло записанное событие
3. Выбирать опции отображения (только дни / дни и месяцы / годы, месяцы и дни)
4. Добавлять цветовые метки к записям
5. Искать и сортировать записи по названию и описанию

## Начало работы

1. Клонируйте репозиторий
2. В терминале перейдите в папку с проектом
3. Все команды доступны через `Makefile`:

- Ознакомиться с доступными командами можно, выполнив команду:

```shell
make help
```

## Скриншоты

| Список записей | Создание новой записи | Выбор опции отображения | Перед сохранением | Сортировка на главном экране |
| --- | --- | --- | --- | --- |
| <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/1-demoList_1768663402836.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/2-chooseDate_1768663408046.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/3-chooseDisplayOption_1768663408770.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/4-beforeSave_1768663410399.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/5-sortByDate_1768663412968.png" alt=""> |

### Релизный процесс

Инструкция по созданию сборки и управлению версиями: [deployment.md](docs/deployment.md)
