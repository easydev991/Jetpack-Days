## 1. Включение экспорта схем

- [x] 1.1 В `app/build.gradle.kts` создать top-level блок `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` (сейчас блока расширения нет — только `ksp(libs...)` в dependencies; вкладывать в `android { }` нельзя, это top-level extension KSP-плагина); проверить: сборка проходит
- [x] 1.2 В `DaysDatabase.kt` заменить `exportSchema = false` на `exportSchema = true`; собрать `./gradlew :app:assembleDebug` и убедиться, что появился файл `app/schemas/com.dayscounter.data.database.DaysDatabase/2.json` (Room KSP-экспорт детерминирован по построению — повторная сборка без изменения исходников даёт идентичный json, отдельная проверка не нужна)
- [x] 1.3 Закоммитить `2.json` в git

## 2. Реконструкция схемы v1

- [x] 2.1 Создать `app/schemas/com.dayscounter.data.database.DaysDatabase/1.json` из `2.json`: удалить entity `reminders` (вместе с его index), поставить `"version": 1`, identityHash оставить без изменений (см. design.md, Risks)
- [x] 2.2 Проверить ревью-условие: entity `items` в `1.json` побайтово равен `items` в `2.json`; отличаются только `version` и отсутствие `reminders`; закоммитить `1.json`

## 3. Миграционный тест

- [x] 3.1 В `gradle/libs.versions.toml` добавить alias `androidx-room-testing` (group `androidx.room`, name `room-testing`, version.ref = `room`) и в `app/build.gradle.kts` подключить `androidTestImplementation(libs.androidx.room.testing)`; проверить: `./gradlew :app:dependencies --configuration githubDebugAndroidTestRuntimeClasspath | grep room-testing` находит alias и `./gradlew :app:assembleDebugAndroidTest` проходит
- [x] 3.2 В `app/build.gradle.kts` в блок `android { }` добавить `sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }` (обязательно при KSP-only конфигурации: без Room Gradle Plugin схемы не попадают в androidTest assets автоматически, иначе `MigrationTestHelper` упадёт в рантайме с `Cannot find the schema file`); проверить: `unzip -l app/build/outputs/apk/androidTest/*/*/app-*-androidTest.apk | grep "com.dayscounter.data.database.DaysDatabase/2.json"` показывает файл в собранном APK (glob `*/*/` покрывает оба flavor — `rustore/` и `github/` — без привязки к значению `FLAVOR` в `make android-test`; `srcDir` мержит содержимое `$projectDir/schemas` в корень assets, поэтому префикса `schemas/` в архиве нет)
- [x] 3.3 Удалить `app/src/test/java/com/dayscounter/data/database/DaysDatabaseMigrationTest.kt` — мок-юнит на `verify { database.execSQL(...) }` с `relaxed = true` не ловит даже синтаксические ошибки SQL и полностью перекрывается интеграционным `MigrationTest` (tasks.md 3.4); по новой конвенции «миграция в паре с MigrationTest» (см. design.md, Решение 4) старый тест — мёртвый груз
- [x] 3.4 Создать `app/src/androidTest/java/com/dayscounter/data/database/MigrationTest.kt` (JUnit 4, `MigrationTestHelper` от `DaysDatabase::class.java`) по контракту из design.md (решение 3): createDatabase v1 → raw-SQL вставка в `items` → `runMigrationsAndValidate(2, MIGRATION_1_2)` → проверка сохранности данных → запись в `reminders`
- [x] 3.5 Прогнать `make android-test` и убедиться, что MigrationTest зелёный и существующие androidTest не сломались

## 4. Верификация и конвенция

- [x] 4.1 Прогнать `make lint`; убедиться, что ktlint/detekt чистые по изменённым файлам
- [x] 4.2 Проверить отсутствие `!!` и наличие KDoc в новом тесте; логи/сообщения — на русском (конвенции проекта)
- [x] 4.3 Добавить в `.opencode/rules/tdd.md` конвенцию: каждая миграция идёт в паре с MigrationTest и новым `N.json`; для простых изменений схемы — `@AutoMigration`; мок-юниты на SQL миграций (`verify { database.execSQL(...) }` с `relaxed = true`) запрещены — не ловят синтаксические ошибки и создают ложное покрытие
