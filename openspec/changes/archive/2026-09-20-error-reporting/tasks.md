# Tasks

## 0. Тестовый паттерн (новый для проекта)

- [x] 0.1 Применить в каждом затронутом тестовом классе `mockkObject(CrashlyticsHelper)` + `unmockkAll()` в `@AfterEach` (образец — `AlarmReminderSchedulerTest`); паттерн раньше не использовался — проверить, что `make test` не ловит протечку мока между тестами

## 1. AlarmReminderScheduler (скрытый сбой доставки уведомлений)

- [x] 1.1 Red: в `AlarmReminderSchedulerTest.schedule_when_security_exception_then_fallback_invoked_and_logged` добавить `mockkObject(CrashlyticsHelper)` + verify `logException` — тест падает (отправки нет)
- [x] 1.2 Green: `CrashlyticsHelper.logException(securityException, "Нет permission SCHEDULE_EXACT_ALARM — fallback на setAndAllowWhileIdle. Уведомления могут приходить с задержкой.")` в catch — дословно сообщение из существующего `logger.w`, чтобы поиск по ключевым словам работал и в logcat, и в Crashlytics; `make test` зелёный

## 2. GetFormattedDaysForItemUseCase (сбой форматирования без причины на экране)

- [x] 2.1 Red: в `GetFormattedDaysForItemUseCaseTest` добавить сценарий «formatter бросил → fallback-текст + verify `logException`» (`mockkObject`) — падает
- [x] 2.2 Green: `CrashlyticsHelper.logException(e, "Ошибка форматирования: ${e.message}")` — дословно текст существующего `Log.e` (канон сообщения в одном месте); `make test` зелёный

## 3. ViewModels: ExactAlarmPermissionViewModel + DetailScreenViewModel

- [x] 3.1 Red: в `ExactAlarmPermissionViewModelTest` сценарий «helper.requestSettings бросил → refresh вызван + verify `logException`» (`mockkObject`) — падает
- [x] 3.2 Green: `CrashlyticsHelper.logException(e, "Не удалось открыть настройки SCHEDULE_EXACT_ALARM")` в существующий широкий catch (ширину не менять — решение владельца)
- [x] 3.3 Red: в `DetailScreenViewModelTest` сценарий «репозиторий бросил `DeleteFailed` → verify `logException`» (`mockkObject`) — падает
- [x] 3.4 Green: `CrashlyticsHelper.logException(e, "Ошибка удаления события: ${e.message}")` в catch рядом с существующим `logger.e`; `make test` зелёный

## 4. IconManager (смена и сброс иконки)

- [x] 4.1 Создать `IconManagerTest` (сейчас класса-теста нет — только мок в `ThemeIconViewModelTest`)
- [x] 4.2 Red: сценарии в `IconManagerTest` (`mockkObject`, verify `logException`) — падают: `changeIcon_when_enable_throws_then_logs_to_crashlytics` (параметризованный, 3 типа исключения: SecurityException / NameNotFoundException / IllegalArgumentException; после ревью три отдельных теста сведены к одному) и `changeIcon_when_disable_component_fails_then_logs_to_crashlytics` (приватный disable тестируется через публичный `changeIcon`; «fails» — сбой деактивации, который disableComponent глотает сам)
- [x] 4.3 Green: в `changeIcon` один `logException` в общем catch `Exception` перед `throw e` («Ошибка смены иконки: …»; после ревью три узких catch сведены к одному); в существующий общий catch `Exception` внутри `disableComponent` — один `logException` («Ошибка сброса иконки: …», throw-семантика не меняется); узкие catch сброса без отчёта; `make test` зелёный

## 5. Верификация

- [x] 5.1 `make format && make lint && make test` — зелёные, без новых падений (актуальное число — репорт `scripts/test_report.py`)
- [x] 5.2 `make android-test` — зелёный (эмулятор API 36; flavor-гейты `Assume RUSTORE_FEATURES` в `MoreScreenTest`, SDK-гейт API 33 в `ReminderAlarmReceiverInstrumentedTest`)
- [x] 5.3 Финал: release-сборка (`make apk FLAVOR=github`), триггер одной ошибки, проверка non-fatal в Firebase Console (Crashlytics → Errors) — сборка из HEAD (dayscounter24.apk, 1.13.4 build 24), триггер на эмуляторе: deny `SCHEDULE_EXACT_ALARM` через appops + событие с напоминанием → `SecurityException` → fallback; non-fatal подтверждён в Crashlytics-datastore устройства (open-session report с `error_message`, 2 события 22:45:30/22:46:42). Визуальная проверка в консоли Firebase — за владельцем (нужна учётка; non-fatal может появляться от минут до часов)
