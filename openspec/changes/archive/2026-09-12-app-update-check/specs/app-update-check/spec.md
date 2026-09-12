## Purpose

Позволяет пользователям github-сборки приложения вручную проверить наличие новой версии через GitHub Releases API и при её наличии перейти на страницу релиза в браузере. Доступ ТОЛЬКО на сборке с `BuildConfig.RUSTORE_FEATURES == false`; на rustore обновления доставляются автоматически и кнопка скрыта.

## ADDED Requirements

### Requirement: Кнопка проверки обновлений видна только на github-сборке

Система MUST отображать кнопку "Проверить обновления" (`R.string.check_for_updates`) на экране `MoreScreen` ТОЛЬКО когда `BuildConfig.RUSTORE_FEATURES == false`. Кнопка MUST располагаться внутри блока `if (!BuildConfig.RUSTORE_FEATURES) { ... }`, после существующей кнопки "GitHub" (`R.string.github_page`), отделённая `Spacer` от предыдущего элемента. На rustore-сборке (`BuildConfig.RUSTORE_FEATURES == true`) кнопка MUST NOT отображаться ни при каких условиях.

#### Scenario: Кнопка отображается на github-сборке

- **WHEN** пользователь открывает `MoreScreen` на сборке с `BuildConfig.RUSTORE_FEATURES == false`
- **THEN** система отображает кнопку "Проверить обновления" сразу под кнопкой "GitHub"

#### Scenario: Кнопка скрыта на rustore-сборке

- **WHEN** пользователь открывает `MoreScreen` на сборке с `BuildConfig.RUSTORE_FEATURES == true`
- **THEN** система MUST NOT отображать кнопку "Проверить обновления" в любом месте экрана

### Requirement: Запрос последней версии к GitHub Releases API

Система MUST отправлять GET-запрос на `https://api.github.com/repos/easydev991/Jetpack-Days/releases/latest` при каждом нажатии пользователем на кнопку "Проверить обновления". MUST иметь таймаут 10 секунд на подключение и чтение, MUST NOT следовать HTTP-редиректам, любой HTTP-статус ≠ 200 MUST обрабатываться как ошибка проверки.

#### Scenario: Успешный запрос при наличии релиза

- **WHEN** пользователь нажимает кнопку "Проверить обновления" и API возвращает HTTP 200 с валидным JSON
- **THEN** система парсит поля `tag_name` и `html_url` и продолжает обработку результата

#### Scenario: Сетевая ошибка

- **WHEN** пользователь нажимает кнопку и запрос завершается `IOException`, таймаутом или HTTP 4xx/5xx
- **THEN** система переводит ViewModel в состояние `Error` (диалог и кнопка "Закрыть" описаны в Requirement «Диалог с результатом проверки», Scenario «Ошибка проверки»)

### Requirement: Семантическое сравнение версий

Система MUST извлекать версию из поля `tag_name` ответа GitHub, удалять ведущий префикс `v` (если есть), MUST парсить строку как семантическую версию `major.minor.patch` и MUST сравнивать её с `BuildConfig.VERSION_NAME` текущего приложения. Сравнение MUST выполняться покомпонентно по правилам semver: при равенстве всех компонентов версии считаются равными; при меньшем `major`, или равном `major` и меньшем `minor`, или равных `major` и `minor` и меньшем `patch` — текущая версия считается устаревшей. **Формат `tag_name` — канонический `v?major.minor.patch`**; поведение при невалидных компонентах (пререлизы, build-metadata, отличное число компонентов) — out of scope (см. `design.md` Risks).

#### Scenario: Текущая версия устарела

- **WHEN** `BuildConfig.VERSION_NAME = "1.2.0"` и `tag_name = "v1.2.3"`
- **THEN** система MUST определить наличие обновления и вернуть `UpdateInfo` с `version = "1.2.3"`

#### Scenario: Текущая версия актуальна

- **WHEN** `BuildConfig.VERSION_NAME = "1.2.3"` и `tag_name = "v1.2.3"`
- **THEN** система MUST определить, что обновление не требуется

#### Scenario: Текущая версия новее

- **WHEN** `BuildConfig.VERSION_NAME = "2.0.0"` и `tag_name = "v1.5.0"`
- **THEN** система MUST определить, что обновление не требуется

#### Scenario: Tag без префикса 'v'

- **WHEN** `tag_name = "1.2.3"` (без префикса)
- **THEN** система MUST корректно распарсить версию как `1.2.3`

### Requirement: Диалог с результатом проверки

Система MUST отображать диалог `UpdateCheckDialog` поверх `MoreScreen` при изменении состояния ViewModel на `UpdateAvailable`, `UpToDate` или `Error`. Диалог MUST локализоваться через `stringResource`, MUST иметь кнопку "Закрыть", которая вызывает `dismissDialog()` и возвращает ViewModel в состояние `Idle`.

#### Scenario: Доступно обновление

- **WHEN** ViewModel переходит в состояние `UpdateAvailable(info: UpdateInfo)`
- **THEN** система MUST отобразить диалог с заголовком "Доступно обновление", сообщением с указанием версии (`%1$s`) и release notes (Trim, максимум 500 символов, с многоточием при обрезке), а также кнопки "Открыть на GitHub" и "Закрыть"

#### Scenario: Установлена последняя версия

- **WHEN** ViewModel переходит в состояние `UpToDate`
- **THEN** система MUST отобразить диалог с заголовком "Актуальная версия" (`R.string.update_up_to_date_title`) и сообщением "У вас установлена последняя версия", а также кнопку "Закрыть"

#### Scenario: Release notes отсутствуют в ответе GitHub

- **WHEN** API возвращает HTTP 200 с валидными `tag_name` и `html_url`, но `body = null` или пустая строка
- **THEN** система MUST отобразить диалог с заголовком "Доступно обновление" и сообщением с версией, **без блока release notes**, и с кнопками "Открыть на GitHub" и "Закрыть"

#### Scenario: Ошибка проверки

- **WHEN** ViewModel переходит в состояние `Error`
- **THEN** система MUST отобразить диалог с заголовком `R.string.update_error_title` ("Error" / "Ошибка"), сообщением "Не удалось проверить обновления" и кнопку "Закрыть"

### Requirement: Открытие страницы релиза в браузере

Система MUST при нажатии пользователем на кнопку "Открыть на GitHub" в диалоге `UpdateCheckDialog` запускать `Intent.ACTION_VIEW` с URL из `UpdateInfo.releaseUrl` через `context.startActivity(...)`. После запуска системный диалог MUST быть закрыт, и ViewModel MUST вернуться в состояние `Idle`.

#### Scenario: Успешный запуск браузера

- **WHEN** пользователь нажимает кнопку "Открыть на GitHub" в диалоге с состоянием `UpdateAvailable`
- **THEN** система MUST запустить `Intent.ACTION_VIEW` с URL из `UpdateInfo.releaseUrl` и закрыть диалог

### Requirement: Локализация диалога и кнопки

Система MUST предоставлять следующие строки в `res/values/strings.xml` (английский, по умолчанию) и `res/values-ru/strings.xml` (русский): `check_for_updates`, `update_available_title`, `update_available_message` (плейсхолдер `%1$s` для версии), `update_open_button`, `update_up_to_date`, `update_up_to_date_title`, `update_error_title`, `update_error`. Английские значения: "Check for updates", "Update available", "Version %1$s is available", "Open on GitHub", "You're up to date", "Up to date", "Error", "Failed to check for updates". Русские значения: "Проверить обновления", "Доступно обновление", "Доступна версия %1$s", "Открыть на GitHub", "У вас установлена последняя версия", "Актуальная версия", "Ошибка", "Не удалось проверить обновления". Текст кнопки "Закрыть" MUST браться из существующего `R.string.close` (уже определён в обоих `strings.xml`); дублирующий ключ `update_close` НЕ вводится.

#### Scenario: Английская локаль

- **WHEN** устройство пользователя использует английскую локаль
- **THEN** диалог и кнопка MUST отображаться на английском языке из `res/values/strings.xml`

#### Scenario: Русская локаль

- **WHEN** устройство пользователя использует русскую локаль
- **THEN** диалог и кнопка MUST отображаться на русском языке из `res/values-ru/strings.xml`
