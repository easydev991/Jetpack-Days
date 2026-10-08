# Running tests — запуск и отчётность

Как запускаются инструментированные тесты JetpackDays и как читать
результаты.

## Запуск

```bash
make android-test    # все androidTest на подключённом устройстве/эмуляторе
```

Команда выполняет:

```makefile
./gradlew connected$(FLAVOR_TITLE)DebugAndroidTest --console=plain
ANDROID_TEST_GRADLE_EXIT_CODE=$$? python3 scripts/android_test_report.py $(FLAVOR)Debug
```

- `connected<Flavor>DebugAndroidTest` — инструментированные тесты debug-сборки
  flavor'а; flavor задаётся `FLAVOR` (дефолт `github` →
  `connectedGithubDebugAndroidTest`)
- Требуется подключённое устройство или запущенный эмулятор.
  Проверка — `adb devices` (или `mobile_list_available_devices` через MCP).
  Без девайса androidTest не запустятся — это ожидаемо, не повторять попытки.

### Масштабы анимаций: авто-защита от дрейфа

`make android-test` **сам** проверяет и чинит масштабы анимаций перед каждым
прогоном (prereq `_ensure_animations_off`; эмулятора нет — no-op) — вызывать
`make emulator-fast` руками больше не обязательно для корректности, только для
скорости первого прогона. `emulator-fast` отключает анимации
(`window_animation_scale`, `transition_animation_scale`,
`animator_duration_scale` → 0; на A/B-замерах 2026-09-26 прогон ~2× быстрее)
со сверкой read-back: запись не прижилась → exit 1.

Зачем защита: масштабы умеют откатываться уже **после** записи — 2026-10-08
`animator_duration_scale` сам вернулся в `null` (= 1.0) через минуты после
`make emulator-fast`, и UI-тесты шли втрое дольше (~2:33 → ~8:40). Причина
сброса — на стороне системы эмулятора; полагаться на память («не забудь
emulator-fast») нельзя — точка проверки = точка использования.

Симптом-диагностика (прямой gradle-вызов мимо защиты): UI-тесты втрое дольше
обычного или `qemu-system-aarch64` под сотнями процентов дольше ~минуты →
`adb shell settings get global animator_duration_scale` (и соседние). Сам по
себе краткий всплеск CPU эмулятора во время UI-теста — норма (рендер +
оркестратор + GPU-эмуляция).

## Один класс

Для быстрой итерации один тест-класс или метод фильтруется через
`ANDROID_TEST_FILTER` (Makefile пробрасывает его в gradle как
`-Pandroid.testInstrumentationRunnerArguments.class=...`). Опция `--tests`
для connected-задач не работает — она только для JVM unit-тестов:

```bash
make android-test ANDROID_TEST_FILTER=com.dayscounter.ui.screens.events.MainScreenSortByTimeOfDayUiTest
make android-test ANDROID_TEST_FILTER=com.dayscounter.data.database.dao.ItemDaoTest#selectAllItems_returnsSortedByTimestamp
```

## Отчёт

`scripts/android_test_report.py` — человекочитаемый отчёт:

- Exit code 0 — все тесты прошли
- Exit code 1 — упавшие тесты или ошибка Gradle
  (`sys.exit(1)` при `failed > 0` или `GRADLE_EXIT_CODE != 0`)

Парсинг XML из `app/build/outputs/androidTest-results/connected/debug/`
(и других возможных путей, включая
`app/build/reports/androidTests/connected/debug/results`).

HTML-отчёт в браузере:

```bash
make android-test-report    # открывает index.html отчёта
```

## Все тесты сразу

```bash
make test-all
```

Результаты:
- Unit: `app/build/test-results/`
- Интеграционные: `app/build/reports/androidTests/connected/<buildType>/flavors/<flavor>/index.html`
  (AGP 9 с flavors; для дефолтного `make android-test` с `FLAVOR=github` —
  `connected/debug/flavors/github/index.html`)

## screenshot-tests модуль

Отдельный Gradle-модуль `screenshot-tests/` — генерация скриншотов
для магазина приложений (fastlane), не обычные тесты:

- `targetProjectPath = ":app"` — зависит от app-модуля
- Кастомный раннер `ScreenshotTestRunner` (`AndroidJUnitRunner`)
- `createAndroidComposeRule<MainActivity>()` + `LocaleTestRule`
  (переключение локалей)
- Скриншоты через `Screengrab` с `UiAutomatorScreenshotStrategy`
- Демо-данные вставляются через `database.itemDao().insertItem()` напрямую
- Результат: `fastlane/metadata/android` (см. `make screenshots`)

## Зависимости androidTest (build.gradle.kts)

```kotlin
androidTestImplementation(libs.androidx.junit)            // androidx.test:junit
androidTestImplementation(libs.turbine)                   // Turbine 1.2.1
androidTestImplementation(platform(libs.androidx.compose.bom))
androidTestImplementation(libs.androidx.compose.ui.test.junit4)
debugImplementation(libs.androidx.compose.ui.test.manifest)
```

`testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"`
