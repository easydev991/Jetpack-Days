# План очистки лишнего кода

## Контекст

Проведён аудит проекта на предмет удаления/сокращения лишнего кода. Ниже — пошаговый план реализации только **безопасных** изменений, плюс отдельный раздел для **опасных** пунктов, которые требуют дополнительного анализа и не входят в текущую задачу.

## Правила проекта (AGENTS.md)

- Никогда не использовать `!!` — только `?`, `?:`, `let`, `checkNotNull`.
- Тесты именовать в `snake_case`, без обратных кавычек.
- После изменений: `make format`, `make test`, `make lint`.
- Clean Architecture: domain/data/presentation разделены.
- Локализация строк — через `R.string`/`R.plurals`.

---

## Часть 1. Безопасные изменения (применяем)

### Этап 1. Удаление boilerplate-тестов

Цель: убрать тесты, которые только проверяют data classes / enums / sealed classes / компиляцию интерфейса / базовый инструментальный тест.

- [ ] Удалить `app/src/test/java/com/dayscounter/domain/model/AppIconTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/domain/model/AppThemeTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/data/database/entity/ItemEntityTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/data/database/entity/ReminderEntityTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/screens/createedit/CreateEditUiStateTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/state/RootScreenStateTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/state/DetailScreenStateTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/screens/createedit/ColorSaverTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/screens/createedit/DisplayOptionSaverTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/screens/createedit/LocalDateSaverTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/ui/screens/createedit/ColorSelectorUtilsTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/analytics/NoopAnalyticsProviderTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/crash/CrashlyticsHelperTest.kt`
- [ ] Удалить `app/src/test/java/com/dayscounter/domain/repository/ItemRepositoryTest.kt`
- [ ] Удалить `app/src/androidTest/java/com/dayscounter/ExampleInstrumentedTest.kt`

**Критерий завершения:**
- `make test` проходит.
- В coverage thresholds (если настроены) не упали ниже лимита, иначе скорректировать threshold или оставить часть тестов.

**Примечание:** следующие тесты **НЕ удаляем**, так как они проверяют реальную логику:
- `CalculateDaysDifferenceUseCaseTest`
- `BackupWrapperTest`
- `ReminderIntentParserTest`
- `DatePickerConversionTest`
- `DisplayOptionTest`
- `TimePeriodTest`
- `DaysFormatterImplTest`

---

### Этап 2. Мёртвый код в domain-моделях

- [ ] Удалить файл `app/src/main/java/com/dayscounter/domain/model/ItemExtensions.kt` (extension `Item.makeDaysCount`).
- [ ] Удалить deprecated-метод `makeDaysCount(currentDate: Long)` из `Item.kt`.
- [ ] Удалить неиспользуемый импорт `GetFormattedDaysForItemUseCase` из `Item.kt`, если останется.

**Критерий завершения:**
- Сборка проходит (`./gradlew assembleDebug`).
- Тест `ItemTest.makeDaysCount_*` удалён вместе с extension.

---

### Этап 3. Упрощение `BackupFormat`

- [ ] Удалить `app/src/main/java/com/dayscounter/domain/usecase/BackupFormatSerializer.kt`.
- [ ] В `BackupFormat.kt` заменить `@Serializable(with = BackupFormatSerializer::class)` на `@Serializable`.
- [ ] Добавить `@SerialName("android")` на `ANDROID` и `@SerialName("ios")` на `IOS`.
- [ ] Убедиться, что ручной сериализатор больше нигде не используется.

**Критерий завершения:**
- `BackupWrapperTest` проходит (проверить lowercase, uppercase, null).
- `ImportBackupUseCaseTest` / `ExportBackupUseCaseTest` проходят.

**Риск:** kotlinx-serialization по умолчанию допускает `@SerialName` и имя enum. Если uppercase `"ANDROID"` перестанет десериализоваться — вернуть custom serializer. Проверить тестом перед удалением serializer.

---

### Этап 4. Упрощение `DaysFormatterImpl`

- [ ] Удалить data class `TimeComponents` в `DaysFormatterImpl.kt`.
- [ ] Заменить `buildComponentsList` + `formatComponents` на прямое построение `List<String>` и `joinToString(" ")`.
- [ ] Убедиться, что все ветки `DisplayOption.DAY`, `MONTH_DAY`, `YEAR_MONTH_DAY` работают корректно.

**Критерий завершения:**
- `DaysFormatterImplTest` проходит.
- Проверить форматирование для случаев: 0 компонентов, 1 компонент, 2 компонента, 3 компонента.

---

### Этап 5. Удаление неиспользуемого UI-метода

- [ ] Удалить `@Composable fun DisplayOption.getLocalizedTitle()` из `DisplayOption.kt`.
- [ ] Удалить связанные импорты `androidx.compose.runtime.Composable` и `androidx.compose.ui.res.stringResource`, если они больше не нужны в файле.

**Критерий завершения:**
- `./gradlew assembleDebug` проходит.
- Compose Preview не сломаны.

---

### Этап 6. Рефакторинг try/catch в репозиториях

- [ ] Создать inline helper `withCrashLogging` (например, в `com.dayscounter.data.repository` или в `util`).
- [ ] Заменить повторяющиеся `try/catch/log/rethrow` в `ItemRepositoryImpl.kt` на вызов helper.
- [ ] Заменить повторяющиеся `try/catch/log/rethrow` в `ReminderRepositoryImpl.kt` на вызов helper.
- [ ] Сохранить сообщения об ошибках на русском языке.

**Критерий завершения:**
- `ItemRepositoryImplTest` и `ReminderRepositoryImplTest` проходят.
- detekt не ругается на `TooGenericExceptionCaught` (либо suppress сохранён).

---

### Этап 7. Удаление неиспользуемых зависимостей из `app`

- [ ] Открыть `app/build.gradle.kts`.
- [ ] Удалить `implementation(libs.androidx.appcompat)`.
- [ ] Удалить `implementation(libs.androidx.compose.material3.adaptive.navigation.suite)`.
- [ ] Удалить `implementation(libs.androidx.compose.material.icons.extended)` — **только после замены иконок** (см. опасные пункты ниже).
- [ ] Удалить `androidTestImplementation(libs.androidx.espresso.core)`.
- [ ] Удалить `testImplementation(libs.robolectric)`.

**Критерий завершения:**
- `./gradlew app:dependencies` не содержит удалённых библиотек в конфигурациях `app`.
- `make build` проходит.

**Примечание:**
- `espresso-core`, `androidx.test:rules`, `uiautomator` используются модулем `screenshot-tests`, поэтому из `libs.versions.toml` их **не удаляем**.
- `kotlinx-coroutines-android` используется в production (`ReminderBootReceiver`) и в тестах, поэтому его **не трогаем**.

---

### Этап 8. Перевод `CrashlyticsHelperTest` на JUnit5

- [ ] Заменить `org.junit.Test` на `org.junit.jupiter.api.Test`.
- [ ] Заменить `org.junit.Assert.assertTrue` на `org.junit.jupiter.api.Assertions.assertTrue`.
- [ ] Удалить `@RunWith`, если появится в будущем.

**Критерий завершения:**
- `make test` проходит.

---

### Этап 9. Финальная проверка

- [ ] Запустить `make format`.
- [ ] Запустить `make lint`.
- [ ] Запустить `make test`.
- [ ] Запустить `make build`.
- [ ] Проверить, что нет `!!` в изменённых файлах.
- [ ] Проверить, что нет unused imports.

---

## Часть 2. Опасные изменения (НЕ применяем сейчас)

Эти пункты требуют отдельного анализа, так как могут сломать бизнес-логику, тестируемость, RTL/accessibility или интеграцию с iOS.

| Пункт | Почему опасно | Что нужно проверить перед применением |
|---|---|---|
| `CalculateDaysDifferenceUseCase.calculateTimePeriod` → `Period.between` | `Period.between` для будущих дат возвращает нормализованный отрицательный период (`P-1Y-2M-5D`), а текущий код даёт `years=0, months=0, days=-N`. Ломает форматирование будущих дат. | Сравнить вывод для 10+ кейсов будущих дат; обновить `DaysFormatterImpl`, если меняется контракт. |
| Удаление `ResourceProvider` + `ResourceIds` + `StubResourceProvider` | Нарушает Clean Architecture: domain use cases зависят от Android Context. Без `StubResourceProvider` unit-тесты форматирования перестанут работать. | Альтернатива — `Context.() -> String`, но это всё равно требует Android Context в domain. |
| `Logger` интерфейс → прямые `android.util.Log` | `android.util.Log` падает в JVM unit tests. Потеряется тестируемость. | Можно только удалить дубль `NoOpLogger` (main + test), оставив один. |
| Удаление `TimePeriod.isEmpty/isNotEmpty` | `isNotEmpty()` используется в production в 16+ местах (`MainScreen`, `DetailContent`, `ListItemView` и др.). | Если inline — дублирование; если удалить — ломается логика. |
| Inline `ReminderIntentParser.resolveReminderOpenItemId` в extension | Отдельная функция unit-testable без Android `Intent`. Inline усложнит тестирование. | Оценить, нужна ли эта функция вне extension. |
| Удаление `material-icons-extended` без замены иконок | `Icons.AutoMirrored.Filled.List`/`ArrowBack` используются в 5 местах. Удаление сломает сборку и RTL. | Найти замену в `material-icons-core` или оставить. |
| Удаление JUnit4 из `androidTest` | Все инструментальные тесты используют `@RunWith(AndroidJUnit4::class)`. Миграция на JUnit5 для Android Tests — отдельная задача. | Проверить поддержку JUnit5 в текущем AGP/Compose Test. |
| `AnalyticsProvider`/`AnalyticsService`/`NoopAnalyticsProvider` → singleton Firebase helper | Это архитектурный рефакторинг ~38 файлов, а не удаление лишнего. | Требуется отдельное решение: нужен ли multi-provider, нужна ли тестируемость. |
| `DaysFormatter` interface → single impl | Интерфейс нужен для изолированного тестирования `FormatDaysTextUseCase`. | Оценить, можно ли мокать object напрямую. |
| `ReminderManager`/`ReminderScheduler` interfaces → concrete classes | Интерфейсы нужны для тестов и `NoOpReminderManager` (default в ViewModels). | Concrete classes усложнят мокирование. |
| Удаление `kotlinx-coroutines-android` | Используется в `ReminderBootReceiver` (`Dispatchers.IO`) и в тестах. | Нельзя удалять без переработки логики. |

---

## Ожидаемый результат

- Удалено ~1,500–1,800 строк boilerplate-тестов и мёртвого кода.
- Убрано ~6 неиспользуемых зависимостей из `app/build.gradle.kts`.
- Проект собирается, проходит lint и все тесты.
- Опасные изменения задокументированы и не применены.

---

## Порядок выполнения

1. Этап 1 — удаление boilerplate-тестов.
2. Этап 2 — удаление мёртвого кода в `Item`.
3. Этап 3 — упрощение `BackupFormat`.
4. Этап 4 — упрощение `DaysFormatterImpl`.
5. Этап 5 — удаление `getLocalizedTitle`.
6. Этап 6 — рефакторинг репозиториев.
7. Этап 7 — удаление зависимостей.
8. Этап 8 — перевод `CrashlyticsHelperTest` на JUnit5.
9. Этап 9 — финальная проверка.
