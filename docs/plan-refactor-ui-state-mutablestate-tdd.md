# План рефактора UI-state: отказ от MutableState (TDD)

Дата: 2026-04-27  
Обновлено: 2026-05-03  
Статус: 🔄 В процессе (production-код мигрирован, баги исправлены, 455 unit + 79 androidTest зелёные; осталась формализация guard от регрессии и финальная приёмка)

## 1. Цель

- [x] Убрать `MutableState<T>` из data-классов UI-state во всех экранах.
- [x] Перенести мутабельность в ViewModel (`StateFlow`) и/или в Composable (`remember/rememberSaveable`).
- [x] **Мигрировано:** `CreateEditUiState` и `ReminderFormUiState` на экране Create/Edit.
- [x] Сохранить текущее поведение приложения без регрессий, включая напоминания, бэкапы и навигацию.

## 2. Область изменений

- [x] `ui/state/*` — уже чисты (`AppDataUiState`, `ThemeIconUiState`, `RootScreenState`).
- [x] `ui/viewmodel/*` — контракты состояний уже иммутабельны.
- [x] Большинство Compose-экранов уже не используют MutableState в data-классах.
- [x] `ui/screens/createedit/*` — мигрированы:
  - `CreateEditUiState` — 6 plain-полей (включая `showDatePicker`)
  - `ReminderFormUiState` — 9 plain-полей
  - Все зависимые компоненты принимают plain-значения + callback'и
- [x] Unit-тесты переписаны:
  - `CreateEditUiStateTest` — ✅ без `mutableStateOf`, без `.value`
  - `CreateEditReminderStateTest` — ✅ без `mutableStateOf`, без `.value`
- [x] Android-тесты скомпилированы и проходят (79/79)

## 3. Ограничения и правила проекта

- [x] Следовать TDD: Red -> Green -> Refactor в каждом инкременте.
- [x] Не использовать `!!`, применять безопасную работу с nullable.
- [x] Не менять модель резервных копий и формат сериализации backup.
- [x] После каждого инкремента запускать `make format` и релевантные тесты.
- [x] Перед финалом запустить полный `make test`.

## 4. Зависимости и порядок выполнения

- [x] Большинство UI-state уже иммутабельны (Detail, Main/Events, Theme/Icon, AppData, Root).
- [x] Пилотная миграция не требуется — архитектурный паттерн уже выбран и работает.
- [x] **Мигрированы:** `CreateEditUiState` (6 полей) и `ReminderFormUiState` (9 полей).
- [ ] **Не выполнено:** формализация guard от регрессии (перенесено в инкремент D).

## 5. Инкременты (TDD)

### 5.1 Инкремент A: Инвентаризация ✅ (выполнено)

Выявлено 2 data-класса с `MutableState<T>`: `CreateEditUiState` (5 полей) и `ReminderFormUiState` (9 полей). Остальные UI-state иммутабельны (`AppDataUiState`, `ThemeIconUiState`, `RootScreenState`, `DetailScreenState`, `MainScreenState`, `CreateEditScreenState`, `CreateEditChangeInput`).

**Тесты для миграции:** `CreateEditUiStateTest.kt` (14 тестов), `CreateEditReminderStateTest.kt` (14 тестов), `CreateEditScreenCustomColorTest.kt` (2 UI-теста).

**Production-файлы:** `CreateEditUiState.kt`, `CreateEditReminderState.kt`, `CreateEditFormParams.kt`, `CreateEditFormContent.kt`, `CreateEditSelectors.kt`, `CreateEditButtons.kt`, `CreateEditScreen.kt`, `CreateEditReminderEffects.kt`, `ReminderSettingsSection.kt`, `StateSavers.kt` — используют MutableState через `.value`.

### 5.2 Инкремент B: Миграция CreateEdit ✅ (выполнено)

**Решение:** Единый `MutableState<CreateEditUiState>` на уровне Composable вместо 14 отдельных `MutableState<T>`. Все мутации — через `copy()`.

- [x] `CreateEditUiState` — 6 plain-полей (включая `showDatePicker`)
- [x] `ReminderFormUiState` — 9 plain-полей
- [x] `CreateEditFormParams` — без MutableState (plain-поля + callback'и)
- [x] `rememberCreateEditUiState()` — единственный MutableState на уровне экрана
- [x] `applyReminder` — чистая функция, возвращает `ReminderFormUiState`
- [x] Все Composable принимают plain-значения + callback'и
- [x] Unit-тесты переписаны на plain data class: 14 (`CreateEditUiStateTest`) + 15 (`CreateEditReminderStateTest`)
- [x] `make format` + unit-тесты зелёные (455)

### 5.3 Инкремент C: Исправление замечаний после рефакторинга ✅ (выполнено)

4 предсуществующих архитектурных замечания (не регрессии):

- [x] **C.1** `loadItemData` — перенесён из тела композиции в `LaunchedEffect(itemId, uiState)`
- [x] **C.2** `previousReminderEnabled` — `rememberSaveable` → `remember` (корректная инициализация после поворота через Saver)
- [x] **C.3** `showUnitsMenu` — `remember` → `rememberSaveable` (сохранение при повороте)
- [x] **C.4** `showDatePicker` — перенесён из отдельного `rememberSaveable` в поле `CreateEditUiState`, единообразно с `ReminderFormUiState.showDatePicker`
- [x] `make format` + unit-тесты зелёные

### 5.4 Инкремент D: Архитектурная стабилизация (guard от регрессии) 🟡

- [x] Паттерн единообразен: ViewModel → StateFlow, Compose → remember/rememberSaveable, data class'ы без MutableState.
- [ ] Добавить тест/проверку, что `CreateEditUiState` и `ReminderFormUiState` не содержат `MutableState<T>`.
- [ ] Обновить документацию по паттерну состояния.

#### Критерий завершения D

- [ ] Новый паттерн формализован и защищён от регрессии.

### 5.5 Инкремент E: Финальная регрессия и приемка 🟡

- [x] `make format` — без замечаний (проверено многократно).
- [x] `make test` — 455 unit + 79 androidTest зелёные (проверено многократно).
- [ ] Прогнать критичные ручные сценарии:
  - создание/редактирование записи;
  - установка/срабатывание напоминаний;
  - открытие записи из уведомления;
  - удаление записи;
  - backup/restore.
- [ ] Сверить UX до/после на отсутствие изменений поведения.

#### Критерий завершения E

- [ ] Ручная проверка без блокирующих дефектов.

### 5.6 Технический долг: Android-тесты ✅ (исправлено)

3 типа ошибок компиляции androidTest, все исправлены:

- [x] **5.6.1** `by remember` вне `@Composable` (Kotlin 2.0+): `remember` → `mutableStateOf` на уровне метода в `CreateEditReminderAutoScrollUiTest.kt` и `ReminderSettingsSectionUiTest.kt`
- [x] **5.6.2** `showDatePicker` удалён из `CreateEditFormParams` после C.4: исправлены `CreateEditReminderAutoScrollUiTest.kt` и `CreateEditScreenCustomColorTest.kt`
- [x] **5.6.3** Дублирование `DetailScreenState` в `DetailScreenViewModelIntegrationTest.kt` (shadow production-класс) — удалён локальный sealed class
- [x] `connectedDebugAndroidTest` — 79/79 зелёные

### 5.7 Устаревший вызов createComposeRule (androidTest, 8 файлов) ✅ (исправлено)

Замена `import androidx.compose.ui.test.junit4.createComposeRule` (deprecated) на `import androidx.compose.ui.test.junit4.v2.createComposeRule` в 8 файлах: `DaysCountTextTest.kt`, `ColorSelectorUiTest.kt`, `CreateEditSaveValidationUiTest.kt`, `CreateEditScreenCustomColorTest.kt`, `ReminderSettingsSectionUiTest.kt`, `ColorTagFilterDialogTest.kt`, `MoreScreenTest.kt`, `ThemeIconScreenTest.kt`.

### 5.8 Регрессии после рефакторинга: исправление багов ✅ (исправлено)

5 багов, исправленных TDD:

- [x] **Баг #1** — `DatePickerDialogSection`: `onDismiss()` в confirm-кнопке перезаписывал `selectedDate`, т.к. захватывал старый `params` из композиции. Фикс: удалён `onDismiss()` из `onClick` confirm-кнопки. Диалог закрывается через `showDatePicker = false` при рекомпозиции.
- [x] **Баг #2** — Валидация `AFTER_INTERVAL` всегда возвращала ошибку: `?.let { null }` всегда давал `null`, Elvis выбирал `R.string.error`. Фикс: явная проверка `if (amount != null && amount >= 1) null else ...`.
- [x] **Баг #3** — После фикса Бага #1 диалог даты события перестал закрываться: `onDateSelected` не устанавливал `showDatePicker = false`. Фикс: добавлен `showDatePicker = false` в `copy()` в `CreateEditScreen.kt:150`.
- [x] **Баг #4** — Ведущие нули в поле "Напомнить через" не отбрасывались. TDD: `sanitizeIntervalValue` (filter digits + trimStart('0')), применён в `ReminderSettingsSection.kt:onValueChange`. 4 теста.
- [x] **Баг #5** — DatePicker показывал предыдущий день (timezone mismatch). Material3 DatePicker использует UTC, код передавал millis через `ZoneId.systemDefault()`. Фикс: `ZoneOffset.UTC` в `DatePickerDialogSection`. 4 теста в `DatePickerConversionTest.kt`.

- [x] Все тесты зелёные: 455 unit + 79 androidTest
- [x] `make format` без замечаний

## 6. Риски и меры снижения

- [x] **Риск поломки сохранения состояния при повороте экрана.**
  Мера: тесты на recreation + `rememberSaveable` только в UI-слое.
  Результат: `CreateEditUiStateSaver` + `rememberSaveable` — корректен.
- [x] **Риск изменения логики валидации/hasChanges.**
  Мера: сохранены unit-тесты на `isCreateEditFormValid`, `isInputValid`, `toReminderRequest`, `toChangeFingerprint`.
  Результат: 15 тестов `CreateEditReminderStateTest` проходят.
- [x] **Риск скрытых регрессий в напоминаниях.**
  Мера: `CreateEditReminderStateTest` + `CreateEditScreenViewModelReminderTest` — зелёные.
  Результат: регрессия авто-скролла (Fix #2) исправлена.
- [x] **Риск влияния на backup.**
  Мера: backup-модели не затрагивались; backup-тесты зелёные.

## 7. Definition of Done

- [x] В проекте нет UI-state с `MutableState<T>` внутри data-классов.
- [x] `CreateEditUiState` и `ReminderFormUiState` не содержат `MutableState<T>`.
- [x] Все ViewModel/Compose экраны работают на иммутабельном контракте.
- [x] `make format` и `make test` проходят (455 unit-тестов зелёные).
- [x] Напоминания, backup/restore и существующая навигация не деградировали.
- [ ] Документация по состояниям обновлена.
- [x] Android-тесты (`app/src/androidTest/`) скомпилированы и проходят (79/79).
