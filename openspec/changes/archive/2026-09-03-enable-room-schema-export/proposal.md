## Why

Room в проекте настроен с `exportSchema = false` (DaysDatabase.kt): история схем БД не сохраняется, единственная миграция `MIGRATION_1_2` — рукописный SQL, который не проверяется ничем — ни компилятором, ни тестом. Опечатка в SQL миграции (имя колонки, индекс, foreign key) обнаружится только крэшем у пользователя при обновлении приложения. Для офлайн-приложения, где данные пользователей хранятся локально, это недопустимый риск. Ревью кода пометило это как технический долг, который нужно закрыть.

## What Changes

- Включить `exportSchema = true` в `@Database` аннотации `DaysDatabase`
- Добавить KSP-аргумент `room.schemaLocation` в `app/build.gradle.kts` — схемы будут экспортироваться в `app/schemas/` и коммититься в git
- Закоммитить сгенерированную схему версии 2 (`app/schemas/com.dayscounter.data.database.DaysDatabase/2.json`)
- Реконструировать схему версии 1 (`1.json`) из `2.json` — на случай устройств, ещё не обновившихся с v1: без неё MigrationTest не сможет создать БД версии 1
- Подключить зависимость `androidx.room:room-testing` для androidTest
- Добавить MigrationTest (androidTest, `MigrationTestHelper`), проверяющий миграцию 1→2: создаёт БД по схеме v1, прогоняет `MIGRATION_1_2`, валидирует результат против схемы v2 и проверяет сохранность данных пользователей (items переживают миграцию)
- Зафиксировать правило на будущее: каждая новая миграция идёт в паре с MigrationTest; для простых изменений схемы предпочтителен `@AutoMigration`

## Capabilities

### New Capabilities

(нет — изменение инструментальное, поведение приложения не меняется; в `.openspec.yaml` установлен `skip_specs: true`)

### Modified Capabilities

(нет — спеки описывают поведение приложения, а экспорт схем и миграционные тесты относятся к инфраструктуре сборки и тестирования)

## Impact

- `app/src/main/java/com/dayscounter/data/database/DaysDatabase.kt` — `exportSchema = true`
- `app/build.gradle.kts` — KSP-аргумент `room.schemaLocation` (top-level `ksp { }`), `androidTestImplementation` для `androidx.room:room-testing`, и в `android { }` блок `sourceSets { getByName("androidTest").assets.srcDir("$projectDir/schemas") }` (обязательно при KSP-only конфигурации — иначе `MigrationTestHelper` не найдёт схемы в androidTest assets)
- `gradle/libs.versions.toml` — новый alias `androidx-room-testing` (version.ref = room)
- `app/schemas/com.dayscounter.data.database.DaysDatabase/1.json` — реконструированная схема v1 (новый файл, коммитится)
- `app/schemas/com.dayscounter.data.database.DaysDatabase/2.json` — сгенерированная схема v2 (новый файл, коммитится)
- `app/src/androidTest/java/com/dayscounter/data/database/MigrationTest.kt` — новый тест миграции 1→2
- `app/src/test/java/com/dayscounter/data/database/DaysDatabaseMigrationTest.kt` — удаление мок-юнита (полностью перекрыт интеграционным `MigrationTest`)
- Сборка: появляется задача KSP по экспорту схемы
- Рисков для прод-поведения нет: изменения затрагивают только аннотацию (флаг экспорта), build-конфиг и androidTest-исходники
