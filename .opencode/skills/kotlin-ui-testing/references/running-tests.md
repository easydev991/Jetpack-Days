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
- Требуется подключённое устройство или запущенный эмулятор

### Ускорение прогонов: `make emulator-fast`

После каждого старта эмулятора вызывайте `make emulator-fast` — отключает
анимации (`window_animation_scale`, `transition_animation_scale`,
`animator_duration_scale` → 0). На A/B-замерах 2026-09-26 `make android-test`
стал в ~2× быстрее (≈63 с → ≈28 с на 108 тестах); откат — те же
`adb shell settings put … 1`. Настройка не переживает перезапуск
эмулятора — это и есть причина отдельной цели, а не вставки в
`android-test`.

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
