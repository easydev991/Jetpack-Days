# Tasks

## 0. Тестовый паттерн (новый для проекта)

- [ ] 0.1 Применить в каждом затронутом тестовом классе `mockkObject(CrashlyticsHelper)` + `unmockkAll()` в `@AfterEach` (образец — `AlarmReminderSchedulerTest`); паттерн раньше не использовался — проверить, что `make test` не ловит протечку мока между тестами

## 1. AlarmReminderScheduler (скрытый сбой доставки уведомлений)

- [ ] 1.1 Red: в `AlarmReminderSchedulerTest.schedule_when_security_exception_then_fallback_invoked_and_logged` добавить `mockkObject(CrashlyticsHelper)` + verify `logException` — тест падает (отправки нет)
- [ ] 1.2 Green: `CrashlyticsHelper.logException(securityException, "Нет permission SCHEDULE_EXACT_ALARM — fallback на setAndAllowWhileIdle. Уведомления могут приходить с задержкой.")` в catch — дословно сообщение из существующего `logger.w`, чтобы поиск по ключевым словам работал и в logcat, и в Crashlytics; `make test` зелёный

## 2. GetFormattedDaysForItemUseCase (сбой форматирования без причины на экране)

- [ ] 2.1 Red: в `GetFormattedDaysForItemUseCaseTest` добавить сценарий «formatter бросил → fallback-текст + verify `logException`» (`mockkObject`) — падает
- [ ] 2.2 Green: `CrashlyticsHelper.logException(e, "Ошибка форматирования: ${e.message}")` — дословно текст существующего `Log.e` (канон сообщения в одном месте); `make test` зелёный

## 3. ViewModels: ExactAlarmPermissionViewModel + DetailScreenViewModel

- [ ] 3.1 Red: в `ExactAlarmPermissionViewModelTest` сценарий «helper.requestSettings бросил → refresh вызван + verify `logException`» (`mockkObject`) — падает
- [ ] 3.2 Green: `CrashlyticsHelper.logException(e, "Не удалось открыть настройки SCHEDULE_EXACT_ALARM")` в существующий широкий catch (ширину не менять — решение владельца)
- [ ] 3.3 Red: в `DetailScreenViewModelTest` сценарий «репозиторий бросил `DeleteFailed` → verify `logException`» (`mockkObject`) — падает
- [ ] 3.4 Green: `CrashlyticsHelper.logException(e, "Ошибка удаления события: ${e.message}")` в catch рядом с существующим `logger.e`; `make test` зелёный

## 4. IconManager (смена и сброс иконки)

- [ ] 4.1 Создать `IconManagerTest` (сейчас класса-теста нет — только мок в `ThemeIconViewModelTest`)
- [ ] 4.2 Red: четыре сценария в `IconManagerTest` (`mockkObject`, verify `logException`) — падают: `changeIcon_when_security_exception_then_logs_to_crashlytics`; `changeIcon_when_name_not_found_then_logs_to_crashlytics`; `changeIcon_when_illegal_argument_then_logs_to_crashlytics`; `changeIcon_when_disable_component_fails_then_logs_to_crashlytics` (приватный disable тестируется через публичный `changeIcon`; «fails» — сбой деактивации, который disableComponent глотает сам)
- [ ] 4.3 Green: в `changeIcon` по одному `logException` в каждый из трёх catch перед `throw e` («Ошибка смены иконки: …»); в существующий общий catch `Exception` внутри `disableComponent` — один `logException` («Ошибка сброса иконки: …», throw-семантика не меняется); узкие catch сброса без отчёта; `make test` зелёный

## 5. Верификация

- [ ] 5.1 `make format && make lint && make test` — зелёные, без новых падений (актуальное число — репорт `scripts/test_report.py`)
- [ ] 5.2 `make android-test` — зелёный (эмулятор API 36; flavor-гейты `Assume RUSTORE_FEATURES` в `MoreScreenTest`, SDK-гейт API 33 в `ReminderAlarmReceiverInstrumentedTest`)
- [ ] 5.3 Финал: release-сборка (`make apk FLAVOR=github`), триггер одной ошибки, проверка non-fatal в Firebase Console (Crashlytics → Errors)
