# План: исправление предупреждений IDE (lint + компилятор)

> **Статус: все фазы (0–8) выполнены** (сентябрь 2026, восемь атомарных коммитов).
> 7.2 (targetSdk 37) — закрыт следом (отдельный коммит): lint **0 errors /
> 7 warnings / 0 hints** (остались только VectorPath 7), компилятор чистый.
> UI-тесты на эмуляторе API 37 заблокированы апстрим-багом espresso
> (wix/Detox#4967); на API 36 полный набор зелёный. Ручной smoke на 37 —
> единственный открытый пункт. Референс: аналогичный план Jetpack-MyWorkouts.

## Контекст

Прогон `./gradlew clean compileGithubDebugKotlin --warning-mode all` +
`./gradlew lintGithubDebug` выявил **67 проблем**: 2 lint-error
(`MissingPermission` в `ReminderAlarmReceiver`, `NewApi` в
`ExactAlarmPermissionHelper`), 63 warnings (UnusedResources 36, VectorPath 7,
ModifierParameter 5, InlinedApi 3, ScopedStorage/ObsoleteSdkInt/TypographyEllipsis/UseKtx
по 2, OldTargetApi/UnusedAttribute/AGP/StaticFieldLeak по 1), 1 hint
(AutoboxingStateCreation) и 1 варнинг компилятора (UNUSED_EXPRESSION).
Из-за errors lint-прогон падал с exit 1. Системная причина накопления:
Android Lint не входил ни в один make-таргет — устранена в Фазе 8.

**TDD:** только Фаза 1 (новая ветка логики — гвард permission); остальные фазы —
механические правки.

## Стратегия

Восемь атомарных коммитов по росту риска (ошибки → удаления → API → зависимости →
процесс), после каждой фазы — `make format && make lint && make test`
(+ `make android-test` для `reminder/` и Compose-правок). Все фазы выполнены.

---

## Фаза 0 — Baseline (отдельного коммита нет) ✅

- [x] Baseline записан: тесты **482/482**; androidTest **107** (105 + 2 flavor-гейта); lint в обоих flavor — **2 errors / 63 warnings / 1 hint**; компилятор — 1 варнинг `UNUSED_EXPRESSION`

---

## Фаза 1 — Ошибки lint: `reminder/` ✅

- [x] TDD: androidTest `onReceive_whenPostNotificationsPermissionNotGranted_thenDoesNotPostNotification` написан первым (контракт: без permission система всё равно молча отбрасывает уведомление)
- [x] `ReminderAlarmReceiver`: гвард перед `.notify(...)` — прямой `ContextCompat.checkSelfPermission` (extension `hasPostNotificationsPermission` не подошёл: лежит в `ui/screens/createedit/`, линт межпроцедурно extension не видит); попутный `InlinedApi` подавлен `@SuppressLint` с обоснованием
- [x] `ExactAlarmPermissionHelper`: оба гварда через private-предикат `@ChecksSdkIntAtLeast(parameter = 0, api = S) atLeastS(sdk)` — `NewApi` и попутный `InlinedApi` ушли, инжектируемый `sdkInt` сохранён

**Верификация:** тесты зелёные (482/482; androidTest 108: 106 + 2, включая новый
permission-тест); lint — **0 errors, exit 0**. Коммит «Исправление lint-ошибок
в напоминаниях».

---

## Фаза 2 — Безопасные удаления ресурсов и манифеста ✅

- [x] `UnusedResources` (36): перекрёстная `rg`-проверка по `app/`, `screenshot-tests/`, `fastlane/` — все мертвы; удалены 10 цветов (tag-цвета остались), 10 dimen, 13 строк + 3 plurals в обеих локалях (лейбл лаунчера — `app_display_name`, не `app_name`); удаления регекспом — ElementTree терял XML-комментарии
- [x] `ObsoleteSdkInt`: `mipmap-anydpi-v26` → `mipmap-anydpi` (12 XML, minSdk = 26). Готча: после `git mv` сбрасывать `app/build/intermediates/{incremental/merge*,merged_res}` обоих flavor — иначе mergeResources стартует с протухшего состояния и AAPT «не находит» ресурсы; недостижимый гвард `SDK_INT < O` удалён
- [x] `ScopedStorage` (переработано): `maxSdkVersion` (32 и 33) линт не устроил — WRITE/READ_EXTERNAL_STORAGE удалены из debug-манифеста целиком: screengrab-права живут в манифесте `screenshot-tests` (мержится в test-APK)
- [x] `UnusedAttribute`: `tools:targetApi="tiramisu"` на `<application>`; `TypographyEllipsis`: «...» → «…» в `loading` обеих локалей

**Верификация:** тесты зелёные; все пять категорий = 0 в обоих flavor. Коммит
«Удаление неиспользуемых ресурсов».

---

## Фаза 3 — Мелкие механические фиксы кода ✅

- [x] `NoopAnalyticsProvider`: тело `Unit` → пустой блок с комментарием; `DetailContent`: `mutableStateOf(0)` → `mutableIntStateOf`; `MoreScreen`: `Uri.parse` → `toUri` ×2 (`android.net.Uri` остался для `Uri.encode`)

**Верификация:** тесты зелёные; компилятор после clean — 0 варнингов, 0 hints.
Коммит «Мелкие исправления по замечаниям линтера».

---

## Фаза 4 — Оставшиеся `InlinedApi` ✅

- [x] `@SuppressLint("InlinedApi")` + комментарии-обоснования в `CreateEditFormContent` (ветка `REQUEST_PERMISSION`) и `hasPostNotificationsPermission` (на API < 33 гвард выше вернёт true); альтернативы без подавления нет — константа инлайнится компилятором, `@RequiresApi` неприменим

**Верификация:** тесты зелёные; `InlinedApi` = 0 — всего 15 warnings. Коммит
«Подавление InlinedApi с обоснованием».

---

## Фаза 5 — `Modifier` первым опциональным параметром ✅

- [x] `DetailContentInner`, `ReadSectionView` — `modifier` переставлен первым опциональным; повторный `rg` подтвердил: все 19 call-sites именованные
- [x] `ReminderSettingsSection` — вариант А: корневой `modifier: Modifier = Modifier` добавлен первым опциональным, секция обёрнута в `Column(modifier)` (визуально идентично: спейсеры на стороне вызывающего); `expandedContentModifier` сохранён — модифицирует расширенный контент, не корень
- [x] `ReminderExpandedContent` — вариант Б: его `Column` и есть корень функции, параметр переименован в `modifier` (приватная функция; `bringIntoViewRequester` попадает в тот же узел)

**Верификация:** тесты зелёные; `ModifierParameter` = 0 в обоих flavor. Коммит
«Modifier первым опциональным параметром».

---

## Фаза 6 — `StaticFieldLeak` ✅

- [x] `AppDataScreenViewModel`: `context: Context` → `context: Application` (используется только `getString`, factory уже передаёт `application`); прямых конструкций VM в тестах нет — проверено `rg` по `app/src/test` и `app/src/androidTest`

**Верификация:** тесты зелёные; `StaticFieldLeak` = 0. Коммит «Типизация
контекста у AppDataScreenViewModel».

---

## Фаза 7 — Зависимости ✅

### 7.1 AGP `9.4.0` → `9.4.1` ✅

- [x] `gradle/libs.versions.toml` обновлён; clean assemble, JVM (482/482), androidTest (106 + 2) зелёные; `AndroidGradlePluginVersion` = 0

### 7.2 `OldTargetApi`: `targetSdk = 36` → `37` ✅ (коммит после Фазы 8)

- [x] Образ `android-37.2` (gphone16k) установлен, AVD с API 37 создан
- [x] `targetSdk = 37`: `OldTargetApi` закрыт → lint **0 errors / 7 warnings / 0 hints** (VectorPath)
- [x] `make test` 482/482; `make android-test` на API 36 — 108: 106 + 2 flavor-гейта;
      на API 37 — 45/108: не-UI (DAO, миграции, receiver'ы, интеграция) зелёные,
      63 UI-теста падают в `InputManagerEventInjectionStrategy` — апстрим-баг
      androidx.test/espresso (метод `InputManager.getInstance` выпилен из API 37;
      открытый issue wix/Detox#4967, фикса в релизах нет — espresso 3.7.0 последний)
- [ ] Ручной smoke на API 37 (создание события, напоминание, backup, смена иконки) —
      после фикса espresso либо руками. Рисков API 37 в коде не найдено:
      ориентация/resizability на sw≥600dp — в манифесте ограничений нет; ECH/CT —
      только update-check github-флейвора, прозрачны; SMS/Bluetooth/аудио/контакты/
      RemoteViews/BAL — не используются

**Верификация:** тесты зелёные; AGP-варнинг = 0. Коммит «Обновление версии AGP».

---

## Фаза 8 — Процесс: lint в make + документация ✅

- [x] `Makefile`: цель `lint-android` (`./gradlew lintGithubDebug lintRustoreDebug`) включена в prereq-цепочку `make lint`; `.PHONY` дополнен; `AGENTS.md` синхронизирован

**Верификация:** `make lint` гоняет Android Lint (~30 с) + ktlint + detekt +
markdownlint — зелёный. Коммит «Добавление Android Lint в make lint».

---

## Финальная верификация (отдельного коммита нет) ✅

- [x] `./gradlew clean compileGithubDebugKotlin --warning-mode all` — **0 варнингов** (`w:`-строк нет)
- [x] Lint обоих flavor — 0 errors, 0 hints; осознанный остаток: `VectorPath: 7` (длинные пути иконок от дизайнера — уменьшение precision рискует качеством; при желании svg-оптимизатор отдельной задачей). `OldTargetApi` закрыт в 7.2 — итоговые 7 warnings
- [x] `make format && make lint && make test` (482/482) и `make android-test` (108: 106 + 2 flavor-гейта) — зелёные

---

## Сводка по коммитам

| # | Категория | Коммит | Изменения | Риск | Статус |
|---|---|---|---|---|---|
| 1 | Ошибки lint | «Исправление lint-ошибок в напоминаниях» | 3 файла + androidTest | средний (TDD) | ✅ |
| 2 | Удаление | «Удаление неиспользуемых ресурсов» | ~19 файлов | низкий | ✅ |
| 3 | Мелкие фиксы | «Мелкие исправления по замечаниям линтера» | 3 файла | низкий | ✅ |
| 4 | Подавления | «Подавление InlinedApi с обоснованием» | 2 файла | низкий | ✅ |
| 5 | Рефакторинг | «Modifier первым опциональным параметром» | 2 файла, 19 call-sites | средний (API) | ✅ |
| 6 | Рефакторинг | «Типизация контекста у AppDataScreenViewModel» | 1 файл | низкий | ✅ |
| 7 | Зависимости | «Обновление версии AGP» | 1 файл | средний | ✅ |
| 8 | Зависимости | «Повышение targetSdk с 36 до 37» | 1 файл + план | высокий | ✅ (выполнен после основной восьмёрки: образ `android-37.2` установлен, API 36 — 106+2, API 37 — 45/108 без UI, рисков в коде нет) |
| 9 | Процесс | «Добавление Android Lint в make lint» | Makefile + docs | низкий | ✅ |

Итог: **0 ошибок lint, 0 варнингов компилятора, 0 hints**; lint — осознанный
остаток `VectorPath: 7`. Было: 2 errors / 63 warnings / 1 hint / 1 компилятор.
Стало: **0 errors / 7 warnings / 0 hints / 0 компилятор**.

Коммиты после основной восьмёрки: `02f1f5a` (detekt `maxIssues` 10 → 5 + правило
в AGENTS.md/openspec), `e46c130` (чистка detekt: 5 → 1 — LongMethod,
TooManyFunctions, ReturnCount ×2; осознанный остаток — широкий catch в
`ExactAlarmPermissionViewModel`), `779ea5d` (targetSdk 37, см. строку 8).
