# План: исправление предупреждений IDE (lint + компилятор)

> **Статус: план готов, не выполнялся** (кроме прогонов анализа — Фаза 0
> и факт-чек 1.2; сентябрь 2026).
> Референс: аналогичный выполненный план проекта
> Jetpack-MyWorkouts (`docs/plans/fix-ide-warnings.md` того репозитория).

## Контекст

Прогон `./gradlew clean compileGithubDebugKotlin --warning-mode all` +
`./gradlew lintGithubDebug` (flavor `github`) выявил **67 проблем** в текущем коде:

| Категория | Кол-во | Источник |
|---|---|---|
| Lint `MissingPermission` ⛔ **error** | 1 | `notify()` без проверки `POST_NOTIFICATIONS` в `ReminderAlarmReceiver` |
| Lint `NewApi` ⛔ **error** | 1 | `AlarmManager#canScheduleExactAlarms` (API 31) в `ExactAlarmPermissionHelper` |
| Lint `UnusedResources` | 36 | мёртвые цвета/dimens/строки в `values/` (+ зеркала в `values-ru/`) |
| Lint `VectorPath` | 7 | длинные пути в иконках (`ic_launcher_monochrome`, `icon_preview_1..6`) |
| Lint `ModifierParameter` | 5 | `DetailContent.kt`, `ReminderSettingsSection.kt` |
| Lint `InlinedApi` | 3 | `POST_NOTIFICATIONS` ×2, `ACTION_REQUEST_SCHEDULE_EXACT_ALARM` ×1 |
| Lint `ScopedStorage` | 2 | WRITE/READ_EXTERNAL_STORAGE в `src/debug/AndroidManifest.xml` |
| Lint `ObsoleteSdkInt` | 2 | гвард `SDK_INT < O` в `ReminderAlarmReceiver`, папка `mipmap-anydpi-v26` при `minSdk = 26` |
| Lint `TypographyEllipsis` | 2 | «...» вместо «…» в `loading` (`values/` + `values-ru/`) |
| Lint `UseKtx` | 2 | `Uri.parse` → `String.toUri` в `MoreScreen.kt` |
| Lint `OldTargetApi` | 1 | `targetSdk = 36` при `compileSdk = 37` |
| Lint `UnusedAttribute` | 1 | `enableOnBackInvokedCallback` в манифесте (только API 33+) |
| Lint `AndroidGradlePluginVersion` | 1 | AGP `9.4.0` → есть `9.4.1` |
| Lint `StaticFieldLeak` | 1 | поле `context: Context` в `AppDataScreenViewModel` |
| Lint hint `AutoboxingStateCreation` | 1 | `mutableStateOf(Int)` в `DetailContent.kt` |
| Kotlin compiler `UNUSED_EXPRESSION` | 1 | `NoopAnalyticsProvider.kt:10` |

**Отличие от референса:** в MyWorkouts errors не было; здесь lint-прогон **падает
с exit 1** из-за 2 errors, поэтому их исправление — первая фаза, а не опция.

**Системная причина накопления:** Android Lint не входит ни в один make-таргет
(`make lint` = ktlint + detekt + markdownlint), поэтому предупреждения копились
молча. Финальная фаза плана устраняет причину.

**TDD:** применяется только к Фазе 1.1 — там добавляется новая ветка логики
(гвард permission в `ReminderAlarmReceiver`), тесты пишутся первыми (androidTest,
`ReminderAlarmReceiver` уже покрыт). Остальные фазы — механические правки без
новой бизнес-логики: TDD не применяется (как в референсе).

## Стратегия коммитов

Девять атомарных коммитов (восемь, если 7.2 решён осознанным пропуском),
сгруппированных по росту риска: ошибки → удаления и мелкие фиксы →
API-изменения → зависимости → процесс. Исключение: Фаза 1 (ошибки lint) идёт
первой, потому что сейчас `lintGithubDebug` красный и baseline-счётчик
«0 errors» должен появиться как можно раньше. Стиль
сообщений — как в истории репо (русские фразы, без conventional-commit
префиксов). После каждой фазы — `make format && make lint && make test`
(как требует AGENTS.md); для фаз с изменениями в `reminder/` или Compose UI —
дополнительно `make android-test`.

> Перед началом — выполнить Фазу 0.

---

## Фаза 0 — Baseline (отдельного коммита нет)

Цель: зафиксировать стартовое состояние, чтобы любые падения после правок
точно атрибутировались к нашим изменениям.

- [x] `git status` — рабочее дерево чистое (`main` = `origin/main`)
- [ ] `make test` — все JVM-тесты зелёные (в анализе не прогонялся — выполнить первым делом)
- [ ] `make android-test` — все androidTest зелёные (в анализе не прогонялся)
- [x] `./gradlew lintGithubDebug` — счётчики записаны: **2 errors / 63 warnings / 1 hint**, exit 1
- [x] `./gradlew lintRustoreDebug` — идентично: **2 errors / 63 warnings / 1 hint**
- [x] `./gradlew clean compileGithubDebugKotlin --warning-mode all` — 1 варнинг
      (`UNUSED_EXPRESSION`, `NoopAnalyticsProvider.kt:10`)

**Критерий завершения:** все команды прогнаны, счётчики записаны.

---

## Фаза 1 — Ошибки lint: `reminder/` (коммит «Исправление lint-ошибок в напоминаниях»)

Обе ошибки — «доказать линту уже существующий рантайм-контракт», переработки
логики не требуется. Но 1.1 добавляет новую ветку — по TDD сначала тест.

### 1.1 `MissingPermission`: `ReminderAlarmReceiver.kt:67`

- [ ] Тест (первым): androidTest на `ReminderAlarmReceiver` — при отсутствии
      `POST_NOTIFICATIONS` (API 33+) `notify()` не вызывается («крэша нет»
      не проверяем: без гварда крэша тоже не будет — см. «Семантику» ниже)
- [ ] Перед `.notify(...)` добавить проверку permission. Переиспользовать
      существующий `Context.hasPostNotificationsPermission()`
      (`ReminderNotificationPermissionPolicy.kt:50`) — не дублировать
- [ ] Факт-чек при выполнении: lint `MissingPermission` — межпроцедурный анализ
      ограничен; если проверку через extension линт «не видит», проверить
      контекстным прогоном. Не поможет — прямой
      `ContextCompat.checkSelfPermission` в receiver'е (семантика та же,
      extension остаётся для policy)
- [ ] Семантика: без permission уведомление система всё равно молча отбрасывает —
      тихий skip ничего не теряет, гвард убирает lint-error

### 1.2 `NewApi` + попутный `InlinedApi`: `ExactAlarmPermissionHelper.kt:57,71`

- [ ] Гвард `sdkInt < Build.VERSION_CODES.S` уже есть (инжектируемый параметр
      для тестов) — линт его статически не видит
- [ ] Факт-чек (эксперимент на этом же файле, 2026-09): split API-31 вызовов в
      private-методы с `@RequiresApi(Build.VERSION_CODES.S)` **не работает** —
      lint не доказывает поток от инжектируемого `sdkInt` к вызову, `NewApi`
      «переезжает» на строки вызова (58, 75), errors становится 3. Рабочий
      вариант — `@ChecksSdkIntAtLeast`: private-предикат
      `@ChecksSdkIntAtLeast(parameter = 0, api = Build.VERSION_CODES.S)
      private fun atLeastS(sdk: Int) = sdk >= Build.VERSION_CODES.S`, оба
      гварда → `if (!atLeastS(sdkInt)) ...`. Подтверждено прогоном: `NewApi`
      исчезает, попутный `InlinedApi` (:71) уходит вместе с ней, инжектируемый
      `sdkInt` и тестопригодность сохраняются
- [ ] Применить вариант `@ChecksSdkIntAtLeast`; запасной (если поведение lint
      изменится после обновления AGP) — прямой `Build.VERSION.SDK_INT` в
      гварде: `if (Build.VERSION.SDK_INT < S || sdkInt < S)`

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные (тесты reminder/AlarmManager, permission-тест из 1.1)
- [ ] `./gradlew lintGithubDebug` — **errors = 0, exit 0**; `InlinedApi` = 2
      (третий ушёл вместе с 1.2); остальные счётчики не выросли

**Критерий завершения:** lint зелёный, 0 errors в обоих flavor, тесты зелёные.

**Коммит:**

```
Исправление lint-ошибок в напоминаниях

- ReminderAlarmReceiver: проверка POST_NOTIFICATIONS перед notify
  (тестируется первым по TDD)
- ExactAlarmPermissionHelper: гварды через @ChecksSdkIntAtLeast-предикат
  atLeastS, инжектируемый sdkInt сохранён
```

---

## Фаза 2 — Безопасные удаления ресурсов и манифеста (коммит «Удаление неиспользуемых ресурсов»)

Только удаление/аннотирование, поведение не меняется.

### 2.1 `UnusedResources` (36)

- [ ] Перекрёстная проверка на момент выполнения (`rg` каждого имени по `app/`,
      `screenshot-tests/`, `fastlane/`): в анализе от 2026-09 ложных
      срабатываний нет; помнить, что дефолтный lint **не видит** использования
      ресурсов модулем `screenshot-tests` (он зависит от `:app`)
- [ ] `values/colors.xml` — удалить 10: `purple_200`, `purple_500`, `purple_700`,
      `teal_200`, `teal_700`, `black`, `white`, `color_primary_green`,
      `color_primary_purple`, `color_primary_yellow`; файл удалить, если стал пустым
- [ ] `values/dimens.xml` — удалить 10: `elevation_none`, `elevation_small`,
      `elevation_medium`, `icon_size_small`, `icon_size_medium`, `icon_size_large`,
      `size_small`, `size_medium`, `size_large`, `spacing_xlarge`
- [ ] `values/strings.xml` + `values-ru/strings.xml` — удалить 16 имён в каждой
      локали (13 строк + 3 plurals): `add_color_tag`, `app_name`, `days_count_full`,
      `details_for_the_item`, `done`, `event_deleted`, `events_tab`,
      `months_count_full`, `more_tab`, `primary_icon`,
      `reminder_notification_title_with_item`, `sort_order`, `title_for_the_item`,
      `unable_to_recover_data_from_the_selected_file`, `variant`, `years_count_full`
- [ ] Факт-чек `app_name` отдельно: лейбл лаунчера задаётся
      `android:label="@string/app_display_name"` (манифест, строка 15) — `app_name`
      действительно мёртв. Удаление не меняет лейбл

### 2.2 `ObsoleteSdkInt`: слить `mipmap-anydpi-v26/` в `mipmap-anydpi/`

- [ ] `git mv` 12 XML в `mipmap-anydpi/`, пустая папка удалена (`minSdk = 26`)

### 2.3 `ObsoleteSdkInt`: гвард `SDK_INT < O` в `ReminderAlarmReceiver.kt:73`

- [ ] Ветка `createNotificationChannel` недостижима при `minSdk = 26` — гвард удалить

### 2.4 `ScopedStorage`: `src/debug/AndroidManifest.xml:9-10`

- [ ] К WRITE/READ_EXTERNAL_STORAGE добавить `android:maxSdkVersion="32"` —
      разрешения нужны только screengrab-прогонам, на современных API они всё
      равно не выдаются

### 2.5 `UnusedAttribute`: `AndroidManifest.xml:12`

- [ ] `android:enableOnBackInvokedCallback="true"` — добавить
      `tools:targetApi="tiramisu"` на `<application>` (атрибут игнорируется
      на API < 33, это ожидаемое поведение)

### 2.6 `TypographyEllipsis`: `loading` (`values/strings.xml:157` + `values-ru/strings.xml:157`)

- [ ] «...» → «…» в обеих локалях

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные (п. 2.3 затрагивает `reminder/` —
      требование стратегии)
- [ ] `./gradlew lintGithubDebug` — `UnusedResources`, `ObsoleteSdkInt`,
      `ScopedStorage`, `UnusedAttribute`, `TypographyEllipsis` = 0

**Критерий завершения:** 0 варнингов в этих пяти категориях, build и тесты зелёные.

**Коммит:**

```
Удаление неиспользуемых ресурсов

- colors.xml: 10 цветов не используются
- dimens.xml: 10 dimen не используются
- strings.xml (en/ru): 13 строк + 3 plurals не используются
  (лейбл лаунчера — app_display_name, app_name мёртв)
- mipmap-anydpi-v26 → mipmap-anydpi (minSdk = 26)
- ReminderAlarmReceiver: недостижимый гвард SDK_INT < O
- debug-манифест: maxSdkVersion=32 для WRITE/READ_EXTERNAL_STORAGE
- манифест: tools:targetApi для enableOnBackInvokedCallback
- strings `loading`: «...» → «…»
```

---

## Фаза 3 — Мелкие механические фиксы кода (коммит «Мелкие исправления по замечаниям линтера»)

Однострочные правки без изменения поведения.

- [ ] `NoopAnalyticsProvider.kt:10` — тело `Unit` → пустой блок/комментарий
      (устраняет единственный варнинг компилятора `UNUSED_EXPRESSION`)
- [ ] `DetailContent.kt` — hint `AutoboxingStateCreation`: `mutableStateOf(Int)` →
      `mutableIntStateOf` + импорт (алфавитный порядок; `getValue`/`setValue` уже есть)
- [ ] `MoreScreen.kt:259,303` — `Uri.parse(x)` → `x.toUri()`
      (импорт `androidx.core.net.toUri`; core-ktx уже в зависимостях)

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные (`DetailContent`, `MoreScreen` — Compose UI,
      требование стратегии)
- [ ] `./gradlew clean compileGithubDebugKotlin --warning-mode all` — **0 варнингов**
- [ ] `./gradlew lintGithubDebug` — 0 hints; `UseKtx` = 0

**Критерий завершения:** компилятор чистый, hints = 0.

**Коммит:**

```
Мелкие исправления по замечаниям линтера

- NoopAnalyticsProvider: тело Unit → пустой блок (UNUSED_EXPRESSION)
- DetailContent: mutableStateOf(Int) → mutableIntStateOf
- MoreScreen: Uri.parse → String.toUri
```

---

## Фаза 4 — Оставшиеся `InlinedApi` (коммит «Подавление InlinedApi с обоснованием»)

Две точки используют платформенные константы-строки: `Policy` — под прямым
гвардом на инжектируемом `sdkInt`, `CreateEditFormContent` — опосредованно
(решение о launch приходит из policy через `decideReminderToggle`). Линт их
статически не видит, а `@RequiresApi` не применим (вызов легален и на старых
API: константа инлайнится компилятором, значение стабильно на всех уровнях
API). Честный фикс — подавление с обоснованием.

- [ ] `CreateEditFormContent.kt:298` — `reminderPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)`:
      `@SuppressLint("InlinedApi")` + комментарий «константа инлайнится, значение
      стабильно; решение о запуске принимает ReminderNotificationPermissionPolicy»
- [ ] `ReminderNotificationPermissionPolicy.kt:57` — та же константа в
      `hasPostNotificationsPermission`: `@SuppressLint("InlinedApi")` +
      комментарий «инлайн константы безопасен на API < 33, гвард выше»
- [ ] Альтернатива (если при выполнении найдётся способ без подавления —
      например, аннотация константы-обёртки) — предпочесть её

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные (экран CreateEdit покрыт androidTest)
- [ ] `./gradlew lintGithubDebug` — `InlinedApi` = 0

**Критерий завершения:** `InlinedApi` = 0, обоснование зафиксировано в коде.

**Коммит:**

```
Подавление InlinedApi с обоснованием

- CreateEditFormContent, ReminderNotificationPermissionPolicy:
  Manifest.permission.POST_NOTIFICATIONS — compile-time константа,
  инлайн безопасен на API < 33
```

---

## Фаза 5 — `Modifier` первым опциональным параметром (коммит «Modifier первым опциональным параметром»)

Механический reorder, но публичные сигнатуры меняются — проверить **всех
вызывающих**. Все найденные вызовы используют именованные аргументы (проверено
в анализе; повторить `rg` на момент выполнения).

Линт насчитал 5 issue на 4 сигнатуры (одна строка несёт две претензии):

| Issue | Место | Функция | Претензия |
|---|---|---|---|
| 1 | `DetailContent.kt:112` | `DetailContentInner` | first optional |
| 2 | `DetailContent.kt:193` | `ReadSectionView` | first optional |
| 3+4 | `ReminderSettingsSection.kt:69` | `ReminderSettingsSection` | named + first optional (обе на `expandedContentModifier`) |
| 5 | `ReminderSettingsSection.kt:126` | `ReminderExpandedContent` | named |

Строки — из lint-отчёта (строки параметров, не деклараций). Объявления:
`DetailContentInner` — 106, `ReadSectionView` — 189, `ReminderSettingsSection` — 65,
`ReminderExpandedContent` — 123; при выполнении ориентироваться на имена функций.

### 5.1 `DetailContent.kt:112` — `DetailContentInner`

- [ ] Порядок: `item`, `modifier`, `reminder = null`, `onCopyTitle`, `onCopyDetails`,
      `getDaysAnalysisTextUseCase` — `modifier` перед опциональным `reminder`
- [ ] Вызывающие (3): `DetailContent.kt:75`, `DetailContentPreviews.kt:322,371` — именованные

### 5.2 `DetailContent.kt:193` — `ReadSectionView`

- [ ] `modifier` перед опциональным `onCopy`
- [ ] Вызывающие (8): `DetailContent.kt` ×3, `DetailContentPreviews.kt` ×3,
      `ReadSectionViewCopyContextMenuUiTest.kt` ×2 — именованные

### 5.3 `ReminderSettingsSection.kt:69,126` — факт-чек семантики

> **Решение на этапе выполнения.** Линт требует параметр с именем `modifier`
> первым опциональным. Но `expandedContentModifier` модифицирует расширенный
> контент секции, а не корень — тупое переименование меняет семантику.

- [ ] Вариант А (предпочтителен): добавить корневой `modifier: Modifier = Modifier`
      первым опциональным параметром в `ReminderSettingsSection` и
      `ReminderExpandedContent`, `expandedContentModifier` оставить как есть —
      если после этого предупреждения не уйдут, подавить оставшиеся с комментарием
- [ ] Вариант Б: переименовать с фактическим изменением API (только если
      параметр реально применяется к корню)
- [ ] Вызывающие (8 существующих): `CreateEditFormContent.kt:176`,
      `ReminderSettingsSection.kt:331,357` (превью), `ReminderSettingsSectionUiTest.kt` ×5 —
      именованные; новые вызовы появятся при варианте А (корневой `modifier`)

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные (детальный экран, CreateEdit, previews)
- [ ] `./gradlew lintGithubDebug` — `ModifierParameter` = 0

**Критерий завершения:** ✅ `ModifierParameter` = 0; androidTest зелёный.

**Коммит:**

```
Modifier первым опциональным параметром

- DetailContent.kt: DetailContentInner, ReadSectionView
- ReminderSettingsSection.kt: корневой modifier добавлен первым
  опциональным; семантика expandedContentModifier сохранена
```

---

## Фаза 6 — `StaticFieldLeak` (коммит «Типизация контекста у AppDataScreenViewModel»)

- [ ] `AppDataScreenViewModel.kt:46` — `context: Context` → `context: Application`:
      поле живёт столько же, сколько ViewModel, хранить `Context` в нём —
      источник предупреждения; используется только `getString(...)` (8 вызовов),
      factory уже передаёт `application`
- [ ] Проверить тесты, конструирующие ViewModel напрямую (unit/androidTest)

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные
- [ ] `./gradlew lintGithubDebug` — `StaticFieldLeak` = 0

**Критерий завершения:** ✅ `StaticFieldLeak` = 0.

**Коммит:**

```
Типизация контекста у AppDataScreenViewModel

- AppDataScreenViewModel: context: Context → Application
  (используется только getString, factory уже передаёт application)
```

---

## Фаза 7 — Зависимости (коммит «Обновление версии AGP»; при выполнении 7.2 — второй коммит «Повышение targetSdk до 37»)

Каждый bump проверяем отдельно.

### 7.1 AGP `9.4.0` → `9.4.1`

- [ ] `gradle/libs.versions.toml:2` — `agp = "9.4.1"`
- [ ] `./gradlew clean assembleGithubDebug` — BUILD SUCCESSFUL
- [ ] `./gradlew lintGithubDebug` — `AndroidGradlePluginVersion` = 0, счётчики не выросли
- [ ] JVM и androidTest зелёные

### 7.2 `OldTargetApi`: `targetSdk = 36` → `37` — решение на этапе выполнения

> В анализе: `compileSdk = 37` уже стоит, `targetSdk = 36` — линт предупреждает
> о compatibility-режимах. Бамп targetSdk включает behavior changes ОС —
> это самый рискованный пункт плана.

- [ ] Прогнать полный набор: `make test && make android-test` + ручной smoke
      (создание события, напоминание, backup, смена иконки)
- [ ] Если обнаружится поведенческий регресс — зафиксировать осознанный пропуск
      в этом пункте по образцу Фазы 5.2 референса (compose-bom): warning остаётся
      до отдельного апгрейда, повторный бамп без устранения причины не делать

**Верификация:**
- [ ] `make format && make lint && make test` — зелёные
- [ ] `make android-test` — зелёные
- [ ] `./gradlew lintGithubDebug` — `AndroidGradlePluginVersion` = 0;
      `OldTargetApi` = 0 (либо 1 осознанно, см. 7.2)

**Критерий завершения:** AGP-варнинг = 0; targetSdk либо бампнут с полным
прогоном, либо пропуск зафиксирован с причиной.

**Коммит:**

```
Обновление версии AGP

- AGP 9.4.0 → 9.4.1
- targetSdk не меняется — при выполнении 7.2 это отдельный коммит
```

**Коммит (только при выполнении 7.2):**

```
Повышение targetSdk до 37

- targetSdk 36 → 37, отдельно от AGP-бампа — bisect-атрибуция регрессий
```

---

## Фаза 8 — Процесс: lint в make + документация (коммит «Добавление Android Lint в make lint»)

Устранение причины накопления: Android Lint не гоняется ни одним make-таргетом.

- [ ] `Makefile` — новая цель `lint-android`: `./gradlew lintGithubDebug lintRustoreDebug`
- [ ] Включить `lint-android` в prereq-цепочку `make lint` (или отдельным шагом
      в `make check` — по итогам обсуждения; главное — попадание в регулярный прогон)
- [ ] `.PHONY` дополнить
- [ ] Документация: упоминание в `AGENTS.md`/`docs/` (где описан `make lint`) —
      синхронизировать; архивные `openspec/changes/archive/...` не трогать
- [ ] Убедиться, что итоговый остаток warning'ов зафиксирован в этом плане
      (раздел «Финальная верификация»)

**Критерий завершения:** `make lint` включает Android Lint для обоих flavor;
остаточные варнинги задокументированы как осознанные.

**Коммит:**

```
Добавление Android Lint в make lint

- Makefile: цель lint-android (github + rustore flavor), включена в make lint
- Документация: синхронизирована с новой целью
```

---

## Финальная верификация (отдельного коммита нет)

- [ ] `./gradlew clean compileGithubDebugKotlin --warning-mode all` — **0 варнингов**
      (после clean задачи реально исполнились, не UP-TO-DATE)
- [ ] `./gradlew lintGithubDebug` — 0 errors, 0 hints; осознанный остаток —
      см. итог под Сводкой
- [ ] `./gradlew lintRustoreDebug` — то же
- [ ] `VectorPath` (7) — осознанно не исправляется: длинные пути в иконках от
      дизайнера; уменьшение точности рискует качеством отрисовки.
      Принять как есть; при желании — уменьшение precision (svg-оптимизатор)
      отдельной задачей
- [ ] `make format && make lint && make test` — зелёные (ktlint и detekt без замечаний;
      теперь make lint включает и Android Lint)
- [ ] `make android-test` — зелёные

**Критерий завершения:** ✅ компилятор чистый; lint в обоих flavor — только
осознанные остатки; тесты зелёные.

---

## Сводка по коммитам

| # | Категория | Коммит | Изменения | Риск |
|---|---|---|---|---|
| 1 | Ошибки lint | «Исправление lint-ошибок в напоминаниях» | 3-4 файла + androidTest | средний (новая ветка логики, TDD) |
| 2 | Удаление | «Удаление неиспользуемых ресурсов» | ~8 файлов | низкий |
| 3 | Мелкие фиксы | «Мелкие исправления по замечаниям линтера» | 3 файла | низкий |
| 4 | Подавления | «Подавление InlinedApi с обоснованием» | 2 файла | низкий |
| 5 | Рефакторинг | «Modifier первым опциональным параметром» | 2 файла, 19 call-sites проверены | средний (API) |
| 6 | Рефакторинг | «Типизация контекста у AppDataScreenViewModel» | 1 файл | низкий |
| 7 | Зависимости | «Обновление версии AGP» | 1 файл | средний |
| 8 | Зависимости | «Повышение targetSdk до 37» — только при выполнении 7.2 | 1 файл | высокий |
| 9 | Процесс | «Добавление Android Lint в make lint» | Makefile + docs | низкий |

После всех фаз: **0 ошибок lint, 0 варнингов компилятора, 0 hints**; lint —
осознанный остаток `VectorPath: 7` (+ `OldTargetApi: 1` при пропуске 7.2) в
обоих flavor. Было: 2 errors / 63 warnings / 1 hint / 1 компилятор.
