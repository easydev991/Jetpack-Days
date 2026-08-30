# План: внедрение product flavors (rustore, github)

> **Контекст.** Сейчас одна сборка (`release`) обслуживает два канала: AAB для RuStore и APK для GitHub. Кнопка «Оценить приложение» ведёт на `apps.rustore.ru/.../reviews` — для APK на GitHub это бесполезно. Кнопка «Поделиться приложением» тоже ведёт на RuStore. Различий становится слишком много для одного buildType → переходим на product flavors.
>
> **Источники требований:**
> - RuStore API: <https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app> — публикация через API (используется в Этапе 6)

## Цели

- Разделить сборки на 2 flavor'а: `rustore`, `github`
- Спрятать кнопку «Оценить приложение» в APK (github)
- Спрятать кнопку «Поделиться приложением» в github (там ведёт на RuStore, который для APK-релиза в GitHub бесполезен)
- Сохранить работоспособность существующих unit-тестов, линта, скриншот-pipeline

## Релизный процесс после flavors

### Команды сборки

| Команда | Артефакт | Назначение |
|---------|----------|------------|
| `make rustore` | `dayscounter{VERSION_CODE}.aab` | Сборка AAB + загрузка в RuStore |
| `make apk FLAVOR=<flavor>` | `dayscounter{VERSION_CODE}.apk` | Сборка APK (по умолчанию `FLAVOR=github`) |
| `make build FLAVOR=<flavor>` | debug APK | Собрать debug APK (по умолчанию `FLAVOR=github`) |
| `make install FLAVOR=<flavor>` | — | Установить debug APK (по умолчанию `FLAVOR=github`) |
| `make test FLAVOR=<flavor>` | — | Unit-тесты (по умолчанию `FLAVOR=github`) |
| `make android-test FLAVOR=<flavor>` | — | UI-тесты (по умолчанию `FLAVOR=github`) |

Столбец «Назначение» — краткое; подробный поток — в таблице статусов автоматизации (раздел «Полная карта автоматизации релизов» ниже).

**Изменения для пользователя:** команда `make release` **удаляется**; `make apk` сохраняется (имя описывает артефакт, а не канал); добавляется `make rustore` для канала с автопубликацией. CI/workflow-файлы в проекте сейчас не используются (`.github/workflows/` отсутствует), поэтому единственная правка, которая нужна — в самом `Makefile`.

**Установка на эмулятор/устройство из разных flavors**: `make install FLAVOR=rustore`, `make install FLAVOR=github`. По умолчанию `FLAVOR=github` — канал разработки (без зависимостей RuStore); `FLAVOR=rustore` — продакшен-канал. Для запуска поверх установки использовать стандартный `adb shell am start` или лаунчер IDE.

### Версионирование

`VERSION_CODE` в `gradle.properties` — **един для обоих flavor'ов**. Увеличивается через `make rustore` перед каждым релизом.

**Hotfix-сценарий для GitHub-only:** если релиз идёт только в GitHub Releases (без `make rustore`), `VERSION_CODE` не вырастет автоматически — вручную поднять значение в `gradle.properties` перед запуском `make apk FLAVOR=github`. Без этого APK-артефакт получит тот же build-номер, что и предыдущий релиз. RuStore-ограничение на рост версии это не затрагивает (ограничение про AAB, не про GitHub).

## Хранение метаданных в проекте

Чтобы при релизе не заполнять руками одни и те же данные в двух местах, метаданные должны храниться в проекте и использоваться для всех каналов (описания приложения — отложены до первого фактического заполнения консоли, см. Этап 5).

Целевая структура файлов и шаги по её созданию описаны в **Этапе 5** (метаданные для публикации, fastlane). Этот раздел фиксирует только стратегические принципы; деревья каталогов и команды копирования не дублируются, чтобы избежать рассинхронизации.

### Что уже есть

- `fastlane/metadata/android/ru-RU/images/phoneScreenshots/*.png` — скриншоты для русской локали
- `fastlane/metadata/android/en-US/images/phoneScreenshots/*.png` — скриншоты для английской локали

### Источник истины

Единый источник для обоих каналов — `fastlane/metadata/android/<locale>/` (контракт файлов и путь `whats_new/<VERSION_NAME>.txt` — см. Этап 5).

## Полная карта автоматизации релизов

| Канал | Сборка | Публикация | Текущий статус | Целевой статус |
|-------|--------|------------|----------------|----------------|
| RuStore — AAB + release notes + модерация | `make rustore` | `make rustore` (через `scripts/rustore_publish.sh`) | **Автоматическая** (`make rustore` запускает `scripts/rustore_publish.sh` после gradle, 4 шага: auth → create-draft → upload → submit) | ✅ Достигнуто |
| RuStore — описание/иконка/скриншоты | — | Ручная через консоль | Ручная | **Остаётся ручной** (см. Этап 6 «Что НЕ покрывает») |
| RuStore — публикация после модерации | — | Ручная через консоль (для `publishType=MANUAL`) | Ручная | **Остаётся ручной** (by design — контроль над моментом релиза) |
| GitHub Releases | `make apk FLAVOR=github` | Ручная загрузка APK в GitHub Release | Ручная | **Остаётся ручной** (см. out-of-scope: автоматизация `gh release create` не блокирует flavors) |

## Архитектурные решения

### 1. Один `flavorDimension = "distribution"`, два flavor'а

| Flavor | Канал | AAB/APK | Firebase | RuStore SDK | Кнопки |
|--------|-------|---------|----------|-------------|--------|
| `rustore` | RuStore | AAB | ✅ единый | ✅ (будущее) | Оценить, Поделиться |
| `github` | GitHub Releases | APK | ✅ единый | ❌ | ❌ обе |

**Firebase:** один общий проект `days-counter-5ee1f` для обоих flavor'ов (файл `app/google-services.json` копируется из `.secrets/` через `_load_secrets` — см. §3 «google-services.json» в «Архитектурные решения» ниже).

### 2. `BuildConfig` — единое место для переключателей

| Поле | Тип | rustore | github |
|------|-----|---------|--------|
| `RUSTORE_FEATURES` | `Boolean` | `true` | `false` |

Один общий флаг достаточен: обе кнопки «Оценить» и «Поделиться» (MoreScreen.kt:126-142) ведут на `apps.rustore.ru` (AppConstants.kt:6,9) и одинаково условные. `FIREBASE_ENABLED` не нужен — Firebase SDK остаётся глобальной зависимостью для обоих flavor'ов (текущее поведение).

### 3. `google-services.json` — модель хранения

google-services plugin 4.5.0 (`gradle/libs.versions.toml:26`) **не предоставляет DSL** для указания произвольного пути к JSON: свойство `googleServicesJson` отсутствует в `BaseFlavor`/`ApplicationBaseFlavor`/`Variant` (проверено декомпиляцией `gradle-api-9.3.2.jar`). Плагин читает файл строго из `app/google-services.json` (модульный корень). Поэтому единственный путь — положить файл в `app/` перед запуском Gradle. **Почему один файл на оба flavor'а:** в проекте одно приложение (`applicationId = "com.dayscounter"`) и один Firebase Console (`days-counter-5ee1f`, `app/google-services.json:4`). Один файл означает общую аналитику, audiences и rate-limit для rustore и github — для проекта с одним продуктом это нормально, разделение по каналам не нужно.

**Стратегия защиты от утечки секретов:**
- `google-services.json` содержит `project_id`, `api_key` и другие Firebase-идентификаторы — не коммитится в репозиторий. Запись `.gitignore:60` (`app/google-services.json`) **остаётся** — файл не должен попасть в git даже случайно.
- Попадает в `.secrets/google-services.json` через цель `_load_secrets` — клонирует приватный репозиторий `easydev991/android-secrets` (`SECRETS_REPO`, Makefile:14, SSH `git@github.com:easydev991/android-secrets.git`) во временную директорию, затем `cp -r "$$TEMP_DIR/jetpackdays/*" .secrets/` копирует все файлы из `jetpackdays/` в `.secrets/`. Двойной `cp -r ... || cp -r ...` (с/без trailing slash) — защита от пустой директории, не от extra-файлов. Для добавления новых секретных файлов в JetpackDays правок в `_load_secrets` **не требуется**: `cp -r` подхватывает всё, что лежит в `jetpackdays/`.
- **Честный регресс:** после этого изменения даже debug-сборка требует наличия `.secrets/` — т.е. разового `make setup` или запуска любой gradle-цели, у которой `_ensure_secrets` в prerequisite. Раньше debug можно было собрать без секретов (плагин в debug не валидировал наличие файла). Без `.secrets/` Gradle получит понятную ошибку «google-services.json not found». Это сделано намеренно: единая модель для debug и release.

**Реализация** (команды и prerequisite-обвязка — не здесь, чтобы не дублировать шаги из этапов): копирование файла добавляется одной строкой в существующую цель `_load_secrets` (Этап 1), prerequisite-обвязка через `_ensure_secrets` — в Этапе 3. Плагин `google-services` применяется глобально — firebase SDK в classpath обоих flavor'ов.

---

## Этапы реализации

### Этап 1. Gradle: конфигурация flavors

> **Зависимости:** нет.

- [x] **Flavors + google-services.json:** `app/build.gradle.kts` — `flavorDimensions + 2 productFlavors` с `BuildConfig.RUSTORE_FEATURES`; `Makefile:_load_secrets` копирует `app/google-services.json` из `.secrets/` (файл перенесён в `android-secrets/jetpackdays/`). 4 assemble-цели (`rustoreDebug/Release`, `githubDebug/Release`) собираются зелёно.

### Этап 2. UI: MoreScreen.kt + тесты на flavor'ах

> **Зависимости:** Этап 1 (нужны `BuildConfig.*` поля).
>
> **TDD: пишем тест ДО UI-правки.** `BuildConfig.RUSTORE_FEATURES → видимость rate/share кнопок` — проверяемая логика. Сначала — UI-тест на условную видимость, потом — правка UI. Это исключение из общего правила «UI-правки без тестов»: для conditional-render по `BuildConfig.*` TDD применимо и нужно. Исключения — только для тривиальных визуальных правок (отступы, цвета), не для conditional-render.

- [x] **MoreScreen + тесты:** rate/share в одном `if (BuildConfig.RUSTORE_FEATURES)`; `Assume.assumeTrue/assumeFalse` для flavor'ов в `MoreScreenTest.kt`.

### Этап 3. Makefile: новые цели и переименование

> **Зависимости:** Этап 1 (нужно знать имена задач uploadCrashlyticsMappingFile).
>
> **Главное правило именования:** для **автоматизированных** каналов имя команды = канал (`make rustore` — собирает AAB и сам загружает в RuStore через bash-скрипт из Этапа 6). Для каналов с **ручной** публикацией имя описывает артефакт, который делает команда (`make apk` — собирает APK, GitHub Release публикуется руками). Прежняя `make release` теряет смысл после введения flavors (одна команда для двух разных артефактов и разных pipeline'ов); `make apk` сохраняется — имя точно описывает результат.
>
> **`_ensure_secrets` как prerequisite для 11 gradle-целей** (`build`, `install`, `test`, `android-test`, `_build_screenshots_apk`, `screenshots*`, `rustore`, `apk`, `_rustore_build_aab`): `processGoogleServices` читает `app/google-services.json`, `signingConfigs` тянет `secrets.properties`. Этап 6 не добавляет новую цель — встраивает `rustore_publish.sh` в существующую `rustore`. `check` получает секреты транзитивно через `build`; `clean`/`format`/`lint`/`update_readme_versions` — намеренно без них (Ponytail: YAGNI — `make format` не должен требовать SSH).

- [x] **`release` → `rustore` + `apk` per-flavor; `FLAVOR ?= github` + guard; `_ensure_secrets` инлайнен в 11 prereq-листов; `scripts/android_test_report.py` параметризован под `<variant>`; `screenshot-tests/build.gradle.kts` — `missingDimensionStrategy("distribution", "rustore")`.** Inline-`_load_secrets` в `apk`/`release` удалены.

### Этап 4. Документация

> **Зависимости:** Этапы 1, 3 (Этап 1 вводит `productFlavors` в `app/build.gradle.kts` — это даёт терминологию для раздела «Каналы дистрибуции» в `docs/deployment.md`; Этап 3 вводит `make rustore`/`make apk FLAVOR=github` и `_ensure_secrets` — это требует синхронизации `AGENTS.md` и `firebase_integration.md`; `GitHub_Release_Automation_Plan.md` больше не затрагивается — имя `apk` сохраняется).

- [x] **Документация:** `docs/deployment.md` (Каналы дистрибуции, `make release` → `make rustore`), `docs/firebase_integration.md` (`google-services.json` под `_load_secrets`, плагины `4.5.0`/`3.0.8`), `README.md`, `AGENTS.md`. `tech-stack.mdc` намеренно не тронут (описывает стек, не сборку).

### Этап 5. Метаданные для публикации (fastlane)

> **Зависимости:** нет (этап только создаёт файлы-метаданные; команда `make rustore` использует эти файлы на Этапе 6; для `make apk FLAVOR=github` файлы не нужны — APK загружается в GitHub Release вручную через консоль).
>
> **Цель:** все описания, скриншоты и release notes должны храниться в проекте как **источник истины** для документации, скриншот-pipeline и ручной загрузки описания в RuStore Console (описания приложения — отложены до первого фактического заполнения консоли, см. чек-бокс ниже). Bash-скрипт публикации RuStore (Этап 6) использует из этой структуры только `whats_new/<VERSION>.txt` для release notes — описание/иконка/скриншоты остаются ручным процессом через консоль (см. Этап 6 «Что НЕ покрывает»).

- [ ] **Создать описания приложения как ручной справочник** — отложено (YAGNI). Bash-скрипт публикации (Этап 6) загружает только AAB и release notes; описания загружаются руками через консоль (отдельные методы API, не покрыты скриптом). **Описания уже заполнены через RuStore Console** (предыдущие ручные релизы) — создание файлов в `fastlane/metadata/` создаст двойной источник истины (Console + git) и потребует постоянной синхронизации. Никакая автоматизация эти файлы сейчас не читает. Триггер для создания: первая реальная необходимость заполнить описание в RuStore **через git** (например, генерация описания из release notes автоматически или добавление второго канала типа Google Play). Тогда — структура (4 файла):
  - `fastlane/metadata/android/ru-RU/short_description.txt` — короткое описание (до 80 символов).
  - `fastlane/metadata/android/ru-RU/full_description.txt` — полное описание (до 4000 символов). Markdown допустим.
  - Те же 2 файла для `fastlane/metadata/android/en-US/`.
- [x] **Release notes:** шаблон `TEMPLATE-whats-new.md` (markdown, категории Исправлено/Добавлено/Изменено); контракт `whats_new/<VERSION_NAME>.txt` (из `gradle.properties` без префикса `v`; `ru-RU` обязательна, `en-US` опциональна); коммитятся в репозиторий.

### Этап 6. Публикация в RuStore через bash + curl + openssl + jq

> **Зависимости:** Этапы 3, 5 (Этап 3 вводит `make rustore`, в который встраивается вызов bash-скрипта; Этап 5 фиксирует шаблон, путь и схему имени `whats_new/<VERSION_NAME>.txt` — сам файл пишется руками перед релизом по шаблону из Этапа 5, см. Этап 7).
>
> **Цель:** `make rustore` не только собирает AAB, но и автоматически загружает его в RuStore с release notes и отправляет на модерацию.
>
> **Подход:** прямые HTTP-вызовы к RuStore API через `bash + curl + openssl + jq`. Весь контракт API документирован RuStore — auth → create draft → upload AAB → submit. Реализация — bash-скрипт `scripts/rustore_publish.sh` (три позиционных аргумента: `<credentials.json> <app.aab> <priority>`). Зависимости Этапа 5 выполнены (шаблон release notes, контракт `whats_new/<VERSION_NAME>.txt`); Этап 6 — следующий в очереди.

#### Почему bash, а не Gradle-плагин поверх HTTP

**bash + curl + jq = ноль зависимостей от Gradle Variant API и Maven Central, видимость каждого HTTP-запроса, читается как обычный скрипт.** Утилиты (`openssl`, `curl`, `jq`) есть в macOS и стандартных CI-образах; `priorityUpdate` (принимается как позиционный аргумент скрипта) и `APP_ID` хардкодятся в скрипте одной строкой (DSL-обёртки часто хардкодят, но прячут). `SEO_TAG_IDS`/`developerContacts`/`minAndroidVersion` намеренно **не** хардкодятся — `seoTagIds`/`developerContacts` уже заполнены в RuStore Console (многократные ручные релизы), `minAndroidVersion` берётся из AAB (`minSdk` в `app/build.gradle.kts`).

#### Что покрывает bash-скрипт

Скрипт последовательно вызывает 4 метода RuStore API. Ошибка на любом шаге завершает работу (fail-fast), черновик при сбое между шагами 2 и 4 удаляется руками через Console или `DELETE /public/v1/application/com.dayscounter/version/{vid}`.

1. **[Авторизация](https://www.rustore.ru/help/work-with-rustore-api/api-authorization-process)** — `POST /public/auth/` с `{keyId, timestamp, signature}`, где signature = `base64(SHA512withRSA(private_key, keyId+timestamp))`. Возвращает `body.jwe` (JWE-токен, TTL 900 с). **Base host: `https://public-api.rustore.ru` — НЕ `www.rustore.ru`.** Документация лежит на `www.rustore.ru/help/...`, а API — на отдельном домене `public-api.rustore.ru`. На `www.rustore.ru/public/auth/` kittenx возвращает 502 Bad Gateway (подтверждено sanity check 30.08.2026). **Формат `timestamp`:** ISO 8601 с timezone offset `+HH:MM`, например `2026-08-30T09:37:29+00:00`. Unix epoch (`1788082638`) и ISO с суффиксом `Z` (`...24.3NZ`) отвергаются сервером с `BAD_REQUEST`. Сообщение для подписи — конкатенация `${keyId}${timestamp}` без разделителя.
2. **[Создание черновика](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/create-draft-version)** — `POST /public/v1/application/{id}/version` с минимальным payload `{whatsNew, publishType:"MANUAL", appType:"MAIN"}` в header `Public-Token: <jwe>`. Возвращает `versionId` в `body` (число). Поле `minAndroidVersion` в API отсутствует — минимальную версию ОС RuStore берёт из загруженного AAB (`minSdk` берётся из `app/build.gradle.kts`). **Остальные поля (`seoTagIds`, `developerContacts`, `shortDescription`, `fullDescription`, `categories`, `ageLegal`) не передаются** — в Console они уже заполнены (многократные ручные релизы), передача поверх создаст дубль или перезапишет Console-значения; пользователь явно заполняет в Console только `что нового` и прикладывает AAB. **Sanity check 30.08.2026: payload `{appType:"MAIN", publishType:"MANUAL", whatsNew:"sanity-check test"}` → HTTP 200, `body:2064793645`. Два последовательных `create-draft` подряд → HTTP 200 на оба (vid 2064793662 и 2064793663), конфликта не возникает — RuStore разрешает несколько черновиков.** **Тот же base host `https://public-api.rustore.ru`.**
3. **[Загрузка AAB](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/apk-file-upload/file-upload-aab)** — `POST /public/v1/application/{id}/version/{vid}/aab` multipart с `file` и **явным `Content-Type: application/octet-stream`** для файла. Без явного типа RuStore интерпретирует файл как APK — сохраняет в «главный APK» слот, commit падает с `There can be only one main APK file`, в Console файл не виден как AAB.
4. **[Отправка на модерацию](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/send-draft-app-for-moderation)** — `POST /public/v1/application/{id}/version/{vid}/commit?priorityUpdate={0-5}` без body. **Параметр `priorityUpdate`** передаётся в URL — по умолчанию `0` (обычное обновление). Для редких случаев «критический багфикс» — изменить аргумент скрипта при запуске (запускать руками, не через `make rustore`).

> **Обработка 409 Conflict не реализована** — sanity check 30.08.2026 подтвердил, что RuStore разрешает несколько черновиков одновременно для одного приложения, 409 на `create-draft` не возникает. Если в будущем API изменится и 409 появится — добавить retry-логику (DELETE существующего + повтор `create-draft`); на сегодня это over-engineering.

#### Защита от низкого VERSION_CODE

Перед `create-draft` скрипт запрашивает `GET /public/v1/application/{id}/version?page=0&size=100` и проверяет что локальный `VERSION_CODE` из `gradle.properties` строго выше `max` существующих. Если нет — `exit 1` с понятным сообщением до того, как RuStore успеет создать «мусорный» черновик. Без проверки RuStore принимает `create-draft` (HTTP 200), но `upload AAB` падает с HTTP 400 — в Console остаётся пустой черновик без файла, который нельзя ни удалить (`DELETE` работает только для `DRAFT` без файла, для заполненных — ошибка), ни откатить. Покрыто тестом `test_low_version_code_fails_before_create_draft`.

После модерации (`publishType = MANUAL`) разработчик публикует версию вручную через RuStore Console.

#### Что НЕ покрывает (ручные шаги через консоль)

- **Описание приложения** (`fullDescription`, `shortDescription`) — отдельные методы API. Загружаются руками через RuStore Console. Для первого релиза нужно заполнить через консоль; для последующих — остаются как есть.
- **Иконка приложения** — отдельный метод API ("Загрузка иконки приложения"). Управляется через `app/src/main/res/mipmap-*/ic_launcher.*` (обновляется через AAB при следующей загрузке).
- **Скриншоты** — отдельный метод API. Управляются через `fastlane/metadata/android/<locale>/images/phoneScreenshots/` и `make screenshots`.

#### Шаги реализации

- [x] **`scripts/rustore_publish.sh` + интеграция:** 4-шаговый bash (auth → draft → upload → submit, fail-fast, `set +x`/`umask 077`); `make rustore` вызывает после `bundleRustoreRelease + uploadCrashlyticsMappingFileRustoreRelease` (`SKIP_PUBLISH=1` escape hatch); `rustore-credentials.json` подхватывается через `_load_secrets`.

### Этап 7. Генерация release notes из git log + разделение `make rustore` на draft/commit

> **Зависимости:** Этап 5 (контракт `whats_new/<VERSION>.txt`, шаблон). Активация отложенного 7 после реального юзкейса: ручной `make rustore` отправляет релиз сразу на модерацию — пользователь хочет видеть release notes до этого.
>
> **Цель:** дать явный двухшаговый workflow — сначала создать черновик (с AAB и сгенерированными release notes), потом вручную отправить на модерацию после проверки в Console. Генерация `whats_new/<VERSION>.txt` из `git log<last_release_tag>..HEAD` — автогенератор для типового случая, ручная правка перед commit остаётся.
>
> **TDD:** тесты пишутся первыми (red), затем реализация (green). Helper `scripts/_generate_whats_new.sh` отделён от Makefile для unit-тестирования.

#### Шаги реализации

- [x] **Helper `_generate_whats_new.sh` + Makefile targets + `RUSTORE_MODE`:** helper создаёт `whats_new/<VERSION>.txt` из `git log<last_tag>..HEAD` (без тегов — пометка `(первый релиз)`; если файл есть — печатает без перезаписи + trailing newline). Цели: `whats-new`, `rustore-draft` (build AAB + `RUSTORE_MODE=upload`, VID → `.secrets/.last_rustore_vid`), `rustore-commit VID=<vid>` (`RUSTORE_MODE=commit`); `_rustore_build_aab` — общий build для `rustore`/`rustore-draft`. `make rustore` получил prerequisite `whats-new`. Upload AAB: `--max-time 600`, прогресс-бар через `curl -f#S` (ранее был polling-цикл в Makefile, удалён в Follow-up review).
- [x] **Тесты (10 по TDD):** `whats_new_test.py` (3), `RustorePublishUploadModeTest`/`CommitModeTest` (3), `MakefileWhatsNewAndDraftTest` (4). Всего Python-тестов: 18 (было 8).

### Этап 8. Финальная проверка

> **Зависимости:** все предыдущие.

- [x] **`make format`/`test` зелёные:** ktlint/detekt/markdownlint без issues (после pre-existing fix MD051); 22 unit-тест-класса + Python-тесты, нулевые failures.
- [x] **Smoke-test + code review:** `bundleRustoreRelease` (33s, AAB 6.3M) и `assembleGithubRelease` (31s, APK 3.0M) — **раздельно** (4 задачи параллельно падали по OOM); `uploadCrashlyticsMappingFileRustoreRelease` загрузил mapping v20 в Firebase. Без `!!` в новом коде, KDoc на `ActionButtons` есть, нет deprecated APIs.

---

## Зависимости между этапами

Зависимости указаны в текстовых «> Зависимости:» в каждом этапе. Компактная таблица вместо ascii-дерева — без визуальной вложенности, которая раньше читалась как «5/6/7 зависят от 4»:

| Этап | Зависит от          | Статус              |
|------|---------------------|---------------------|
| 1    | —                   | ✅ Выполнен         |
| 2    | 1                   | ✅ Выполнен         |
| 3    | 1                   | ✅ Выполнен         |
| 4    | 1, 3                | ✅ Выполнен         |
| 5    | —                   | ✅ Выполнен (только шаблон; описания приложения — отложены, YAGNI) |
| 6    | 3, 5                | ✅ Выполнен         |
| 7    | 5 *(deferred → active)* | ✅ Выполнен (helper + targets + 10 тестов по TDD) |
| 8    | 1, 2, 3, 4, 5, 6    | ✅ Выполнен (smoke-test; команды release раздельно из-за OOM-риска) |

## Референсы

### Документация RuStore API

- [Загрузка и публикация приложений с помощью API RuStore](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app) — общий обзор процесса публикации через API
- [Создание черновика версии](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/create-draft-version) — `POST /application/{packageName}/version`, возвращает `versionId`; описание полей `whatsNew`, `shortDescription`, `fullDescription`
- [Удаление черновика версии](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app) — `DELETE /application/{packageName}/version/{versionId}`, без body.
- [Загрузка AAB-файла](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/apk-file-upload/file-upload-aab) — `POST /application/{packageName}/version/{versionId}/aab`, multipart upload; **ограничения**: только `.aab`, до 5 Гб, версия должна быть выше текущей активной, **требует предварительной загрузки подписи AAB в RuStore Console**
- [Отправка на модерацию черновика](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/send-draft-app-for-moderation) — `POST /application/{packageName}/version/{versionId}/commit?priorityUpdate={0-5}`, без body
- [Авторизация в RuStore API](https://www.rustore.ru/help/work-with-rustore-api/api-authorization-process) — описание процесса генерации ключевой пары и подписи запросов; сами `KEY_ID` и `CLIENT_SECRET` получают в RuStore Консоли (`console.rustore.ru`) → API RuStore → Создать ключ

### Инструменты публикации в RuStore

Публикация — bash-скрипт `scripts/rustore_publish.sh`, прямые HTTP-вызовы к API: [auth](https://www.rustore.ru/help/work-with-rustore-api/api-authorization-process) → [create draft](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/create-draft-version) → [upload AAB](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/apk-file-upload/file-upload-aab) → [submit](https://www.rustore.ru/help/work-with-rustore-api/api-upload-publication-app/send-draft-app-for-moderation). Каждый шаг подробно расписан в Этапе 6; здесь — только ссылки.

**Хранение секретов для публикации** — приватный репозиторий [`easydev991/android-secrets`](https://github.com/easydev991/android-secrets) (SSH: `git@github.com:easydev991/android-secrets.git`, см. `Makefile:14`). Содержит для JetpackDays:

| Файл | Назначение |
|---|---|
| `jetpackdays/keystore/dayscounter-release.keystore` | Upload keystore — Gradle использует при `bundleRustoreRelease` |
| `jetpackdays/secrets.properties` | Пароли keystore (`KEYSTORE_PASSWORD`, `KEYSTORE_FILE`, `KEY_ALIAS`, `KEY_PASSWORD`); `_load_secrets` патчит `KEYSTORE_FILE` через `sed` |
| `jetpackdays/google-services.json` | Firebase-конфиг (`project_id`, `api_key` и др.) — общий для обоих flavor'ов, копируется в `app/google-services.json` через `_load_secrets` (Этап 1) |
| `jetpackdays/certificates/pepk_out.zip` + `uploadcert.pem` | Для однократной ручной загрузки в RuStore Console → «Настройки приложения → Подписи» |
| `jetpackdays/rustore-credentials.json` (добавлен в Этапе 6 в `android-secrets`) | `key_id` + `client_secret` для RuStore API auth (отдельная RSA-пара, **не** upload keystore) |

В JetpackDays все файлы подтягиваются через цель `_load_secrets` — полное описание механизма в §3 «google-services.json» в «Архитектурные решения».

### Смежные инструменты

- [fastlane screengrab](https://docs.fastlane.tools/actions/capture_android_screenshots/) — генерация скриншотов через локализованные тесты (уже используется в проекте для `fastlane/metadata/android/<locale>/images/phoneScreenshots/`)

## Что НЕ делается в этом плане (out of scope)

- Подключение RuStore SDK (отдельная задача, добавляется после flavor-миграции)
- Добавление F-Droid как третьего flavor'а (отложено — требует manifest-merger с `tools:node="remove"`, flavor-specific NoOp-провайдеров, условного apply плагинов, `metadata/com.dayscounter.yml` для fdroiddata; инфраструктура велика для одного канала, решение принято в пользу двух flavor'ов)
- Полная автоматизация GitHub Releases через `gh release create` (текущий процесс ручной — `make apk FLAVOR=github` → загрузка в GitHub Release; можно автоматизировать, но это не блокирует flavors)
- Изменение `applicationId` в production (оставляем `com.dayscounter` для всех flavor'ов — иначе сломается совместимость данных). Допустимо: `applicationIdSuffix = ".debug"` в debug buildType для параллельной установки debug/release; отдельный `dev` buildType с suffix `.dev` не вводится (YAGNI — разовая проверка публикации не требует третьего канала).
- Вынесение Firebase в отдельный `:analytics` модуль (overkill на текущем масштабе)
- Изменение структуры `MainActivity`/DI

## Follow-up (после плана flavors)

- ✅ **Pre-existing фиксы и тесты:** `.gitignore` для `.serena/memories/`; MD051 в `docs/deployment.md:38`; smoke-test `rustore_publish.sh` (happy-path с fake curl через PATH) + баг `$2` в `make rustore` + интеграционный тест Makefile target; snake_case rename 5 `@Test` в `MoreScreenTest`.
- ✅ **Этап 7: фиксы UX `rustore-draft`:** trailing newline в `_generate_whats_new.sh`; VID → `.secrets/.last_rustore_vid` (Makefile подставляет в подсказку); upload AAB: убран `> /dev/null` для диагностики, `--max-time 600`.
- ✅ **Root cause «AAB загружен, но в Console его нет»:** без явного `Content-Type` RuStore интерпретирует AAB как APK, commit падает с `There can be only one main APK file`. Фикс: `-F "file=@$AAB_FILE;type=application/octet-stream"`. Всего Python-тестов: 19.
- ✅ **Долг: документация под Этап 7 → закрыт:** `docs/deployment.md` (TOC + «Двухшаговый workflow» + «Release notes» с `make whats-new`), `README.md:45`, `AGENTS.md:107-115`.
- ✅ **Защита от низкого VERSION_CODE:** скрипт падает ДО `create-draft` если `VERSION_CODE` из `gradle.properties` ≤ max существующих в RuStore. Без этого RuStore создавал пустой черновик (HTTP 200 на create-draft), а upload AAB падал с HTTP 400 — черновик оставался в Console без файла. Тест `test_low_version_code_fails_before_create_draft`. Всего Python-тестов: 20.
- ✅ **Over-engineering review (11 справедливых + 1 несправедливый):** чистый выигрыш **-104 строки** в 6 файлах. **Makefile**: убраны `_GRADLE_PREREQS` обёртка, `FLAVOR_LOWER` round-trip, дублирование `VERSION_CODE`/`OUTPUT_FILE` в `rustore`, trailing `0` в `rustore-commit`, watch-цикл в `rustore-draft` (заменён на `curl -f#S` прогресс-бар). **`android_test_report.py`**: убраны whitelist `variant not in (...)` и env-fallback `ANDROID_TEST_VARIANT` (никто не ставит). **Тесты**: `_ModeTestBase` поднят выше `HappyPathTest` (наследование через `self._run({})`), убран `create_draft_calls` list-comp; `_MakefileTestBase` для двух setUp'ов, убран спекулятивный `if helper_src.exists()`. **Несправедливый** (сохранён): реконструкция query-string в fake-curl для `page=` asserts — без неё asserts падают, потому что curl `-G --data-urlencode` переносит параметры в URL. Все тесты зелёные (22 Kotlin + 20 Python), lint чист.
