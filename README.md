# Счётчик дней

<!-- BEGIN_VERSIONS -->
[<img alt="Kotlin Version" src="https://img.shields.io/badge/Kotlin_Version-2.4.20-purple">](https://kotlinlang.org/)
[<img alt="Android SDK" src="https://img.shields.io/badge/Android_SDK-37-green">](https://developer.android.com/)
[<img alt="Min SDK" src="https://img.shields.io/badge/Min_SDK-26-informational">](https://developer.android.com/)
[<img alt="Gradle" src="https://img.shields.io/badge/Gradle-9.7.1-blue">](https://gradle.org/)
[<img alt="AGP" src="https://img.shields.io/badge/AGP-9.4.1-green">](https://developer.android.com/tools/releases/gradle-plugin)
<!-- END_VERSIONS -->

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
| <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/1-demoList_1786560117567.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/2-chooseDate_1786560123117.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/3-chooseDisplayOption_1786560123895.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/4-beforeSave_1786560125655.png" alt=""> | <img src="./fastlane/metadata/android/ru-RU/images/phoneScreenshots/5-sortByDate_1786560128389.png" alt=""> |

### Релизный процесс

Проект собирается в двух `productFlavors` — `rustore` (RuStore, основной канал) и `github` (GitHub Release, альтернативный). Команды `make rustore` (RuStore — все 4 шага одной командой) или `make rustore-draft` → `make rustore-commit VID=<vid>` (черновик + ручная отправка на модерацию); `make apk FLAVOR=github` для APK.

Инструкция по созданию сборки и управлению версиями: [deployment.md](docs/deployment.md)
