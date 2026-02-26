# Документация: Совместимость резервных копий iOS ↔ Android

## Обзор

Android-приложение полностью совместимо с резервными копиями iOS-версии. При импорте автоматически определяется формат (iOS или Android) и выполняется конвертация данных.

---

## Импорт резервных копий

### Определение формата

При импорте автоматически определяется тип резервной копии:

| Поле            | iOS формат                              | Android формат            |
|-----------------|-----------------------------------------|---------------------------|
| `timestamp`     | Секунды с 1970-01-01                    | Миллисекунды с 1970-01-01 |
| `colorTag`      | Base64 NSKeyedArchiver (bplist00)       | Hex-строка (#RRGGBB)      |
| `displayOption` | camelCase (day, monthDay, yearMonthDay) | camelCase (идентично)     |

### Логика определения timestamp

Если значение timestamp < 10¹² (примерно до 2001 года в мс), считается что это секунды (iOS) и умножается на 1000 для конвертации в миллисекунды.

### Логика определения colorTag

1. Если начинается с `#` → hex-формат (Android)
2. Если Base64 декодируется в `bplist00` → NSKeyedArchiver (iOS)
3. Иначе → некорректный формат, colorTag = null

### Обработка colorTag

**iOS → Android:**

- Base64 NSKeyedArchiver парсится для извлечения RGBA компонентов UIColor
- Конвертируется в ARGB Int для хранения в Android
- Поддерживаются любые цвета (не ограничено палитрой Android)

**Android → iOS:**

- ARGB Int конвертируется в Base64 NSKeyedArchiver
- Генерируется валидный bplist00 формат для UIColor

### Обнаружение дубликатов

При импорте проверяется наличие события с идентичными:

- `title` (название)
- `timestamp` (дата)
- `displayOption` (формат отображения)

Дубликаты пропускаются, уникальные события добавляются.

---

## Экспорт резервных копий

### Форматы экспорта

Приложение поддерживает два формата экспорта:

1. **Android формат** — для импорта в Android-приложение
   - `timestamp`: миллисекунды
   - `colorTag`: hex-строка (#RRGGBB)

2. **iOS формат** — для импорта в iOS-приложение
   - `timestamp`: секунды
   - `colorTag`: Base64 NSKeyedArchiver

### Совместимость с iOS

Резервные копии, созданные в Android-приложении в iOS формате, полностью совместимы с iOS-версией:

- JSON структура идентична
- timestamp конвертируется в секунды
- colorTag конвертируется в NSKeyedArchiver с UIColor
- displayOption использует те же значения enum

---

## Структура JSON резервной копии

```json
[
  {
    "title": "Название события",
    "details": "Описание",
    "timestamp": 1234567890,
    "colorTag": "#FF0000",
    "displayOption": "day"
  }
]
```

### Поля

| Поле            | Тип     | Описание                               |
|-----------------|---------|----------------------------------------|
| `title`         | String  | Название события                       |
| `details`       | String? | Описание (опционально)                 |
| `timestamp`     | Long    | Дата в секундах (iOS) или мс (Android) |
| `colorTag`      | String? | Цвет: hex или Base64 (опционально)     |
| `displayOption` | String  | `day`, `monthDay` или `yearMonthDay`   |

---

## Файлы реализации

| Файл                        | Назначение                                             |
|-----------------------------|--------------------------------------------------------|
| `BackupItem.kt`             | DTO для резервных копий, конвертация Item ↔ BackupItem |
| `IosBackupItem.kt`          | DTO для iOS-формата, конвертация timestamp и colorTag  |
| `NsKeyedArchiverParser.kt`  | Парсер Base64 NSKeyedArchiver → UIColor компоненты     |
| `NsKeyedArchiverBuilder.kt` | Генератор UIColor → Base64 NSKeyedArchiver             |
| `ExportBackupUseCase.kt`    | Логика экспорта в Android/iOS формат                   |
| `ImportBackupUseCase.kt`    | Логика импорта с автоопределением формата              |

---

## Тестирование

- Unit-тесты для конвертации colorTag (hex и Base64)
- Unit-тесты для конвертации timestamp
- Интеграционные тесты с реальным iOS-бекапом (`ios-backup-sample.json`)
- Тесты обнаружения дубликатов
- Тесты невалидных данных

---

## Технический долг

**Detekt предупреждения** (не блокируют сборку):

- `BackupItem.kt`: ReturnCount в `parseColorTag()`
