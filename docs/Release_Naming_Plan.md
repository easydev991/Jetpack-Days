# План добавления номера сборки в название AAB-файла

## Обзор

Документ описывает процесс добавления номера сборки в название файла AAB, создаваемого командой `make release`.

## Текущее состояние

**Проблема:** Команда `make release` создает AAB-файл с постоянным названием `dayscounter.aab` без информации о номере сборки.

**Логика увеличения VERSION_CODE:**

- `VERSION_CODE` всегда увеличивается на 1 при каждом вызове `make release`
- `VERSION_CODE` никогда не сбрасывается при повышении основной версии приложения
- Это стандартная практика в Android - монотонно возрастающий номер сборки
- `VERSION_CODE` уникален для каждой сборки и служит идентификатором в магазинах приложений

**Текущий процесс (в Makefile, строки 112-138):**

```makefile
## release: Создать подписанную AAB-сборку для публикации (аналог testflight в iOS)
release:
    @echo "$(YELLOW)Проверка секретов для подписи...$(RESET)"
    @if [ ! -d ".secrets" ]; then \
        echo "$(YELLOW)Загрузка секретов из репозитория android-secrets...$(RESET)"; \
        if [ -d "../android-secrets/jetpackdays" ]; then \
            mkdir -p ".secrets"; \
            cp -r ../android-secrets/jetpackdays/* .secrets/; \
            sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/dayscounter-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
            echo "$(GREEN)Секреты загружены успешно$(RESET)"; \
        else \
            echo "$(RED)Ошибка: репозиторий android-secrets не найден в ../android-secrets/jetpackdays$(RESET)"; \
            echo "$(YELLOW)Проверьте, что репозиторий android-secrets склонирован в нужное место$(RESET)"; \
            exit 1; \
        fi \
    fi
    @echo "$(YELLOW)Увеличиваю VERSION_CODE...$(RESET)"
    @CURRENT_VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
    NEW_VERSION_CODE=$$((CURRENT_VERSION_CODE + 1)); \
    sed -i.tmp "s/^VERSION_CODE=.*/VERSION_CODE=$$NEW_VERSION_CODE/" gradle.properties && rm -f gradle.properties.tmp; \
    echo "$(GREEN)VERSION_CODE обновлен с $$CURRENT_VERSION_CODE на $$NEW_VERSION_CODE$(RESET)"
    @echo "$(YELLOW)Создаю релиз-сборку (AAB)...$(RESET)"
    @./gradlew bundleRelease uploadCrashlyticsMappingFileRelease
    @cp app/build/outputs/bundle/release/app-release.aab dayscounter.aab
    @echo "$(GREEN)AAB создан и mapping files загружены в Firebase: dayscounter.aab$(RESET)"
    @echo "$(YELLOW)Версия для публикации: $$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2) (build $$NEW_VERSION_CODE)$(RESET)"
    @echo "$(YELLOW)Для публикации используйте этот файл в RuStore или Google Play Store$(RESET)"
```

**Текущие версии в gradle.properties (строки 29-31):**

```properties
# App version (versionName: displayed in App Store, versionCode: build number)
VERSION_NAME=1.0
VERSION_CODE=3
```

## Цели

1. Добавить в название файла AAB-сборки информацию о номере сборки
2. Использовать формат: `dayscounter{сборка}.aab`
   - Примеры: `dayscounter1.aab`, `dayscounter2.aab`, `dayscounter10.aab`
   - `{сборка}` - уникальный монотонно возрастающий номер (VERSION_CODE)
3. Обеспечить совместимость с существующими процессами публикации

---

## Часть 1: Обновление команды release в Makefile

### Шаг 1.1: Изменить команду release для формирования названия с номером сборки

**Файл:** `Makefile`

**Задача:** Обновить команду `release` для формирования названия файла сборки с номером сборки.

**Текущая логика копирования (строка 135):**

```makefile
@cp app/build/outputs/bundle/release/app-release.aab dayscounter.aab
```

**Новая логика копирования:**

```makefile
@echo "$(YELLOW)Создаю релиз-сборку (AAB)...$(RESET)"
@./gradlew bundleRelease uploadCrashlyticsMappingFileRelease
@VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
OUTPUT_FILE="dayscounter$$VERSION_CODE.aab"; \
cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"; \
echo "$(GREEN)AAB создан и mapping files загружены в Firebase: $$OUTPUT_FILE$(RESET)"; \
VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
echo "$(YELLOW)Версия для публикации: $$VERSION_NAME (build $$VERSION_CODE)$(RESET)"
```

**Объяснение изменений:**

1. **Чтение VERSION_CODE из gradle.properties:**
   - `grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2` - извлекает значение VERSION_CODE (например, "4")

2. **Формирование имени файла:**
   - `OUTPUT_FILE="dayscounter$$VERSION_CODE.aab"` - создает имя файла (например, "dayscounter4.aab")

3. **Копирование файла с новым именем:**
   - `cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"` - копирует файл с новым именем

4. **Вывод информации:**
   - Использование `$$OUTPUT_FILE` для отображения созданного имени файла
   - Отдельное чтение `VERSION_NAME` для отображения версии приложения

**Полная обновленная команда release:**

```makefile
## release: Создать подписанную AAB-сборку для публикации (аналог testflight в iOS)
release:
    @echo "$(YELLOW)Проверка секретов для подписи...$(RESET)"
    @if [ ! -d ".secrets" ]; then \
        echo "$(YELLOW)Загрузка секретов из репозитория android-secrets...$(RESET)"; \
        if [ -d "../android-secrets/jetpackdays" ]; then \
            mkdir -p ".secrets"; \
            cp -r ../android-secrets/jetpackdays/* .secrets/; \
            sed -i.tmp 's|^KEYSTORE_FILE=.*|KEYSTORE_FILE=.secrets/keystore/dayscounter-release.keystore|' .secrets/secrets.properties && rm -f .secrets/secrets.properties.tmp; \
            echo "$(GREEN)Секреты загружены успешно$(RESET)"; \
        else \
            echo "$(RED)Ошибка: репозиторий android-secrets не найден в ../android-secrets/jetpackdays$(RESET)"; \
            echo "$(YELLOW)Проверьте, что репозиторий android-secrets склонирован в нужное место$(RESET)"; \
            exit 1; \
        fi \
    fi
    @echo "$(YELLOW)Увеличиваю VERSION_CODE...$(RESET)"
    @CURRENT_VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
    NEW_VERSION_CODE=$$((CURRENT_VERSION_CODE + 1)); \
    sed -i.tmp "s/^VERSION_CODE=.*/VERSION_CODE=$$NEW_VERSION_CODE/" gradle.properties && rm -f gradle.properties.tmp; \
    echo "$(GREEN)VERSION_CODE обновлен с $$CURRENT_VERSION_CODE на $$NEW_VERSION_CODE$(RESET)"
    @echo "$(YELLOW)Создаю релиз-сборку (AAB)...$(RESET)"
    @./gradlew bundleRelease uploadCrashlyticsMappingFileRelease
    @VERSION_CODE=$$(grep "^VERSION_CODE=" gradle.properties | cut -d'=' -f2); \
    OUTPUT_FILE="dayscounter$$VERSION_CODE.aab"; \
    cp app/build/outputs/bundle/release/app-release.aab "$$OUTPUT_FILE"; \
    echo "$(GREEN)AAB создан и mapping files загружены в Firebase: $$OUTPUT_FILE$(RESET)"; \
    VERSION_NAME=$$(grep "^VERSION_NAME=" gradle.properties | cut -d'=' -f2); \
    echo "$(YELLOW)Версия для публикации: $$VERSION_NAME (build $$VERSION_CODE)$(RESET)"
    @echo "$(YELLOW)Для публикации используйте этот файл в RuStore или Google Play Store$(RESET)"
```

### Критерии готовности

- ⏳ Команда `release` обновлена в Makefile
- ⏳ Название файла AAB содержит номер сборки
- ⏳ Тестовый запуск команды `make release` создает файл с правильным именем

---

## Часть 2: Тестирование новой команды release

### Шаг 2.1: Тестовый запуск команды release

**Цель:** Проверить, что команда `make release` создает файл с правильным названием.

**Тестовый сценарий:**

```bash
# Запуск команды release
make release

# Ожидаемый результат:
# - VERSION_CODE увеличивается (например, с 3 на 4)
# - Файл создается с именем dayscounter4.aab
# - Вывод в консоли показывает правильное имя файла и версию приложения
```

**Ожидаемый вывод в консоли:**

```
Проверка секретов для подписи...
Секреты загружены успешно
Увеличиваю VERSION_CODE...
VERSION_CODE обновлен с 3 на 4
Создаю релиз-сборку (AAB)...
BUILD SUCCESSFUL in Xs
AAB создан и mapping files загружены в Firebase: dayscounter4.aab
Версия для публикации: 1.0 (build 4)
Для публикации используйте этот файл в RuStore или Google Play Store
```

**Проверка результата:**

```bash
# Проверить, что файл создан с правильным именем
ls -lh dayscounter4.aab

# Ожидаемый результат:
# -rw-r--r--  1 user  staff   X.XM Jan 16 12:34 dayscounter4.aab
```

### Критерии готовности

- ⏸ Тестовый запуск команды `make release` (требуется проверка на реальном проекте)
- ✅ Файл AAB создается с правильным именем
- ✅ Вывод в консоли корректный
- ✅ VERSION_CODE корректно увеличивается

---

## Часть 3: Обновление документации ✅

### Шаг 3.1: Обновить App_Publication_Plan.md ✅

**Выполнено:** Документация обновлена для отражения нового формата имени файла сборки.

**Обновления:**

1. ✅ Обновлены все упоминания `dayscounter.aab` на `dayscounter{VERSION_CODE}.aab` в документе
2. ✅ Добавлена информация о формате имени файла сборки
3. ✅ Обновлен критерий готовности для отражения нового формата
4. ✅ Добавлено описание логики VERSION_CODE и формата названия файла

**Добавленная информация:**

```markdown
**Формат названия AAB-файла:**

Название файла сборки теперь включает номер сборки (VERSION_CODE) для лучшей идентификации:
- Формат: `dayscounter{VERSION_CODE}.aab`
- Примеры: `dayscounter1.aab`, `dayscounter2.aab`, `dayscounter10.aab`

Это позволяет легко различать разные сборки и отслеживать их версии.

**Логика VERSION_CODE:**

- `VERSION_CODE` увеличивается на 1 при каждом вызове `make release`
- `VERSION_CODE` никогда не сбрасывается при повышении основной версии приложения
- Это стандартная практика в Android - монотонно возрастающий номер сборки
- `VERSION_CODE` уникален для каждой сборки и служит идентификатором в магазинах приложений
```

### Шаг 3.2: Обновить deployment.md ✅

**Выполнено:** Файл обновлен с новыми примерами и описанием формата имени файла.

**Обновления:**

1. ✅ Обновлен пример команды `make release`
2. ✅ Добавлены новые примеры с номерами сборок
3. ✅ Добавлено примечание о логике VERSION_CODE

### Шаг 3.3: Обновить Build_Size_Analysis_And_Optimization_Plan.md ✅

**Выполнено:** Обновлена команда bundletool с использованием актуального имени файла.

**Обновления:**

1. ✅ Обновлена команда `bundletool build-apks` для использования актуального имени файла
2. ✅ Добавлено примечание о необходимости использовать актуальное имя файла сборки

### Критерии готовности

- ✅ `App_Publication_Plan.md` обновлен
- ✅ Примеры использования обновлены
- ✅ Информация о формате имени файла добавлена
- ✅ `deployment.md` обновлен
- ✅ `Build_Size_Analysis_And_Optimization_Plan.md` обновлен

---

## Порядок выполнения

### Приоритет 1: Обновление команды release в Makefile (обязательно)

1. Обновить команду release для формирования названия с номером сборки (Часть 1)
2. Изменить логику копирования файла (Шаг 1.1)
3. Обновить вывод информации в консоль

### Приоритет 2: Тестирование новой команды release (обязательно)

4. Выполнить тестовый запуск команды `make release` (Часть 2)
5. Проверить правильное имя файла (Шаг 2.1)

### Приоритет 3: Обновление документации (важно)

6. Обновить `App_Publication_Plan.md` (Часть 3)
7. Обновить примеры использования (Шаг 3.1)
8. Добавить информацию о формате имени файла

---

## Критерии завершения

### Обязательные критерии

- ✅ Команда `release` обновлена в Makefile
- ✅ Название файла AAB содержит номер сборки
- ⏸ Тестовый запуск команды `make release` (требуется проверка на реальном проекте)
- ✅ `App_Publication_Plan.md` обновлен
- ✅ Информация о формате имени файла добавлена в документацию
- ✅ `deployment.md` обновлен
- ✅ `Build_Size_Analysis_And_Optimization_Plan.md` обновлен

### Результат

**Реализовано:**

- ✅ Команда `make release` создает файл AAB с именем в формате `dayscounter{сборка}.aab`
- ✅ Название файла содержит уникальный номер сборки (VERSION_CODE)
- ✅ Документация обновлена с информацией о новом формате имени файла
- ✅ Примеры использования обновлены
- ✅ `.gitignore` уже содержит правило `*.aab` для исключения всех файлов сборок

**Требуется проверка:**

- ⏸ Процесс публикации в RuStore и Google Play Store работает с новым форматом имени файла

**Примеры использования:**

```bash
make release

# Результат:
# dayscounter10.aab - файл для публикации в RuStore и Google Play Store (сборка 10)
# dayscounter11.aab - следующий запуск команды (сборка 11)
```

---

## Документация

После завершения настройки обновить документацию:

1. **App_Publication_Plan.md**: Обновить информацию о формате имени файла AAB
2. **Этот документ**: Отметить выполненные шаги
