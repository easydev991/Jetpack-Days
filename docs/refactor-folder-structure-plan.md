# План рефакторинга структуры папок JetpackDays

## Прогресс: 17/17 этапов (100%) ✅

- [x] Этап 17: Удаление некорректных интеграционных тестов ViewModel
- [x] Этап 16: Исправление Android тестов

- [x] Этап 1: Подготовка и анализ
- [x] Этап 2: Миграция ViewModel в ui/viewmodel
- [x] Этап 3: Миграция экранов в ui/screens
- [x] Этап 4: Миграция UI компонентов в ui/ds
- [x] Этап 5: UI State проверка
- [x] Этап 6: Миграция formatter в data/provider
- [x] Этап 7: DI модули (оставлены на месте)
- [x] Этап 8: Analytics/crash (оставлены на месте)
- [x] Этап 9: Mapper → database/
- [x] Этап 10: Converters → database/
- [x] Этап 11: ui/utils (ThemeUtils остается в util/)
- [x] Этап 12: ui/model (не нужно - Params рядом с экранами)
- [x] Этап 13: Тесты - unit тесты проходят
- [x] Этап 14: Документация - QWEN.md обновлен
- [x] Этап 15: Финальная проверка - компиляция OK, линтеры OK

## Цель

Привести структуру папок проекта JetpackDays к аналогичной структуре Jetpack-WorkoutApp для унификации архитектуры и улучшения организации кода.

## Текущая структура (после рефакторинга)

```
com/dayscounter/
├── analytics/, crash/
├── data/database/{dao,entity}/, provider/, preferences/, repository/
│   ├── DisplayOptionConverter.kt, ItemMapper.kt (бывшие converters/, mapper/)
│   └── DaysDatabase.kt
├── di/
├── domain/{exception,model,repository,usecase}/
├── navigation/
├── ui/ds/, screens/, state/, theme/, viewmodel/
└── util/
```

---

## Выполненные этапы

### Этап 1-5: Базовая миграция ✅

- Создана ветка `refactor/folder-structure`
- 7 ViewModel + 8 тестов → `ui/viewmodel/`
- 28 файлов экранов → `ui/screens/` с подпапками (appdata, common, createedit, detail, events, more, root, themeicon)
- 4 UI компонента → `ui/ds/` (DaysCountText, DaysCountTextStyle, ListItemParams, ListItemView)
- UI State файлы проверены и на месте

### Этап 6: Formatter → data/provider ✅

- 6 файлов перемещены: DaysFormatter, DaysFormatterImpl, ResourceIds, ResourceProvider, ResourceProviderImpl, StubResourceProvider
- Тест перемещен: DaysFormatterImplTest
- Удалена директория `data/formatter/`

### Этап 7-8: DI, analytics, crash ✅

- DI модули оставлены на месте (AppModule, FormatterModule)
- analytics/, crash/ оставлены на месте

### Этап 9-10: Mapper и Converters → database/ ✅

- ItemMapper.kt + тест перемещены в `database/`
- DisplayOptionConverter.kt + тест перемещены в `database/`
- Удалены директории `database/mapper/` и `database/converters/`

### Этап 11-12: ui/utils и ui/model ✅

- ThemeUtils остается в `util/` (используется в domain слое)
- ui/model не нужен - Params файлы уже рядом с экранами

---

## Этап 13: Обновление тестов ✅

- Unit тесты: `make test` - проходят
- Android тесты: пропущены (требуют подключенное устройство)

## Этап 14: Обновление документации ✅

- QWEN.md: раздел "Структура проекта" обновлен
- README.md: не требует изменений (нет упоминаний структуры)

## Этап 15: Финальная проверка ✅

- Компиляция: `./gradlew compileDebugKotlin compileReleaseKotlin` - OK
- Тесты: `./gradlew test` - OK
- Линтеры: ktlint OK, detekt OK (исправлен UnusedImport в DaysDatabase.kt)

---

## Результат

Рефакторинг завершен успешно. Структура проекта приведена к целевому виду.

**Коммиты:**
- `3ecfcba` - Этапы 1-2
- `bafe1d8` - Этап 3
- `33aacfa` - Этапы 4-5
- `421cb31` - Этап 6
- `24cd7f1` - Этапы 7-12
- `232157f` - Сжатие плана
- Финальный коммит - Этапы 13-15

---

## Этап 16: Исправление Android тестов ✅

### Проблемы найдены при `make android-test`

1. **MoreScreenTest.moreScreen_whenDisplayed_thenShowsAppVersion** - FAILED → ИСПРАВЛЕНО
   - Причина: тест использовал захардкоженную версию "1.0" вместо `BuildConfig.VERSION_NAME`
   - Решение: использовать реальную версию из BuildConfig

2. **CreateEditScreenViewModelIntegrationTest** - 11 SKIPPED → УДАЛЕНО
   - Файл удален согласно best practices из Jetpack-WorkoutApp
   - Причина: интеграционные тесты для ViewModel не рекомендуются (QWEN.md)
   - Функциональность уже покрыта unit тестами CreateEditScreenViewModelTest

---

## Этап 17: Удаление некорректных интеграционных тестов ViewModel ✅

### Анализ

Сравнение с Jetpack-WorkoutApp показало:
- В WorkoutApp ViewModel тестируются только через **unit тесты** с MockK (в `src/test/`)
- Интеграционные тесты только для DAO и data-layer компонентов
- Нет интеграционных тестов для ViewModel

В JetpackDays:
- Уже есть полноценные unit тесты для `CreateEditScreenViewModel` (778 строк)
- Интеграционные тесты (`CreateEditScreenViewModelIntegrationTest`) дублируют функциональность
- Интеграционные тесты помечены `@Ignore("Тест написан с ошибками")`

### Решение

Удален файл `app/src/androidTest/java/com/dayscounter/ui/viewmodel/CreateEditScreenViewModelIntegrationTest.kt`:
- Дублирует unit тесты
- Написан с ошибками (не дожидается асинхронных операций)
- Нарушает best practices проекта (QWEN.md запрещает интеграционные тесты с ViewModel)
