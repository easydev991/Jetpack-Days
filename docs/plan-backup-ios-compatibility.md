# План: Совместимость резервных копий iOS ↔ Android

## Анализ текущего состояния

### Формат BackupItem

| Поле | iOS | Android | Совместимость |
|------|-----|---------|---------------|
| `title` | String | String | ✅ Полная |
| `details` | String | String? | ✅ Полная |
| `timestamp` | Date (секунды, Double) | Long (миллисекунды) | ❌ НЕСОВМЕСТИМО |
| `colorTag` | Data? (Base64 NSKeyedArchiver) | String? (hex #RRGGBB) | ❌ НЕСОВМЕСТИМО |
| `displayOption` | String? (rawValue) | String | ✅ Полная |

### Проблема 1: colorTag

**iOS** кодирует цвет через `NSKeyedArchiver` (бинарный plist `bplist00`):

```json
"colorTag": "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05TS2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9yQ29tcG9uZW50Q291bnRWVUlHcmVlblZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlNDb2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNsYXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAIABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDsAPcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg=="
```

**Характеристики NSKeyedArchiver для UIColor (iOS):**
- Размер: **~410 байт** (Base64 строка ~560 символов)
- Формат: бинарный plist (`bplist00`)
- Класс: `UIColor` (не NSColor)
- Структура:
  - `UIColorComponentCount` = 4 (RGBA)
  - `UIGreen`, `UIBlue`, `UIAlpha`, `UIRed` - float values
  - `NSColorSpace` - цветовое пространство (например, "M1 0.228 0.187")

**Android** ожидает hex-строку:

```json
"colorTag": "#FF0000"
```

**Вывод:** Парсинг NSKeyedArchiver для извлечения цвета технически возможен. Размер данных небольшой (~410 байт), нужно распарсить bplist и извлечь RGBA компоненты.

### Проблема 2: timestamp

**iOS** использует `Date` который кодируется как секунды (с плавающей точкой) с 1970-01-01:

```json
"timestamp": 699417600.0
```

**Android** ожидает миллисекунды (Long):

```json
"timestamp": 699417600000
```

`kotlinx.serialization` **не сможет** автоматически конвертировать Double в Long - это вызовет ошибку парсинга!

---

## Этап 1: Создание отдельной модели для парсинга iOS-бекапа (Domain Layer)

- [ ] Создать `IosBackupItem.kt` - модель для парсинга JSON из iOS
  - `timestamp: Double` - секунды с 1970 (как в iOS)
  - `colorTag: String?` - Base64-строка (как в iOS)
  - Остальные поля аналогичны BackupItem
- [ ] Добавить функцию конвертации `IosBackupItem.toBackupItem(): BackupItem?`
  - timestamp: умножить на 1000 (секунды → миллисекунды)
  - colorTag: распарсить Base64 NSKeyedArchiver или вернуть null

## Этап 2: Реализация парсинга colorTag из iOS (Domain Layer)

**Выбранный подход:** Парсить NSKeyedArchiver и конвертировать в hex

- [ ] Реализовать парсер бинарного plist формата (bplist00)
  - Распарсить заголовок и объекты
  - Найти UIColor объект в дереве
  - Извлечь RGBA компоненты (float 0.0-1.0)
- [ ] Конвертировать RGBA в hex-строку (#RRGGBB)
  - `red * 255`, `green * 255`, `blue * 255`
  - Формат: `String.format("#%02X%02X%02X", r, g, b)`
- [ ] Добавить тесты с реальными примерами из iOS
- [ ] Обработать edge cases (nil color, некорректные данные → вернуть null)

**Оценка сложности:**
- Бинарный plist парсер: ~300-500 строк кода (упрощённая версия для UIColor)
- Данные небольшие (~410 байт), без ICC профиля
- Тестирование: есть реальный пример из iOS
- Риск: низкий (формат стабилен)

## Этап 3: Обновление ImportBackupUseCase (Domain Layer)

- [ ] Добавить определение формата бекапа (iOS или Android)
  - Если `timestamp` - это Double/Float → iOS формат
  - Если `timestamp` - это Long → Android формат
- [ ] Использовать соответствующую модель для парсинга
- [ ] Добавить тесты для обоих форматов

## Этап 4: Тестирование (TDD)

- [ ] Написать тесты для `IosBackupItem`
  - Парсинг timestamp (Double → Long миллисекунды)
  - Парсинг displayOption
  - Конвертация в BackupItem
- [ ] Создать тестовый JSON с примером из iOS
- [ ] Написать интеграционный тест импорта iOS-бекапа

---

## Технические детали

### Формат JSON из iOS (реальный пример)

```json
{
  "title": "Оффер",
  "details": "Компания, 999 гросс",
  "timestamp": 769363669.529918,
  "colorTag": "YnBsaXN0MDDUAQIDBAUGBwpYJHZlcnNpb25ZJGFyY2hpdmVyVCR0b3BYJG9iamVjdHMSAAGGoF8QD05TS2V5ZWRBcmNoaXZlctEICVRyb290gAGjCwwdVSRudWxs2A0ODxAREhMUFRYXGBkaGxxfEBVVSUNvbG9yQ29tcG9uZW50Q291bnRWVUlHcmVlblZVSUJsdWVXVUlBbHBoYVVOU1JHQlYkY2xhc3NVVUlSZWRcTlNDb2xvclNwYWNlEAQiPmkSDiI+Py6wIj+AAABNMSAwLjIyOCAwLjE4N4ACIj+ADl8QAtMeHyAhIiRaJGNsYXNzbmFtZVgkY2xhc3Nlc1skY2xhc3NoaW50c1dVSUNvbG9yoiEjWE5TT2JqZWN0oSVXTlNDb2xvcgAIABEAGgAkACkAMgA3AEkATABRAFMAVwBdAG4AhgCOAJUAnQCjAKoAsAC9AL8AxADJAM4A3ADeAOMA5QDsAPcBAAEMARQBFwEgASIAAAAAAAACAQAAAAAAAAAmAAAAAAAAAAAAAAAAAAABKg==",
  "displayOption": "day"
}
```

**Примечание:** `colorTag` в iOS - это Base64-закодированный бинарный plist (~410 байт), содержащий UIColor без ICC профиля.

### Формат JSON из Android (текущий)

```json
[
  {
    "title": "Название события",
    "details": "Описание",
    "timestamp": 699417600000,
    "colorTag": "#FF0000",
    "displayOption": "day"
  }
]
```

### Проблема с обнаружением формата

JSON не имеет типа данных - всё это строки/числа. Но:
- iOS timestamp: Double с десятичной точкой (например, `699417600.0`)
- Android timestamp: Long без десятичной точки (например, `699417600000`)

Можно использовать `JsonElement` для определения формата во время парсинга.

### Формат NSKeyedArchiver для UIColor (справочно)

NSKeyedArchiver создаёт бинарный plist (bplist00) следующей структуры:

```
00000000: 62 70 6c 69 73 74 30 30  (magic: "bplist00")
...
```

**Структура архива (реальный пример из iOS):**

```
bplist00
├── $version: 100000
├── $archiver: "NSKeyedArchiver"
├── $top: { root = object(2) }
└── $objects: [
      null,
      "NSKeyedArchiver",
      {                         // UIColor object
        UIColorComponentCount: 4,
        UIGreen: float,         // 0.0-1.0
        UIBlue: float,          // 0.0-1.0
        UIAlpha: float,         // 0.0-1.0
        NSRGB: float array,     // RGB components
        $class: UIColor,
        UIRed: float,           // 0.0-1.0
        NSColorSpace: "M1 0.228 0.187"
      },
      "UIColor",
      ...
    ]
```

**Ключевые поля для извлечения цвета:**
- `UIRed`, `UIGreen`, `UIBlue`, `UIAlpha` - float values (0.0-1.0)
- `NSRGB` - альтернативный источник RGB
- `NSColorSpace` - цветовое пространство

**Размер архива:** ~410 байт на один цвет (без ICC профиля)

**Конвертация в hex:**

```kotlin
val r = (uiRed * 255).toInt().coerceIn(0, 255)
val g = (uiGreen * 255).toInt().coerceIn(0, 255)
val b = (uiBlue * 255).toInt().coerceIn(0, 255)
val hex = "#%02X%02X%02X".format(r, g, b)
```

**Парсинг в Android:**
- Сложность: средняя (бинарный формат plist, но без ICC профиля)
- Размер данных небольшой (~410 байт)
- Преимущество: сохранение цвета при импорте из iOS

---

## Критерии завершения

- [ ] Android успешно импортирует JSON-бекап из iOS без ошибок парсинга
- [ ] title, details, displayOption импортируются корректно
- [ ] timestamp конвертируется из секунд в миллисекунды
- [ ] colorTag корректно конвертируется из NSKeyedArchiver в hex
- [ ] Дубликаты корректно обнаруживаются
- [ ] Тесты проходят для обоих форматов (iOS и Android)
- [ ] Android-бекапы продолжают работать без изменений
