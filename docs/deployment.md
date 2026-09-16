# Релизный процесс

- [Секреты для подписи](#секреты-для-подписи)
- [Каналы дистрибуции](#каналы-дистрибуции)
- [Создание сборки](#создание-сборки)
  - [Релизный AAB (для публикации в RuStore)](#релизный-aab-для-публикации-в-rustore)
    - [Двухшаговый workflow (draft → commit)](#двухшаговый-workflow-draft--commit)
  - [Релизный APK (для GitHub Release)](#релизный-apk-для-github-release)
- [Управление версией](#управление-версией)
  - [Версия приложения (VERSION_NAME)](#версия-приложения-version_name)
  - [Номер сборки (VERSION_CODE)](#номер-сборки-version_code)
- [Публикация в RuStore](#публикация-в-rustore)
  - [Release notes](#release-notes)
  - [API-авторизация](#api-авторизация)
  - [Что делать при сбое публикации](#что-делать-при-сбое-публикации)
- [Скриншоты](#скриншоты)
  - [Генерация скриншотов](#генерация-скриншотов)
  - [Обновление README.md](#обновление-readmemd)

## Секреты для подписи

Секреты для подписи хранятся в отдельном приватном репозитории: `git@github.com:easydev991/android-secrets.git`

Команды `make rustore` и `make apk` автоматически загружают их по SSH во временную директорию и копируют в `.secrets/` перед сборкой (цель `_ensure_secrets` клонирует репозиторий, только если `.secrets/` или `app/google-services.json` ещё отсутствуют). Временная директория очищается автоматически.

**Требования:**
- SSH-доступ к GitHub (настраивается через `make setup_ssh`)
- Ключ добавлен в GitHub аккаунт

## Каналы дистрибуции

Проект собирается в двух `productFlavors` (`app/build.gradle.kts:36-44`) — flavor определяет канал дистрибуции и набор интегрированных фич.

| Flavor | Канал | `BuildConfig.RUSTORE_FEATURES` | Назначение |
| --- | --- | --- | --- |
| `rustore` | RuStore (основной) | `true` | Видны кнопки «Оценить приложение» и «Поделиться» в `MoreScreen`; AAB публикуется в RuStore через `make rustore`. |
| `github` | GitHub Release (альтернативный) | `false` | Те же кнопки скрыты; APK распространяется вручную через `make apk FLAVOR=github` и загружается на GitHub Release через консоль. |

Оба flavor'а используют один и тот же Firebase-проект (`days-counter-5ee1f`, Android App `com.dayscounter`) и один `app/google-services.json` (см. [google-services.json](firebase_integration.md#google-servicesjson)).

## Создание сборки

### Релизный AAB (для публикации в RuStore)

Для создания подписанной AAB-сборки выполните:

```bash
make rustore
```

Канал — `rustore` (по умолчанию). Команда автоматически:

1. Увеличивает номер сборки (`VERSION_CODE`) на 1
2. Создает подписанную AAB-сборку: `dayscounter{VERSION_CODE}.aab` (например, `dayscounter1.aab`, `dayscounter2.aab`) — flavor `rustore`
3. Загружает mapping-файлы Crashlytics в Firebase Console (для деобфускации стектрейсов крашей)
4. Копирует AAB в корень проекта: `dayscounter{N}.aab`
5. Загружает AAB в RuStore через RuStore API (см. [Публикация в RuStore](#публикация-в-rustore))

Для локальной отладки или когда нужно собрать AAB без загрузки в RuStore:

```bash
make rustore SKIP_PUBLISH=1
```

#### Двухшаговый workflow (draft → commit)

По умолчанию `make rustore` отправляет релиз сразу на модерацию. Если хочется сначала проверить release notes в RuStore Console — разделите процесс:

```bash
# 1. Сгенерировать release notes из git log (если файла ещё нет).
#    Если файл уже есть — содержимое печатается и НЕ перезаписывается.
make whats-new

# 2. Собрать AAB и создать черновик в RuStore (без модерации).
make rustore-draft
# → Найдите versionId в RuStore Console → Черновики.

# 3. Отредактируйте release notes (опционально) и/или подтвердите черновик.
$EDITOR fastlane/metadata/android/ru-RU/whats_new/<VERSION_NAME>.txt

# 4. Отправить черновик на модерацию.
make rustore-commit VID=<versionId>
```

Под капотом `make rustore-draft` вызывает `scripts/rustore_publish.sh` с `RUSTORE_MODE=upload` (auth → create-draft → upload AAB, без commit), `make rustore-commit VID=<vid>` — с `RUSTORE_MODE=commit` (только POST `/commit?priorityUpdate=0`). Найденный `versionId` скрипт дополнительно сохраняет в `.secrets/.last_rustore_vid` — `make rustore-draft` читает его и печатает готовую команду `make rustore-commit VID=...`. Полный 4-шаговый `make rustore` остаётся как shortcut для типового случая.

### Релизный APK (для GitHub Release)

Для создания подписанного APK выполните:

```bash
make apk FLAVOR=github
```

Канал — `github`. По умолчанию `make apk` использует `FLAVOR=github` (канал разработки); для RuStore-канала передавайте `FLAVOR=rustore` явно. Команда автоматически:

1. Создает подписанный APK-файл: `dayscounter{VERSION_CODE}.apk` (например, `dayscounter1.apk`, `dayscounter2.apk`) — flavor `github`
2. Отображает версию и номер сборки
3. **Не увеличивает** номер сборки (`VERSION_CODE`)

Файл попадает в `app/build/outputs/apk/github/release/app-github-release.apk`, затем копируется в корень проекта как `dayscounter{N}.apk`. После сборки APK нужно вручную загрузить на GitHub Release через консоль.

## Управление версией

### Версия приложения (VERSION_NAME)

Отображается в магазине приложений. Изменяется вручную в `gradle.properties`:

```properties
VERSION_NAME=1.0
```

Примеры версий:

- `1.0` - первый релиз
- `1.1` - минорное обновление с новыми функциями
- `2.0` - мажорное обновление с изменениями в функционале

### Номер сборки (VERSION_CODE)

Автоматически увеличивается при каждом `make rustore`. Изменения вручную не требуется.

Формат: целое число, монотонно возрастающее.

```
VERSION_CODE=1
```

Пример:

```
VERSION_NAME=1.0, VERSION_CODE=1 → make rustore → VERSION_CODE=2 → AAB: dayscounter2.aab (1.0 build 2)
VERSION_NAME=1.0, VERSION_CODE=2 → make rustore → VERSION_CODE=3 → AAB: dayscounter3.aab (1.0 build 3)
VERSION_NAME=1.1, VERSION_CODE=3 → make rustore → VERSION_CODE=4 → AAB: dayscounter4.aab (1.1 build 4)
```

**Примечание:** Номер сборки (VERSION_CODE) никогда не сбрасывается при повышении версии приложения, поэтому он всегда уникален и монотонно возрастает.

## Публикация в RuStore

Публикация происходит автоматически через `make rustore` (см. [Создание сборки](#создание-сборки)). Команда вызывает `scripts/rustore_publish.sh`, который последовательно выполняет проверку и 4 запроса к RuStore API:

1. **Авторизация** (`POST /public/auth/`) — получает JWE-токен (TTL 900 с)
2. **Проверка VERSION_CODE** (`GET /public/v1/application/{id}/version`) — сравнивает `VERSION_CODE` из `gradle.properties` с максимальным `versionCode` уже существующих версий; если он не выше — скрипт падает **до** создания черновика (иначе RuStore принял бы create-draft, но отклонил upload AAB с HTTP 400, оставив orphan-черновик)
3. **Создание черновика** (`POST /public/v1/application/{id}/version`) — payload `{whatsNew, publishType:"MANUAL", appType:"MAIN"}`, остальные поля (`seoTagIds`, `developerContacts`, описания) не передаются — они уже заполнены в RuStore Console
4. **Загрузка AAB** (`POST /public/v1/application/{id}/version/{vid}/aab`) — multipart (таймаут 600 с)
5. **Отправка на модерацию** (`POST /public/v1/application/{id}/version/{vid}/commit?priorityUpdate=0`)

Скрипт использует base host `https://public-api.rustore.ru` (документация на `www.rustore.ru/help/...`, API на отдельном домене).

### Release notes

Release notes берутся из файла `fastlane/metadata/android/ru-RU/whats_new/<VERSION_NAME>.txt` — содержимое файла отправляется в поле `whatsNew` при создании черновика.

**Генерация из git log** (опционально, перед релизом):

```bash
make whats-new
```

Команда вызывает `scripts/_generate_whats_new.sh`, который берёт последний релизный тег (`git tag --sort=-version:refname | grep -E '^[0-9]' | head -1`) и пишет в `whats_new/<VERSION_NAME>.txt` заголовок `Что нового в <VERSION>` + буллеты из `git log <tag>..HEAD`. Если файл уже существует — печатает содержимое и **не перезаписывает** (для ручной редактуры).

`make whats-new` — prerequisite для `make rustore` и `make rustore-draft`: если файла нет, он будет сгенерирован автоматически.

Шаблон для ручной редактуры: [`fastlane/metadata/TEMPLATE-whats-new.md`](../fastlane/metadata/TEMPLATE-whats-new.md). Контракт формата описан в [Этапе 5 плана flavors-implementation](plans/flavors-implementation.md#этап-5-метаданные-для-публикации-fastlane).

### API-авторизация

Скрипт читает `key_id` и `client_secret` (RSA private key в формате PKCS8, base64) из `.secrets/rustore-credentials.json`. Этот файл автоматически подгружается из приватного репозитория `easydev991/android-secrets` (см. [Секреты для подписи](#секреты-для-подписи)).

Подпись запроса авторизации: `base64(SHA512withRSA(private_key, keyId + timestamp))`, где `timestamp` — ISO 8601 с timezone offset (`+HH:MM`).

### Что делать при сбое публикации

Скрипт использует fail-fast — ошибка на любом шаге прерывает выполнение. При сбое между шагами 2 и 4 (после создания черновика, но до отправки на модерацию) в RuStore Console останется **orphan-черновик** — пустой черновик без AAB.

Проверить orphan-черновики можно в RuStore Console → Versions → статус «Черновик». Очистить — ручным удалением в Console или через прямой DELETE-запрос (см. [Этап 6 плана flavors-implementation](plans/flavors-implementation.md#что-покрывает-bash-скрипт), шаг 2a/3 зарезервирован под будущую автоматику).

После очистки запустите `make rustore` повторно — все шаги будут выполнены заново, включая пересборку AAB.

## Скриншоты

### Генерация скриншотов

Для генерации скриншотов выполните:

```bash
make screenshots
```

Эта команда:

1. Собирает APK для скриншотов
2. Запускает fastlane для захвата скриншотов на эмуляторе
3. Генерирует скриншоты для локалей `ru-RU` и `en-US` в папку `fastlane/metadata/android/`

Примечание по структуре проекта:

- `screenshot-tests/` — модуль инструментальных тестов, который делает снимки
- `fastlane/metadata/android/` — папка с итоговыми PNG для публикации

Для генерации скриншотов только одной локали есть отдельные цели:

```bash
make screenshots-ru   # только ru-RU
make screenshots-en   # только en-US
```

(они не вызывают `update_readme` — обновление README выполняется отдельно).

**Важно:** Для успешного создания скриншотов необходимо чтобы был запущен эмулятор с соответствующими требованиями для RuStore:

- Соотношение сторон экрана: 9 x 16
- Рекомендуемое разрешение: 1080 х 1920

### Обновление README.md

Команда `make screenshots` автоматически вызывает `make update_readme` в конце выполнения.

При необходимости можно запустить обновление README.md отдельно:

```bash
make update_readme
```

Команда `make update_readme` автоматически:

1. Находит актуальные файлы скриншотов по шаблону `{номер}-{описание}_{временнаяМетка}.png`
2. Заменяет HTML-комментарии в README.md на реальные теги `<img>` с путями к файлам
3. Использует скриншоты только из локали `ru-RU`
4. Обновляет версии библиотек в бейджах README.md (вызывает `make update_readme_versions`)

Версии можно обновить и отдельно, без скриншотов:

```bash
make update_readme_versions
```

Порядок работы:

```bash
# 1. Сгенерировать скриншоты
make screenshots

# 2. Зафиксировать изменения в git
```
