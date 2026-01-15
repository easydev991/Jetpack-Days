# Релизный процесс

## Создание сборки

Для создания подписанной AAB-сборки выполните:

```bash
make release
```

Команда автоматически:

1. Загружает секреты для подписи из репозитория `android-secrets`
2. Увеличивает номер сборки (`VERSION_CODE`) на 1
3. Создает подписанную AAB-сборку: `dayscounter{VERSION_CODE}.aab` (например, `dayscounter1.aab`, `dayscounter2.aab`)
4. Отображает версию и номер сборки

## Управление версией

### Версия приложения (VERSION_NAME)

Отображается в магазине приложений. Изменяется вручную в `gradle.properties`:

```properties
VERSION_NAME=1.0
```

Примеры версий:

- `1.0` - первый релиз
- `1.1` - минорное обновление с новыми функциями
- `2.0` - мажорное обновление с изменениями в функционале

### Номер сборки (VERSION_CODE)

Автоматически увеличивается при каждом `make release`. Изменения вручную не требуется.

Формат: целое число, монотонно возрастающее.

```
VERSION_CODE=1
```

Пример:

```
VERSION_NAME=1.0, VERSION_CODE=1 → make release → VERSION_CODE=2 → AAB: dayscounter2.aab (1.0 build 2)
VERSION_NAME=1.0, VERSION_CODE=2 → make release → VERSION_CODE=3 → AAB: dayscounter3.aab (1.0 build 3)
VERSION_NAME=1.1, VERSION_CODE=3 → make release → VERSION_CODE=4 → AAB: dayscounter4.aab (1.1 build 4)
```

**Примечание:** Номер сборки (VERSION_CODE) никогда не сбрасывается при повышении версии приложения, поэтому он всегда уникален и монотонно возрастает.

## Публикация

Используйте файл `dayscounter.aab` для публикации в магазине приложений

## Секреты

Секреты для подписи хранятся в отдельном репозитории: `../android-secrets/jetpackdays`

Команда `make release` автоматически загружает их в папку `.secrets/` перед сборкой.
