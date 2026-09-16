# Резервные копии в Android

## Обзор

Приложение поддерживает:
- **Импорт**: Android и iOS форматы (автоопределение)
- **Экспорт**: только Android-формат

При импорте iOS-файлов автоматически конвертируются timestamp (секунды с 2001-01-01 → миллисекунды с 1970-01-01) и colorTag (Base64 NSKeyedArchiver → hex #RRGGBB).

---

## Импорт резервных копий

### Порядок парсинга файлов

`ImportBackupUseCase` пытается распарсить файл в следующем порядке:

1. **BackupWrapper** (Android формат с полем `format: "android"`)
2. **IosBackupWrapper** (iOS формат с полем `format: "ios"`)
3. **List<BackupItem>** (старый Android-формат без обёртки — fallback)

Порядок основан на типе `timestamp`: у Android он `Long`, у iOS — `Double`,
поэтому iOS-файл не декодируется как `BackupWrapper` и автоматически проваливается на шаг 2.

**Ограничение:** старые iOS-бэкапы без обёртки (массив с дробным `timestamp`) не
распознаются шагом 3 — Use Case вернёт ошибку парсинга. В тестах такие файлы
парсятся напрямую через `List<IosBackupItem>` (см. `BackupImportRealFilesTest`).

JSON при импорте читается с `ignoreUnknownKeys = true` — неизвестные поля игнорируются.

### Обработка некорректных записей

- Невалидный `displayOption` (не входит в `day`/`monthDay`/`yearMonthDay`) → запись пропускается (`toItem()`/`toBackupItem()` возвращают `null`).
- Нераспознанный `colorTag` → запись импортируется без цвета (`colorTag = null`).

### Обработка ошибок

`ImportBackupUseCase` маппит исключения в `BackupException`:
`FileNotFoundException`, `IOException`, `SerializationException`, `SQLException`.

### Обнаружение дубликатов

При импорте проверяется наличие события с идентичными:

- `title` (название)
- `details` (описание)
- `timestamp` (дата)
- `displayOption` (формат отображения)

Дубликаты пропускаются, уникальные события добавляются.

---

## Экспорт резервных копий

### Android-формат

```json
{
  "format": "android",
  "items": [
    {
      "title": "Название события",
      "details": "Описание",
      "timestamp": 699417600000,
      "colorTag": "#FF5722",
      "displayOption": "day"
    }
  ]
}
```

### Поля

| Поле            | Тип     | Описание                                   |
|-----------------|---------|-------------------------------------------|
| `title`         | String  | Название события                          |
| `details`       | String? | Описание (опционально)                    |
| `timestamp`     | Long    | Миллисекунды с 1970-01-01                 |
| `colorTag`      | String? | Hex-цвет #RRGGBB (опционально)            |
| `displayOption` | String  | `day`, `monthDay` или `yearMonthDay`      |

---

## Файлы реализации

| Файл                        | Назначение                                      |
|-----------------------------|-------------------------------------------------|
| `BackupItem.kt`             | DTO для Android-формата, Item ↔ BackupItem      |
| `BackupWrapper.kt`          | Обёртка с полем format                          |
| `BackupFormat.kt`           | Enum формата (`android`/`ios`)                  |
| `BackupFormatSerializer.kt` | Сериализация enum в lowercase-строки            |
| `ExportBackupUseCase.kt`    | Экспорт в Android-формат                        |
| `ImportBackupUseCase.kt`    | Импорт с автоопределением формата               |

iOS-совместимость (импорт):
- `IosBackupItem.kt` — DTO для iOS-формата
- `IosBackupWrapper.kt` — обёртка для iOS
- `NsKeyedArchiverParser.kt` — парсер colorTag из iOS

---

## Тестирование

### Unit-тесты

- `BackupItemTest.kt` — конвертация Item ↔ BackupItem
- `IosBackupItemTest.kt` — конвертация iOS timestamp/colorTag
- `NsKeyedArchiverParserTest.kt` — парсинг iOS colorTag
- `BackupWrapperTest.kt` — сериализация wrapper'ов
- `IosBackupIntegrationTest.kt` — интеграция iOS → Android конвертации

### Интеграционные тесты

Файлы в `app/src/test/resources/`:
- `new-backup-sample.json` — Android с wrapper
- `new-ios-backup.json` — iOS с wrapper
- `old-backup-sample.json` — старый Android (массив)
- `old-ios-backup-sample.json` — старый iOS (массив)

Тесты в `BackupImportRealFilesTest.kt` проверяют парсинг всех форматов.
