# Экран 5.3: App Data Screen (Управление данными приложения)

## Статус

**Реализован и доступен из More Screen**.

## Назначение

Экран для операций с данными приложения:

- создание резервной копии;
- восстановление из резервной копии;
- удаление всех данных с подтверждением.

## Текущее поведение UI

- Если есть записи: показываются кнопки **Создать резервную копию**, **Восстановить из резервной копии**, **Удалить все данные**.
- Если записей нет: доступна только кнопка **Восстановить из резервной копии**.
- Есть кнопка "Назад" в `TopAppBar`.
- Для результатов операций используются `Toast`.

## Форматы backup

### Экспорт

- Экспортируется **Android-формат** с оберткой `BackupWrapper` и полем `format: "android"`.
- `colorTag` сохраняется как hex-строка `#RRGGBB`.

### Импорт

Поддерживаются:

- Android wrapper (`BackupWrapper`);
- iOS wrapper (`IosBackupWrapper`) с конвертацией `timestamp` и `colorTag`;
- legacy-формат (массив `BackupItem` без wrapper).

## Навигация

- Переход из `MoreScreen` по кнопке "Данные приложения" на `Screen.AppData`.

## Ключевые файлы

- `app/src/main/java/com/dayscounter/ui/screens/appdata/AppDataScreen.kt`
- `app/src/main/java/com/dayscounter/ui/viewmodel/AppDataScreenViewModel.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/ExportBackupUseCase.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/ImportBackupUseCase.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/BackupWrapper.kt`
- `app/src/main/java/com/dayscounter/domain/usecase/IosBackupWrapper.kt`

## Примечание

Функциональность ориентирована на офлайн-режим и совместимость импорта с iOS-резервными копиями.
