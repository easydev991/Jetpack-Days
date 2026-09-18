# План: вынос автоматизации публикации в RuStore в переиспользуемый тулкит

## Контекст и цели

Автоматизация публикации (`make rustore` / `rustore-draft` / `rustore-commit`, скрипт
`rustore_publish.sh`, генерация release notes, Python-тесты) была реализована в
JetpackDays (коммиты `61a44374`, `c1c7a8e`). Теперь та же публикация нужна ещё двум
приложениям:

| Репозиторий | Статус | Что нужно |
|---|---|---|
| **JetpackDays** | эталон, всё работает | только параметризация (Этап 1) |
| **Jetpack-MyWorkouts** (`com.myworkouts`) | flavors влиты в main (задача `rustore-flavor-buttons` архивирована), публикации в RuStore нет | тулкит + make-проводка (Этап 2) |
| **Jetpack-WorkoutApp** (`com.swparks`) | секреты уже настроены (`SECRETS_DIR=swparks`), flavors нет | то же самое — следующий потребитель (Этап 3) |

**Что переносим в общий тулкит** (одинаковый для всех приложений):

- `scripts/rustore_publish.sh` — вся логика RuStore API (auth → draft → upload → commit,
  guard на VERSION_CODE, подпись PKCS8). После Этапа 1 — без единой app-специфичной строки.
- `scripts/_generate_whats_new.sh` — уже генерик, переезжает как есть.
- `scripts/rustore_publish_test.py`, `scripts/whats_new_test.py` — тесты едут вместе со скриптами.

**Что НЕ переносим** (остаётся per-app, копируется):

- Makefile-проводка (`rustore`, `rustore-draft`, `rustore-commit`, `whats-new`,
  `_rustore_build_aab`) — различается: `RUSTORE_APP_ID`, префикс артефактов, flavor-пути,
  `SECRETS_DIR`. 40 строк make-кода дешевле копировать, чем абстрагировать.
- Flavor-разделение (gradle, `BuildConfig.RUSTORE_FEATURES`, UI-гейтинг, Assume-тесты) —
  это код конкретного приложения; переиспользуется как рецепт (см. Референсы).

**Триггер этапов:** Этап 1 — сразу. Этап 2 — следующим: переиспользование в MyWorkouts
запрошено уже сейчас, flavors для него уже влиты (тулкит делается под конкретного
потребителя, не «на будущее»). Этап 3 — WorkoutApp: flavors для подключения не нужны,
очередь поменяна в пользу более активного MyWorkouts.

---

## Выбор механизма переиспользования

Рассматривались три альтернативы: `git subtree`, «просто скопировать скрипты», инсталлируемый
CLI (gem/brew/curl). Выбран `git subtree` — обоснование ниже.

### Что такое git subtree

Обычная git-команда (встроена в git, ничего ставить не надо). Она копирует файлы из чужого
репозитория **внутрь** дерева твоего репо как обычные файлы (`tools/release/...`) и
запоминает, из какого репо/коммита они пришли. Внутри это обычный merge-коммит — то есть
ты и так умеешь с этим работать.

### Subtree vs submodule

| | submodule | subtree |
|---|---|---|
| Что лежит в твоём репо | указатель на коммит чужого репо | сами файлы |
| Клонирование проекта | нужен `--recurse-submodules`, иначе пустая папка | обычный `git clone`, ничего помнить не надо |
| Читаемость | grep/diff/rg по скриптам не работают без checkout | работают нативно — это просто файлы |
| Ежедневный workflow | `git submodule update`, «detached head», забыл — CI красный | ничего не делать |
| Обновление версии | один `git pull` внутри submodule | одна команда `git subtree pull` (редко, по кнопке) |

**Почему subtree здесь:**

1. **Offline-констрейнт проекта** — скрипты физически в репо; подключение и тесты
   не требуют сети, публикация ходит в RuStore API как и раньше.
2. **Обновления редкие** — тулкит меняется только при правке RuStore API (раз в месяцы).
   Подплачивать ежедневной submodule-фрикцией ради редкой операции — невыгодно.
3. **Не нужно ничего настраивать** — ни в IDE, ни в CI, ни в AGENTS-инструкциях для агентов.

**Ключевые команды (обе, которые реально нужны):**

```bash
# Подключить тулкит (один раз, из корня приложения):
git subtree add --prefix tools/release git@github.com:easydev991/android-release-toolkit.git v1.0.0 --squash

# Обновить до новой версии тулкита (по тегу, не по main — пин к стабильности):
git subtree pull --prefix tools/release git@github.com:easydev991/android-release-toolkit.git v1.1.0 --squash
```

**Два предостережения:**

- `--squash` обязателен — иначе в историю твоего приложения втянется вся история тулкита.
- **Не редактировать файлы `tools/release/` напрямую в приложении.** Следующий `pull` молча
  затрёт локальные правки. Багфикс делается в тулкит-репо (правка → тесты → тег), затем
  `subtree pull` во всех приложениях. В этом и есть смысл тулкита: одна копия логики.

### Почему не «просто скопировать» скрипты в каждое приложение

Промежуточная ступень лестницы — честная опция для 2–3 приложений: setup ~40 мин против
~2–2.5 ч у subtree, те же offline-гарантии, тесты едут вместе со скриптами, агенту файлы
так же видны. Copy проигрывает на распространении фиксов: каждый багфикс RuStore API — ручные правки
во всех копиях и риск забыть третью (break-even subtree — примерно 2 года при редких
фиксах). Плюс Этап 2 мигрирует JetpackDays на единственную копию логики — copy этот шаг
не даёт в принципе.

### Почему не инсталлируемый CLI «в стиле fastlane»

Вариант: тулкит публикуется на GitHub как устанавливаемый артефакт (Ruby gem в Gemfile,
Homebrew tap, `curl | sh`), ставится в любой проект одной командой, обновляется независимо.

| Критерий | Subtree | Инсталлируемый CLI |
|---|---|---|
| Где пин версии | в git-истории каждого репо (тег в `subtree pull`) | на машине, не в репо → дрейф local/CI; пин придётся изобретать заново |
| Offline-констрейнт | файлы в репо, ноль сети | каждая новая машина/CI — сетевой шаг установки |
| Тесты | едут в каждый репо, `make scripts-test` гоняет их всегда | живут в репо тулкита; приложение не может проверить тулкит, с которым публикует |
| Работа агентов | скрипты grep-аются и правятся в контексте проекта | установленный бинарник — чёрный ящик для агента |
| Стоимость | часы (параметризация + `subtree add`) | дни: переписать боевые bash+pytest в пакет; зашитые багфиксы (octet-stream, PKCS8, guard VERSION_CODE) находятся заново |

«Одна команда установки» — не преимущество: `git subtree add` — тоже одна команда, и
выполняется один раз за жизнь приложения. Re-write ради неё не оправдан.

**Триггер пересмотра:** тулкит вырастает в мульти-стор CLI (RuStore + Play + подпись),
появляется внешний потребитель, или потребителей становится 4+. Самый легитимный вариант
тогда — gem в Gemfile (пин через lock-файл, rbenv/bundler уже в `make setup`).

---

## Этапы реализации

### Этап 1. JetpackDays: параметризация скрипта публикации

> **Зависимости:** нет. **Статус:** выполнено (ветка `feature/reusable-toolkit`).

- [x] **TDD:** тесты на обязательность `RUSTORE_APP_ID` (скрипт падает с внятной ошибкой
  до сетевых вызовов); в окружении обоих тестов (`rustore_publish_test.py`,
  `makefile_rustore_target_test.py`) — нейтральный dummy `com.example.test`, не
  `com.dayscounter`; в `makefile_rustore_target_test.py` добавлен ассерт на keystore-путь.
- [x] **`rustore_publish.sh`:** хардкод `APP_ID` → обязательная env-переменная
  `RUSTORE_APP_ID`; app-специфичный комментарий переписан через `$APP_ID`.
- [x] **Makefile:** `RUSTORE_APP_ID ?= com.dayscounter` + `export`; `APP_NAME ?= dayscounter`
  вместо хардкода префикса артефактов; `KEYSTORE_FILE` в `_load_secrets` собирается из
  `$(APP_NAME)` (`.secrets/keystore/$(APP_NAME)-release.keystore`); комментарии целей обновлены.
- [x] **`app/build.gradle.kts`:** удалён мёртвый fallback keystore — после `_load_secrets`
  `KEYSTORE_FILE` всегда задан.
- [x] **Проверено:** `make test`, `make scripts-test`, `make apk FLAVOR=github`,
  `make rustore SKIP_PUBLISH=1`; grep по `com.dayscounter` в скриптах публикации пуст
  (в `scripts/` единственный хит — fixture-строка стектрейса в `android_test_report_test.py`,
  к публикации не относится; идентификатор остался только в дефолтах Makefile).
- [x] **Документация:** `docs/deployment.md` — раздел «Конфигурация приложения
  (RUSTORE_APP_ID / APP_NAME)».

### Этап 2. Тулкит-репозиторий + подключение JetpackDays и MyWorkouts

> **Зависимости:** Этап 1 (в тулкит заезжает уже параметризованный скрипт).
>
> **Скоуп MyWorkouts:** тулкит + make-проводка + метаданные. Flavors `github`/`rustore`
> уже влиты в main (задача `rustore-flavor-buttons` архивирована) — flavor-проводка
> копируется из JetpackDays как есть.

- [x] **Создать private-репо** `android-release-toolkit` на GitHub; залить 4 файла
  (`rustore_publish.sh`, `_generate_whats_new.sh`, `rustore_publish_test.py`,
  `whats_new_test.py`) из JetpackDays **в `scripts/` тулкита** (после `git subtree add
  --prefix tools/release` они разложатся в `tools/release/scripts/` — именно на это
  рассчитаны все правки путей ниже); README (~20 строк): `RUSTORE_APP_ID`, конвенции путей
  (`gradle.properties`, `fastlane/metadata/android/ru-RU/whats_new/<VERSION>.txt`,
  `.secrets/rustore-credentials.json`), команды subtree, как выпускать новую версию
  (правка → тесты → тег). Тег `v1.0.0`.
- [x] **JetpackDays переходит на тулкит:** `git subtree add --prefix tools/release … v1.0.0 --squash`;
  удалить локальные копии 4 файлов из `scripts/`; Makefile: пути
  `scripts/rustore_publish.sh` → `tools/release/scripts/rustore_publish.sh` (3 места —
  цели `rustore`, `rustore-draft`, `rustore-commit`) **и
  `scripts/_generate_whats_new.sh` → `tools/release/scripts/_generate_whats_new.sh`**
  (4-е место: цель `whats-new` — иначе она сломается после удаления локальной копии);
  `makefile_rustore_target_test.py` — пути копируемых файлов → `tools/release/scripts/`;
  `make scripts-test` — добавить второй discover-вызов
  (`unittest discover -s tools/release/scripts -p "*_test.py"`) после существующего
  `-s scripts`. Паттерн `-p "*_test.py"` обязателен: дефолтный pattern `unittest discover` —
  `test*.py`, не совпадает с конвенцией `*_test.py`; без `-p` discover молча найдёт 0 тестов и
  завершится с кодом 0 — sync-цикл (см. «Тренировочный sync-цикл» ниже в этом этапе)
  станет ложно-зелёным. Старый discover
  **оставить**: в `scripts/` остаются `android_test_report_test.py` и другие тесты;
  удалять его не нужно.
- [x] **MyWorkouts подключает тулкит:** `git subtree add` (та же команда); make-проводка
  копией из JetpackDays — только rustore-цели (`rustore`, `rustore-draft`, `rustore-commit`,
  `whats-new`, `_rustore_build_aab`); flavor-таск `bundleRustoreRelease`, flavor-путь
  артефакта `app/build/outputs/bundle/rustoreRelease/…` и
  `uploadCrashlyticsMappingFileRustoreRelease` копируются как есть — flavors уже в main.
  Замены: `RUSTORE_APP_ID ?= com.myworkouts`; строку `APP_NAME` из копии не переносить —
  в MyWorkouts `APP_NAME ?= myworkouts` уже объявлен. `_load_secrets` в MyWorkouts уже
  целевого вида (`SECRETS_DIR=myworkouts`, keystore-путь собирается из `$(APP_NAME)`) —
  JetpackDays-версию поверх не копировать (риск затереть отличающуюся структуру); это
  независимое подтверждение схемы Этапа 1. Заодно убрать fallback keystore в
  `app/build.gradle.kts` (`?: ".secrets/keystore/myworkouts-release.keystore"`) — после
  `_load_secrets` он недостижим. **Тест проводки копируется вместе с проводкой:**
  `makefile_rustore_target_test.py` с заменой `dayscounter21.aab` → `myworkouts{N}.aab`
  (через дефолт `APP_NAME`) и путей fake-скриптов → `tools/release/scripts/` — три таргета
  (`rustore`/`rustore-draft`/`rustore-commit`) проверяются тем же тестом, что и в JetpackDays.
  **Плюс добавить в Makefile MyWorkouts цель `scripts-test`** (сейчас её нет) — два
  discover, как в JetpackDays:
  `python3 -m unittest discover -s scripts -p "*_test.py"` и
  `python3 -m unittest discover -s tools/release/scripts -p "*_test.py"`
  (в `scripts/` MyWorkouts появится копия `makefile_rustore_target_test.py` — её
  гоняет первый discover); без раннера скопированный тест никто не запускает,
  и критерий тренировочного sync-цикла «`make scripts-test` зелёный в обоих»
  недостижим.
- [ ] **Секреты MyWorkouts:** добавить только `rustore-credentials.json` в существующий
  `android-secrets/myworkouts/` — keystore и google-services.json там уже есть и работают
  (`_load_secrets` кладёт всё содержимое каталога через `cp -r`, отдельной правки не нужно).
  Выполняется **после первого ручного релиза** — пока у `com.myworkouts` нет активной версии,
  его нельзя включить в API-ключ (см. «Первый релиз» ниже).
- [x] **Тренировочный sync-цикл** (отработать механизм до боевого применения): правка в
  тулките (косметическая) → тег `v1.0.1` → `git subtree pull` в JetpackDays и MyWorkouts →
  `make scripts-test` зелёный в обоих.
- [x] **Fastlane-метаданные** (`fastlane/metadata/android/ru-RU/whats_new/`) — до первого
  релиза (сейчас `fastlane/` в MyWorkouts нет совсем).
- [ ] **Первый релиз MyWorkouts — вручную через веб-консоль:** RuStore API требует
  «хотя бы 1 активную версию приложения» (доки api-upload-publication-app), и пока её нет,
  `com.myworkouts` нельзя выбрать в API-ключе. `make rustore SKIP_PUBLISH=1` даёт
  подписанный AAB — загрузить его в Console руками. Последующие релизы — через
  `make rustore-draft` (черновик безопасен — commit отправляется вручную после проверки
  в Console), после добавления `rustore-credentials.json` в секреты.
- [x] **Документация:** `docs/deployment.md` в MyWorkouts нет — создать с разделом
  «Публикация в RuStore» (зеркально JetpackDays).

**Критерии завершения:** `make rustore SKIP_PUBLISH=1` в обоих приложениях; источник логики
публикации один — тулкит-репо (копии в `tools/release/` обновляются только `git subtree pull`,
ручных правок в них нет); тренировочный pull отработан; MyWorkouts собран
(`make rustore SKIP_PUBLISH=1`) и первый релиз загружен в Console вручную — без правок
логики в скриптах, только конфигурация.

### Этап 3. WorkoutApp (параллельная ветка — flavors не нужны)

> **Зависимости.** Жёсткая — из Этапа 2 нужны только тулкит-репо с тегом и отработанный
> тренировочный sync-цикл (буллеты 1 и 5 Этапа 2). Подключение и релиз MyWorkouts —
> не гейт для WorkoutApp: образцом make-проводки служит JetpackDays или MyWorkouts —
> кто на тот момент подключен. Триггер — см. «Триггер этапов».
>
> **Скоуп WorkoutApp:** тулкит + make-проводка + метаданные. Flavor-разделение WorkoutApp —
> отдельная задача по рецепту `flavors-implementation.md` (см. Референсы), в этот этап
> не входит.

- [ ] `git subtree add` из актуального тега тулкита.
- [ ] Makefile-проводка (копией из JetpackDays/MyWorkouts — кто на тот момент подключен
  к тулкиту) — только rustore-цели (`rustore`,
  `rustore-draft`, `rustore-commit`, `whats-new`, `_rustore_build_aab`):
  `RUSTORE_APP_ID ?= com.swparks`, `APP_NAME ?= swparks`
  (`SECRETS_DIR=swparks` уже настроен; keystore-путь в `_load_secrets` следует за
  `APP_NAME` автоматически — см. Этап 1). **В `_rustore_build_aab` заменить flavor-таск
  `bundleRustoreRelease` на общий `bundleRelease`** и flavor-путь артефакта
  `app/build/outputs/bundle/rustoreRelease/…` на общий — flavors во WorkoutApp нет;
  вернуть flavor-таск и `uploadCrashlyticsMappingFileRustoreRelease` после flavor-задачи
  (зеркально к тому, что MyWorkouts-копия зовёт flavor-таск). Заодно убрать fallback
  keystore в `app/build.gradle.kts:28`
  (`?: ".secrets/keystore/swparks-release.keystore"`) по той же логике — после
  `_load_secrets` он недостижим.
- [ ] Тест проводки + раннер: скопировать `makefile_rustore_target_test.py` (замены — как
  в Этапе 2 для MyWorkouts: через дефолт `APP_NAME` → `swparks{N}.aab`, пути fake-скриптов →
  `tools/release/scripts/`) и добавить в Makefile `scripts-test` (цели сейчас нет) — два
  discover, как в JetpackDays: `python3 -m unittest discover -s scripts -p "*_test.py"`
  и `python3 -m unittest discover -s tools/release/scripts -p "*_test.py"`.
- [ ] **Секреты WorkoutApp:** `rustore-credentials.json` (API-ключ из RuStore Console →
  репозиторий `android-secrets/swparks/`); структура
  `fastlane/metadata/android/ru-RU/whats_new/` и шаблон `TEMPLATE-whats-new.md`
  по конвенции.
- [ ] **Первый релиз WorkoutApp** через `make rustore-draft` — механика как в Этапе 2.
- [ ] **Документация:** в `docs/doc-deployment.md` WorkoutApp — раздел
  «Публикация в RuStore» (у WorkoutApp файл называется `doc-deployment.md`).

**Критерии завершения:** первый релиз WorkoutApp через `make rustore-draft` → `rustore-commit`
без единой правки тулкита — только конфигурация (с flavor-оговоркой: до flavor-задачи
`_rustore_build_aab` зовёт общий `bundleRelease`).

---

## Зависимости между этапами

```
Этап 1 (JetpackDays: параметризация) ──► Этап 2 (тулкит + JetpackDays + MyWorkouts) ──► Этап 3 (WorkoutApp)
                                                                                    │
                                                                                    └── Этап 3 ждёт от Этапа 2 только
                                                                                        буллеты 1 и 5 (тулкит-репо с тегом
                                                                                        + sync-цикл); релиз MyWorkouts —
                                                                                        не гейт
```

Этапы 2 и 3 можно разнести на месяцы — subtree от этого не страдает: каждый шаг пинается
на конкретный тег тулкита. Обратное движение тоже безопасно: тулкит не знает о приложениях.

## Референсы

- `docs/plans/flavors-implementation.md` — Этапы 6–7 (реализация публикации в RuStore);
  весь план — рецепт flavor-разделения для порта в WorkoutApp (в MyWorkouts flavors
  уже есть).
- `scripts/rustore_publish.sh` — конвенции путей и контракты (Этап 1 — параметризация
  на месте; в тулкит переезжает в Этапе 2).
- `docs/deployment.md` → «Каналы дистрибуции» — термины flavor'ов и релизные команды.
- Репозиторий секретов: `easydev991/android-secrets` (JetpackDays / swparks / myworkouts).
