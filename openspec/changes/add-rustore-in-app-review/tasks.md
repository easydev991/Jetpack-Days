# Tasks

## 1. Gradle: SDK-зависимость и flavor source-sets

- [ ] 1.1 Добавить версию SDK в `gradle/libs.versions.toml` (`ru.rustore.sdk:review:10.5.2`) и репозиторий `https://nexus-external.rustore.ru/repository/maven-rustore-exposed` с content-filter по группе `ru.rustore.sdk`. Verify: sync проходит; `./gradlew app:dependencies --configuration rustoreDebugRuntimeClasspath` содержит артефакт `review:10.5.2` и не содержит сетевых стеков (Retrofit/OkHttp/Ktor) в транзитивных зависимостях SDK; это прямой gradle-вызов мимо make — `_ensure_secrets` он не подтягивает и на чистом чекауте упадёт на конфигурации без `google-services.json`, поэтому запускать после любой make-цели с секретами (например, `make build FLAVOR=rustore`).
- [ ] 1.2 Создать первые flavor source-sets `app/src/rustore/` и `app/src/github/` с временными фабриками-заглушками review-флоу одинакового публичного FQN (контракт лаунчера появится только в 2.3, поэтому обе фабрики — временные заглушки; финальные реализации: rustore — 4.1, github no-op — 4.2) и подключить `rustoreImplementation`. Verify: `make build FLAVOR=github` и `make build FLAVOR=rustore` собираются; github-вариант компилируется без SDK-зависимости (изоляция подтверждена самой компиляцией).
- [ ] 1.3 Задокументировать в `docs/deployment.md` новый maven-репозиторий и flavor source-sets; в `AGENTS.md` зафиксировать RuStore Review SDK как второе разрешённое исключение офлайн-правила (сеть — внутри приложения RuStore, свой сетевой стек не добавляется). Verify: `make lint` проходит, включая markdownlint.

## 2. Домен: milestone-логика и контракт лаунчера (TDD)

- [ ] 2.1 Написать unit-тесты milestone-логики (тесты до реализации): milestone 1 на первой созданной записи, milestone 5 и 15 на соответствующих порогах, выбор наивысшего достигнутого непройденного, не более одного авто-запроса за сессию, достижение более высокого milestone при непоказанном pending — решение «не запрашивать» (pending не перезаписывается, промежуточный milestone остаётся непройденным), попытанный milestone не повторяется, редактирование не увеличивает счётчик, после всех порогов решений нет, сброс возвращает к milestone 1. Verify: `make test` — красная фаза TDD: прогон падает на компиляции или выполнении новых тестов (реализации ещё нет — без заглушек падение будет ошибкой компиляции, это тоже валидная красная фаза).
- [ ] 2.2 Реализовать чистый use case milestone-логики в `domain/usecase/` (пороги `[1, 5, 15]` — константа use case; вход — состояние счётчиков, выход — решение «запланировать milestone N / не запрашивать»). Verify: тесты из 2.1 проходят, `make test` зелёный.
- [ ] 2.3 Определить в домене контракт лаунчера review-флоу без android-типов и модель исхода со своей доменной классификацией ошибок (форма показана / ошибка; типы ошибок — доменные, без классов SDK); маппинг иерархии `RuStoreException` в эту модель выполняет flavor-реализация (см. 4.1). Добавить unit-тесты на fake-реализациях контракта. Verify: `make test` зелёный, `DomainPurityTest` проходит.

## 3. Данные: хранилище счётчиков

- [ ] 3.1 Написать unit-тесты хранилища (тесты до реализации): инкремент счётчика, отметка milestone пройденным, чтение pending-milestone, сброс, персистентность между инстансами хранилища. Verify: `make test` — красная фаза TDD: прогон падает на компиляции или выполнении новых тестов (реализации хранилища ещё нет).
- [ ] 3.2 Реализовать хранилище в `data/preferences/` по образцу `AppSettingsDataStore` (новый preferences DataStore: int-счётчик созданных записей, пройденные milestones, pending-milestone). Verify: тесты из 3.1 проходят, `make test` зелёный.
- [ ] 3.3 Вызвать сброс счётчиков из существующего флоу полного удаления данных (экран «Данные приложения», действие «Удалить все данные»); дополнить unit-тесты флоу удаления проверкой сброса. Verify: `make test` зелёный, включая существующие тесты экрана «Данные приложения».

## 4. Flavor-реализация rustore: SDK-лаунчер

- [ ] 4.1 Реализовать SDK-лаунчер: `RuStoreReviewManagerFactory.create(context)`, двухфазный флоу (подготовка → показ), маппинг иерархии `RuStoreException` в доменную модель исхода; unit-тесты на fake-обёртке менеджера (MockK) проверяют порядок фаз, успешный исход и маппинг каждой ошибки (`RuStoreNotInstalledException`, `RuStoreOutdatedException`, `RuStoreUserUnauthorizedException`, `RuStoreRequestLimitReached`, `RuStoreReviewExists`, базовая `RuStoreException`); тесты — в `app/src/testRustore/` (в общем `app/src/test/` они не скомпилируются под github-вариант — классы под тестом существуют только в rustore). Verify: `make test FLAVOR=rustore` зелёный (дефолтный `make test` — github — flavor-тесты не запускает).
- [ ] 4.2 Реализовать github no-op реализацию контракта — лаунчер (функциональность review в github flavor отсутствует — лаунчер никогда не вызывается). Verify: `make build FLAVOR=github` собирается; на github-сборке никакой кодовой путь не ссылается на SDK.

## 5. Аналитика review-флоу

- [ ] 5.1 Расширить `AnalyticsEvent` новым событием review-флоу (только авто-запрос; исход: форма показана / ошибка с типом) с unit-тестом прокидки события через `AnalyticsService` на fake-провайдере. Verify: `make test` зелёный.

## 6. DI: сборка review-флоу

- [ ] 6.1 Собрать реальный wiring в `di/AppModule.kt`: фабрику review-флоу (лаунчер; ссылка на flavor-фабрику — заглушки из 1.2 заменяются реализациями из 4.1/4.2) — по обоим flavor-вариантам; in-memory holder сессии — один общий singleton в main (D2: от SDK не зависит, flavor-вариантов не имеет). Кнопка «Оценить приложение» в `MoreScreen` не меняется. Verify: `make build FLAVOR=github` и `make build FLAVOR=rustore` собираются с реальным wiring.

## 7. UI: авто-триггер

- [ ] 7.1 Добавить хук регистрации созданной записи в `CreateEditScreenViewModel.saveItem` — ветка создания (`itemId == null`), после успешных `persistItem` и `applyReminderChange`, непосредственно перед `completeSuccessfulSave` (момент закрытия экрана: `onSaved()` → навигация назад); хук гейтится флагом `isReviewEnabled: Boolean` — инъекция через фабрику ViewModel (передаёт `BuildConfig.RUSTORE_FEATURES`), на github-сборке счётчик не ведётся вовсе; редактирование счётчик не трогает; импорт бэкапа хук не проходит. Unit-тесты ViewModel на fake use case с явным флагом: `isReviewEnabled = true` — создание инкрементит и планирует milestone, редактирование — нет, частичный успех (вставка прошла, напоминание упало, экран остался с ошибкой) счётчик не увеличивает; holder сессии сообщает, что авто-запрос в этой сессии уже выполнялся — ViewModel передаёт это состояние в use case, планирования нет (чтение holder'а из D2); `isReviewEnabled = false` — счётчик не трогается даже при успешном создании. Verify: `make test` зелёный, включая существующие `CreateEditScreenViewModelTest` / `CreateEditScreenViewModelReminderTest`.
- [ ] 7.2 Реализовать отложенный показ pending на экране списка записей (Events): при наличии pending-milestone задержка ~0.8 с → запуск SDK-флоу → отметка milestone пройденным → логирование auto-исхода; ошибки — тихо, без UI и фолбэка. Unit-тесты логики показа на fake-лаунчере (успех, ошибка — интерфейс не меняется, повторное срабатывание в сессии исключено). Verify: `make test` зелёный.

## 8. Интеграционные проверки и ручной чек-лист

- [ ] 8.1 Полный прогон автоматических проверок: `make test` и `make test FLAVOR=rustore` (flavor unit-тесты из `app/src/testRustore/` дефолтный `make test` не выполняет); затем `make android-test FLAVOR=rustore` и отдельной командой `make android-test FLAVOR=github`; `make lint` (Detekt — без новых замечаний, лимит не повышается); `make memory-check` (затронуты UI и ViewModel). Verify: все прогоны зелёные, отчёты (`scripts/test_report.py`, `scripts/android_test_report.py`) без упавших тестов.
- [ ] 8.2 Release-сборка rustore flavor без SDK-сюрпризов: `make apk FLAVOR=rustore` (release; `make build` собирает debug без R8 и proguard не проверяет) — R8/proguard без ошибок; при необходимости добавить правила в `app/proguard-rules.pro`. Установка: `adb install -r app/build/outputs/apk/rustore/release/app-rustore-release.apk` (`make install` ставит debug и для этого шага не подходит). Verify: сборка и установка release-APK успешны, приложение запускается.
- [ ] 8.3 Ручная проверка SDK-ветки на реальном устройстве с установленным RuStore (автотестами не покрывается): авто-триггер на 1-й созданной записи показывает форму с задержкой на экране списка; после завершения флоу нет дополнительных форм, связанных с оценкой; при исчерпанном лимите отображений авто-запрос тихо завершается без UI; RuStore не установлен → авто-запрос тихо завершается без UI (ошибка подготовки данных); кнопка «Оценить приложение» ведёт себя как раньше; полное удаление данных сбрасывает триггеры (следующая запись снова запрашивает оценку). Verify: чек-лист пройден, результаты зафиксированы в описании PR.

## Workflow follow-up

- После ревью заархивировать change (`openspec archive`) и проверить синхронизацию specs.
- Обновить бейджи версий в README — `make update_readme_versions` при выпуске релиза с этой фичей.
