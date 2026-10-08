# Правила запуска тестов в JetpackDays

## Один прогон → один отчёт

`make test` / `make android-test` уже запускают `scripts/test_report.py` /
`scripts/android_test_report.py` и печатают статистику и **список каждого
упавшего** `ClassName::testName`. Ничего дополнительно гонять или парсить
не нужно.

**Запрещено** повторно запускать `make test` / `make android-test` ради
чтения вывода (`| tail`, `| grep 'BUILD'`, `find` по XML). Прогон — 30
секунд (JVM) или минуты (androidTest: установка APK + эмулятор); 4 прогона
= минуты впустую. Если тесты упали: читать фейлы из вывода скрипта или
HTML-отчёта `app/build/reports/tests/.../index.html`, после фикса
перечитывать отчёт скриптом, не новым `make`.

## Flavors: только по одному

`make android-test FLAVOR=rustore && make android-test FLAVOR=github` в
одной команде нельзя: один прогон — один флейвор, следующий — только
отдельной командой после полного завершения предыдущей.

## Команды

```bash
make test                                    # JVM (JUnit 5) — XML в app/build/test-results/
make android-test                            # androidTest (JUnit 4) — XML в app/build/outputs/androidTest-results/
make android-test ANDROID_TEST_FILTER=<FQN>  # один класс/метод (Class#method) — итерация по экрану
make android-test-report                     # открыть HTML-отчёт androidTest в браузере
make emulator-fast                           # после старта эмулятора — выключает анимации; android-test чинит их сам перед каждым прогоном

# Перечитать отчёт БЕЗ нового прогона (make clean стирает XML):
python3 scripts/test_report.py                       # JVM
python3 scripts/android_test_report.py GithubDebug   # androidTest (аргумент — $(FLAVOR_TITLE)Debug)
```

Полный `make android-test` — перед коммитом задачи и перед релизом.
Отладочный запуск без отчёта — `./gradlew testGithubDebugUnitTest --tests
"..." --info` (голый `test` агрегатор, `--tests` не принимает), но
результат всё равно проверять через `test_report.py` по XML на диске, не
через повторный `make`.

**Gotcha AGP 9:** имена gradle-задач flavor-aware — `testDebugUnitTest` /
`compileDebugUnitTestKotlin` НЕ существуют (candidates:
`testGithubDebugUnitTest`, `compileGithubDebugUnitTestKotlin`, ...).
Работают корневые `test` / `lint` / `ktlintCheck` / `app:detekt`.

## Эмулятор для androidTest

Без подключённого эмулятора/устройства androidTest не запустятся — это
ожидаемо, не повторять попытки. Проверка — `adb devices` (или
`mobile_list_available_devices` через MCP).

## Полные руководства

- `kotlin-testing/SKILL.md` — JVM unit-тесты; `kotlin-testing/references/running-tests.md` — команды и фильтрация.
- `kotlin-ui-testing/SKILL.md` — androidTest, Compose Testing, Room in-memory.
